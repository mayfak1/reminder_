package com.example.reminder.service.reminder;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.domain.reminder.ReminderSpecifications;
import com.example.reminder.domain.user.User;
import com.example.reminder.dto.pagination.PageResponse;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import com.example.reminder.dto.reminder.ReminderResponse;
import com.example.reminder.exceptions.ForbiddenException;
import com.example.reminder.exceptions.NotFoundException;
import com.example.reminder.mapper.ReminderCreateAndUpdateMapper;
import com.example.reminder.repository.ReminderRepository;
import com.example.reminder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService{
    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final ReminderCreateAndUpdateMapper reminderMapper;

    @Override
    public ReminderResponse create(Long userId, ReminderCreateAndUpdateRequest request) {
        log.info("Reminder:create started userId{}",userId);
        User user=userRepository.findById(userId)
                .orElseThrow(()-> {
                    log.error("Reminder:create user not found userId={}",userId);
                    return new NotFoundException("User not found");
                });
        Reminder reminder=reminderMapper.toEntity(request);
        reminder.setUser(user);
        Reminder saved=reminderRepository.save(reminder);
        log.info("Reminder:create success  userId={} reminderId={} remindAt={}",
                userId,saved.getId(),saved.getRemind());
        return reminderMapper.toResponse(saved);

    }

    @Override
    @Transactional
    public ReminderResponse update(Long userId, Long reminderId, ReminderCreateAndUpdateRequest update) {
        log.info("Reminder:update started userId={} reminderId={}", userId, reminderId);
        Reminder reminder=findOwnedReminder(userId,reminderId,"update");
        equlsUserId(reminder,userId,"update");

        reminder.updateEntity(update,reminder);

        Reminder saved=reminderRepository.save(reminder);
        log.info("Reminder:update success userId={} reminderId={} remindAt={}",
                userId, saved.getId(), saved.getRemind());
        return reminderMapper.toResponse(reminder);
    }



    @Override
    public void delete(Long userId, Long reminderId) {
        log.info("Reminder:delete started userId={} reminderId={}", userId, reminderId);
        Reminder reminder=findOwnedReminder(userId,reminderId,"delete");
        equlsUserId(reminder,userId,"delete");
        reminderRepository.delete(reminder);
        log.info("Reminder:delete success userId={} reminderId={}", userId, reminderId);

    }

    @Override
    public ReminderResponse getById(Long userId, Long reminderId) {
        log.debug("Reminder:getById userId={} reminderId={}", userId, reminderId);

        Reminder reminder=findOwnedReminder(userId,reminderId,"getById");
        equlsUserId(reminder,userId,"getById");

        return reminderMapper.toResponse(reminder);
    }

    private void equlsUserId(Reminder reminder, Long userId, String where) {
        if(!reminder.getUser().getId().equals(userId)){
            log.warn("Reminder:{} forbidden userId={} reminderId={} ownerId={}",
                    where,userId, reminder.getId(), reminder.getUser().getId());
            throw new ForbiddenException("Access denied");
        }
    }

    @Override
    public PageResponse<ReminderResponse> getAll(
            Long userId,
            String q,
            LocalDate date,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        log.debug("Reminder:getAll userId={} q='{}' date={} page={} size={} sortBy={} dir={}",
                userId, safe(q), date, page, size, sortBy, direction);

        Sort sort = Sort.by(parseDirection(direction), resolveSortProperty(sortBy));
        Pageable pageable = PageRequest.of(safePage(page), safeSize(size), sort);

        Specification<Reminder> spec = Specification
                .where(ReminderSpecifications.belongsToUser(userId))
                .and(ReminderSpecifications.search(q))
                .and(ReminderSpecifications.filterByDate(date));

        Page<Reminder> result = reminderRepository.findAll(spec, pageable);

        log.debug("Reminder:getAll result userId={} total={} pages={} returned={}",
                userId, result.getTotalElements(), result.getTotalPages(), result.getNumberOfElements());

        return PageResponse.from(result.map(reminderMapper::toResponse));
    }

    private Sort.Direction parseDirection(String direction) {
        return "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

    private int safePage(int page) {
        return Math.max(page, 0);
    }

    private int safeSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 100);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String resolveSortProperty(String sortBy) {
        if(sortBy==null||sortBy.isBlank())return "remind";
        return switch (sortBy.toLowerCase()){
            case "name","title"->"title";
            case "date","time","remind"->"remind";
            default -> "remind";
        };
    }

    private  Reminder findOwnedReminder(Long userId, Long reminderId,String s) {
        return reminderRepository.findByIdAndUserId( reminderId,userId)
                .orElseThrow(()-> {
                    log.warn("Reminder:{} not found userId={} reminderId={}",s, userId, reminderId);
                    return new NotFoundException("reminder not found");
                });

    }
}
