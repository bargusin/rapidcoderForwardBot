package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.handler.MessageHandler;

import java.io.File;
import java.util.List;

import static org.mockito.Mockito.*;

public class MessageHandlerTest {
    private static final String TEST_DB = "test_chat.db";
    private final Long adminUserId = 100L;
    private MessageHandler messageHandler;
    private Bot botSpy;

    @BeforeAll
    @AfterAll
    static void cleanup() {
        new File(TEST_DB).delete();
    }

    @BeforeEach
    void setUp() {
        Bot bot = new Bot("testBot", "testToken", TEST_DB, List.of(100L));
        botSpy = spy(bot);

        doNothing().when(botSpy)
                .sendMessage(any(), any(), any());
        doNothing().when(botSpy)
                .updateMessage(any(), any(), any(), any());

        messageHandler = new MessageHandler(botSpy, TEST_DB, List.of(adminUserId));
    }

    private Update createUpdateWithText(Long chatId, Long userId, String text) {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        User user = new User();
        chat.setId(chatId);
        user.setId(userId);
        message.setChat(chat);
        message.setText(text);
        message.setFrom(user);

        update.setMessage(message);

        return update;
    }

    private Update createUpdateWithCallbackQuery(Long chatId, Long userId, String callbackData) {
        Update update = new Update();
        Message message = new Message();
        CallbackQuery callbackQuery = new CallbackQuery();
        Chat chat = new Chat();
        User user = new User();
        user.setId(userId);
        chat.setId(chatId);
        message.setChat(chat);
        callbackQuery.setData(callbackData);
        callbackQuery.setMessage(message);
        callbackQuery.setFrom(user);

        update.setCallbackQuery(callbackQuery);

        return update;
    }

    @Test
    void testShowMainMenu() {
        Update update = createUpdateWithText(1L, adminUserId, "/start");
        messageHandler.handleCommand(update);

        verify(botSpy).showMainMenu(1L, null);
        verify(botSpy, never()).showMainMenu(2L, null);

        update = createUpdateWithText(1L, adminUserId, "/help");
        messageHandler.handleCommand(update);
        verify(botSpy).showMainMenu(1L, null);
    }

    @Test
    void testShowMainMenuNotAuthorizedUser() {
        Update update = createUpdateWithText(1L, 2L, "/start");
        messageHandler.handleCommand(update);
        verify(botSpy).showRequestAccessMenu(2L);
        verify(botSpy, never()).showMainMenu(1L, null);
    }

    @Test
    void testShowHelpMenu() {
        Update update = createUpdateWithText(1L, adminUserId, "/help");
        messageHandler.handleCommand(update);
        verify(botSpy, never()).showMainMenu(1L, null);
        verify(botSpy).showHelpMenu(1L, null);
    }

    @Test
    void testShowSettingsMenu() {
        Update update = createUpdateWithText(1L, adminUserId, "/settings");
        messageHandler.handleCommand(update);
        verify(botSpy).showSettingsMenu(1L, null);
    }

    @Test
    void testCatchTextForwardMessage() {
        Update update = createUpdateWithText(1L, adminUserId, "TEST");
        messageHandler.handleCommand(update);
        verify(botSpy).showSendMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackMenuHelp() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_help");
        messageHandler.handleCallback(update);
        verify(botSpy).showHelpMenu(1L, null);
    }

    @Test
    void testHandleCallbackMenuHelpNotAuthorizedUser() {
        Update update = createUpdateWithCallbackQuery(1L, 2L, "menu_help");
        messageHandler.handleCallback(update);
        verify(botSpy, never()).showHelpMenu(1L, null);
    }

    @Test
    void testHandleCallbackMenuChatHistory() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_chats_history");
        messageHandler.handleCallback(update);
        verify(botSpy).showChatsHistoryMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackMenuSendingHistory() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_sending_history");
        messageHandler.handleCallback(update);
        verify(botSpy).showSendingHistoryMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackMenuChats() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_chats");
        messageHandler.handleCallback(update);
        verify(botSpy).showChatsMenu(any(), any(), any());
    }
}
