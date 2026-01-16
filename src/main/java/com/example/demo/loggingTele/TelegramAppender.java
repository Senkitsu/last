package com.example.demo.loggingTele;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class TelegramAppender extends AppenderBase<ILoggingEvent> {
    
    private final String botToken = "8427237335:AAF_lDzXJjUzcEUHdrNbmlkvCYEI5C0GmEQ";
    private final String chatId = "648084323";
    private final RestTemplate restTemplate = new RestTemplate();
    
    // Логгеры, которые мы хотим игнорировать (спам от библиотек)
    private final List<String> IGNORED_LOGGERS = Arrays.asList(
        "org.hibernate",
        "org.springframework.boot.autoconfigure",
        "org.apache.catalina",
        "org.apache.coyote"
    );
    
    // Сообщения, которые мы хотим игнорировать
    private final List<String> IGNORED_MESSAGES = Arrays.asList(
        "HHH90000025",
        "spring.jpa.open-in-view is enabled by default"
    );
    
    @Override
    public void start() {
        System.out.println("🚀 TelegramAppender запускается...");
        System.out.println("🤖 Бот: " + botToken.substring(0, Math.min(10, botToken.length())) + "...");
        System.out.println("💬 Chat ID: " + chatId);
        super.start();
    }
    
    @Override
    protected void append(ILoggingEvent event) {
        // Проверяем, нужно ли игнорировать этот лог
        if (shouldIgnore(event)) {
            return;
        }
        
        // Отправляем только WARN и ERROR от нашего приложения
        if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
            System.out.println("📨 Отправляю в Telegram: [" + event.getLevel() + "] " + 
                event.getLoggerName());
            
            String message = formatCompactMessage(event);
            sendToTelegram(message);
        }
    }
    
    private boolean shouldIgnore(ILoggingEvent event) {
        String loggerName = event.getLoggerName();
        String message = event.getFormattedMessage();
        
        // Игнорируем логгеры из списка
        if (IGNORED_LOGGERS.stream().anyMatch(loggerName::startsWith)) {
            return true;
        }
        
        // Игнорируем сообщения из списка
        if (IGNORED_MESSAGES.stream().anyMatch(message::contains)) {
            return true;
        }
        
        // Игнорируем INFO и DEBUG уровни
        if (event.getLevel().isGreaterOrEqual(Level.INFO) && 
            !event.getLevel().isGreaterOrEqual(Level.WARN)) {
            return true;
        }
        
        return false;
    }
    
    private String formatCompactMessage(ILoggingEvent event) {
        String icon = getIconForLevel(event.getLevel());
        String appName = extractAppName(event.getLoggerName());
        
        // Компактный формат
        StringBuilder sb = new StringBuilder();
        sb.append(icon).append(" <b>").append(event.getLevel()).append("</b>\n");
        sb.append("├─ <i>").append(appName).append("</i>\n");
        
        String message = event.getFormattedMessage();
        if (message.length() > 200) {
            message = message.substring(0, 197) + "...";
        }
        sb.append("└─ ").append(escapeHtml(message));
        
        // Добавляем информацию об ошибке если есть
        if (event.getThrowableProxy() != null) {
            String exception = event.getThrowableProxy().getClassName();
            sb.append("\n\n💥 <code>").append(exception).append("</code>");
        }
        
        return sb.toString();
    }
    
    private String extractAppName(String loggerName) {
        if (loggerName.startsWith("com.example.demo")) {
            return loggerName.substring("com.example.demo".length());
        }
        return loggerName;
    }
    
    private String getIconForLevel(Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT: return "🔴";
            case Level.WARN_INT: return "⚠️";
            default: return "📝";
        }
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("&", "&amp;");
    }
    
    private void sendToTelegram(String message) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("chat_id", chatId);
            params.put("text", message);
            params.put("parse_mode", "HTML");
            
            restTemplate.postForObject(url, params, String.class);
            
        } catch (Exception e) {
            System.out.println("❌ Ошибка отправки в Telegram: " + e.getMessage());
        }
    }
}