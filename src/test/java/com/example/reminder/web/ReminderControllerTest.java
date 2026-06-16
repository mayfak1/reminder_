package com.example.reminder.web;

import com.example.reminder.config.SecurityConfig;
import com.example.reminder.dto.reminder.ReminderResponse;
import com.example.reminder.dto.pagination.PageResponse;
import com.example.reminder.exceptions.ConflictException;
import com.example.reminder.exceptions.ForbiddenException;
import com.example.reminder.exceptions.NotFoundException;
import com.example.reminder.service.reminder.ReminderService;
import com.example.reminder.service.user.CurrentUserService;
import com.example.reminder.web.reminder.ReminderController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ReminderController.class)
@Import(SecurityConfig.class)
class ReminderControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean ReminderService reminderService;
    @MockitoBean CurrentUserService currentUserService;

    @MockitoBean JwtDecoder jwtDecoder;

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
        mvc.perform(get("/api/v1/reminder"))
                .andExpect(status().isUnauthorized());
        verify(reminderService, never()).getAll(anyLong(), any(), any(), anyInt(), anyInt(), any(), any());
    }

    @Test
    void shouldReturn200_whenTokenPresent() throws Exception {
        when(reminderService.getAll(eq(1L), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(PageResponse.empty());

        mvc.perform(get("/api/v1/reminder")
                        .with(token()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn400_whenCreateTitleBlank() throws Exception {
        String body = """
                {
                  "title":"   ",
                  "description":"monthly",
                  "remind":"%s"
                }
                """.formatted(Instant.now().plusSeconds(3600));

        mvc.perform(post("/api/v1/reminder")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));

        verify(reminderService, never()).create(anyLong(), any());
    }

    @Test
    void shouldReturn400_whenCreateTitleTooLong() throws Exception {
        String longTitle = "x".repeat(256);
        String body = """
                {
                  "title":"%s",
                  "description":"monthly",
                  "remind":"%s"
                }
                """.formatted(longTitle, Instant.now().plusSeconds(3600));

        mvc.perform(post("/api/v1/reminder")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));

        verify(reminderService, never()).create(anyLong(), any());
    }

    @Test
    void shouldReturn400_whenCreateRemindIsNull() throws Exception {
        String body = """
                {
                  "title":"Pay rent",
                  "description":"monthly",
                  "remind":null
                }
                """;

        mvc.perform(post("/api/v1/reminder")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("remind")));

        verify(reminderService, never()).create(anyLong(), any());
    }

    @Test
    void shouldReturn400_whenCreateRemindInPast() throws Exception {
        String body = """
                {
                  "title":"Pay rent",
                  "description":"monthly",
                  "remind":"%s"
                }
                """.formatted(Instant.now().minusSeconds(60));

        mvc.perform(post("/api/v1/reminder")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("remind")));

        verify(reminderService, never()).create(anyLong(), any());
    }

    @Test
    void shouldReturn400_whenUpdateBodyInvalid() throws Exception {
        String body = """
                {
                  "title":"",
                  "description":"desc",
                  "remind":"%s"
                }
                """.formatted(Instant.now().plusSeconds(300));

        mvc.perform(put("/api/v1/reminder/10")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));

        verify(reminderService, never()).update(anyLong(), anyLong(), any());
    }

    @Test
    void shouldReturn404_whenReminderNotFound() throws Exception {
        when(reminderService.getById(1L, 99L)).thenThrow(new NotFoundException("reminder not found"));

        mvc.perform(get("/api/v1/reminder/99").with(token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("reminder not found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn403_whenReminderBelongsToAnotherUser() throws Exception {
        doThrow(new ForbiddenException("Access denied"))
                .when(reminderService).delete(1L, 12L);

        mvc.perform(delete("/api/v1/reminder/12").with(token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldReturn409_whenInvalidStateTransition() throws Exception {
        String body = """
                {
                  "title":"Pay rent",
                  "description":"monthly",
                  "remind":"%s"
                }
                """.formatted(Instant.now().plusSeconds(600));

        when(reminderService.update(eq(1L), eq(50L), any()))
                .thenThrow(new ConflictException("Invalid status transition"));

        mvc.perform(put("/api/v1/reminder/50")
                        .with(token())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid status transition"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void shouldPassAllListParamsToService() throws Exception {
        when(reminderService.getAll(1L, "abc", LocalDate.of(2026, 3, 2), 1, 5, "title", "desc"))
                .thenReturn(new PageResponse<>(0, 1, 5, 0, java.util.List.<ReminderResponse>of()));

        mvc.perform(get("/api/v1/reminder")
                        .with(token())
                        .queryParam("q", "abc")
                        .queryParam("date", "2026-03-02")
                        .queryParam("page", "1")
                        .queryParam("size", "5")
                        .queryParam("sortBy", "title")
                        .queryParam("direction", "desc"))
                .andExpect(status().isOk());

        verify(reminderService).getAll(1L, "abc", LocalDate.of(2026, 3, 2), 1, 5, "title", "desc");
    }
}
