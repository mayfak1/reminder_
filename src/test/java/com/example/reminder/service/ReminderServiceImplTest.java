package com.example.reminder.service;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.user.User;
import com.example.reminder.dto.pagination.PageResponse;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import com.example.reminder.dto.reminder.ReminderResponse;
import com.example.reminder.exceptions.ForbiddenException;
import com.example.reminder.exceptions.NotFoundException;
import com.example.reminder.mapper.ReminderCreateAndUpdateMapper;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import com.example.reminder.service.reminder.ReminderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceImplTest {

    @Mock
    ReminderRepository reminderRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    ReminderCreateAndUpdateMapper reminderMapper;

    @InjectMocks
    ReminderServiceImpl reminderService;

    @Test
    void createShouldSaveReminderForUser() {
        Long userId = 1L;
        User user = user(userId);
        ReminderCreateAndUpdateRequest request = new ReminderCreateAndUpdateRequest(
                "Pay rent", "monthly", Instant.parse("2026-03-16T10:00:00Z"));
        Reminder mapped = new Reminder();
        mapped.setTitle(request.title());
        mapped.setDescription(request.description());
        mapped.setRemind(request.remind());
        Reminder saved = new Reminder();
        saved.setId(10L);
        saved.setUser(user);
        saved.setTitle(request.title());
        saved.setDescription(request.description());
        saved.setRemind(request.remind());
        ReminderResponse response = new ReminderResponse(10L, "Pay rent", "monthly", request.remind());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(reminderMapper.toEntity(request)).thenReturn(mapped);
        when(reminderRepository.save(mapped)).thenReturn(saved);
        when(reminderMapper.toResponse(saved)).thenReturn(response);

        ReminderResponse result = reminderService.create(userId, request);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(mapped.getUser()).isSameAs(user);
        verify(reminderRepository).save(mapped);
    }

    @Test
    void createShouldThrowWhenUserNotFound() {
        ReminderCreateAndUpdateRequest request = new ReminderCreateAndUpdateRequest(
                "Pay rent", "monthly", Instant.parse("2026-03-16T10:00:00Z"));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.create(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void updateShouldUpdateAllowedFieldsForOwnedReminder() {
        Long userId = 1L;
        Long reminderId = 7L;
        Reminder reminder = reminder(reminderId, user(userId), "Old", "old", Instant.parse("2026-03-16T10:00:00Z"));
        ReminderCreateAndUpdateRequest update = new ReminderCreateAndUpdateRequest(
                "New title", "new desc", Instant.parse("2026-03-20T10:00:00Z"));
        ReminderResponse response = new ReminderResponse(reminderId, "New title", "new desc", update.remind());

        when(reminderRepository.findByIdAndUserId(reminderId, userId)).thenReturn(Optional.of(reminder));
        when(reminderRepository.save(reminder)).thenReturn(reminder);
        when(reminderMapper.toResponse(reminder)).thenReturn(response);

        ReminderResponse result = reminderService.update(userId, reminderId, update);

        assertThat(result.title()).isEqualTo("New title");
        assertThat(reminder.getTitle()).isEqualTo("New title");
        assertThat(reminder.getDescription()).isEqualTo("new desc");
        assertThat(reminder.getRemind()).isEqualTo(update.remind());
        verify(reminderRepository).save(reminder);
    }

    @Test
    void updateShouldThrowForbiddenWhenRepositoryReturnsForeignReminder() {
        Long userId = 1L;
        Long reminderId = 7L;
        User owner = user(2L);
        Reminder reminder = reminder(reminderId, owner, "Old", "old", Instant.parse("2026-03-16T10:00:00Z"));
        ReminderCreateAndUpdateRequest update = new ReminderCreateAndUpdateRequest(
                "New title", "new desc", Instant.parse("2026-03-20T10:00:00Z"));

        when(reminderRepository.findByIdAndUserId(reminderId, userId)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.update(userId, reminderId, update))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Access denied");
    }

    @Test
    void deleteShouldDeleteOwnedReminder() {
        Long userId = 1L;
        Long reminderId = 11L;
        Reminder reminder = reminder(reminderId, user(userId), "Pay", "rent", Instant.now());
        when(reminderRepository.findByIdAndUserId(reminderId, userId)).thenReturn(Optional.of(reminder));

        reminderService.delete(userId, reminderId);

        verify(reminderRepository).delete(reminder);
    }

    @Test
    void deleteShouldThrowWhenNotFound() {
        when(reminderRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.delete(1L, 11L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("reminder not found");
    }

    @Test
    void getAllShouldBuildPageableAndReturnMappedResponse() {
        Reminder r1 = reminder(1L, user(1L), "Milk", "buy milk", Instant.parse("2026-03-16T10:00:00Z"));
        Reminder r2 = reminder(2L, user(1L), "Rent", "pay", Instant.parse("2026-03-16T11:00:00Z"));
        Pageable incomingPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "title"));
        Page<Reminder> page = new PageImpl<>(List.of(r1, r2), incomingPageable, 12);
        ReminderResponse rr1 = new ReminderResponse(1L, "Milk", "buy milk", r1.getRemind());
        ReminderResponse rr2 = new ReminderResponse(2L, "Rent", "pay", r2.getRemind());

        when(reminderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(reminderMapper.toResponse(r1)).thenReturn(rr1);
        when(reminderMapper.toResponse(r2)).thenReturn(rr2);

        PageResponse<ReminderResponse> result = reminderService.getAll(
                1L, "abc", LocalDate.of(2026, 3, 2), 1, 5, "title", "desc");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<Specification<Reminder>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(reminderRepository).findAll(specCaptor.capture(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(specCaptor.getValue()).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("title")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.DESC);

        assertThat(result.total()).isEqualTo(12);
        assertThat(result.pages()).isEqualTo(3);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).id()).isEqualTo(1L);
        assertThat(result.items().get(1).id()).isEqualTo(2L);
    }

    @Test
    void getAllShouldApplySafeDefaultsForPageSizeDirectionAndSort() {
        when(reminderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> Page.empty(inv.getArgument(1)));

        reminderService.getAll(7L, null, null, -3, 0, "unexpected", "invalid");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reminderRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("remind")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("remind").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setOauth2Subject("subject-" + id);
        return user;
    }

    private Reminder reminder(Long id, User user, String title, String description, Instant remindAt) {
        Reminder reminder = new Reminder();
        reminder.setId(id);
        reminder.setUser(user);
        reminder.setTitle(title);
        reminder.setDescription(description);
        reminder.setRemind(remindAt);
        return reminder;
    }
}

