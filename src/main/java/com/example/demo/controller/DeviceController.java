package com.example.demo.controller;

import com.example.demo.dto.CsvImportDto;
import com.example.demo.dto.DeviceRequestDto;
import com.example.demo.dto.DeviceResponseDto;
import com.example.demo.mapper.DeviceMapper;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceType;
import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.service.CsvParserUtil;
import com.example.demo.service.DeviceService;
import com.example.demo.service.RoomService;
import com.example.demo.service.UserService;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@Tag(name = "device-management-controller", description = """
    Контроллер для управления устройствами системы умного дома.
    """)
public class DeviceController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);
    private final DeviceService deviceService;
    private final UserService userService;
    private final DeviceRepository deviceRepository;
    private final RoomService roomService;
    private final CsvParserUtil csvParserUtil;
    
    public DeviceController(DeviceService deviceService, UserService userService, 
    DeviceRepository deviceRepository, RoomService roomService, CsvParserUtil csvParserUtil) {
        this.deviceService = deviceService;
        this.userService = userService;
        this.deviceRepository = deviceRepository;
        this.roomService = roomService;
        this.csvParserUtil = csvParserUtil;
    }
    
    @Operation(
        summary = "Получить список устройств с фильтрацией и пагинацией",
        description = """
            Возвращает постраничный список устройств с поддержкой фильтрации.
            
            ### Фильтры (все опциональны):
            - **title** - поиск по названию устройства (регистронезависимый, частичное совпадение)
            - **type** - фильтр по типу устройства (LIGHT, HEATER, AC, VENTILATION, APPLIANCE)
            - **minPower/maxPower** - фильтр по диапазону мощности (Вт)
            - **active** - фильтр по статусу активности
            
            ### Пагинация:
            - По умолчанию: страница 0, размер 3, сортировка по title
            - Поддерживаются параметры: page, size, sort
            - Пример: `/api/devices?page=0&size=10&sort=power,desc`
            
            ### Права доступа:
            - **USER**: видит только устройства в своих комнатах
            - **ADMIN**: видит все устройства системы
            
            ### Примеры использования:
            - Получить все активные светильники: `/api/devices?type=LIGHT&active=true`
            - Найти устройства мощностью от 100 до 1000 Вт: `/api/devices?minPower=100&maxPower=1000`
            - Поиск по названию: `/api/devices?title=кондиционер`
            """,
        tags = {"device-management-controller", "read-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список устройств успешно получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные параметры фильтрации или пагинации"
        )
    })
    @GetMapping
    public ResponseEntity<Page<DeviceResponseDto>> getAllDevices(
            @Parameter(
                description = "Название устройства (частичное совпадение)",
                example = "свет"
            )
            @RequestParam(required = false) String title,
            
            @Parameter(
                description = "Тип устройства",
                schema = @Schema(implementation = DeviceType.class)
            )
            @RequestParam(required = false) DeviceType type,
            
            @Parameter(
                description = "Минимальная мощность (Вт)",
                example = "100.0"
            )
            @RequestParam(required = false) Double minPower,
            
            @Parameter(
                description = "Максимальная мощность (Вт)",
                example = "1000.0"
            )
            @RequestParam(required = false) Double maxPower,
            
            @Parameter(
                description = "Статус активности",
                example = "true"
            )
            @RequestParam(required = false) Boolean active,
            
            @Parameter(hidden = true)
            Authentication authentication,
            
            @Parameter(
                description = "Параметры пагинации",
                schema = @Schema(
                    type = "object",
                    example = "{\"page\": 0, \"size\": 10, \"sort\": [\"title,asc\"]}"
                )
            )
            @PageableDefault(page = 0, size = 3, sort = "title") Pageable pageable) {

        String username = authentication.getName();
        User user = userService.getUserByUsername(username);
        
        Page<Device> devices;
        
        if (user.getRole().getName().equals("USER")) {
            devices = deviceService.getDevicesByUserRoomsWithFilter(
                user.getId(), title, type, minPower, maxPower, active, pageable);
        } else {
            devices = deviceRepository.findAll(pageable);
        }
        
        Page<DeviceResponseDto> deviceDtos = devices.map(DeviceMapper::toDto);
        return ResponseEntity.ok(deviceDtos);
    }
    
    @Operation(
        summary = "Получить устройство по ID",
        description = """
            Возвращает подробную информацию об устройстве по его идентификатору.
            
            ### Возвращаемая информация:
            - Основные данные устройства (название, тип, мощность)
            - Статус активности
            - Информация о комнате (если устройство привязано)
            - Время последнего изменения
            
            ### Обработка ошибок:
            - Если устройство не найдено → 404 Not Found
            - Если нет прав доступа → 403 Forbidden
            
            ### Пример использования:
            Получение информации о конкретном устройстве для отображения на дашборде
            """,
        tags = {"device-management-controller", "read-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Устройство найдено",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeviceResponseDto.class),
                examples = @ExampleObject(
                    name = "Пример ответа",
                    value = """
                        {
                          "id": 123,
                          "title": "Умный светильник в гостиной",
                          "type": "LIGHT",
                          "power": 50.0,
                          "active": true,
                          "roomId": 1,
                          "roomName": "Гостиная",
                          "createdAt": "2024-01-10T10:30:00Z",
                          "updatedAt": "2024-01-15T14:25:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Устройство с указанным ID не найдено"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав доступа к данному устройству"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> getDeviceById(
            @Parameter(
                description = "Идентификатор устройства",
                required = true,
                example = "123"
            )
            @PathVariable Long id) {
        
        Device device = deviceService.getDeviceById(id);
        if (device != null) {
            return ResponseEntity.ok(DeviceMapper.toDto(device));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Создать новое устройство",
        description = """
            Создает новое устройство в системе умного дома.
            
            ### Обязательные поля:
            - **title** - название устройства (строка, не пустая)
            - **type** - тип устройства (enum: LIGHT, HEATER, AC, VENTILATION, APPLIANCE)
            - **power** - потребляемая мощность в ваттах (число с плавающей точкой)
            - **active** - начальное состояние устройства (boolean)
            
            ### Опциональные поля:
            - **roomId** - идентификатор комнаты для привязки устройства
            
            ### Валидация:
            - Мощность не может быть отрицательной
            - Название должно быть уникальным в рамках системы
            - Комната должна существовать, если указана
            - Тип должен быть из допустимых значений
            
            ### Примеры использования:
            - Добавление нового умного светильника
            - Создание обогревателя с привязкой к комнате
            """,
        tags = {"device-management-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Устройство успешно создано",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeviceResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные устройства или нарушение валидации"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для создания устройства"
        )
    })
    @PostMapping
    public ResponseEntity<DeviceResponseDto> createDevice(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Данные нового устройства",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeviceRequestDto.class),
                    examples = {
                        @ExampleObject(
                            name = "Создание светильника",
                            value = """
                                {
                                  "title": "Умная лампа в спальне",
                                  "type": "LIGHT",
                                  "power": 25.5,
                                  "active": false,
                                  "roomId": 2
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Создание обогревателя без привязки",
                            value = """
                                {
                                  "title": "Мобильный обогреватель",
                                  "type": "HEATER",
                                  "power": 1500.0,
                                  "active": true
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody DeviceRequestDto deviceRequest) {
        
        logger.debug("POST /api/devices - creating device: {}", deviceRequest);
        try {
            Device device = new Device();
            device.setTitle(deviceRequest.title());
            device.setType(deviceRequest.type());
            device.setPower(deviceRequest.power());
            device.setActive(deviceRequest.active());
            
            if (deviceRequest.roomId() != null) {
                Room room = roomService.getRoomById(deviceRequest.roomId());
                if (room == null) {
                    logger.warn("Room with id {} not found", deviceRequest.roomId());
                    return ResponseEntity.badRequest().build();
                }
                device.setRoom(room);
            }
            
            Device createdDevice = deviceService.createDevice(device);
            return ResponseEntity.status(HttpStatus.CREATED).body(DeviceMapper.toDto(createdDevice));
            
        } catch (Exception e) {
            logger.error("Error creating device: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Обновить существующее устройство",
        description = """
            Обновляет данные существующего устройства.
            
            ### Особенности:
            - Можно обновить все поля устройства
            - Поддерживается изменение привязки к комнате
            - Можно отвязать устройство от комнаты (указать null)
            - Все поля опциональны (не указанные поля остаются без изменений)
            
            ### Ограничения:
            - Нельзя изменить ID устройства
            - Требуются права на управление устройством
            - Проверка уникальности названия (если изменяется)
            
            ### Примеры использования:
            - Переименование устройства
            - Изменение мощности устройства
            - Перемещение устройства в другую комнату
            """,
        tags = {"device-management-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Устройство успешно обновлено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Устройство с указанным ID не найдено"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверные данные для обновления"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав на обновление устройства"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> updateDevice(
            @Parameter(
                description = "Идентификатор обновляемого устройства",
                required = true,
                example = "123"
            )
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Новые данные устройства",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeviceRequestDto.class)
                )
            )
            @RequestBody DeviceRequestDto deviceRequest) {
        
        logger.debug("PUT /api/devices/{} - updating device: {}", id, deviceRequest);
        
        try {
            Device deviceDetails = new Device();
            deviceDetails.setTitle(deviceRequest.title());
            deviceDetails.setType(deviceRequest.type());
            deviceDetails.setPower(deviceRequest.power());
            deviceDetails.setActive(deviceRequest.active());
            
            if (deviceRequest.roomId() != null) {
                Room room = roomService.getRoomById(deviceRequest.roomId());
                if (room == null) {
                    logger.warn("Room with id {} not found", deviceRequest.roomId());
                    return ResponseEntity.badRequest().build();
                }
                deviceDetails.setRoom(room);
            }
            
            Device updatedDevice = deviceService.updateDevice(id, deviceDetails);
            if (updatedDevice != null) {
                return ResponseEntity.ok(DeviceMapper.toDto(updatedDevice));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error updating device: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
        summary = "Удалить устройство",
        description = """
            Удаляет устройство из системы.
            
            ### Особенности удаления:
            - Удаление происходит мягкое (soft delete) - устройство помечается как удаленное
            - История изменений сохраняется для аудита
            - При удалении автоматически отвязываются все связи
            - Восстановление возможно только администратором
            
            ### Проверки:
            - Устройство должно существовать
            - У пользователя должны быть права на удаление
            - Устройство не должно быть активно в момент удаления
            
            ### Примеры использования:
            - Удаление старого или неиспользуемого оборудования
            - Очистка тестовых данных
            """,
        tags = {"device-management-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Устройство успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Устройство с указанным ID не найдено"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Устройство активно или используется в других процессах"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав на удаление устройства"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @Parameter(
                description = "Идентификатор удаляемого устройства",
                required = true,
                example = "123"
            )
            @PathVariable Long id) {
        
        logger.debug("DELETE /api/devices/{}", id);
        boolean deleted = deviceService.deleteDevice(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Device ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Импорт устройств из CSV файла",
        description = """
            Массовый импорт устройств из CSV файла.
            
            ### Поддерживаемый формат CSV:
            - Разделитель: запятая (,)
            - Кодировка: UTF-8
            - Обязательные колонки: title, type
            - Опциональные колонки: power, active, roomBus
            
            ### Пример CSV файла:
            ```
            title,type,power,active,roomBus
            Свет в гостиной,LIGHT,50.0,true,Гостиная
            Обогреватель,HEATER,1500.0,false,Спальня
            Кондиционер,AC,2000.0,true,
            ```
            
            ### Обработка ошибок:
            - Валидация каждой строки CSV
            - Продолжение импорта после ошибок
            - Детальный отчет об успешных и неуспешных операциях
            - Поддержка частичного импорта
            
            ### Лимиты:
            - Максимальный размер файла: 10MB
            - Максимальное количество строк: 1000
            - Поддерживаемые форматы: .csv, text/csv
            
            ### Возвращаемый отчет:
            - Количество успешно импортированных устройств
            - Количество ошибок
            - Детальный список ошибок (если есть)
            """,
        tags = {"device-management-controller", "bulk-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Импорт завершен (возможно с ошибками)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CsvImportDto.class),
                examples = {
                    @ExampleObject(
                        name = "Успешный импорт",
                        value = """
                            {
                              "message": "Import completed successfully",
                              "successCount": 50,
                              "errorCount": 0,
                              "errors": [],
                              "hasErrors": false
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Импорт с ошибками",
                        value = """
                            {
                              "message": "Import completed with errors",
                              "successCount": 45,
                              "errorCount": 5,
                              "errors": [
                                "Device 10 'Некорректное устройство': Invalid device type: UNKNOWN",
                                "Device 25 'Устройство без названия': Title is required"
                              ],
                              "hasErrors": true
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный файл (не CSV или поврежден)"
        ),
        @ApiResponse(
            responseCode = "413",
            description = "Файл слишком большой (> 10MB)"
        ),
        @ApiResponse(
            responseCode = "415",
            description = "Неподдерживаемый тип файла"
        )
    })
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CsvImportDto> importDevicesFromCsv(
            @Parameter(
                description = "CSV файл с устройствами",
                required = true,
                content = @Content(mediaType = "multipart/form-data")
            )
            @RequestParam("file") MultipartFile file,
            
            @Parameter(hidden = true)
            Authentication authentication) {
        
        log.debug("POST /api/devices/import/csv - importing devices from CSV");
        
        if (!isCsvFile(file)) {
            return ResponseEntity.badRequest().body(new CsvImportDto(
                "File must be CSV format", 0, 0, List.of("Invalid file type"), true
            ));
        }

        try {
            CsvParserUtil.CsvParseResult<Device> parseResult = csvParserUtil.parseCsvFile(
                file, this::mapCsvRecordToDevice
            );

            List<Device> savedDevices = new ArrayList<>();
            List<String> saveErrors = new ArrayList<>(parseResult.getErrors());

            for (int i = 0; i < parseResult.getRecords().size(); i++) {
                try {
                    Device device = parseResult.getRecords().get(i);
                    Device savedDevice = deviceService.createDevice(device);
                    savedDevices.add(savedDevice);
                } catch (Exception e) {
                    String error = String.format("Device %d '%s': %s", 
                        i + 1, parseResult.getRecords().get(i).getTitle(), e.getMessage());
                    saveErrors.add(error);
                }
            }

            boolean hasErrors = !saveErrors.isEmpty() || parseResult.isHasErrors();
            
            if (hasErrors) {
                return ResponseEntity.ok(new CsvImportDto(
                    "Import completed with errors",
                    savedDevices.size(),
                    saveErrors.size(),
                    saveErrors,
                    true
                ));
            } else {
                return ResponseEntity.ok(new CsvImportDto(
                    "Import completed successfully",
                    savedDevices.size(),
                    0,
                    List.of(),
                    false
                ));
            }

        } catch (Exception e) {
            log.error("CSV import failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new CsvImportDto(
                "Import failed: " + e.getMessage(), 0, 1, List.of(e.getMessage()), true
            ));
        }
    }

    private Device mapCsvRecordToDevice(CSVRecord record) {
        String title = record.get("title");
        String typeStr = record.get("type");
        String powerStr = record.get("power");
        String activeStr = record.get("active");
        String roomBus = record.get("roomBus");

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (typeStr == null || typeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Type is required");
        }

        DeviceType type;
        try {
            type = DeviceType.valueOf(typeStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid device type: " + typeStr);
        }

        double power = 0.0;
        if (powerStr != null && !powerStr.trim().isEmpty()) {
            try {
                power = Double.parseDouble(powerStr.trim());
                if (power < 0) {
                    throw new IllegalArgumentException("Power cannot be negative");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid power value: " + powerStr);
            }
        }

        boolean active = false;
        if (activeStr != null && !activeStr.trim().isEmpty()) {
            String activeLower = activeStr.trim().toLowerCase();
            if ("true".equals(activeLower) || "1".equals(activeLower) || "yes".equals(activeLower)) {
                active = true;
            } else if (!"false".equals(activeLower) && !"0".equals(activeLower) && !"no".equals(activeLower)) {
                throw new IllegalArgumentException("Invalid active value: " + activeStr);
            }
        }

        Device device = new Device();
        device.setTitle(title.trim());
        device.setType(type);
        device.setPower(power);
        device.setActive(active);

        if (roomBus != null && !roomBus.trim().isEmpty()) {
            List<Room> rooms = roomService.getRoomByBus(roomBus.trim());
            if (!rooms.isEmpty()) {
                device.setRoom(rooms.get(0));
            } else {
                throw new IllegalArgumentException("Room not found with bus: " + roomBus);
            }
        }

        return device;
    }

    private boolean isCsvFile(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        return "text/csv".equals(contentType) || 
            (fileName != null && fileName.toLowerCase().endsWith(".csv"));
    }
}