package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.ApiExceptions;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private TransferService transferService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
    @DisplayName("UserController: USER can list own cards")
    @WithMockUser(username = "user", roles = {"USER"})
    void userCanListOwnCards() throws Exception {
        PagedResponse<CardDto> resp = new PagedResponse<>(List.of(), 0, 20, 0, 0);
        Mockito.when(cardService.listUserCards(eq(1L), any(CardFilter.class), eq(0), eq(20)))
                .thenReturn(resp);

        mockMvc.perform(get("/api/user/users/1/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UserController: anonymous forbidden")
    void anonymousForbidden() throws Exception {
        mockMvc.perform(get("/api/user/users/1/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("UserController: USER can request block card")
    @WithMockUser(username = "user", roles = {"USER"})
    void userCanBlockCard() throws Exception {
        Long cardId = 10L;
        CardDto dto = new CardDto();
        dto.setId(cardId);
        dto.setMaskedCardNumber("**** **** **** 1234");
        dto.setOwner("User Test");
        dto.setExpiryDate(LocalDate.now().plusYears(1));
        dto.setStatus(CardStatus.BLOCKED);
        dto.setBalance(new BigDecimal("100.00"));
        Mockito.when(cardService.requestBlock(eq(cardId))).thenReturn(dto);

        mockMvc.perform(post("/api/user/cards/" + cardId + "/block").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UserController: USER can request unblock card (200)")
    @WithMockUser(username = "user", roles = {"USER"})
    void userCanUnblockCard() throws Exception {
        Long cardId = 11L;
        CardDto dto = new CardDto();
        dto.setId(cardId);
        dto.setStatus(CardStatus.ACTIVE);
        Mockito.when(cardService.requestUnblock(eq(cardId))).thenReturn(dto);

        mockMvc.perform(post("/api/user/cards/" + cardId + "/unblock").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UserController: cannot unblock EXPIRED card (400)")
    @WithMockUser(username = "user", roles = {"USER"})
    void cannotUnblockExpiredCard() throws Exception {
        Long cardId = 12L;
        Mockito.when(cardService.requestUnblock(eq(cardId)))
                .thenThrow(new ApiExceptions.BadRequestException("Cannot activate expired card"));

        mockMvc.perform(post("/api/user/cards/" + cardId + "/unblock").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UserController: USER can get card balance")
    @WithMockUser(username = "user", roles = {"USER"})
    void userCanGetBalance() throws Exception {
        Long cardId = 13L;
        Mockito.when(cardService.getBalance(eq(cardId))).thenReturn(new BalanceDto(new BigDecimal("250.50")));

        mockMvc.perform(get("/api/user/cards/" + cardId + "/balance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UserController: USER can transfer between own cards")
    @WithMockUser(username = "user", roles = {"USER"})
    void userCanTransfer() throws Exception {
        Long userId = 1L;
        Mockito.doNothing().when(transferService).transfer(eq(userId), any(TransferRequest.class));

        String body = "{\"fromCardNumber\":\"4111111111111111\",\"toCardNumber\":\"5555555555554444\",\"amount\":10.00}";
        mockMvc.perform(post("/api/user/users/" + userId + "/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
