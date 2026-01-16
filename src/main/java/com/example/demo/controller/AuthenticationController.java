package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordDto;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.service.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;


@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth-controller", description = """
    Контроллер для управления аутентификацией и авторизацией пользователей.
    """)
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthService authService;
    
    @Operation(
        summary = "Аутентификация пользователя",
        description = """
            Выполняет вход пользователя в систему с получением JWT токенов.
            
            ### Процесс аутентификации:
            1. Проверяются переданные cookies (если есть)
            2. Валидируются учетные данные пользователя
            3. Генерируются новые access и refresh токены
            4. Токены устанавливаются в HTTP-only cookies
            5. Возвращается информация о пользователе
            
            ### Особенности:
            - Если передан валидный access_token, пользователь считается уже аутентифицированным
            - Refresh_token используется для продления сессии
            - Все токены имеют ограниченное время жизни
            
            ### Требования к запросу:
            - Для нового входа требуется username и password
            - Для продолжения сессии можно передать существующие токены
            """,
        tags = {"auth-controller"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешная аутентификация",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные учетные данные или формат запроса"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Неверный или просроченный токен"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервера"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @CookieValue(name = "access_token", required = false) String access,
        @CookieValue(name = "refresh_token", required = false) String refresh,
        @RequestBody LoginRequest loginRequest) {
            return authService.login(loginRequest, access, refresh);
    }
    
    @Operation(
        summary = "Обновление токенов доступа",
        description = """
            Обновляет пару токенов (access и refresh) с использованием валидного refresh-токена.
            
            ### Когда использовать:
            - Когда access-токен истек (статус 401)
            - Для продления сессии без повторного ввода учетных данных
            - При смене устройства или браузера
            
            ### Процесс обновления:
            1. Проверяется валидность refresh-токена
            2. Старый refresh-токен инвалидируется
            3. Генерируется новая пара токенов
            4. Новые токены устанавливаются в cookies
            
            ### Важная информация:
            - Refresh-токен имеет больший срок жизни, чем access-токен
            - Каждый refresh-токен может быть использован только один раз
            - При обновлении старый refresh-токен становится недействительным
            """,
        tags = {"auth-controller"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токены успешно обновлены",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Неверный или просроченный refresh-токен"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Refresh-токен не предоставлен"
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
        @CookieValue(name = "refresh_token", required = false) String refresh) {
            return authService.refresh(refresh);
    }

    @Operation(
        summary = "Выход из системы",
        description = """
            Выполняет выход пользователя из системы с инвалидацией токенов.
            
            ### Процесс выхода:
            1. Проверяется валидность access-токена
            2. Токен добавляется в черный список (blacklist)
            3. Удаляются токены из cookies пользователя
            4. Сессия пользователя завершается
            
            ### Особенности:
            - После выхода access-токен больше не может быть использован
            - Refresh-токен также инвалидируется
            - Пользователю требуется повторная аутентификация для доступа
            - Черный список токенов очищается по истечении их срока жизни
            """,
        tags = {"auth-controller"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешный выход из системы",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Токен не предоставлен или имеет неверный формат"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Неверный или просроченный токен"
        )
    })
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(
        @CookieValue(name = "access_token", required = false) String access) {
        return authService.logout(access);
    }

    @Operation(
        summary = "Смена пароля пользователя",
        description = """
            Позволяет авторизованному пользователю изменить свой пароль.
            
            ### Требования:
            - Пользователь должен быть аутентифицирован
            - Требуется указать текущий пароль для подтверждения
            - Новый пароль должен соответствовать политике безопасности
            
            ### Процесс смены пароля:
            1. Проверяется аутентификация пользователя
            2. Валидируется текущий пароль
            3. Проверяется соответствие нового пароля требованиям
            4. Пароль обновляется в базе данных
            5. При необходимости инвалидируются текущие токены
            
            ### Политика паролей (пример):
            - Минимум 8 символов
            - Наличие заглавных и строчных букв
            - Наличие цифр и специальных символов
            - Не должен совпадать с предыдущими паролями
            """,
        tags = {"auth-controller"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пароль успешно изменен"
        ),
        @ApiResponse(
            responseCode = "400",
            description = """
                Неверные данные запроса:
                - Неверный текущий пароль
                - Новый пароль не соответствует требованиям
                - Новый пароль совпадает с текущим
                """
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для выполнения операции"
        )
    })
    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordDto changePasswordDto,
            Authentication authentication) {
        
        return authService.changePassword(changePasswordDto, authentication.getName());
    }
}