package com.example.reminder.mapper;

import com.example.reminder.domain.user.UserProfile;
import com.example.reminder.dto.user.UserProfileRequestAndResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel="spring")
public interface UserProfileMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "telegramChatId",source = "telegramId")
    UserProfile toEntity(UserProfileRequestAndResponse requestAndResponse);
    @Mapping(target = "telegramId",source = "telegramChatId")
    UserProfileRequestAndResponse toResponse(UserProfile userProfile);

}
