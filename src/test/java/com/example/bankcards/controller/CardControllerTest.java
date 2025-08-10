package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.PagedResponse;
import com.example.bankcards.service.CardService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты пользовательских эндпоинтов (раздел USER) для операций с картами.
 * <p>
 * Используется SpringBootTest + MockMvc. Фильтрация JWT подменена, чтобы
 * фокусироваться на проверке авторизации по ролям и корректности ответов.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private com.example.bankcards.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Делегирует вызовы через JwtAuthenticationFilter дальше по цепочке,
     * не блокируя тестовые запросы.
     */
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

    /**
     * Пользователь с ролью USER может получить список собственных карт.
     * Ожидания: HTTP 200 OK.
     */
    @Test
    @DisplayName("User can list own cards via /api/user/users/{userId}/cards")
    @WithMockUser(username = "user", roles = {"USER"})
    void userCanListOwnCards() throws Exception {
        Mockito.when(cardService.listUserCards(eq(1L), any(), eq(0), eq(20)))
                .thenReturn(new PagedResponse<CardDto>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/user/users/1/cards")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Пользователь с ролью USER может запросить блокировку своей карты.
     * Ожидания: HTTP 200 OK.
     */
    @Test
    @DisplayName("User can request block for own card via /api/user/cards/{cardId}/block")
    @WithMockUser(username = "user", roles = {"USER"})
    void userCanRequestBlock() throws Exception {
        Mockito.when(cardService.requestBlock(eq(100L)))
                .thenReturn(new CardDto());

        mockMvc.perform(post("/api/user/cards/100/block")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    /**
     * Анонимный пользователь не имеет доступа к пользовательским эндпоинтам.
     * Ожидания: HTTP 403 Forbidden.
     */
    @Test
    @DisplayName("Anonymous forbidden")
    void anonymousForbidden() throws Exception {
        mockMvc.perform(get("/api/user/users/1/cards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
