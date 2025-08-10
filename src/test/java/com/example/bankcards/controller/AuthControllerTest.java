package com.example.bankcards.controller;

import com.example.bankcards.entity.UserEntity;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenService;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Юнит-тесты для эндпоинтов регистрации в {@link AuthController}.
 * <p>
 * Тесты используют standalone MockMvc с замоканными зависимостями, чтобы проверить:
 * - успешный сценарий регистрации и корректность возвращаемого ответа;
 * - ошибки валидации и бизнес-правил (занятый логин, некорректный fullName, шаблоны username/password).
 */
class AuthControllerTest {

    private MockMvc mockMvc;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenService jwtTokenService;
    private CardService cardService;
    private ObjectMapper objectMapper;

    /**
     * Инициализирует MockMvc со standalone-экземпляром AuthController и
     * подготавливает замоканные зависимости (UserRepository, PasswordEncoder, JwtTokenService, CardService).
     */
    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenService = mock(JwtTokenService.class);
        cardService = mock(CardService.class);
        AuthController controller = new AuthController(userRepository, passwordEncoder, jwtTokenService, cardService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice()
                .build();
        objectMapper = new ObjectMapper();
    }

    /**
     * Проверяет успешную регистрацию при валидных данных и свободном имени пользователя.
     * Ожидания:
     * - HTTP 200 OK;
     * - JSON содержит userId, username, fullName, token;
     * - UserRepository.save вызывается с корректно распарсенными ФИО;
     * - Пароль кодируется, генерируется JWT, для пользователя выпускается карта автоматически.
     */
    @Test
    @DisplayName("Register: success with valid data")
    void register_success() throws Exception {
        when(userRepository.existsByUsername("john.doe")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtTokenService.generateToken(anyString(), anyString())).thenReturn("JWT");

        Map<String, String> body = Map.of(
                "username", "john.doe",
                "password", "Zz9@zzzz",
                "fullName", "Иванов Иван Иванович"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.username", is("john.doe")))
                .andExpect(jsonPath("$.fullName", is("Иванов Иван Иванович")))
                .andExpect(jsonPath("$.token", is("JWT")));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        UserEntity saved = captor.getValue();
        assertEquals("john.doe", saved.getUsername());
        assertEquals("HASH", saved.getPasswordHash());
        assertEquals("Иванов", saved.getLastName());
        assertEquals("Иван", saved.getFirstName());
        assertEquals("Иванович", saved.getPatronymic());
        assertEquals("Иванов Иван Иванович", saved.getFullName());
        verify(cardService).issueAutoCard(1L, "Иванов Иван Иванович");
    }

    /**
     * Проверяет, что попытка регистрации с уже занятым username возвращает HTTP 409 Conflict
     * и пользователь не сохраняется.
     */
    @Test
    @DisplayName("Register: username already taken -> 409")
    void register_username_taken() throws Exception {
        when(userRepository.existsByUsername("john"))
                .thenReturn(true);

        Map<String, String> body = Map.of(
                "username", "john",
                "password", "Zz9@zzzz",
                "fullName", "Петров Петр Петрович"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Username")));

        verify(userRepository, never()).save(any());
    }

    /**
     * Проверяет, что регистрация завершается ошибкой (HTTP 400), если fullName состоит не ровно из трёх частей
     * (фамилия, имя, отчество).
     */
    @Test
    @DisplayName("Register: fullName must have exactly 3 parts -> 400")
    void register_fullName_wrong_parts() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        Map<String, String> body = Map.of(
                "username", "john",
                "password", "Zz9@zzzz",
                "fullName", "ТолькоДва Слова"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("exactly 3 parts")));

        verify(userRepository, never()).save(any());
    }

    /**
     * Проверяет, что регистрация завершается ошибкой (HTTP 400), если любая часть fullName короче 2 символов.
     */
    @Test
    @DisplayName("Register: each fullName part min length 2 -> 400")
    void register_fullName_min_length() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        Map<String, String> body = Map.of(
                "username", "john",
                "password", "Zz9@zzzz",
                "fullName", "И Ив Иванович"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("at least 2 characters")));

        verify(userRepository, never()).save(any());
    }

    /**
     * Проверяет, что регистрация завершается ошибкой (HTTP 400), если username не соответствует разрешённому шаблону
     * (например, содержит пробел).
     */
    @Test
    @DisplayName("Register: invalid username pattern -> 400")
    void register_invalid_username() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        Map<String, String> body = Map.of(
                "username", "bad user", // пробел недопустим
                "password", "Zz9@zzzz",
                "fullName", "Сидоров Сидор Сидорович"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }

    /**
     * Проверяет, что регистрация завершается ошибкой (HTTP 400), если пароль не удовлетворяет требованиям надёжности
     * (например, отсутствует заглавная буква или спецсимволы).
     */
    @Test
    @DisplayName("Register: invalid password pattern -> 400")
    void register_invalid_password() throws Exception {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        Map<String, String> body = Map.of(
                "username", "jane",
                // нет заглавной буквы, нет спецсимвола
                "password", "abc12345",
                "fullName", "Смирнова Анна Сергеевна"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verify(userRepository, never()).save(any());
    }
}
