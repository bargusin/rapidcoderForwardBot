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
import ru.rapidcoder.forward.bot.dto.*;
import ru.rapidcoder.forward.bot.handler.MessageHandler;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Bot extends TelegramLongPollingBot {

    public static final String BACK_TO_MAIN_CALLBACK_DATA = "back_to_main";
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    private final String botName;
    private final MessageHandler messageHandler;
    private final Map<Long, List<Message>> messagesForSend = new ConcurrentHashMap<>();
    private final Map<Long, Set<Integer>> selectedChats = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Bot(String botName, String tokenId, String storageFile, List<Long> admins) {
        super(tokenId);
        this.botName = botName;

        messageHandler = new MessageHandler(this, storageFile, admins);
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
                //Если в сообщении пересылается только ссылка, то такое сообщение определяется не как пересылаемое, а как текстовое
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

    public void showMainMenu(Long chatId, Integer messageId, boolean isAdmin) {
        String text = "\uD83C\uDFE0 <b>Главное меню</b>";
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (getMessagesForSend().get(chatId) != null && !getMessagesForSend().get(chatId)
                .isEmpty()) {
            rows.add(List.of(new KeyboardButton("✉\uFE0F Рассылка текущего сообщения", "menu_send")));
        }

        rows.add(List.of(new KeyboardButton("\uD83D\uDCE2 Подписка на каналы", "menu_chats")));

        if (isAdmin) { // Обработка запросов на доступ к боту доступна только админам бота
            rows.add(List.of(new KeyboardButton("\uD83D\uDD12 Доступ к боту", "menu_access")));
        }
        if (isAdmin) {
            rows.add(List.of(new KeyboardButton("❓Запросы на доступ к боту", "menu_access_requests")));
        }
        //rows.add(List.of(new KeyboardButton("⚙\uFE0F Настройки", "menu_settings")));
        rows.add(List.of(new KeyboardButton("\uD83D\uDCCB История рассылок", "menu_sending_history"), new KeyboardButton("\uD83D\uDCAC Помощь", "menu_help")));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    public void showChatsMenu(Long chatId, Integer messageId, List<ChatMembership> chats) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCE2 <b>Подписка на каналы</b>\n\n");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (ChatMembership chat : chats) {
            sb.append(String.format("✔\uFE0F <b>%s</b> (тип: %s, роль: %s)%n", chat.getChatTitle(), chat.getChatType(), chat.getBotNewStatus()));
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
        sb.append("✉\uFE0F <b>Отправка сообщения в каналы</b>\n\n");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        Set<Integer> userSelection = selectedChats.getOrDefault(chatId, new HashSet<>());
        for (int i = 0; i < chats.size(); i++) {
            ChatMembership chat = chats.get(i);
            String chatName = chat.getChatTitle();
            boolean isDisabled = userSelection.contains(i);

            String callbackData = "chat_toggle_" + i;

            InlineKeyboardButton chatButton = new InlineKeyboardButton();
            chatButton.setText((!isDisabled ? "✅ " : "❌ ") + chatName);
            chatButton.setCallbackData(callbackData);

            rows.add(List.of(chatButton));
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
        sb.append("\uD83D\uDCCB <b>История подписок</b>\n\n");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

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

    public void showSendingHistoryMenu(Long chatId, Integer messageId, List<HistorySending> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCCB <b>История отправки сообщений</b>\n\n");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (HistorySending send : history) {
            sb.append(String.format("%s %s [%s...] (userName=%s, channel=%s)%n", formatter.format(send.getAddedDate()), getLinkMessage(send.getChatId(), send.getMessageId()), send.getText(), send.getUserName(), send.getChatTitle()));
        }

        rows.add(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA)));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, sb.toString(), keyboard);
        } else {
            sendMessage(chatId, sb.toString(), keyboard);
        }
    }

    public String getLinkMessage(Long chatId, Integer messageId) {
        return String.format("https://t.me/c/%s/%d", chatId.toString()
                .substring(4), messageId);
    }

    public void showHelpMenu(Long chatId, Integer messageId) {
        String text = """
                \uD83D\uDCAC <b>Помощь по боту</b>
                
                <b>Основные команды:</b>
                /start - Главное меню
                /help - Помощь
                
                Для того чтобы бот смог отправлять сообщения в каналы, его необходимо добавить администратором в эти каналы с соответствующим доступом.
                
                В боте реализована ролевая модель доступа к боту. Список администраторов, которые могут управлять доступом к боту для других пользователей, указывается при установке бота на сервер. Обычный пользователь запустив бот, не сможет получить к нему доступ. Он увидит кнопку *[Запросить доступ]*, нажав на которую, отправит уведомление на предоставления доступа.
                
                Если пользователю предоставлен доступ, или пользователь является администратором бота, то ему доступен следующий функционал:
                <b>[Подписка на каналы]</b> - список каналов, в которых бот является администратором и может отправлять в них сообщения.
                     • <b>[История подписок]</b> - история действий, которые производились с ботом в каналах, включая исключение его из администраторов.
                     • <b>[Выгрузить данные о подписках]</b> - выгружается бэкап с данными сервера.
                <b>[Доступ к боту]</b> - (доступно только администраторам) управление предоставлением доступа пользователей к боту (возможно как заблокировать, так и разблокировать пользователя).
                <b>[Запросы на доступ к боту]</b> - (доступно только администраторам) список запросов от пользователей на предоставление доступа.
                <b>[История рассылок]</b> - список сообщений, которые отправлялись из бота в каналы.
                
                Как работает отправка сообщений в каналы:
                1. Пользователь пересылает сообщение в бот.
                2. Спустя 2 секунды появляется список каналов, на который подписан бот, а также кнопки <b>[Отправить]</b> и <b>[Очистить]</b>.
                3. Пользователь может убрать из списка каналов те, в которые он не хочет отправлять сообщение.
                4. После нажатия кнопки <b>[Отправить]</b> сообщение будет отправлено в выбранные каналы, история о рассылке сохранится.
                5. После завершения рассылки вверху интерфейса телеграм появится соответствующее всплывающее сообщение и пропадут кнопки управления отправкой.
                6. Бот снова готов к приему пересылаемого сообщения.
                """;

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA))));
        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    public void showRequestAccessMenu(Long userId) {
        String text = "<b>Запрос на доступ к боту</b>";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(List.of(new KeyboardButton("❓ Запросить доступ", "menu_request_access"))));
        sendMessage(userId, text, keyboard);
    }

    public void showAccessRequestsMenu(Long chatId, Integer messageId, List<AccessRequest> accessRequests) {
        String text = String.format("❓ <b>Запросы на доступ к боту</b>%n%n%s", accessRequests.isEmpty() ? "Запросов на доступ к боту нет" : "");
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (AccessRequest request : accessRequests) {
            Long userId = request.getUserId();
            rows.add(List.of(new KeyboardButton(String.format("\uD83D\uDC64 %s (%s)%n", request.getUserName(), request.getStatus()), "access_request_info" + userId)));
            rows.add(List.of(new KeyboardButton("✅ Принять", "access_request_accept_" + userId), new KeyboardButton("❌ Отклонить", "access_request_reject_" + userId)));
        }
        rows.add(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA)));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    public void showGrantedAccessMenu(Long chatId, Integer messageId, List<PermissionUser> users) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String text = "\uD83D\uDD12 <b>Доступ к боту</b>";
        for (PermissionUser user : users) {
            KeyboardButton action = null;
            if (PermissionUser.UserStatus.BLOCKED.equals(user.getStatus())) {
                action = new KeyboardButton("✅ Разблокировать", "grant_access_active_" + user.getUserId());
            } else {
                action = new KeyboardButton("❌ Заблокировать", "grant_access_blocked_" + user.getUserId());
            }
            rows.add(List.of(new KeyboardButton(String.format("\uD83D\uDC64 %s (%s)%n", user.getUserName(), user.getStatus()), "grant_access_" + user.getUserId()), action));
        }
        rows.add(List.of(new KeyboardButton("\uD83C\uDFE0 Главное меню", BACK_TO_MAIN_CALLBACK_DATA)));
        keyboard.setKeyboard(rows);

        if (messageId != null) {
            updateMessage(chatId, messageId, text, keyboard);
        } else {
            sendMessage(chatId, text, keyboard);
        }
    }

    public void showSettingsMenu(Long chatId, Integer messageId) {
        String text = "⚙\uFE0F <b>Настройки бота</b>\n";
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
        message.setParseMode(ParseMode.HTML);
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
        message.setParseMode(ParseMode.HTML);
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

    public Map<Long, Set<Integer>> getSelectedChats() {
        return selectedChats;
    }
}
