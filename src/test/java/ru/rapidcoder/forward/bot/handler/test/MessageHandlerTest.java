package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.handler.MessageHandler;

import static org.mockito.Mockito.*;

public class MessageHandlerTest {
    private MessageHandler messageHandler;
    private Bot botSpy;

    @BeforeEach
    void setUp() {
        Bot bot = new Bot("testBot", "testToken", "/tmp/test_storage.db");
        botSpy = spy(bot);

        doNothing().when(botSpy)
                .sendMessage(any(), any(), any());
        doNothing().when(botSpy)
                .updateMessage(any(), any(), any(), any());

        messageHandler = new MessageHandler(botSpy, "/tmp/test_storage.db");
    }

    private Update createUpdateWithText(Long chatId, String text) {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);

        message.setChat(chat);
        message.setText(text);

        update.setMessage(message);

        return update;
    }

    private Update createUpdateWithCallbackQuery(Long chatId, String callbackData) {
        Update update = new Update();
        Message message = new Message();
        CallbackQuery callbackQuery = new CallbackQuery();
        Chat chat = new Chat();
        chat.setId(chatId);

        message.setChat(chat);

        callbackQuery.setData(callbackData);
        callbackQuery.setMessage(message);

        update.setCallbackQuery(callbackQuery);

        return update;
    }

    @Test
    void testShowMainMenu() {
        Update update = createUpdateWithText(1L, "/start");
        messageHandler.handleCommand(update);

        verify(botSpy).showMainMenu(1L, null);
        verify(botSpy, never()).showMainMenu(2L, null);

        update = createUpdateWithText(1L, "/help");
        messageHandler.handleCommand(update);
        verify(botSpy).showMainMenu(1L, null);
    }

    @Test
    void testShowHelpMenu() {
        Update update = createUpdateWithText(1L, "/help");
        messageHandler.handleCommand(update);
        verify(botSpy, never()).showMainMenu(1L, null);
        verify(botSpy).showHelpMenu(1L, null);
    }

    @Test
    void testShowSettingsMenu() {
        Update update = createUpdateWithText(1L, "/settings");
        messageHandler.handleCommand(update);
        verify(botSpy).showSettingsMenu(1L, null);
    }

    @Test
    void testCatchTextForwardMessage() {
        Update update = createUpdateWithText(1L, "TEST");
        messageHandler.handleCommand(update);
        verify(botSpy).showSendMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackMenuHelp() {
        Update update = createUpdateWithCallbackQuery(1L, "menu_help");
        messageHandler.handleCallback(update);
        verify(botSpy).showHelpMenu(1L, null);
    }

    @Test
    void testHandleCallbackMenuChatHistory() {
        Update update = createUpdateWithCallbackQuery(1L, "menu_chats_history");
        messageHandler.handleCallback(update);
        verify(botSpy).showChatsHistoryMenu(any(), any(), any());
    }
}
