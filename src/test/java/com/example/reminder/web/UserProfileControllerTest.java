package com.example.reminder.web;

import com.example.reminder.config.SecurityConfig;
import com.example.reminder.dto.user.UserProfileRequestAndResponse;
import com.example.reminder.exceptions.ConflictException;
import com.example.reminder.exceptions.NotFoundException;
import com.example.reminder.service.user.CurrentUserService;
import com.example.reminder.service.user.UserProfileServiceImpl;
import com.example.reminder.web.user.UserProfileController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import(SecurityConfig.class)
class UserProfileControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserProfileServiceImpl userProfileService;

    @MockitoBean
    CurrentUserService currentUserService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        when(currentUserService.getOrCreateUserId(any())).thenReturn(1L);
    }

    private RequestPostProcessor token() {
        return jwt().jwt(j -> j
                .tokenValue("test-token")
                .claim("sub", "sub-u1")
                .claim("preferred_username", "test-user")
        );
    }

    @Test
    void shouldReturn401_whenNoToken() throws Exception {
        mvc.perform(get("/api/v1/profile"))
                .andExpect(status().isUnauthorized());
        verify(userProfileService, never()).getProfile(anyLong());
    }

    @Test
    void shouldReturn200_whenGetProfileWithToken() throws Exception {
        when(userProfileService.getProfile(1L))
                .thenReturn(new UserProfileRequestAndResponse("user@test.com", 123L));

        mvc.perform(get("/api/v1/profile").with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.telegramId").value(123));
    }

    @Test
    void shouldReturn200_whenUpsertProfileValid() throws Exception {
        when(userProfileService.upsertProfile(eq(1L), any()))
                .thenReturn(new UserProfileRequestAndResponse("user@test.com", 123L));

        String body = """
                {
                  "email":"user@test.com",
                  "telegramId":123
                }
                """;

        mvc.perform(put("/api/v1/profile")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.telegramId").value(123));
    }

    @Test
    void shouldReturn400_whenEmailInvalid() throws Exception {
        String body = """
                {
                  "email":"wrong-email",
                  "telegramId":123
                }
                """;

        mvc.perform(put("/api/v1/profile")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("email")));

        verify(userProfileService, never()).upsertProfile(anyLong(), any());
    }

    @Test
    void shouldReturn400_whenTelegramIdNegative() throws Exception {
        String body = """
                {
                  "email":"user@test.com",
                  "telegramId":-7
                }
                """;

        mvc.perform(put("/api/v1/profile")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("telegramId")));

        verify(userProfileService, never()).upsertProfile(anyLong(), any());
    }

    @Test
    void shouldReturn409_whenEmailAlreadyExists() throws Exception {
        when(userProfileService.upsertProfile(eq(1L), any()))
                .thenThrow(new ConflictException("Email already in use"));

        String body = """
                {
                  "email":"user@test.com",
                  "telegramId":123
                }
                """;

        mvc.perform(put("/api/v1/profile")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldReturn404_whenProfileNotFound() throws Exception {
        when(userProfileService.getProfile(1L))
                .thenThrow(new NotFoundException("Profile not found"));

        mvc.perform(get("/api/v1/profile").with(token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Profile not found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}

