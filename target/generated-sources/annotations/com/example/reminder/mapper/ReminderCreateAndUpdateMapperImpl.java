package com.example.reminder.mapper;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import com.example.reminder.dto.reminder.ReminderResponse;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-04T07:34:20+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 25 (Eclipse Adoptium)"
)
@Component
public class ReminderCreateAndUpdateMapperImpl implements ReminderCreateAndUpdateMapper {

    @Override
    public Reminder toEntity(ReminderCreateAndUpdateRequest requestAndResponse) {
        if ( requestAndResponse == null ) {
            return null;
        }

        Reminder reminder = new Reminder();

        reminder.setTitle( requestAndResponse.title() );
        reminder.setDescription( requestAndResponse.description() );
        reminder.setRemind( requestAndResponse.remind() );

        return reminder;
    }

    @Override
    public ReminderResponse toResponse(Reminder reminder) {
        if ( reminder == null ) {
            return null;
        }

        Long id = null;
        String title = null;
        String description = null;
        Instant remind = null;

        id = reminder.getId();
        title = reminder.getTitle();
        description = reminder.getDescription();
        remind = reminder.getRemind();

        ReminderResponse reminderResponse = new ReminderResponse( id, title, description, remind );

        return reminderResponse;
    }
}
