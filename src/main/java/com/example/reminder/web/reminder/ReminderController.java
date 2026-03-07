package com.example.reminder.web.reminder;

import com.example.reminder.dto.pagination.PageResponse;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import com.example.reminder.dto.reminder.ReminderResponse;
import com.example.reminder.service.reminder.ReminderService;
import com.example.reminder.service.user.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reminder")
@RequiredArgsConstructor
@Slf4j
public class ReminderController {
    private final CurrentUserService currentUserService;
    private final ReminderService reminderService;

    @GetMapping
    public PageResponse<ReminderResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Long userId = currentUserService.getOrCreateUserId(jwt);
        log.debug("Api: reminder list userId={} q='{}' date={} page={} size={} sortBy={} direction={}",
                userId, q == null ? "" : q, date, page, size, sortBy, direction);
        PageResponse<ReminderResponse> response = reminderService.getAll(userId, q, date, page, size, sortBy, direction);
        log.debug("Api: reminder list success userId={} total={} returned={}",
                userId, response.total(), response.items().size());
        return response;
    }

    @GetMapping("/{id}")
    public ReminderResponse getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ){
        Long userId = currentUserService.getOrCreateUserId(jwt);
        log.debug("Api: reminder getById userId={} reminderId={}", userId, id);
        ReminderResponse response = reminderService.getById(userId, id);
        log.debug("Api: reminder getById success userId={} reminderId={}", userId, id);
        return response;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReminderResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReminderCreateAndUpdateRequest request
            ){
        Long userId = currentUserService.getOrCreateUserId(jwt);
        log.info("Api: reminder create userId={} remindAt={}", userId, request.remind());
        ReminderResponse response = reminderService.create(userId, request);
        log.info("Api: reminder create success userId={} reminderId={}", userId, response.id());
        return response;
    }

    @PutMapping("/{id}")
    public ReminderResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody ReminderCreateAndUpdateRequest request
    ){
        Long userId = currentUserService.getOrCreateUserId(jwt);
        log.info("Api: reminder update userId={} reminderId={} remindAt={}", userId, id, request.remind());
        ReminderResponse response = reminderService.update(userId, id, request);
        log.info("Api: reminder update success userId={} reminderId={}", userId, id);
        return response;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ){
        Long userId = currentUserService.getOrCreateUserId(jwt);
        log.info("Api: reminder delete userId={} reminderId={}", userId, id);
        reminderService.delete(userId, id);
        log.info("Api: reminder delete success userId={} reminderId={}", userId, id);
    }
}
