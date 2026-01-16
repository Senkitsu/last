package com.example.demo.controller;

import com.example.demo.service.ModeService;

import lombok.extern.slf4j.Slf4j;

import com.example.demo.model.ModeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Slf4j
@RestController
@RequestMapping("/api/modes")
@Tag(name = "mode-controller", description = """
    Контроллер для управления сценариями и режимами работы умного дома.
    """)
public class ModeController {
    private final ModeService modeService;
    private static final Logger logger = LoggerFactory.getLogger(ModeController.class);

    public ModeController(ModeService modeService) {
        this.modeService = modeService;
    }

    @Operation(
        summary = "Активировать режим работы умного дома",
        description = """
            Включает указанный режим работы системы умного дома.
            
            ### Процесс активации:
            1. Проверяется возможность активации режима
            2. Сохраняются текущие настройки устройств (для возможности отката)
            3. Применяются предустановленные настройки для режима
            4. Отправляются команды на соответствующие устройства
            5. Режим записывается как активный
            
            ### Поддерживаемые режимы:
            - **ECONOMY** - экономный режим:
              - Минимальное освещение
              - Отключение неиспользуемых устройств
              - Оптимизация температуры
              - Снижение энергопотребления на 30-50%
            
            - **COMFORT** - комфортный режим:
              - Оптимальное освещение
              - Поддержание комфортной температуры
              - Фоновое освещение в проходах
              - Включение необходимых устройств
            
            - **GUEST** - гостевой режим:
              - Ограниченный доступ к управлению
              - Основные устройства доступны
              - Гостевой Wi-Fi включен
              - Безопасные настройки
            
            - **VACATION** - режим отпуска:
              - Симуляция присутствия людей
              - Случайное включение/выключение света
              - Минимальное энергопотребление
              - Безопасность и охрана
            
            - **PARTY** - праздничный режим:
              - Специальное освещение (цветные лампы)
              - Фоновая музыка
              - Комфортная температура
              - Декоративная подсветка
            
            ### Ограничения:
            - Некоторые режимы несовместимы между собой
            - Ночной режим имеет высший приоритет
            - Для активации некоторых режимов требуются специальные устройства
            """,
        tags = {"mode-controller", "automation"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Режим успешно активирован",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string"),
                examples = {
                    @ExampleObject(
                        name = "Активация экономного режима",
                        value = "Экономный режим активирован. Отключены 5 устройств, снижена яркость освещения."
                    ),
                    @ExampleObject(
                        name = "Активация гостевого режима",
                        value = "Гостевой режим активирован. Доступ предоставлен к 8 основным устройствам."
                    ),
                    @ExampleObject(
                        name = "Активация режима отпуска",
                        value = "Режим 'Отпуск' активирован. Симуляция присутствия включена на 14 дней."
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Недопустимый тип режима или режим не может быть активирован"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для активации данного режима"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Конфликт режимов: невозможно активировать одновременно с текущим активным режимом"
        )
    })
    @PostMapping("/{modeType}/activate")
    public ResponseEntity<String> activateMode(
            @Parameter(
                description = "Тип режима для активации",
                required = true,
                schema = @Schema(implementation = ModeType.class),
                examples = {
                    @ExampleObject(name = "Экономный", value = "ECONOMY"),
                    @ExampleObject(name = "Комфортный", value = "COMFORT"),
                    @ExampleObject(name = "Гостевой", value = "GUEST"),
                    @ExampleObject(name = "Отпуск", value = "VACATION"),
                    @ExampleObject(name = "Праздничный", value = "PARTY")
                }
            )
            @PathVariable ModeType modeType) {
        
        logger.debug("POST/{modeType}/activate");
        String result = modeService.activateMode(modeType);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Активировать ночной режим",
        description = """
            Включает специальный ночной режим работы системы.
            
            ### Особенности ночного режима:
            1. **Освещение**:
               - Основной свет отключается
               - Включается ночное дежурное освещение (светодиодные ленты, ночники)
               - Яркость снижается до минимальной
               - В спальнях оставляются только ночники
            
            2. **Температура**:
               - Поддерживается комфортная для сна температура (18-20°C)
               - Отключаются обогреватели и кондиционеры
               - Включается режим энергосбережения климатических систем
            
            3. **Безопасность**:
               - Включается охранный режим
               - Датчики движения настраиваются на чувствительный режим
               - Камеры переходят в ночной режим съемки
            
            4. **Устройства**:
               - Отключаются неиспользуемые устройства
               - Зарядные устройства переводятся в экономичный режим
               - Умные розетки отключают питание неважных приборов
            
            5. **Автоматизация**:
               - Режим автоматически включается по расписанию (например, с 23:00 до 7:00)
               - Можно настроить индивидуальное время для разных дней недели
               - Поддерживается отложенная активация
            
            ### Преимущества:
            - Экономия энергии до 60%
            - Создание идеальных условий для сна
            - Повышение безопасности в ночное время
            - Продление срока службы устройств
            """,
        tags = {"mode-controller", "automation", "night-mode"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Ночной режим успешно активирован",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(
                    value = "Ночной режим активирован. Отключено 12 устройств, включено ночное освещение, установлена температура 19°C."
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Ночной режим уже активен или не может быть активирован"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Ошибка при настройке устройств для ночного режима"
        )
    })
    @PostMapping("/night")
    public ResponseEntity<String> activateNightMode() {
        logger.debug("POST/night");
        String result = modeService.activateNightMode();
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Выключить все устройства",
        description = """
            Мгновенно отключает все устройства в системе умного дома.
            
            ### Особенности работы:
            1. **Приоритетное отключение**:
               - Сначала отключаются энергоемкие устройства (обогреватели, кондиционеры)
               - Затем освещение
               - В последнюю очередь - системы безопасности
            
            2. **Исключения** (не отключаются):
               - Критические системы (охранная сигнализация, пожарные датчики)
               - Холодильники и морозильные камеры
               - Медицинское оборудование (если настроено)
               - Серверное оборудование
            
            3. **Безопасность**:
               - Поэтапное отключение для избежания скачков напряжения
               - Проверка состояния устройств перед отключением
               - Логирование всех операций отключения
            
            4. **Восстановление**:
               - Сохраняются текущие настройки каждого устройства
               - Можно восстановить предыдущее состояние командой "all-on"
               - История отключений доступна в журнале
            
            ### Использование:
            - Перед длительным отсутствием
            - В случае аварийной ситуации
            - Для экономии энергии
            - Перед проведением работ с электричеством
            
            ### Предупреждение:
            - Операция необратима без команды включения
            - Некоторые устройства могут требовать ручного включения
            - Системы безопасности остаются активными
            """,
        tags = {"mode-controller", "emergency", "power-management"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все устройства успешно выключены",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(
                    value = "Все устройства выключены. Отключено 25 устройств, сохранены настройки, системы безопасности активны."
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для выполнения операции (требуется подтверждение)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Ошибка при отключении некоторых устройств"
        )
    })
    @PostMapping("/all-off")
    public ResponseEntity<String> turnOffAll() {
        logger.debug("POST/all-off");
        String result = modeService.turnOffAllDevices();
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Включить все устройства",
        description = """
            Включает все устройства в системе умного дома в стандартном режиме.
            
            ### Процесс включения:
            1. **Последовательное включение**:
               - Сначала включаются системы безопасности и инфраструктура
               - Затем освещение в коридорах и общих зонах
               - Потом климатические системы
               - В последнюю очередь - бытовая техника
            
            2. **Восстановление настроек**:
               - Устройства возвращаются к своим последним сохраненным настройкам
               - Освещение включается с предыдущей яркостью
               - Температура устанавливается согласно расписанию
               - Приборы запускаются в стандартном режиме
            
            3. **Проверки**:
               - Проверка доступности устройств
               - Проверка электрической нагрузки
               - Валидация корректности работы
               - Тестирование связи с устройствами
            
            4. **Исключения** (устройства, которые не включаются):
               - Устройства, отмеченные как "не включать автоматически"
               - Неисправные устройства (требуют ремонта)
               - Устройства с истекшим сроком службы
               - Заблокированные администратором устройства
            
            ### Использование:
            - После возвращения домой
            - Утром после ночного режима
            - После отключения электричества
            - Для тестирования системы
            
            ### Особенности:
            - Включение происходит постепенно для избежания перегрузки сети
            - Устройства с проблемами пропускаются (не блокируют процесс)
            - Отчет о включенных/невключенных устройствах
            """,
        tags = {"mode-controller", "power-management", "recovery"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все устройства успешно включены",
            content = @Content(
                mediaType = "text/plain",
                examples = {
                    @ExampleObject(
                        name = "Полное включение",
                        value = "Все устройства включены. Успешно: 28 устройств, не включено: 2 устройства (требуют внимания)."
                    ),
                    @ExampleObject(
                        name = "Частичное включение",
                        value = "Включено 25 из 30 устройств. 3 устройства не отвечают, 2 отключены администратором."
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Слишком частые попытки включения (лимит: 1 раз в 5 минут)"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Критическая ошибка при включении устройств"
        )
    })  
    @PostMapping("/all-on")
    public ResponseEntity<String> turnOnAll() {
        logger.debug("POST/all-on");
        String result = modeService.turnOnAllDevices();
        return ResponseEntity.ok(result);
    }
}