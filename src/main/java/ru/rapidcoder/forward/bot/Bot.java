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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.component.KeyboardButton;
import ru.rapidcoder.forward.bot.dto.ChatMembership;
import ru.rapidcoder.forward.bot.dto.HistoryChatMembership;
import ru.rapidcoder.forward.bot.handler.MessageHandler;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Bot extends TelegramLongPollingBot {

    public static final String BACK_TO_MAIN_CALLBACK_DATA = "back_to_main";
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final String botName;
    private final MessageHandler messageHandler;
    private final Map<Long, List<Message>> messagesForSend = new ConcurrentHashMap<>();

    public Bot(String botName, String tokenId, String storageFile) {
        super(tokenId);
        this.botName = botName;

        messageHandler = new MessageHandler(this, storageFile);
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
                    handleForwardMessage(update);
                } else if (message.hasText()) {
                    handleCommand(update);
                }
            } else if (update.hasCallbackQuery()) {
                handleCallback(update);
            } else if (update.hasMyChatMember()) {
                handleChatMember(update);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void removeKeyboard(Long chatId) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText("⌨️ Клавиатура удалена");

            // Создаем объект для удаления клавиатуры
            ReplyKeyboardRemove keyboardRemove = new ReplyKeyboardRemove();
            keyboardRemove.setRemoveKeyboard(true);
            keyboardRemove.setSelective(false); // Удалить для всех пользователей

            message.setReplyMarkup(keyboardRemove);

            execute(message);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void handleCommand(Update update) {
        messageHandler.handleCommand(update);
    }

    public void handleCallback(Update update) {
        messageHandler.handleCallback(update);
    }

    public void handleForwardMessage(Update update) {
        messageHandler.handleForwardMessage(update);
    }

    public void handleChatMember(Update update) {
        messageHandler.handleChatMember(update);
    }

    public void showMainMenu(Long chatId, Integer messageId) {
        String text = "*\uD83C\uDFE0 Главное меню*";
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (getMessagesForSend().get(chatId) != null && !getMessagesForSend().get(chatId)
                .isEmpty()) {
            rows.add(List.of(new KeyboardButton("✉\uFE0F Рассылка текущего сообщения", "menu_send")));
        }
        rows.add(List.of(new KeyboardButton("\uD83D\uDCE2 Подписка на каналы", "menu_chats"), new KeyboardButton("⚙\uFE0F Настройки", "menu_settings"), new KeyboardButton("\uD83D\uDCAC Помощь", "menu_help")));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    public void showChatsMenu(Long chatId, Integer messageId, List<ChatMembership> chats) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCE2 *Подписка на каналы*\n\n");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ChatMembership chat : chats) {
            sb.append(String.format("Канал '%s', тип: '%s', роль: '%s'%n", chat.getChatTitle(), chat.getChatType(), chat.getBotNewStatus()));
        }

        rows.add(List.of(new KeyboardButton("\uD83D\uDCCB История подписок", "menu_chats_history")));
        rows.add(List.of(new KeyboardButton("⬇\uFE0F Выгрузить данные о подписках", "menu_chats_upload")));
        rows.add(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA)));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, sb.toString(), keyboard);
        } else {
            sendMessage(chatId, sb.toString(), keyboard);
        }
    }

    public void showSendMenu(Long chatId, Integer messageId, List<ChatMembership> chats) {
        StringBuilder sb = new StringBuilder();
        sb.append("✉\uFE0F *Отправка сообщения в каналы*\n\n");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ChatMembership chat : chats) {
            sb.append(String.format("Канал '%s', тип: '%s', роль: '%s'%n", chat.getChatTitle(), chat.getChatType(), chat.getBotNewStatus()));
        }

        rows.add(List.of(new KeyboardButton("✉\uFE0F Отправить", "menu_send_message"), new KeyboardButton("\uD83D\uDDD1\uFE0F Очистить", "menu_send_message_clear")));
        rows.add(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA)));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, sb.toString(), keyboard);
        } else {
            sendMessage(chatId, sb.toString(), keyboard);
        }
    }

    public void showChatsHistoryMenu(Long chatId, Integer messageId, List<HistoryChatMembership> chats) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCCB *История подписок*\n\n");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (HistoryChatMembership chat : chats) {
            sb.append(String.format("%s deleted=%b, chatId=%d, userId=%d, userName=%s, channel='%s', currentStatus='%s', oldStatus='%s'%n", formatter.format(chat.getAddedDate()), chat.isDeleted(), chat.getChatId(), chat.getUserId(), chat.getUserName(), chat.getChatTitle(), chat.getBotNewStatus(), chat.getBotOldStatus()));
        }

        rows.add(List.of(new KeyboardButton("\uD83D\uDCE2 Подписка на каналы", "menu_chats")));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, sb.toString(), keyboard);
        } else {
            sendMessage(chatId, sb.toString(), keyboard);
        }
    }

    public void showHelpMenu(Long chatId, Integer messageId) {
        String text = """
                \uD83D\uDCAC *Помощь по боту*
                                
                *Основные команды:*
                `/start` - Главное меню
                                
                Описание работы бота""";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA))));
        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    public void showSettingsMenu(Long chatId, Integer messageId) {
        String text = "⚙\uFE0F *Настройки бота*\n";
        if (messageId != null) {
            updateMessage(chatId, messageId, text, createSettingsKeyboard());
        } else {
            sendMessage(chatId, text, createSettingsKeyboard());
        }
    }

    private InlineKeyboardMarkup createSettingsKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(new KeyboardButton("🔄 Сбросить настройки", "settings_reset"));
        row3.add(new KeyboardButton("💾 Сохранить", "settings_save"));
        rows.add(row3);

        List<InlineKeyboardButton> row4 = new ArrayList<>();
        row4.add(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA));
        rows.add(row4);

        markup.setKeyboard(rows);

        return markup;
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

    public Map<Long, List<Message>> getMessagesForSend() {
        return messagesForSend;
    }
}
