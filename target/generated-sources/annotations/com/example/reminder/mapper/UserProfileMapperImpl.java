package com.example.reminder.mapper;

import com.example.reminder.domain.user.UserProfile;
import com.example.reminder.dto.user.UserProfileRequestAndResponse;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-04T07:43:40+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Eclipse Adoptium)"
)
@Component
public class UserProfileMapperImpl implements UserProfileMapper {

    @Override
    public UserProfile toEntity(UserProfileRequestAndResponse requestAndResponse) {
        if ( requestAndResponse == null ) {
            return null;
        }

        UserProfile userProfile = new UserProfile();

        userProfile.setTelegramChatId( requestAndResponse.telegramId() );
        userProfile.setEmail( requestAndResponse.email() );

        return userProfile;
    }

    @Override
    public UserProfileRequestAndResponse toResponse(UserProfile userProfile) {
        if ( userProfile == null ) {
            return null;
        }

        Long telegramId = null;
        String email = null;

        telegramId = userProfile.getTelegramChatId();
        email = userProfile.getEmail();

        UserProfileRequestAndResponse userProfileRequestAndResponse = new UserProfileRequestAndResponse( email, telegramId );

        return userProfileRequestAndResponse;
    }
}
