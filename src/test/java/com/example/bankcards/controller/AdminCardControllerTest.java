package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.dto.UserWithCardCountDto;
import com.example.bankcards.dto.CardUpdateStatusRequest;
import com.example.bankcards.service.CardService;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.exception.ApiExceptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserRepository userRepository;

    @BeforeEach
    void setupFilterChainDelegation() throws Exception {
        Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest req = invocation.getArgument(0);
            jakarta.servlet.ServletResponse res = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("AdminController: admin can list all cards")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanListAll() throws Exception {
        Mockito.when(cardService.listAllCards(any(), eq(0), eq(20)))
                .thenReturn(new PagedResponse<CardDto>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/admin/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AdminController: user forbidden to access admin endpoints")
    @WithMockUser(username = "user", roles = {"USER"})
    void userForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AdminController: admin can list users with card counts")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanListUsersWithCounts() throws Exception {
        Mockito.when(userRepository.findAllWithCardCounts()).thenReturn(java.util.List.of());
        mockMvc.perform(get("/api/admin/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AdminController: user forbidden to list users with card counts")
    @WithMockUser(username = "user", roles = {"USER"})
    void userForbiddenUsersList() throws Exception {
        mockMvc.perform(get("/api/admin/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AdminController: cannot unblock EXPIRED card (400)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCannotUnblockExpiredCard() throws Exception {
        Long cardId = 123L;
        Mockito.when(cardService.updateStatus(eq(cardId), any(CardUpdateStatusRequest.class)))
                .thenThrow(new ApiExceptions.BadRequestException("Cannot activate expired card"));

        mockMvc.perform(post("/api/admin/cards/" + cardId + "/unblock").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
