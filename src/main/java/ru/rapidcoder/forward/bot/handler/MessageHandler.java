package ru.rapidcoder.forward.bot.handler;

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
import ru.rapidcoder.forward.bot.dto.ChatMembership;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static ru.rapidcoder.forward.bot.Bot.BACK_TO_MAIN_CALLBACK_DATA;

public class MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final NavigationManager navigationManager;
    private final ChannelManager channelManager;
    private final Bot bot;

    public MessageHandler(Bot bot, String storageFile) {
        navigationManager = new NavigationManager(storageFile);
        channelManager = new ChannelManager(storageFile);
        this.bot = bot;
    }

    public void handleCommand(Update update) {
        Long chatId = update.getMessage()
                .getChatId();
        String messageText = update.getMessage()
                .getText();

        if ("/start".equals(messageText)) {
            bot.showMainMenu(chatId, null);
        } else if ("/help".equals(messageText)) {
            bot.showHelpMenu(chatId, null);
        } else if ("/settings".equals(messageText)) {
            bot.showSettingsMenu(chatId, null);
        }
    }

    public void handleCallback(Update update) {
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

        switch (callbackData) {
            case "menu_help" -> {
                navigationManager.setState(chatId, "HELP");
                bot.showHelpMenu(chatId, messageId);
            }
            case BACK_TO_MAIN_CALLBACK_DATA -> {
                navigationManager.setState(chatId, "MAIN");
                bot.showMainMenu(chatId, messageId);
            }
            case "menu_settings" -> {
                navigationManager.setState(chatId, "SETTINGS");
                bot.showSettingsMenu(chatId, messageId);
            }
            case "menu_chats" -> {
                navigationManager.setState(chatId, "CHATS");
                bot.showChatsMenu(chatId, messageId, channelManager.getAll());
            }
            case "menu_chats_history" -> {
                navigationManager.setState(chatId, "CHATS");
                bot.showChatsHistoryMenu(chatId, messageId, channelManager.getHistory());
            }
            case "menu_chats_upload" -> {
                try {
                    bot.execute(channelManager.uploadData(chatId));
                } catch (TelegramApiException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            case "menu_send" -> {
                bot.showSendMenu(chatId, messageId, channelManager.getAll());
            }
            case "menu_send_message_clear" -> {
                //bot.setMessageForSend(null);
                bot.showMainMenu(chatId, messageId);
            }
            case "menu_send_message" -> {
                List<ChatMembership> chats = channelManager.getAll();
                for (ChatMembership chat : chats) {
                    logger.debug("Try send froward message into {}", chat.getChatId());
                    List<Message> messages = bot.getMessagesForSend()
                            .get(chatId);
                    //                    for (Message message : messages) {
                    //                        sendForwardMessage(chat.getChatId()
                    //                                .toString(), message);
                    //                    }
                    processMediaGroup(chat.getChatId()
                            .toString(), messages);
                }
                bot.showNotification(callbackId, "✅ Сообщение отправлено адресатам");

                //bot.setMessageForSend(null);
                bot.showMainMenu(chatId, messageId);
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

    public void handleForwardMessage(Update update) {
        Message message = update.getMessage();
        logger.debug("Catch message for send with id={}", message.getMessageId());
        Long chatId = message.getChatId();
        bot.getMessagesForSend()
                .computeIfAbsent(chatId, k -> new ArrayList<>())
                .add(message);
        //bot.showSendMenu(chatId, null, channelManager.getAll());
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

    private void sendForwardMessage(String chatId, Message message) {
        try {
            //            if (message.getForwardFromChat() != null) {
            //                ForwardMessage forward = new ForwardMessage();
            //                forward.setChatId(chatId);
            //                forward.setFromChatId(message.getForwardFromChat().getId().toString());
            //                forward.setMessageId(message.getForwardFromMessageId());
            //                bot.execute(forward);
            //            } else {
            CopyMessage copy = new CopyMessage();
            copy.setChatId(chatId);
            copy.setFromChatId(message.getChatId()
                    .toString());
            copy.setMessageId(message.getMessageId());
            bot.execute(copy);
            //            }
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void processMediaGroup(String chatId, List<Message> groupMessages) {
        groupMessages.sort(Comparator.comparing(Message::getMessageId));

        // Создаем медиа-группу
        List<InputMedia> mediaList = new ArrayList<>();
        String caption = null;

        List<Message> textMessages = new ArrayList<>();

        for (Message message : groupMessages) {
            if (message.hasText()) {
                textMessages.add(message);
            } else {
                if (message.hasPhoto()) {
                    InputMediaPhoto photo = new InputMediaPhoto();
                    List<PhotoSize> photos = message.getPhoto();
                    photo.setMedia(photos.get(photos.size() - 1)
                            .getFileId());
                    photo.setParseMode("HTML");
                    if (message.getCaption() != null && caption == null) {
                        caption = message.getCaption();
                        photo.setCaption(caption);
                    }
                    mediaList.add(photo);
                } else if (message.hasVideo()) {
                    InputMediaVideo video = new InputMediaVideo();
                    video.setMedia(message.getVideo()
                            .getFileId());
                    video.setParseMode("HTML");
                    if (message.getCaption() != null && caption == null) {
                        caption = message.getCaption();
                        video.setCaption(caption);
                    }
                    mediaList.add(video);
                }
            }
        }

        for (Message textMessage : textMessages) {
            CopyMessage copyMessage = new CopyMessage();
            copyMessage.setChatId(chatId);
            copyMessage.setFromChatId(textMessage.getChatId()
                    .toString());
            copyMessage.setMessageId(textMessage.getMessageId());
            try {
                bot.execute(copyMessage);
            } catch (TelegramApiException e) {
                logger.error(e.getMessage(), e);
            }
        }
        if (!mediaList.isEmpty()) {
            SendMediaGroup mediaGroup = new SendMediaGroup(chatId, mediaList);
            try {
                bot.execute(mediaGroup);
            } catch (TelegramApiException e) {
                logger.error(e.getMessage(), e);
            }
        }
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
