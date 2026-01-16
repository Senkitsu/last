package com.example.demo.controller;

import com.example.demo.service.DeviceControlService;
import com.example.demo.service.TemperatureService;

import lombok.extern.slf4j.Slf4j;

import com.example.demo.dto.DeviceResponseDto;
import com.example.demo.dto.DeviceToggleDto;
import com.example.demo.mapper.DeviceMapper;
import com.example.demo.model.Device;
import com.example.demo.model.DeviceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
@RequestMapping("/api/control")
@Tag(name = "device-control-controller", description = """
    Контроллер для управления умными устройствами и мониторинга энергопотребления.
    """)
public class DeviceControlController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceControlController.class);
    private final DeviceControlService deviceControlService;
    private final TemperatureService temperatureService;

    public DeviceControlController(DeviceControlService deviceControlService, 
                                 TemperatureService temperatureService) {
        this.deviceControlService = deviceControlService;
        this.temperatureService = temperatureService;
    }

    @Operation(
        summary = "Включение/выключение конкретного устройства",
        description = """
            Управляет состоянием отдельного устройства по его идентификатору.
            
            ### Процесс управления:
            1. Проверяется существование устройства
            2. Проверяются права пользователя на управление устройством
            3. Изменяется состояние устройства (active/inactive)
            4. Записывается лог изменения состояния
            5. Отправляется команда на физическое устройство (если подключено)
            
            ### Особенности:
            - Устройство должно быть связано с помещением пользователя
            - При выключении устройства прекращается потребление энергии
            - При включении начинается отсчет потребляемой мощности
            - Возвращается обновленное состояние устройства
            
            ### Примеры сценариев:
            - Включение света в спальне
            - Выключение кондиционера в гостиной
            - Перезагрузка умной розетки
            """,
        tags = {"device-control-controller"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Состояние устройства успешно изменено",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeviceResponseDto.class),
                examples = @ExampleObject(
                    name = "Успешное включение устройства",
                    value = """
                        {
                          "id": 123,
                          "name": "Свет в гостиной",
                          "type": "LIGHT",
                          "active": true,
                          "powerConsumption": 50.0,
                          "roomId": 1,
                          "roomName": "Гостиная",
                          "lastToggleTime": "2024-01-15T14:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверный формат запроса или отсутствует параметр active"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет прав на управление данным устройством"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Устройство с указанным ID не найдено"
        )
    })
    @PostMapping("/devices/{deviceId}/toggle")
    public ResponseEntity<DeviceResponseDto> toggleDevice(
            @Parameter(
                description = "Идентификатор устройства",
                required = true,
                example = "123",
                schema = @Schema(type = "integer", format = "int64")
            )
            @PathVariable Long deviceId,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Запрос на изменение состояния устройства",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeviceToggleDto.class),
                    examples = {
                        @ExampleObject(
                            name = "Включение устройства",
                            value = """
                                {
                                  "active": true
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Выключение устройства",
                            value = """
                                {
                                  "active": false
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody DeviceToggleDto request) {
        
        Boolean active = request.active();
        if (active == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Device device = deviceControlService.toggleDevice(deviceId, active);
        if (device != null) {
            return ResponseEntity.ok(DeviceMapper.toDto(device));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Автоматическое управление устройствами по температуре",
        description = """
            Анализирует текущую температуру в помещении и автоматически управляет устройствами.
            
            ### Алгоритм работы:
            1. Получает текущую температуру в указанном помещении
            2. Сравнивает с установленными пороговыми значениями
            3. Принимает решения об управлении устройствами:
               - При низкой температуре включаются обогреватели
               - При высокой температуре включаются кондиционеры
               - При нормальной температуре отключаются климатические устройства
            4. Возвращает отчет о выполненных действиях
            
            ### Параметры температуры:
            - **Комфортный диапазон**: 20-24°C (настраивается)
            - **Минимальная температура**: ниже 18°C → включение обогрева
            - **Максимальная температура**: выше 26°C → включение охлаждения
            
            ### Поддерживаемые устройства:
            - Обогреватели (HEATER)
            - Кондиционеры (AC)
            - Вентиляция (VENTILATION)
            """,
        tags = {"device-control-controller", "automation"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Управление выполнено успешно",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "string"),
                examples = {
                    @ExampleObject(
                        name = "Включен обогрев",
                        value = "Включены обогреватели в помещении Гостиная (температура: 17.5°C)"
                    ),
                    @ExampleObject(
                        name = "Включено охлаждение",
                        value = "Включены кондиционеры в помещении Офис (температура: 28.2°C)"
                    ),
                    @ExampleObject(
                        name = "Температура нормальная",
                        value = "Температура в норме (22.5°C). Климатические устройства отключены."
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверный формат данных (отсутствуют roomId или temperature)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Помещение с указанным ID не найдено"
        )
    })
    @PostMapping("/temperature")
    public ResponseEntity<String> controlByTemperature(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Данные о температуре в помещении",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        name = "Пример запроса",
                        value = """
                            {
                              "roomId": 1,
                              "temperature": 22.5
                            }
                            """
                    )
                )
            )
            @RequestBody Map<String, Object> request) {
        
        logger.debug("POST /api/control/temperature");
        Long roomId = Long.valueOf(request.get("roomId").toString());
        Double temperature = Double.valueOf(request.get("temperature").toString());
        
        String result = temperatureService.controlByTemperature(roomId, temperature);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Получение общего энергопотребления",
        description = """
            Рассчитывает суммарное энергопотребление всех активных устройств.
            
            ### Метод расчета:
            1. Получает список всех активных устройств
            2. Суммирует их текущую потребляемую мощность
            3. При необходимости конвертирует единицы измерения
            4. Возвращает общее значение в ваттах (Вт)
            
            ### Учет потребления:
            - Учитываются только активные (включенные) устройства
            - Мощность берется из настроек каждого устройства
            - Для некоторых устройств мощность может динамически меняться
            - Результат кэшируется на короткое время для производительности
            
            ### Пример использования:
            - Мониторинг энергопотребления дома/офиса
            - Оценка стоимости электроэнергии
            - Автоматическое отключение при превышении лимита
            """,
        tags = {"device-control-controller", "monitoring"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешно рассчитано общее энергопотребление",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Пример ответа",
                    value = """
                        {
                          "totalPower": 1250.75
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        )
    })
    @GetMapping("/power")
    public ResponseEntity<Map<String, Double>> getTotalPower() {
        logger.debug("GET /api/control/power");
        double totalPower = deviceControlService.getTotalPowerConsumption();
        return ResponseEntity.ok(Map.of("totalPower", totalPower));
    }

    @Operation(
        summary = "Групповое управление устройствами по типу",
        description = """
            Включает или выключает все устройства определенного типа.
            
            ### Особенности работы:
            1. Находит все устройства указанного типа
            2. Проверяет права пользователя на каждое устройство
            3. Массово изменяет состояние устройств
            4. Возвращает список обновленных устройств
            
            ### Примеры использования:
            - Выключить все светильники при выходе из дома
            - Включить все обогреватели вечером
            - Отключить все бытовые приборы при срабатывании датчика дыма
            
            ### Возможные типы устройств:
            - `LIGHT` - управление освещением
            - `HEATER` - управление отоплением
            - `AC` - управление кондиционерами
            - `VENTILATION` - управление вентиляцией
            - `APPLIANCE` - управление бытовой техникой
            """,
        tags = {"device-control-controller", "bulk-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Состояние устройств успешно изменено",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = DeviceResponseDto.class, type = "array")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Неверный тип устройства или отсутствует параметр active"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        )
    })
    @PostMapping("/type/{type}")
    public ResponseEntity<List<DeviceResponseDto>> toggleDevicesByType(
            @Parameter(
                description = "Тип устройств для управления",
                required = true,
                schema = @Schema(implementation = DeviceType.class),
                examples = {
                    @ExampleObject(name = "Освещение", value = "LIGHT"),
                    @ExampleObject(name = "Обогреватели", value = "HEATER"),
                    @ExampleObject(name = "Кондиционеры", value = "AC")
                }
            )
            @PathVariable DeviceType type,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Запрос на изменение состояния устройств",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DeviceToggleDto.class)
                )
            )
            @RequestBody DeviceToggleDto request) {
        
        Boolean active = request.active();
        if (active == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Device> devices = deviceControlService.toggleDevicesByType(type, active);
        List<DeviceResponseDto> deviceDtos = devices.stream()
                .map(DeviceMapper::toDto)
                .toList();
        return ResponseEntity.ok(deviceDtos);
    }
}