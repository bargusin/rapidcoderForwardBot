package ru.rapidcoder.forward.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.component.KeyboardButton;
import ru.rapidcoder.forward.bot.component.UserSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSettingsManager {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingsManager.class);
    private static final String ACTION_SETTINGS_TOGGLE_FIELD = "settings_toggle_field";
    private static final String MENU_SETTINGS = "SETTINGS";
    private final Bot bot;
    private final NavigationManager navigationManager = NavigationManager.getInstance();
    private final Map<Long, UserSettings> userSettings = new HashMap<>();

    public UserSettingsManager(Bot bot) {
        this.bot = bot;
    }

    public void showSettingsMenu(Long chatId, Integer messageId) {
        userSettings.putIfAbsent(chatId, new UserSettings());
        UserSettings settings = userSettings.get(chatId);

        String text = "⚙\uFE0F *Настройки бота*\n\n" + getCurrentSettingsText(settings);
        if (messageId != null) {
            bot.updateMessage(chatId, messageId, text, createSettingsKeyboard(settings));
        } else {
            bot.sendMessage(chatId, text, createSettingsKeyboard(settings));
        }
    }

    public void handleTextInput(Long chatId, String callbackId, String text) {
        logger.debug("Handle input text {}", text);
        UserSettings settings = userSettings.get(chatId);
        String inputType = settings.getExpectedInputType();
        if (inputType.equals(settings.getFieldBoolean()
                .getFieldName())) {
            bot.showNotification(callbackId, "✅ Значение обновлено");
        }

        settings.setWaitingForTextInput(false);
        settings.setExpectedInputType(null);
    }

    public void handleSettingsAction(Long chatId, Integer messageId, String action, String callbackId) {
        UserSettings settings = userSettings.get(chatId);
        switch (action) {
            case ACTION_SETTINGS_TOGGLE_FIELD -> {
                settings.getFieldBoolean()
                        .setValue(!settings.getFieldBoolean()
                                .getValue());
                navigationManager.saveNavigationState(chatId, MENU_SETTINGS, ACTION_SETTINGS_TOGGLE_FIELD);
                showSettingsMenu(chatId, messageId);
            }
            case "settings_reset" -> {
                userSettings.put(chatId, new UserSettings());
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

    public UserSettings getSettings(Long chatId) {
        userSettings.putIfAbsent(chatId, new UserSettings());
        return userSettings.get(chatId);
    }

    public boolean isChangingSettings(Long chatId) {
        return getSettings(chatId).isWaitingForTextInput();
    }
}
