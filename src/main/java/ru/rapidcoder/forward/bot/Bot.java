package ru.rapidcoder.forward.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.component.KeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final String botName;

    public Bot(String botName, String tokenId, String storageFile) {
        super(tokenId);
        this.botName = botName;
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
        }
    }

    private void handleCallback(Update update) throws TelegramApiException {
        String callbackData = update.getCallbackQuery()
                .getData();
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
            // TODO
        }

        switch (callbackData) {
            case "menu_help" ->
                    showHelpMenu(chatId, messageId);
            case "back_to_main" ->
                    showMainMenu(chatId);
        }
    }

    private void showMainMenu(long chatId) {
        String menuText = "*Выбор действия*\n";
        InlineKeyboardMarkup keyboard = createMainMenuKeyboard();
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(menuText);
        msg.setParseMode(ParseMode.MARKDOWN);
        msg.setReplyMarkup(keyboard);
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

        rows.add(List.of(new KeyboardButton("⚙️ Настройки", "menu_settings"), new KeyboardButton("\uD83D\uDCAC Помощь", "menu_help")));
        keyboard.setKeyboard(rows);

        return keyboard;
    }
}
