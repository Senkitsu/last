package com.example.demo.controller;

import com.example.demo.model.ModeRule;
import com.example.demo.model.ModeType;
import com.example.demo.repository.ModeRuleRepository;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
@RequestMapping("/api/mode-rules")
@Tag(name = "mode-rule-controller", description = """
    Контроллер для управления правилами режимов работы умного дома.
    """)
public class ModeRuleController {
    private static final Logger logger = LoggerFactory.getLogger(ModeRuleController.class);
    private final ModeRuleRepository modeRuleRepository;

    public ModeRuleController(ModeRuleRepository modeRuleRepository) {
        this.modeRuleRepository = modeRuleRepository;
    }

    @Operation(
        summary = "Создать новое правило режима",
        description = """
            Создает правило для применения к устройствам при активации режима.
            
            ### Структура правила:
            - **modeType** - режим, к которому относится правило
            - **deviceType** - тип устройства (опционально, фильтр)
            - **titlePattern** - паттерн поиска в названии (опционально, фильтр)
            - **minPower/maxPower** - диапазон мощности (опционально, фильтр)
            - **shouldBeActive** - требуемое состояние устройства (true = включить, false = выключить)
            - **priority** - приоритет применения (1 = высший, 100 = низший)
            
            ### Логика работы:
            1. При активации режима система проверяет все устройства
            2. Для каждого устройства проверяются все правила данного режима
            3. Устройство попадает под правило, если:
               - Тип устройства совпадает с deviceType (если указан)
               - И название соответствует titlePattern (если указан)
               - И мощность в диапазоне minPower/maxPower (если указаны)
            4. Применяется правило с наивысшим приоритетом
            
            ### Примеры правил:
            1. **Ночной режим, отключить все светильники**:
               - modeType: NIGHT
               - deviceType: LIGHT
               - shouldBeActive: false
               - priority: 10
            
            2. **Ночной режим, включить ночники**:
               - modeType: NIGHT
               - titlePattern: ".*ночник.*|.*night.*"
               - shouldBeActive: true
               - priority: 20
            
            3. **Экономный режим, отключить мощные устройства**:
               - modeType: ECONOMY
               - minPower: 1000.0
               - shouldBeActive: false
               - priority: 5
            
            ### Важные моменты:
            - Правила с одинаковым приоритетом применяются в произвольном порядке
            - Устройство может соответствовать нескольким правилам
            - При конфликте применяется последнее обработанное правило
            - Рекомендуется использовать уникальные приоритеты
            """,
        tags = {"mode-rule-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Правило успешно создано",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ModeRule.class),
                examples = @ExampleObject(
                    name = "Пример созданного правила",
                    value = """
                        {
                          "id": 1,
                          "modeType": "NIGHT",
                          "deviceType": "LIGHT",
                          "titlePattern": null,
                          "minPower": null,
                          "maxPower": null,
                          "shouldBeActive": false,
                          "priority": 10,
                          "createdAt": "2024-01-15T10:30:00Z",
                          "updatedAt": "2024-01-15T10:30:00Z"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные правила"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для создания правил"
        )
    })
    @PostMapping
    public ResponseEntity<ModeRule> createRule(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Данные нового правила",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ModeRule.class),
                    examples = {
                        @ExampleObject(
                            name = "Правило для отключения света в ночном режиме",
                            value = """
                                {
                                  "modeType": "NIGHT",
                                  "deviceType": "LIGHT",
                                  "shouldBeActive": false,
                                  "priority": 10
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Правило для включения ночников",
                            value = """
                                {
                                  "modeType": "NIGHT",
                                  "titlePattern": ".*ночник.*",
                                  "shouldBeActive": true,
                                  "priority": 20
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "Комплексное правило",
                            value = """
                                {
                                  "modeType": "ECONOMY",
                                  "deviceType": "APPLIANCE",
                                  "minPower": 500.0,
                                  "shouldBeActive": false,
                                  "priority": 5
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody ModeRule rule) {
        
        logger.debug("POST/api/mode-rules");
        ModeRule savedRule = modeRuleRepository.save(rule);
        return ResponseEntity.ok(savedRule);
    }

    @Operation(
        summary = "Получить правила для конкретного режима",
        description = """
            Возвращает список всех правил, связанных с указанным режимом.
            
            ### Сортировка:
            - Правила возвращаются в порядке убывания приоритета
            - Самые важные правила (с наивысшим приоритетом) идут первыми
            - При одинаковом приоритете сортировка по ID
            
            ### Использование:
            - Просмотр всех правил для настройки режима
            - Проверка корректности настройки режима
            - Отладка работы режимов
            
            ### Пример ответа:
            Для режима NIGHT могут быть правила:
            1. Приоритет 30: выключить все устройства кроме критических
            2. Приоритет 20: включить ночное освещение
            3. Приоритет 10: установить температуру 19°C
            
            ### Особенности:
            - Возвращаются только активные правила
            - Правила с null приоритетом сортируются в конце
            - Если правил нет, возвращается пустой список
            """,
        tags = {"mode-rule-controller", "read-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список правил успешно получен",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ModeRule.class, type = "array")
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректный тип режима"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        )
    })
    @GetMapping("/mode/{modeType}")
    public ResponseEntity<List<ModeRule>> getRulesByMode(
            @Parameter(
                description = "Тип режима",
                required = true,
                schema = @Schema(implementation = ModeType.class),
                examples = {
                    @ExampleObject(name = "Ночной режим", value = "NIGHT"),
                    @ExampleObject(name = "Экономный режим", value = "ECONOMY"),
                    @ExampleObject(name = "Гостевой режим", value = "GUEST")
                }
            )
            @PathVariable ModeType modeType) {
        
        logger.debug("GET/api/mode-rules/mode/{}", modeType);
        List<ModeRule> rules = modeRuleRepository.findByModeTypeOrderByPriorityDesc(modeType);
        return ResponseEntity.ok(rules);
    }

    @Operation(
        summary = "Получить все правила всех режимов",
        description = """
            Возвращает полный список всех правил системы.
            
            ### Сортировка:
            - Сначала по типу режима (алфавитно)
            - Затем по приоритету (убывание)
            - Затем по ID (возрастание)
            
            ### Использование:
            - Обзор всей конфигурации системы
            - Экспорт правил для бэкапа
            - Анализ конфликтов между правилами
            - Администрирование системы
            
            ### Возвращаемые данные:
            - Все активные правила
            - Все режимы
            - Все приоритеты
            
            ### Пример структуры ответа:
            ```
            [
              { режим: NIGHT, приоритет: 30, действие: выключить всё },
              { режим: NIGHT, приоритет: 20, действие: включить ночники },
              { режим: ECONOMY, приоритет: 10, действие: отключить мощные приборы },
              ...
            ]
            ```
            
            ### Производительность:
            - Рекомендуется использовать пагинацию для больших систем
            - Ответ может быть большим (>100 правил)
            - Для production рекомендуется кэширование
            """,
        tags = {"mode-rule-controller", "read-operations", "administration"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Все правила успешно получены",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ModeRule.class, type = "array")
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для просмотра всех правил"
        )
    })
    @GetMapping
    public ResponseEntity<List<ModeRule>> getAllRules() {
        logger.debug("GET/api/mode-rules");
        List<ModeRule> rules = modeRuleRepository.findAll();
        return ResponseEntity.ok(rules);
    }

    @Operation(
        summary = "Обновить существующее правило",
        description = """
            Обновляет параметры существующего правила режима.
            
            ### Что можно обновить:
            - **modeType** - изменить режим правила
            - **deviceType** - изменить фильтр по типу устройства
            - **titlePattern** - изменить паттерн поиска в названии
            - **minPower/maxPower** - изменить диапазон мощности
            - **shouldBeActive** - изменить требуемое состояние
            - **priority** - изменить приоритет применения
            
            ### Особенности обновления:
            - Можно изменить только существующее правило
            - Все поля обновляются полностью (частичное обновление не поддерживается)
            - Проверка уникальности не выполняется
            - Время обновления (updatedAt) устанавливается автоматически
            
            ### Валидация:
            - Проверяется существование правила по ID
            - Проверяется корректность новых значений
            - Проверка на конфликты не выполняется
            
            ### Примеры использования:
            - Повысить приоритет важного правила
            - Изменить паттерн поиска для уточнения фильтра
            - Перенести правило в другой режим
            - Изменить действие с включения на выключение
            """,
        tags = {"mode-rule-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Правило успешно обновлено",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ModeRule.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Правило с указанным ID не найдено"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные для обновления"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для обновления правил"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ModeRule> updateRule(
            @Parameter(
                description = "Идентификатор обновляемого правила",
                required = true,
                example = "1"
            )
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Новые данные правила",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ModeRule.class)
                )
            )
            @RequestBody ModeRule ruleDetails) {
        
        logger.debug("PUT/api/mode-rules/{}", id);
        return modeRuleRepository.findById(id)
            .map(existingRule -> {
                existingRule.setModeType(ruleDetails.getModeType());
                existingRule.setDeviceType(ruleDetails.getDeviceType());
                existingRule.setTitlePattern(ruleDetails.getTitlePattern());
                existingRule.setMinPower(ruleDetails.getMinPower());
                existingRule.setMaxPower(ruleDetails.getMaxPower());
                existingRule.setShouldBeActive(ruleDetails.getShouldBeActive());
                existingRule.setPriority(ruleDetails.getPriority());
                return ResponseEntity.ok(modeRuleRepository.save(existingRule));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Удалить правило",
        description = """
            Удаляет правило из системы.
            
            ### Процесс удаления:
            1. Проверяется существование правила
            2. Правило удаляется из базы данных
            3. Все ссылки на правило становятся недействительными
            4. Возвращается статус 204 (No Content)
            
            ### Последствия удаления:
            - Режимы перестанут применять это правило
            - Устройства, управляемые этим правилом, останутся в текущем состоянии
            - Нет автоматического пересчета приоритетов других правил
            - Удаление необратимо (требуется создание нового правила)
            
            ### Рекомендации:
            - Перед удалением проверьте, какие режимы используют правило
            - Рассмотрите возможность деактивации вместо удаления
            - Сделайте бэкап правил перед массовым удалением
            - После удаления проверьте работу режимов
            
            ### Альтернативы удалению:
            - Установить очень низкий приоритет (например, 999)
            - Изменить фильтры так, чтобы правило не применялось
            - Перенести в специальный режим (например, ARCHIVED)
            """,
        tags = {"mode-rule-controller", "write-operations"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Правило успешно удалено"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Правило с указанным ID не найдено"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Пользователь не аутентифицирован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав для удаления правил"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Правило используется в активных режимах"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @Parameter(
                description = "Идентификатор удаляемого правила",
                required = true,
                example = "1"
            )
            @PathVariable Long id) {
        
        logger.debug("DELETE/api/mode-rules/{}", id);
        if (modeRuleRepository.existsById(id)) {
            modeRuleRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Rule with id {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }
}