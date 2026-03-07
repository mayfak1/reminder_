package com.example.reminder.mapper;


import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import com.example.reminder.dto.reminder.ReminderResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface ReminderCreateAndUpdateMapper {
    Reminder toEntity(ReminderCreateAndUpdateRequest requestAndResponse);
    ReminderResponse toResponse(Reminder reminder);

}
