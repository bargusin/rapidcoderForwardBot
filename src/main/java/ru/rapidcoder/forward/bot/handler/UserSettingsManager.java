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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSettingsManager {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsManager.class);
    private final Bot bot;
    private final NavigationManager navigationManager = NavigationManager.getInstance();
    private final Map<Long, UserSettings> userSettings = new HashMap<>();

    private static final String ACTION_SETTINGS_TOGGLE_FIELD = "settings_toggle_field";
    private static final String MENU_SETTINGS = "SETTINGS";

    public UserSettingsManager(Bot bot) {
        this.bot = bot;
    }

    public void showSettingsMenu(Long chatId, Long userId, Integer messageId) {
        userSettings.putIfAbsent(userId, new UserSettings());
        UserSettings settings = userSettings.get(userId);

        String text = "⚙\uFE0F *Настройки бота*\n\n" + getCurrentSettingsText(settings);
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
        logger.debug("Handle input text {}", text);
        UserSettings settings = userSettings.get(userId);
        String inputType = settings.getExpectedInputType();
        if (inputType.equals(settings.getFieldBoolean()
                .getFieldName())) {
            bot.showNotification(callbackId, "✅ Значение обновлено");
        }

        settings.setWaitingForTextInput(false);
        settings.setExpectedInputType(null);
    }

    public void handleSettingsAction(Long chatId, Long userId, Integer messageId, String action, String callbackId) {
        UserSettings settings = userSettings.get(userId);
        switch (action) {
            case ACTION_SETTINGS_TOGGLE_FIELD -> {
                settings.getFieldBoolean()
                        .setValue(!settings.getFieldBoolean()
                                .getValue());
                navigationManager.saveNavigationState(chatId, MENU_SETTINGS, ACTION_SETTINGS_TOGGLE_FIELD);
                showSettingsMenu(chatId, userId, messageId);
            }
            case "settings_reset" -> {
                userSettings.put(userId, new UserSettings());
                bot.showNotification(callbackId, "✅ Настройки сброшены к значениям по умолчанию");
                navigationManager.saveNavigationState(chatId, MENU_SETTINGS, null);
                //TODO
            }
            case "settings_save" -> {
                bot.showNotification(callbackId, "✅ Настройки сохранены");
                navigationManager.saveNavigationState(chatId, MENU_SETTINGS, null);
                //TODO
            }
            default -> {
                logger.warn("Action not fefined {}", action);
            }
        }
    }

    private String getCurrentSettingsText(UserSettings settings) {
        StringBuilder str = new StringBuilder();
        str.append(String.format("\uD83D\uDCA1 Значение: %b", settings.getFieldBoolean()
                .getValue()));
        str.append("\n\n");
        return str.toString();
    }

    private InlineKeyboardMarkup createSettingsKeyboard(UserSettings settings) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(new KeyboardButton((settings.getFieldBoolean()
                .getValue() ? "✅ " : "❌ ") + "Пример настройки (boolean)", ACTION_SETTINGS_TOGGLE_FIELD));
        rows.add(row1);

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
