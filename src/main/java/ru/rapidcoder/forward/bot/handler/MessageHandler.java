package ru.rapidcoder.forward.bot.handler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.dto.AccessRequest;
import ru.rapidcoder.forward.bot.dto.ChatMembership;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static ru.rapidcoder.forward.bot.Bot.BACK_TO_MAIN_CALLBACK_DATA;

public class MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final ChannelManager channelManager;
    private final PermissionManager permissionManager;
    private final Bot bot;
    private final Map<Long, ScheduledFuture<?>> userTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public MessageHandler(Bot bot, String storageFile, List<Long> admins) {
        channelManager = new ChannelManager(storageFile);
        permissionManager = new PermissionManager(storageFile, admins);
        this.bot = bot;
    }

    public void handleCommand(Update update) {
        Long chatId = update.getMessage()
                .getChatId();
        Long userId = update.getMessage()
                .getFrom()
                .getId();
        String messageText = update.getMessage()
                .getText();

        if (!permissionManager.hasAccess(userId)) {
            logger.debug("User dosn't access to bot by userId={}", userId);
            bot.showRequestAccessMenu(userId);
        } else {
            if ("/start".equals(messageText)) {
                bot.showMainMenu(chatId, null, permissionManager.isAdmin(chatId));
            } else if ("/help".equals(messageText)) {
                bot.showHelpMenu(chatId, null);
            } else if ("/settings".equals(messageText)) {
                bot.showSettingsMenu(chatId, null);
            } else { // Переслали в бот текстовое сообщение
                Message message = update.getMessage();
                logger.debug("Catch text message for send with id={}", message.getMessageId());
                bot.getMessagesForSend()
                        .computeIfAbsent(chatId, k -> new ArrayList<>())
                        .add(message);
                bot.showSendMenu(chatId, null, channelManager.getAll());
            }
        }
    }

    public void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery()
                .getData();
        String callbackId = update.getCallbackQuery()
                .getId();
        Long chatId = update.getCallbackQuery()
                .getMessage()
                .getChatId();
        Long userId = update.getCallbackQuery()
                .getFrom()
                .getId();
        Integer messageId = update.getCallbackQuery()
                .getMessage()
                .getMessageId();
        String userName = OptionalUtils.resolve(() -> update.getCallbackQuery()
                        .getFrom()
                        .getUserName())
                .orElse(update.getCallbackQuery()
                        .getFrom()
                        .getFirstName() + " " + update.getCallbackQuery()
                        .getFrom()
                        .getLastName());

        if ("menu_request_access".equals(callbackData)) {
            if (permissionManager.hasAccess(userId)) {
                bot.showMainMenu(chatId, null, permissionManager.isAdmin(userId));
            } else {
                permissionManager.saveRequest(userId, userName);
                bot.sendMessage(chatId, "Запрос на предоставление доступа к боту отправлен", null);
            }
        } else if (permissionManager.hasAccess(userId)) {
            if (callbackData.startsWith("chat_toggle_")) {
                int chatIndex = Integer.parseInt(callbackData.substring("chat_toggle_".length()));
                Set<Integer> userSelection = bot.getSelectedChats()
                        .getOrDefault(chatId, new HashSet<>());
                if (userSelection.contains(chatIndex)) {
                    userSelection.remove(chatIndex);
                } else {
                    userSelection.add(chatIndex);
                }
                bot.getSelectedChats()
                        .put(chatId, userSelection);
                bot.showSendMenu(chatId, update.getCallbackQuery()
                        .getMessage()
                        .getMessageId(), channelManager.getAll());
            } else if (callbackData.startsWith("grant_access_blocked_")) {
                userId = Long.parseLong(callbackData.substring("grant_access_blocked_".length()));
                permissionManager.blockedUser(userId);
                bot.showGrantedAccessMenu(chatId, messageId, permissionManager.getUsers());
            } else if (callbackData.startsWith("grant_access_active_")) {
                userId = Long.parseLong(callbackData.substring("grant_access_active_".length()));
                permissionManager.activeUser(userId);
                bot.showGrantedAccessMenu(chatId, messageId, permissionManager.getUsers());
            } else if (callbackData.startsWith("access_request_accept_")) {
                userId = Long.parseLong(callbackData.substring("access_request_accept_".length()));
                permissionManager.approvedRequest(userId);
                AccessRequest request = permissionManager.findRequestById(userId);
                permissionManager.saveUser(userId, request.getUserName());
                bot.showAccessRequestsMenu(chatId, messageId, permissionManager.getRequests());
            } else if (callbackData.startsWith("access_request_reject_")) {
                userId = Long.parseLong(callbackData.substring("access_request_reject_".length()));
                permissionManager.rejectRequest(userId);
                bot.showAccessRequestsMenu(chatId, messageId, permissionManager.getRequests());
            } else {
                switch (callbackData) {
                    case "menu_help" -> {
                        bot.showHelpMenu(chatId, messageId);
                    }
                    case BACK_TO_MAIN_CALLBACK_DATA -> {
                        bot.showMainMenu(chatId, messageId, permissionManager.isAdmin(userId));
                    }
                    case "menu_settings" -> {
                        bot.showSettingsMenu(chatId, messageId);
                    }
                    case "menu_chats" -> {
                        bot.showChatsMenu(chatId, messageId, channelManager.getAll());
                    }
                    case "menu_chats_history" -> {
                        bot.showChatsHistoryMenu(chatId, messageId, channelManager.getHistory());
                    }
                    case "menu_chats_upload" -> {
                        try {
                            bot.execute(channelManager.uploadData(chatId));
                        } catch (
                                TelegramApiException e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                    case "menu_send" -> {
                        bot.showSendMenu(chatId, messageId, channelManager.getAll());
                    }
                    case "menu_send_message_clear" -> {
                        bot.getMessagesForSend()
                                .put(chatId, new ArrayList<>());
                        bot.showMainMenu(chatId, messageId, permissionManager.isAdmin(userId));
                    }
                    case "menu_send_message" -> {
                        List<ChatMembership> chats = channelManager.getAll();
                        Set<Integer> userSelection = bot.getSelectedChats()
                                .getOrDefault(chatId, new HashSet<>());
                        for (int i = 0; i < chats.size(); i++) {
                            ChatMembership chat = chats.get(i);
                            logger.debug("Send forward message into {} ready", chat.getChatTitle());
                            if (!userSelection.contains(i)) {
                                logger.debug("Try send forward message into {}", chat.getChatTitle());
                                sendForwardMessage(chat, userId, userName, bot.getMessagesForSend()
                                        .get(chatId));
                            }
                        }
                        bot.showNotification(callbackId, "✅ Сообщение рассылается адресатам...");
                        bot.getMessagesForSend()
                                .put(chatId, new ArrayList<>());
                        bot.showMainMenu(chatId, messageId, permissionManager.isAdmin(userId));
                    }
                    case "menu_sending_history" -> {
                        bot.showSendingHistoryMenu(chatId, messageId, channelManager.getHistorySending());
                    }
                    case "menu_access_requests" -> {
                        bot.showAccessRequestsMenu(chatId, messageId, permissionManager.getRequests());
                    }
                    case "menu_access" -> {
                        bot.showGrantedAccessMenu(chatId, messageId, permissionManager.getUsers());
                    }
                    case "settings_reset" -> {
                        bot.showNotification(callbackId, "✅ Настройки сброшены к значениям по умолчанию");
                        //TODO
                    }
                    case "settings_save" -> {
                        bot.showNotification(callbackId, "✅ Настройки сохранены");
                        //TODO
                    }
                    default -> {
                        logger.warn("Unknown callbackData {}", callbackData);
                    }
                }
            }
        } else {
            bot.showRequestAccessMenu(userId);
        }
    }

    public void handleForwardMessage(Update update) {
        Message message = update.getMessage();
        Long userId = message.getFrom()
                .getId();
        if (!permissionManager.hasAccess(userId)) {
            logger.warn("User call handleForwardMessage by userId={} without access", userId);
        } else {
            logger.debug("Catch message for send with id={}", message.getMessageId());
            Long chatId = message.getChatId();
            bot.getMessagesForSend()
                    .computeIfAbsent(chatId, k -> new ArrayList<>())
                    .add(message);

            ScheduledFuture<?> oldTimer = userTimers.get(chatId);
            if (oldTimer != null) {
                oldTimer.cancel(false);
            }

            ScheduledFuture<?> newTimer = scheduler.schedule(() -> {
                bot.showSendMenu(chatId, null, channelManager.getAll());
                userTimers.remove(chatId);
            }, 2, TimeUnit.SECONDS);

            userTimers.put(chatId, newTimer);
        }
    }

    public void handleChatMember(Update update) {
        ChatMemberUpdated chatMember = update.getMyChatMember();
        Chat chat = chatMember.getChat();
        String status = chatMember.getNewChatMember()
                .getStatus();
        String chatType = getChatType(chat);
        logger.debug("Bot status changed chat '{}' from {} to '{}'", chat.getTitle(), OptionalUtils.resolve(() -> chatMember.getOldChatMember()
                        .getStatus())
                .orElse("not defined"), status);
        switch (status) {
            case "administrator", "restricted":
                channelManager.save(chat.getId(), OptionalUtils.resolve(() -> chatMember.getFrom()
                                .getId())
                        .orElse(-1L), OptionalUtils.resolve(() -> chatMember.getFrom()
                                .getUserName())
                        .orElse("not defined"), chat.getTitle(), chatType, status, OptionalUtils.resolve(() -> chatMember.getOldChatMember()
                                .getStatus())
                        .orElse("not defined"));
                break;
            case "member":
                break;
            case "left", "kicked":
                channelManager.delete(chat.getId());
                break;
            default:
                logger.warn("Unknown status {}", status);
                break;
        }
    }

    private String getChatType(Chat chat) {
        if (chat.isChannelChat()) {
            return "channel";
        } else if (chat.isGroupChat()) {
            return "group";
        } else if (chat.isSuperGroupChat()) {
            return "supergroup";
        } else if (chat.isUserChat()) {
            return "private";
        }
        return "unknown";
    }

    private void sendForwardMessage(ChatMembership chat, Long userId, String userName, List<Message> groupMessages) {
        executorService.schedule(() -> {
            List<InputMedia> mediaList = new ArrayList<>();
            String caption = null;
            for (Message message : groupMessages) {
                if (message.hasPhoto()) {
                    InputMediaPhoto photo = new InputMediaPhoto();
                    List<PhotoSize> photos = message.getPhoto();
                    photo.setMedia(photos.get(photos.size() - 1)
                            .getFileId());
                    if (message.getCaption() != null && caption == null) {
                        caption = message.getCaption();
                        photo.setCaption(caption);
                    }
                    photo.setCaptionEntities(message.getCaptionEntities());
                    mediaList.add(photo);
                } else if (message.hasVideo()) {
                    InputMediaVideo video = new InputMediaVideo();
                    video.setMedia(message.getVideo()
                            .getFileId());
                    if (message.getCaption() != null && caption == null) {
                        caption = message.getCaption();
                        video.setCaption(caption);
                    }
                    video.setCaptionEntities(message.getCaptionEntities());
                    mediaList.add(video);
                }
            }

            if (!mediaList.isEmpty() && mediaList.size() > 1) {
                SendMediaGroup mediaGroup = new SendMediaGroup(chat.getChatId()
                        .toString(), mediaList);
                try {
                    List<Message> sending = bot.execute(mediaGroup);
                    try {
                        for (Message message : sending) {
                            channelManager.saveHistorySending(chat.getChatId(), userId, userName, chat.getChatTitle(), message.getMessageId(), getPartMessageText(message));
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } catch (TelegramApiException e) {
                    logger.error(e.getMessage(), e);
                }
            }

            if (!groupMessages.isEmpty() && mediaList.size() < 2) {
                sendCopyMessage(chat, userId, userName, groupMessages.get(0));
            }
        }, 2, TimeUnit.SECONDS);
    }

    public void sendCopyMessage(ChatMembership chat, Long userId, String userName, Message message) {
        CopyMessage copy = new CopyMessage();
        copy.setChatId(chat.getChatId());
        copy.setFromChatId(message.getChatId()
                .toString());
        copy.setMessageId(message.getMessageId());
        copy.setCaptionEntities(message.getCaptionEntities());
        try {
            MessageId sending = bot.execute(copy);
            try {
                channelManager.saveHistorySending(chat.getChatId(), userId, userName, chat.getChatTitle(), sending.getMessageId()
                        .intValue(), getPartMessageText(message));
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private String getPartMessageText(Message message) {
        String text = message.getText();
        if (!StringUtils.isEmpty(text)) {
            text = message.getText();
        } else if (!StringUtils.isEmpty(message.getCaption())) {
            text = message.getCaption();
        } else {
            return null;
        }
        return text.substring(0, Math.min(text.length(), 50));
    }

    private static class OptionalUtils {
        public static <T> Optional<T> resolve(Supplier<T> resolver) {
            try {
                T result = resolver.get();
                return Optional.ofNullable(result);
            } catch (NullPointerException e) {
                return Optional.empty();
            }
        }
    }
}
