package com.example.demo.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserCreateDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.service.UserService;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "user-controller", description = """
    Контроллер для управления пользователями системы умного дома.
    """)
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
        summary = "Получить список всех пользователей",
        description = """
            Возвращает полный список всех зарегистрированных пользователей системы.
            
            ### Возвращаемая информация:
            - ID пользователя
            - Имя пользователя (username)
            - Роль (USER/ADMIN)
            - Дата создания аккаунта
            - Статус активности
            
            ### Права доступа:
            - Только пользователи с ролью ADMIN
            - Требуется право `USER:WRITE`
            - USER не может видеть список всех пользователей
            
            ### Пример ответа:
            ```json
            [
              {
                "id": 1,
                "username": "admin",
                "role": "ADMIN",
                "createdAt": "2024-01-10T10:30:00Z",
                "active": true
              },
              {
                "id": 2,
                "username": "ivan.ivanov",
                "role": "USER",
                "createdAt": "2024-01-12T14:20:00Z",
                "active": true
              },
              {
                "id": 3,
                "username": "maria.petrova",
                "role": "USER",
                "createdAt": "2024-01-14T09:15:00Z",
                "active": false
              }
            ]
            ```
            
            ### Фильтрация и пагинация:
            - В текущей реализации пагинация не поддерживается
            - Для большого количества пользователей рекомендуется добавить пагинацию
            - Можно добавить фильтры по роли, активности, дате создания
            
            ### Использование:
            - Администрирование пользователей
            - Просмотр списка пользователей для назначения прав
            - Аудит активности пользователей
            """,
        tags = {"user-controller", "admin-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список пользователей успешно получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDto.class, type = "array")
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав (требуется право USER:WRITE)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервера"
        )
    })
    @GetMapping
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        logger.info("Getting all users");
        try {
            List<User> users = userService.getAllUsers();
            List<UserResponseDto> userDtos = users.stream()
                    .map(UserMapper::toUserResponseDto)
                    .toList();
            return ResponseEntity.ok(userDtos);
        } catch (Exception e) {
            logger.error("Error getting all users: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Получить пользователя по ID",
        description = """
            Возвращает информацию о пользователе по его идентификатору.
            
            ### Возвращаемые данные:
            - Основная информация о пользователе
            - Роль и права
            - Статус активности
            - История активности (если настроено)
            
            ### Проверки:
            1. Существование пользователя с указанным ID
            2. Права доступа у запрашивающего пользователя
            3. Активность пользователя (не заблокирован ли)
            
            ### Пример ответа:
            ```json
            {
              "id": 2,
              "username": "ivan.ivanov",
              "role": "USER",
              "createdAt": "2024-01-12T14:20:00Z",
              "lastLogin": "2024-01-15T09:30:00Z",
              "active": true,
              "email": "ivan@example.com",
              "phone": "+79991234567",
              "managedRooms": 3
            }
            ```
            
            ### Особенности:
            - ADMIN может получить информацию о любом пользователе
            - USER может получить информацию только о себе
            - При запросе несуществующего пользователя возвращается 404
            - Скрытые поля (пароль, токены) никогда не возвращаются
            
            ### Использование:
            - Просмотр профиля пользователя
            - Настройка прав доступа
            - Проверка активности пользователя
            """,
        tags = {"user-controller", "read-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь найден",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь с указанным ID не найден"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для просмотра информации о пользователе"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        )
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<UserResponseDto> getUserById(
            @Parameter(
                description = "Идентификатор пользователя",
                required = true,
                example = "1"
            )
            @PathVariable Long id) {
        
        logger.debug("GET /api/users/{}", id);
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(UserMapper.toUserResponseDto(user));
            } else {
                logger.warn("User with id {} not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting user by id {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Создать нового пользователя",
        description = """
            Создает нового пользователя в системе с указанной ролью.
            
            ### Обязательные поля:
            - **username** - уникальное имя пользователя (3-50 символов)
            - **password** - пароль (мин. 8 символов, буквы и цифры)
            - **roleId** - идентификатор роли (1=ADMIN, 2=USER)
            
            ### Процесс создания:
            1. Валидация входных данных
            2. Проверка уникальности username
            3. Хеширование пароля (BCrypt)
            4. Создание записи в базе данных
            5. Назначение роли пользователю
            6. Отправка уведомления (если настроено)
            
            ### Требования к паролю:
            - Минимум 8 символов
            - Хотя бы одна заглавная буква
            - Хотя бы одна цифра
            - Можно использовать специальные символы
            - Не должен совпадать с username
            
            ### Пример запроса:
            ```json
            {
              "username": "new.user",
              "password": "SecurePass123",
              "roleId": 2
            }
            ```
            
            ### Пример ответа:
            ```json
            {
              "id": 4,
              "username": "new.user",
              "role": "USER",
              "createdAt": "2024-01-15T14:30:00Z",
              "active": true,
              "message": "User created successfully"
            }
            ```
            
            ### Ограничения:
            - ADMIN может создавать пользователей с любой ролью
            - USER не может создавать пользователей
            - Username должен быть уникальным
            - Нельзя создать пользователя с несуществующей ролью
            
            ### Рекомендации:
            - Используйте сложные пароли
            - Назначайте роли в соответствии с обязанностями
            - Регулярно обновляйте пароли пользователей
            - Используйте двухфакторную аутентификацию для администраторов
            """,
        tags = {"user-controller", "admin-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Пользователь успешно создан",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = """
                Неверные данные запроса:
                - Неверный формат username или пароля
                - Username уже существует
                - Несуществующая роль
                """
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для создания пользователей"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка при создании пользователя"
        )
    })
    @PostMapping
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<UserResponseDto> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Данные нового пользователя",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserCreateDto.class),
                    examples = {
                        @ExampleObject(
                            name = "Создание администратора",
                            value = """
                                {
                                  "username": "system.admin",
                                  "password": "AdminPass123!",
                                  "roleId": 1
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Создание обычного пользователя",
                            value = """
                                {
                                  "username": "home.user",
                                  "password": "HomeUser456!",
                                  "roleId": 2
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody UserCreateDto userCreateDto) {
        
        logger.debug("POST /api/users - {}", userCreateDto);
        try {
            User createdUser = userService.createUserWithRole(userCreateDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UserMapper.toUserResponseDto(createdUser));
        } catch (IllegalArgumentException e) {
            logger.error("Error creating user: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error creating user: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Создать первого пользователя системы",
        description = """
            Специальный публичный эндпоинт для инициализации системы.
            
            ### Особенности:
            - Доступен без аутентификации
            - Работает только когда в системе нет пользователей
            - Создает пользователя с ролью ADMIN
            - После успешного создания эндпоинт блокируется
            
            ### Когда использовать:
            - При первом запуске системы
            - После полной очистки базы данных
            - При развертывании на новом сервере
            
            ### Процесс работы:
            1. Проверка: есть ли пользователи в системе
            2. Если пользователи есть → возвращается ошибка 403
            3. Если пользователей нет → создается администратор
            4. Роль автоматически устанавливается как ADMIN (roleId=1)
            5. Возвращается созданный пользователь
            
            ### Пример запроса:
            ```json
            {
              "username": "admin",
              "password": "StrongAdminPass123!"
            }
            ```
            
            ### Пример ответа при успехе:
            ```json
            {
              "id": 1,
              "username": "admin",
              "role": "ADMIN",
              "createdAt": "2024-01-15T14:30:00Z",
              "active": true,
              "message": "First user created successfully"
            }
            ```
            
            ### Пример ответа если пользователи уже есть:
            ```json
            {
              "message": "First user already exists. Use regular user creation endpoint with authentication.",
              "timestamp": "2024-01-15T14:35:00Z"
            }
            ```
            
            ### Важные моменты:
            - Используйте надежный пароль для первого пользователя
            - После создания первого пользователя создайте резервного администратора
            - Храните учетные данные первого пользователя в безопасном месте
            - Эндпоинт следует отключить в production после инициализации
            
            ### Безопасность:
            - Рекомендуется использовать этот эндпоинт только в закрытой сети
            - Можно добавить ограничение по IP-адресу
            - После использования рассмотрите возможность отключения эндпоинта
            - Мониторьте использование этого эндпоинта
            """,
        tags = {"user-controller", "system-initialization"},
        security = {}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Первый пользователь успешно создан",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные пользователя"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Пользователи уже существуют в системе"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Ошибка при создании пользователя"
        )
    })
    @PostMapping("/create-first")
    public ResponseEntity<UserResponseDto> createFirstUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Данные первого пользователя (роль будет установлена как ADMIN)",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserCreateDto.class),
                    examples = @ExampleObject(
                        name = "Создание первого администратора",
                        value = """
                            {
                              "username": "admin",
                              "password": "VeryStrongPassword123!"
                            }
                            """
                    )
                )
            )
            @RequestBody UserCreateDto userCreateDto) {
        
        logger.debug("POST /api/users/create-first - {}", userCreateDto);
        try {
            if (userService.hasUsers()) {
                logger.error("First user already exists");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(UserResponseDto.builder()
                        .message("First user already exists. Use regular user creation endpoint with authentication.")
                        .build());
            }
            
            if (userCreateDto.roleId() == null) {
                userCreateDto = new UserCreateDto(
                    userCreateDto.username(),
                    userCreateDto.password(),
                    1L // ID роли ADMIN
                );
            }
            
            User createdUser = userService.createUserWithRole(userCreateDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(UserMapper.toUserResponseDto(createdUser));
        } catch (IllegalArgumentException e) {
            logger.error("Error creating first user: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Unexpected error creating first user: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Обновить данные пользователя",
        description = """
            Обновляет информацию существующего пользователя.
            
            ### Что можно обновить:
            - **username** - изменить имя пользователя
            - **password** - изменить пароль
            
            ### Особенности:
            - Можно обновлять оба поля или только одно
            - Если поле не указано, оно не изменяется
            - Пароль автоматически хешируется перед сохранением
            - При изменении username проверяется уникальность
            
            ### Проверки:
            1. Существование пользователя с указанным ID
            2. Права доступа у обновляющего пользователя
            3. Уникальность нового username (если изменяется)
            4. Корректность нового пароля (если изменяется)
            
            ### Примеры запросов:
            
            **Изменение пароля:**
            ```json
            {
              "password": "NewSecurePass456!"
            }
            ```
            
            **Изменение username:**
            ```json
            {
              "username": "new.username"
            }
            ```
            
            **Полное обновление:**
            ```json
            {
              "username": "updated.user",
              "password": "NewPass789!"
            }
            ```
            
            ### Ограничения:
            - ADMIN может обновлять любого пользователя
            - USER может обновлять только свой профиль
            - Нельзя изменить роль через этот эндпоинт
            - Нельзя деактивировать пользователя через этот эндпоинт
            
            ### Безопасность:
            - При изменении пароля все активные сессии пользователя должны сбрасываться
            - Уведомление об изменении данных отправляется пользователю
            - Все изменения логируются для аудита
            """,
        tags = {"user-controller", "admin-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь успешно обновлен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные для обновления"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь с указанным ID не найден"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для обновления пользователя"
        )
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<UserResponseDto> updateUser(
            @Parameter(
                description = "Идентификатор обновляемого пользователя",
                required = true,
                example = "1"
            )
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Новые данные пользователя",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserCreateDto.class)
                )
            )
            @RequestBody UserCreateDto userCreateDto) {
        
        logger.debug("PUT /api/users/{} - {}", id, userCreateDto);
        try {
            User userDetails = new User();
            userDetails.setUsername(userCreateDto.username());
            userDetails.setPassword(userCreateDto.password());
            
            User updatedUser = userService.updateUser(id, userDetails);
            if (updatedUser != null) {
                return ResponseEntity.ok(UserMapper.toUserResponseDto(updatedUser));
            } else {
                logger.warn("User with id {} not found for update", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating user {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Удалить пользователя",
        description = """
            Удаляет пользователя из системы.
            
            ### Процесс удаления:
            1. Проверка существования пользователя
            2. Проверка прав на удаление
            3. Проверка зависимостей:
               - Является ли пользователь менеджером комнат
               - Есть ли активные сессии
               - Используется ли в активных сценариях
            4. Мягкое удаление (деактивация) или физическое удаление
            
            ### Последствия удаления:
            - Все активные сессии пользователя завершаются
            - Пользователь теряет доступ к системе
            - Комнаты, где пользователь был менеджером, требуют нового менеджера
            - Данные пользователя могут быть сохранены для аудита
            
            ### Альтернативы удалению:
            1. **Деактивация** - временное отключение доступа
            2. **Смена роли** - понижение прав до USER
            3. **Блокировка** - временная блокировка аккаунта
            4. **Сброс пароля** - без удаления аккаунта
            
            ### Восстановление:
            - Удаленные пользователи помещаются в корзину
            - Восстановление возможно в течение 30 дней
            - После 30 дней удаление становится физическим
            - Резервные копии сохраняются для аудита
            
            ### Ограничения:
            - Нельзя удалить последнего администратора
            - Нельзя удалить себя самого
            - USER не может удалять других пользователей
            - ADMIN может удалять только пользователей с ролью USER
            
            ### Примеры использования:
            - Увольнение сотрудника
            - Удаление тестовых аккаунтов
            - Очистка неактивных аккаунтов
            - Удаление аккаунтов по запросу пользователя
            """,
        tags = {"user-controller", "admin-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Пользователь успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь с указанным ID не найден"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления пользователя или нельзя удалить последнего администратора"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Невозможно удалить пользователя (есть зависимости)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Ошибка при удалении пользователя"
        )
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<Void> deleteUser(
            @Parameter(
                description = "Идентификатор удаляемого пользователя",
                required = true,
                example = "2"
            )
            @PathVariable Long id) {
        
        logger.debug("DELETE /api/users/{}", id);
        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                logger.info("User with id {} successfully deleted", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("User with id {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Найти пользователя по имени",
        description = """
            Ищет пользователя по его уникальному имени (username).
            
            ### Особенности поиска:
            - Поиск по точному совпадению username
            - Регистрозависимый поиск
            - Возвращает первого найденного пользователя
            - Если пользователей несколько с одинаковым username (невозможно), возвращает первого
            
            ### Примеры использования:
            - Проверка доступности username при регистрации
            - Поиск пользователя для назначения прав
            - Автодополнение в интерфейсе администратора
            - Восстановление доступа по username
            
            ### Пример ответа:
            ```json
            {
              "id": 2,
              "username": "ivan.ivanov",
              "role": "USER",
              "createdAt": "2024-01-12T14:20:00Z",
              "active": true,
              "lastLogin": "2024-01-15T09:30:00Z"
            }
            ```
            
            ### Ошибки:
            - 404 - пользователь с таким username не найден
            - 403 - недостаточно прав для поиска пользователей
            - 401 - пользователь не аутентифицирован
            
            ### Альтернативы:
            - Для поиска по части имени используйте фильтрацию в getAllUsers
            - Для нечеткого поиска требуется дополнительная реализация
            - Для поиска по email или другим полям нужны дополнительные методы
            
            ### Производительность:
            - Username индексируется в базе данных
            - Поиск выполняется быстро даже при большом количестве пользователей
            - Рекомендуется использовать кэширование для часто запрашиваемых пользователей
            """,
        tags = {"user-controller", "search-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь найден",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь с указанным username не найден"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для поиска пользователей"
        )
    })
    @GetMapping("/username/{username}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<UserResponseDto> getUserByUsername(
            @Parameter(
                description = "Имя пользователя для поиска",
                required = true,
                example = "ivan.ivanov"
            )
            @PathVariable String username) {
        
        logger.debug("GET /api/users/username/{}", username);
        try {
            User user = userService.getUserByUsername(username);
            if (user != null) {
                return ResponseEntity.ok(UserMapper.toUserResponseDto(user));
            } else {
                logger.warn("User with username {} not found", username);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error getting user by username {}: {}", username, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}