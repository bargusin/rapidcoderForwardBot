package ru.rapidcoder.forward.bot.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.Bot;

import static ru.rapidcoder.forward.bot.Bot.BACK_TO_MAIN_CALLBACK_DATA;

public class MessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    private final NavigationManager navigationManager;
    private final ChatManager chatManager;
    private final Bot bot;

    public MessageHandler(Bot bot) {
        String environment = System.getenv("botEnv") != null ? System.getenv("botEnv") : "dev";
        navigationManager = new NavigationManager(System.getenv(environment + "MenuStorageFile"));
        chatManager = new ChatManager(System.getenv(environment + "ChatStorageFile"));

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
                bot.showChatsMenu(chatId, messageId, chatManager.getAll());
            }
            case "menu_chats_upload" -> {
                try {
                    bot.execute(chatManager.uploadData(chatId));
                } catch (TelegramApiException e) {
                    logger.error(e.getMessage(), e);
                }
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
        //TODO
    }

    public void handleChatMember(Update update) {
        ChatMemberUpdated chatMember = update.getMyChatMember();
        Chat chat = chatMember.getChat();
        String status = chatMember.getNewChatMember()
                .getStatus();
        Long chatId = chat.getId();
        String chatType = getChatType(chat);
        logger.info("Bot's status changed from chat '{}' to '{}'", chat.getTitle(), status);
        switch (status) {
            case "administrator", "restricted":
                chatManager.save(chatId, chat.getTitle(), chatType, status);
                break;
            case "member":
                break;
            case "left", "kicked":
                chatManager.delete(chatId);
                break;
            default:
                logger.info("Unknown status {}", status);
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
}
