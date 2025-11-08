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
        long chatId = update.getMessage()
                .getChatId();
        String messageText = update.getMessage()
                .getText();
        Long userId = update.getMessage()
                .getFrom()
                .getId();

        if ("/start".equals(messageText)) {
            showMainMenu(chatId);
        } else if (userSettingsManager.isChangingSettings(userId)) {
            String callbackId = update.getCallbackQuery()
                    .getId();
            // Если пользователь вводит текст, проверяем, не ожидаем ли мы ввод нового значения настройки
            userSettingsManager.handleTextInput(userId, callbackId, messageText);
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
        Long userId = update.getCallbackQuery()
                .getFrom()
                .getId();

        if (callbackData.startsWith("settings_")) {
            userSettingsManager.handleSettingsAction(chatId, userId, messageId, callbackData, callbackId);
        }

        switch (callbackData) {
            case "menu_help" -> {
                navigationManager.saveNavigationState(chatId, "HELP", null);
                showHelpMenu(chatId, messageId);
            }
            case "back_to_main" -> {
                navigationManager.saveNavigationState(chatId, "MAIN", null);
                updateMainMenu(chatId, messageId);
            }
            case "menu_settings" -> {
                navigationManager.saveNavigationState(chatId, "SETTINGS", null);
                userSettingsManager.showSettingsMenu(chatId, userId, messageId);
            }
        }
    }

    private void updateMainMenu(Long chatId, Integer messageId) {
        String menuText = "*Выбор действия*\n";
        EditMessageText msg = new EditMessageText();
        msg.setChatId(chatId);
        msg.setMessageId(messageId);
        msg.setText(menuText);
        msg.setParseMode(ParseMode.MARKDOWN);
        msg.setReplyMarkup(createMainMenuKeyboard());
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void showMainMenu(Long chatId) {
        String menuText = "*Выбор действия*\n";
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(menuText);
        msg.setParseMode(ParseMode.MARKDOWN);
        msg.setReplyMarkup(createMainMenuKeyboard());
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void showHelpMenu(long chatId, int messageId) {
        String helpText = """
                \uD83D\uDCAC *Помощь по боту*
                
                *Основные команды:*
                `/start` - Главное меню
                
                Описание работы бота""";

        EditMessageText msg = new EditMessageText();
        msg.setChatId(chatId);
        msg.setMessageId(messageId);
        msg.setText(helpText);
        msg.setParseMode(ParseMode.MARKDOWN);
        msg.setReplyMarkup(new InlineKeyboardMarkup(List.of(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", "back_to_main")))));
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(new KeyboardButton("⚙\uFE0F Настройки", "menu_settings"), new KeyboardButton("\uD83D\uDCAC Помощь", "menu_help")));
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWN);
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
