package ru.rapidcoder.forward.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.component.KeyboardButton;
import ru.rapidcoder.forward.bot.handler.NavigationManager;
import ru.rapidcoder.forward.bot.handler.UserSettingsManager;

import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final String botName;
    private final UserSettingsManager userSettingsManager;
    private final NavigationManager navigationManager = NavigationManager.getInstance();

    public Bot(String botName, String tokenId) {
        super(tokenId);
        this.botName = botName;

        userSettingsManager = new UserSettingsManager(this);
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                Long userId = message.getFrom()
                        .getId();
                long chatId = message.getChatId();
                logger.debug("Обработка сообщения chatId={}, userId={}", chatId, userId);
                if (message.getForwardDate() != null) {
                    // Сообщение передано в бот, добавляем его в очередь для обработки
                    // TODO
                } else if (message.hasText()) {
                    handleCommand(update);
                } else if (message.hasDocument()) {
                    // Обработка загрузки документов
                }
            } else if (update.hasCallbackQuery()) {
                handleCallback(update);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void handleCommand(Update update) {
        Long chatId = update.getMessage()
                .getChatId();
        String messageText = update.getMessage()
                .getText();

        if ("/start".equals(messageText)) {
            showMainMenu(chatId, null);
        } else if ("/help".equals(messageText)) {
            showHelpMenu(chatId, null);
        } else if ("/settings".equals(messageText)) {
            userSettingsManager.showSettingsMenu(chatId, null);
        } else if (userSettingsManager.isChangingSettings(chatId)) {
            String callbackId = update.getCallbackQuery()
                    .getId();
            // Если пользователь вводит текст, проверяем, не ожидаем ли мы ввод нового значения настройки
            userSettingsManager.handleTextInput(chatId, callbackId, messageText);
        }
    }

    private void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery()
                .getData();
        String callbackId = update.getCallbackQuery()
                .getId();
        long chatId = update.getCallbackQuery()
                .getMessage()
                .getChatId();
        int messageId = update.getCallbackQuery()
                .getMessage()
                .getMessageId();

        if (callbackData.startsWith("settings_")) {
            userSettingsManager.handleSettingsAction(chatId, messageId, callbackData, callbackId);
        }

        switch (callbackData) {
            case "menu_help" -> {
                navigationManager.saveNavigationState(chatId, "HELP", null);
                showHelpMenu(chatId, messageId);
            }
            case "back_to_main" -> {
                navigationManager.saveNavigationState(chatId, "MAIN", null);
                showMainMenu(chatId, messageId);
            }
            case "menu_settings" -> {
                navigationManager.saveNavigationState(chatId, "SETTINGS", null);
                userSettingsManager.showSettingsMenu(chatId, messageId);
            }
        }
    }

    private void showMainMenu(Long chatId, Integer messageId) {
        String text = "*Выбор действия*\n";
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(new KeyboardButton("⚙\uFE0F Настройки", "menu_settings"), new KeyboardButton("\uD83D\uDCAC Помощь", "menu_help")));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    private void showHelpMenu(Long chatId, Integer messageId) {
        String text = """
                \uD83D\uDCAC *Помощь по боту*
                
                *Основные команды:*
                `/start` - Главное меню
                
                Описание работы бота""";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", "back_to_main"))));
        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    public void sendMessage(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWN);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void updateMessage(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId(messageId);
        message.setParseMode(ParseMode.MARKDOWN);
        message.setReplyMarkup(keyboard);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void showNotification(String callbackQueryId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(text);
        answer.setShowAlert(false); // false - всплывающее уведомление, true - alert-окно
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
