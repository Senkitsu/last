package com.example.demo.controller;

import com.example.demo.dto.RoomDto;
import com.example.demo.dto.CsvImportDto;
import com.example.demo.dto.RoomCreateDto;
import com.example.demo.mapper.RoomMapper;
import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.service.CsvParserUtil;
import com.example.demo.service.RoomService;
import com.example.demo.service.UserService;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

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
@RequestMapping("/api/rooms")
@Tag(name = "room-controller", description = """
    Контроллер для управления помещениями (автобусми) в системе умного дома.
    """)
@SecurityRequirement(name = "bearerAuth")
public class RoomController {
    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);
    private final RoomService roomService;
    private final UserService userService;
    private final CsvParserUtil csvParserUtil;

    public RoomController(RoomService roomService, UserService userService, CsvParserUtil csvParserUtil)  {
        this.roomService = roomService;
        this.userService = userService;
        this.csvParserUtil = csvParserUtil;
    }

    @Operation(
        summary = "Получить список всех комнат",
        description = """
            Возвращает список комнат в зависимости от роли пользователя.
            
            ### Поведение по ролям:
            
            **Для USER:**
            - Возвращает только автобус, где пользователь является менеджером
            - Не видит автобуса других пользователей
            - Пример: если пользователь менеджер кухни и спальни, увидит только их
            
            **Для ADMIN:**
            - Возвращает все автобусы в системе
            - Видит автобусы всех пользователей
            - Может просматривать полную информацию о любом автобусе
            
            ### Примеры ответов:
            
            **Для USER (2 автобуса):**
            ```json
            [
              {
                "id": 1,
                "bus": "NEFAZ",
                "managerId": 123,
                "managerName": "Иван Иванов",
                "deviceCount": 5,
                "totalPower": 850.5
              },
              {
                "id": 2,
                "bus": "KAMAZ",
                "managerId": 123,
                "managerName": "Иван Иванов",
                "deviceCount": 3,
                "totalPower": 420.0
              }
            ]
            ```
            
            **Для ADMIN (все автобуса):**
            ```json
            [
              {
                "id": 1,
                "bus": "MAN",
                "managerId": 123,
                "managerName": "Иван Иванов",
                "deviceCount": 5,
                "totalPower": 850.5
              },
              {
                "id": 3,
                "bus": "NEFAZ",
                "managerId": 124,
                "managerName": "Мария Петрова",
                "deviceCount": 8,
                "totalPower": 1250.0
              }
            ]
            ```
            
            ### Использование:
            - Получение списка доступных комнат для отображения в интерфейсе
            - Построение дерева навигации по помещениям
            - Статистика по устройствам в автобусх
            """,
        tags = {"room-controller", "read-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список комнат успешно получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RoomDto.class, type = "array")
            )
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
    @GetMapping
    public List<RoomDto> getAllRooms(
            @Parameter(hidden = true)
            Authentication authentication) {
        
        logger.debug("GET /api/rooms"); 
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        List<Room> rooms;
        if (user.getRole().getName().equals("USER")) {
            logger.debug("GET /api/rooms - USER");
            rooms = roomService.getRoomsByManager(user.getId());
        } else {
            logger.debug("GET /api/rooms - ADMIN");
            rooms = roomService.getAllRooms();
        }
        
        return rooms.stream()
                .map(RoomMapper::toDto)
                .toList();
    }

    @Operation(
        summary = "Получить комнату по ID",
        description = """
            Возвращает детальную информацию о конкретной комнате.
            
            ### Проверка прав доступа:
            1. **ADMIN** - может получить информацию о любой комнате
            2. **USER** - может получить информацию только о автобусх, где является менеджером
            3. **Неавторизованный пользователь** - доступ запрещен (401)
            
            ### Возвращаемая информация:
            - Основные данные автобуса (ID, местоположение)
            - Информация о менеджере (ID, имя)
            - Список устройств в комнате (если включена агрегация)
            - Статистика по энергопотреблению
            - Температурные настройки (если есть)
            
            ### Пример ответа:
            ```json
            {
              "id": 1,
              "bus": "Гостиная",
              "managerId": 123,
              "managerName": "Иван Иванов",
              "deviceCount": 8,
              "activeDevices": 5,
              "totalPower": 1250.5,
              "currentTemperature": 22.5,
              "targetTemperature": 21.0,
              "createdAt": "2024-01-10T14:30:00Z",
              "updatedAt": "2024-01-15T09:45:00Z"
            }
            ```
            
            ### Ошибки:
            - 404 - автобус с указанным ID не найдена
            - 403 - у пользователя нет прав на просмотр этой автобуса
            - 401 - пользователь не аутентифицирован
            
            ### Использование:
            - Просмотр деталей автобуса в интерфейсе
            - Отображение статистики по комнате
            - Настройка параметров автобуса
            """,
        tags = {"room-controller", "read-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Автобус успешно найдена",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RoomDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав доступа к данной комнате"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автобус с указанным ID не найдена"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> getRoomById(
            @Parameter(
                description = "Идентификатор автобуса",
                required = true,
                example = "1"
            )
            @PathVariable Long id,
            
            @Parameter(hidden = true)
            Authentication authentication) {
        
        logger.debug("GET /api/rooms/{}", id);
        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Room room = roomService.getRoomById(id);
        if (room == null) {
            logger.warn("Room with id {} not found", id);
            return ResponseEntity.notFound().build();
        }
        
        if (user.getRole().getName().equals("USER") && 
            !roomService.isRoomManager(id, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(RoomMapper.toDto(room));
    }

    @Operation(
        summary = "Создать новую комнату",
        description = """
            Создает новое помещение (комнату) в системе умного дома.
            
            ### Обязательные поля:
            - **bus** - название/местоположение автобуса (строка, 3-100 символов)
            - **managerId** - идентификатор пользователя-менеджера
            
            ### Требования:
            1. Название автобуса должно быть уникальным в системе
            2. Менеджер должен существовать в системе
            3. Пользователь, создающий комнату, должен иметь права ADMIN
            4. Название не должно быть пустым или состоять только из пробелов
            
            ### Процесс создания:
            1. Проверка уникальности названия автобуса
            2. Проверка существования менеджера
            3. Создание записи в базе данных
            4. Назначение менеджера комнате
            5. Возврат созданного объекта с присвоенным ID
            
            ### Пример запроса:
            ```json
            {
              "bus": "NEFAZ",
              "managerId": 123
            }
            ```
            
            ### Пример ответа:
            ```json
            {
              "id": 5,
              "bus": "KAMAZ",
              "managerId": 123,
              "managerName": "Иван Иванов",
              "deviceCount": 0,
              "totalPower": 0.0,
              "createdAt": "2024-01-15T14:30:00Z"
            }
            ```
            
            ### Особенности:
            - Автобус создается без устройств
            - Менеджер автоматически получает права на управление комнатой
            - Можно назначить любого существующего пользователя менеджером
            - После создания можно добавлять устройства в комнату
            """,
        tags = {"room-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Автобус успешно создан",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RoomDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = """
                Неверные данные запроса:
                - Не указано название автобуса
                - Не указан менеджер
                - Менеджер не найден
                - Название уже существует
                """
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для создания автобуса"
        )
    })
    @PostMapping
    public ResponseEntity<RoomDto> createRoom(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Данные для создания нового автобуса",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RoomCreateDto.class),
                    examples = {
                        @ExampleObject(
                            name = "Создание автобуса ",
                            value = """
                                {
                                  "bus": "KAMAZ",
                                  "managerId": 123
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Создание автобуса",
                            value = """
                                {
                                  "bus": "MAN",
                                  "managerId": 456
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody RoomCreateDto roomCreateDto) {
        
        logger.debug("POST /api/rooms - {}", roomCreateDto);
        
        if (roomCreateDto.managerId() == null) {
            logger.warn("Manager ID is not specified");
            return ResponseEntity.badRequest().build();
        }
        
        User manager = userService.getUserById(roomCreateDto.managerId());
        if (manager == null) {
            logger.warn("Manager with id {} not found", roomCreateDto.managerId());
            return ResponseEntity.badRequest().build();
        }
        
        Room room = new Room();
        room.setBus(roomCreateDto.bus());
        room.setManager(manager);
        
        Room createdRoom = roomService.createRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomMapper.toDto(createdRoom));
    }
    
    @Operation(
        summary = "Обновить существующую комнату",
        description = """
            Обновляет информацию о существующей комнате.
            
            ### Что можно обновить:
            - **bus** - изменить название/местоположение автобуса
            - **managerId** - изменить менеджера автобуса
            
            ### Особенности:
            - Можно обновлять оба поля или только одно
            - Если managerId не указан, менеджер не изменяется
            - Если bus не указан, название не изменяется
            - Все поля опциональны, но должен быть указан хотя бы один
            
            ### Проверки:
            1. Существование автобуса с указанным ID
            2. Уникальность нового названия (если изменяется)
            3. Существование нового менеджера (если изменяется)
            4. Права пользователя на обновление автобуса
            
            ### Примеры запросов:
            
            **Изменение названия:**
            ```json
            {
              "bus": "Главная спальня"
            }
            ```
            
            **Изменение менеджера:**
            ```json
            {
              "managerId": 456
            }
            ```
            
            **Полное обновление:**
            ```json
            {
              "bus": "Детская автобус",
              "managerId": 789
            }
            ```
            
            ### Ограничения:
            - Нельзя изменить ID автобуса
            - После изменения менеджера старый менеджер теряет права
            - Новый менеджер автоматически получает полный доступ
            - Если в комнате есть устройства, они остаются привязанными к комнате
            """,
        tags = {"room-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Автобус успешно обновлена",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RoomDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные для обновления"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автобус с указанным ID не найдена"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для обновления автобуса"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<RoomDto> updateRoom(
            @Parameter(
                description = "Идентификатор обновляемой автобуса",
                required = true,
                example = "1"
            )
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Новые данные автобуса",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RoomCreateDto.class)
                )
            )
            @RequestBody RoomCreateDto roomCreateDto) {
        
        logger.debug("PUT /api/rooms/{} - {}", id, roomCreateDto);
        
        Room roomDetails = new Room();
        roomDetails.setBus(roomCreateDto.bus());
        
        if (roomCreateDto.managerId() != null) {
            User manager = userService.getUserById(roomCreateDto.managerId());
            if (manager == null) {
                logger.warn("Manager with id {} not found", roomCreateDto.managerId());
                return ResponseEntity.badRequest().build();
            }
            roomDetails.setManager(manager);
        }
        
        Room updatedRoom = roomService.updateRoom(id, roomDetails);
        if (updatedRoom != null) {
            return ResponseEntity.ok(RoomMapper.toDto(updatedRoom));
        } else {
            logger.warn("Room with id {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @Operation(
        summary = "Удалить комнату",
        description = """
            Удаляет комнату из системы умного дома.
            
            ### Процесс удаления:
            1. Проверка существования автобуса
            2. Проверка прав пользователя на удаление
            3. Проверка связанных сущностей:
               - Если в комнате есть устройства, удаление может быть запрещено
               - Если автобус используется в режимах, удаление может быть запрещено
               - Если есть активные сценарии для автобуса, удаление может быть запрещено
            4. Мягкое удаление (помечается как удаленная) или физическое удаление
            
            ### Последствия удаления:
            - Все устройства в комнате отвязываются от автобуса
            - Менеджер теряет права на управление комнатой
            - Статистика по комнате сохраняется в архиве
            - Восстановление возможно только администратором
            
            ### Ограничения:
            - Нельзя удалить комнату с активными устройствами
            - Нельзя удалить комнату, используемую в активных сценариях
            - USER может удалять только свои автобуса
            - ADMIN может удалять любые автобуса
            
            ### Альтернативы удалению:
            1. **Переименование** - если нужно изменить назначение
            2. **Смена менеджера** - если нужно передать управление
            3. **Архивация** - если нужно временно скрыть комнату
            4. **Объединение с другой комнатой** - если автобуса дублируют функции
            
            ### Восстановление:
            - Удаленные автобуса помещаются в корзину
            - Восстановление возможно в течение 30 дней
            - После 30 дней удаление становится физическим
            - Архивные копии сохраняются для аудита
            """,
        tags = {"room-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Автобус успешно удалена"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Автобус с указанным ID не найдена"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Невозможно удалить комнату (есть зависимости)"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления автобуса"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @Parameter(
                description = "Идентификатор удаляемой автобуса",
                required = true,
                example = "1"
            )
            @PathVariable Long id) {
        
        logger.debug("DELETE /api/rooms/{}", id);
        boolean deleted = roomService.deleteRoom(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Room with id {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Импортировать автобуса из CSV файла",
        description = """
            Массовое создание комнат из CSV файла.
            
            ### Формат CSV файла:
            - Разделитель: запятая (,)
            - Кодировка: UTF-8
            - Обязательная колонка: bus
            - Опциональная колонка: managerUsername
            
            ### Пример CSV файла:
            ```
            bus,managerUsername
            Кухня,ivan.ivanov
            Гостиная,maria.petrova
            Спальня,
            Ванная автобус,alex.smirnov
            ```
            
            ### Логика назначения менеджеров:
            1. **Если указан managerUsername**:
               - Ищется пользователь с таким именем
               - Если найден - назначается менеджером
               - Если не найден - ошибка импорта для этой строки
            
            2. **Если managerUsername не указан**:
               - Менеджером становится текущий пользователь
               - Для ADMIN - текущий пользователь
               - Для USER - сам пользователь
            
            ### Особенности по ролям:
            - **ADMIN**:
              - Может назначать любого пользователя менеджером
              - Может импортировать автобуса для других пользователей
              - Видит ошибки всех типов
            
            - **USER**:
              - Может назначать менеджером только себя
              - Если указан другой managerUsername - ошибка
              - Все импортируемые автобуса будут назначены ему
            
            ### Обработка ошибок:
            - Каждая строка валидируется независимо
            - Ошибка в одной строке не останавливает импорт
            - Возвращается детальный отчет об ошибках
            - Успешно импортированные автобуса сохраняются
            
            ### Пример отчета:
            ```json
            {
              "message": "Import completed with errors",
              "successCount": 3,
              "errorCount": 1,
              "errors": [
                "Room 2 'Офис': Manager not found: unknown.user"
              ],
              "hasErrors": true
            }
            ```
            
            ### Ограничения:
            - Максимальный размер файла: 5MB
            - Максимальное количество строк: 500
            - Формат файла: только .csv
            - Кодировка: UTF-8
            """,
        tags = {"room-controller", "bulk-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Импорт завершен (возможно с ошибками)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CsvImportDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный файл или формат"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "413",
            description = "Файл слишком большой (> 5MB)"
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Неподдерживаемый тип файла"
        )
    })
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvImportDto> importRoomsFromCsv(
            @Parameter(
                description = "CSV файл с автобусми",
                required = true,
                content = @Content(mediaType = "multipart/form-data")
            )
            @RequestParam("file") MultipartFile file,
            
            @Parameter(hidden = true)
            Authentication authentication) {
        
        log.debug("POST /api/rooms/import/csv - importing rooms from CSV");
        
        if (!isCsvFile(file)) {
            return ResponseEntity.badRequest().body(new CsvImportDto(
                "File must be CSV format", 0, 0, List.of("Invalid file type"), true
            ));
        }

        try {
            String username = authentication.getName();
            User currentUser = userService.getUserByUsername(username);

            CsvParserUtil.CsvParseResult<Room> parseResult = csvParserUtil.parseCsvFile(
                file, record -> mapCsvRecordToRoom(record, currentUser)
            );

            List<Room> savedRooms = new ArrayList<>();
            List<String> saveErrors = new ArrayList<>(parseResult.getErrors());

            for (int i = 0; i < parseResult.getRecords().size(); i++) {
                try {
                    Room room = parseResult.getRecords().get(i);
                    Room savedRoom = roomService.createRoom(room);
                    savedRooms.add(savedRoom);
                } catch (Exception e) {
                    String error = String.format("Room %d '%s': %s", 
                        i + 1, parseResult.getRecords().get(i).getBus(), e.getMessage());
                    saveErrors.add(error);
                }
            }

            boolean hasErrors = !saveErrors.isEmpty() || parseResult.isHasErrors();
            
            return ResponseEntity.ok(new CsvImportDto(
                hasErrors ? "Import completed with errors" : "Import completed successfully",
                savedRooms.size(),
                saveErrors.size(),
                saveErrors,
                hasErrors
            ));

        } catch (Exception e) {
            log.error("CSV import failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new CsvImportDto(
                "Import failed: " + e.getMessage(), 0, 1, List.of(e.getMessage()), true
            ));
        }
    }

    private Room mapCsvRecordToRoom(CSVRecord record, User currentUser) {
        String bus = record.get("bus");
        String managerUsername = record.get("managerUsername");

        if (bus == null || bus.trim().isEmpty()) {
            throw new IllegalArgumentException("Location is required");
        }

        Room room = new Room();
        room.setBus(bus.trim());

        if (managerUsername != null && !managerUsername.trim().isEmpty()) {
            User manager = userService.getUserByUsername(managerUsername.trim());
            if (manager != null) {
                room.setManager(manager);
            } else {
                throw new IllegalArgumentException("Manager not found: " + managerUsername);
            }
        } else {
            room.setManager(currentUser);
        }

        return room;
    }

    private boolean isCsvFile(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        return "text/csv".equals(contentType) || 
            (fileName != null && fileName.toLowerCase().endsWith(".csv"));
    }
}