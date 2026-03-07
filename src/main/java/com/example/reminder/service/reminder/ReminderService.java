package com.example.reminder.service.reminder;

import com.example.reminder.dto.pagination.PageResponse;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import com.example.reminder.dto.reminder.ReminderResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReminderService {
    ReminderResponse create(Long userId, ReminderCreateAndUpdateRequest request);

    ReminderResponse update(Long userId,Long reminderId,ReminderCreateAndUpdateRequest update);

    void delete(Long userId,Long reminderId);

    ReminderResponse getById(Long userId,Long reminderId);

    PageResponse<ReminderResponse> getAll(Long userId,
                                          String q,
                                          LocalDate date,
                                          int page,
                                          int size,
                                          String sortBy,
                                          String direction);
}
