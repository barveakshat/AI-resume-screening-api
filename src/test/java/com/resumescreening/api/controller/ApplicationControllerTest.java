package com.resumescreening.api.controller;

import com.resumescreening.api.exception.GlobalExceptionHandler;
import com.resumescreening.api.exception.UnauthorizedException;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Role;
import com.resumescreening.api.service.ApplicationService;
import com.resumescreening.api.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationControllerTest {

    private final ApplicationService applicationService = mock(ApplicationService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ApplicationController(applicationService, currentUserService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void applicationDetailsReturnsForbiddenWhenServiceRejectsOwnership() throws Exception {
        User user = new User();
        user.setId(99L);
        user.setEmail("other@example.com");
        user.setRole(Role.CANDIDATE);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("other@example.com", null);
        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
        when(applicationService.getApplicationById(eq(50L), eq(user)))
                .thenThrow(new UnauthorizedException("You don't have permission to access this application"));

        mockMvc.perform(get("/api/v1/applications/50")
                        .principal(authentication))
                .andExpect(status().isForbidden());
    }
}
