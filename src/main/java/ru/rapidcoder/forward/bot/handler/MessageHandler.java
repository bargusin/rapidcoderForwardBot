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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static ru.rapidcoder.forward.bot.Bot.BACK_TO_MAIN_CALLBACK_DATA;

public class MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final ChannelManager channelManager;
    private final Bot bot;
    private final Map<Long, ScheduledFuture<?>> userTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public MessageHandler(Bot bot, String storageFile) {
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
                bot.showHelpMenu(chatId, messageId);
            }
            case BACK_TO_MAIN_CALLBACK_DATA -> {
                bot.showMainMenu(chatId, messageId);
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
                } catch (TelegramApiException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            case "menu_send" -> {
                bot.showSendMenu(chatId, messageId, channelManager.getAll());
            }
            case "menu_send_message_clear" -> {
                bot.getMessagesForSend()
                        .put(chatId, new ArrayList<>());
                bot.showMainMenu(chatId, messageId);
            }
            case "menu_send_message" -> {
                List<ChatMembership> chats = channelManager.getAll();
                for (ChatMembership chat : chats) {
                    logger.debug("Try send forward message into {}", chat.getChatId());
                    sendForwardMessage(chat.getChatId()
                            .toString(), bot.getMessagesForSend()
                            .get(chatId));
                }
                bot.showNotification(callbackId, "✅ Сообщение отправлено адресатам");
                bot.getMessagesForSend()
                        .put(chatId, new ArrayList<>());
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

    private void sendForwardMessage(String chatId, List<Message> groupMessages) {
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
            SendMediaGroup mediaGroup = new SendMediaGroup(chatId, mediaList);
            try {
                bot.execute(mediaGroup);
            } catch (TelegramApiException e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (!groupMessages.isEmpty() && mediaList.size() < 2) {
            sendCopyMessage(chatId, groupMessages.get(0));
        }
    }

    public void sendCopyMessage(String chatId, Message message) {
        CopyMessage copy = new CopyMessage();
        copy.setChatId(chatId);
        copy.setFromChatId(message.getChatId()
                .toString());
        copy.setMessageId(message.getMessageId());
        copy.setCaptionEntities(message.getCaptionEntities());
        try {
            bot.execute(copy);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage(), e);
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
