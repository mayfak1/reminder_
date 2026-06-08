package com.example.reminder.mapper;

import com.example.reminder.domain.reminder.Reminder;
import com.example.reminder.dto.reminder.ReminderCreateAndUpdateRequest;
import com.example.reminder.dto.reminder.ReminderResponse;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-08T15:29:14+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 24.0.2 (Homebrew)"
)
@Component
public class ReminderCreateAndUpdateMapperImpl implements ReminderCreateAndUpdateMapper {

    @Override
    public Reminder toEntity(ReminderCreateAndUpdateRequest requestAndResponse) {
        if ( requestAndResponse == null ) {
            return null;
        }

        Reminder.ReminderBuilder reminder = Reminder.builder();

        reminder.title( requestAndResponse.title() );
        reminder.description( requestAndResponse.description() );
        reminder.remind( requestAndResponse.remind() );

        return reminder.build();
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

    @Override
    public Reminder updateEntity(ReminderCreateAndUpdateRequest update) {
        if ( update == null ) {
            return null;
        }

        Reminder.ReminderBuilder reminder = Reminder.builder();

        reminder.title( update.title() );
        reminder.description( update.description() );
        reminder.remind( update.remind() );

        return reminder.build();
    }
}
