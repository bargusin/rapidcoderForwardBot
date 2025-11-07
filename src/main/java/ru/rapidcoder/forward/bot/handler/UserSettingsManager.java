package ru.rapidcoder.forward.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.component.KeyboardButton;
import ru.rapidcoder.forward.bot.component.UserSettings;

import java.util.*;

public class UserSettingsManager {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsManager.class);

    private final Bot bot;
    // Хранилище настроек пользователей
    private final Map<Long, UserSettings> userSettings = new HashMap<>();

    public UserSettingsManager(Bot bot) {
        this.bot = bot;
    }

    public void showSettingsMenu(Long chatId, Long userId, Integer messageId) {
        userSettings.putIfAbsent(userId, new UserSettings());
        UserSettings settings = userSettings.get(userId);

        String text = "⚙\uFE0F *Настройки бота*\n\n" + "Текущие настройки:\n" + getCurrentSettingsText(settings);
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWN);
        message.setReplyMarkup(createSettingsKeyboard(settings));

        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void handleTextInput(Long userId, String callbackId, String text) {
        UserSettings settings = userSettings.get(userId);
        String inputType = settings.getExpectedInputType();
        if (inputType.equals(settings.getFieldString()
                .getFieldName())) {
            if (text.length() > 1000) {
                //bot.showNotification(callbackId, "❌ Текст слишком длинный. Максимум 1000 символов.");
                return;
            }
            settings.getFieldString()
                    .setValue(text);
            //bot.showNotification(callbackId, "✅ Текст для строкового поля обновлен");
        }

        settings.setWaitingForTextInput(false);
        settings.setExpectedInputType(null);
        //showSettingsMenu(chatId, userId, messageId);
    }

    public void handleSettingsAction(Long chatId, Long userId, Integer messageId, String action, String callbackId) {
        UserSettings settings = userSettings.get(userId);
        switch (action) {
            case "settings_set_text" -> {
                settings.setWaitingForTextInput(true);
                settings.setExpectedInputType(settings.getFieldString()
                        .getFieldName());
                bot.showNotification(callbackId, "Введите новый текст для текстового поля:");
            }
            case "settings_reset" -> {
                userSettings.put(userId, new UserSettings());
                bot.showNotification(callbackId, "✅ Настройки сброшены к значениям по умолчанию");
            }
            case "settings_save" -> {
                bot.showNotification(callbackId, "✅ Настройки сохранены");
            }
            default -> {

            }
        }

        // Обновляем сообщение с настройками
        //updateSettingsMessage(chatId, messageId, settings);
    }

    public void updateSettingsMessage(Long chatId, Integer messageId, UserSettings settings) {
        String text = "⚙\uFE0F *Настройки бота*\n\n" + getCurrentSettingsText(settings) + new Date();//TODO Временная заглушка, чтобы текст менялся

        InlineKeyboardMarkup keyboard = createSettingsKeyboard(settings);

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        editMessage.setParseMode(ParseMode.MARKDOWN);
        editMessage.setReplyMarkup(keyboard);

        try {
            bot.execute(editMessage);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String getCurrentSettingsText(UserSettings settings) {
        StringBuilder str = new StringBuilder();
        str.append(String.format("\uD83D\uDCA1 Текстовое поле: %s\n\n", settings.getFieldString()
                .getValue()));
        return str.toString();
    }

    private InlineKeyboardMarkup createSettingsKeyboard(UserSettings settings) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(new KeyboardButton("📝 Изменить текстовое поле", "settings_set_text"));
        rows.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton("🔄 Сбросить настройки", "settings_reset"));
        row3.add(new KeyboardButton("💾 Сохранить", "settings_save"));
        rows.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(new KeyboardButton("\uD83C\uDFE0 Главное меню", "back_to_main"));
        rows.add(row4);

        markup.setKeyboard(rows);

        return markup;
    }

    public UserSettings getSettings(Long userId) {
        userSettings.putIfAbsent(userId, new UserSettings());
        return userSettings.get(userId);
    }

    public boolean isChangingSettings(Long userId) {
        return getSettings(userId).isWaitingForTextInput();
    }
}
