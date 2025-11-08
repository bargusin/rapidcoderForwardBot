package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.handler.ChatManager;
import ru.rapidcoder.forward.bot.handler.UserSettingsManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class MessageHandlerTest {

    @Mock
    private UserSettingsManager userSettingsManager;
    @Mock
    private ChatManager chatManager;
    @Spy
    private Bot botSpy;

    @BeforeEach
    void setUp() {
        Bot bot = new Bot("testBot", "testToken");
        botSpy = spy(bot);
    }

    @Test
    void testHandleCommand() {
        Update update = createTextUpdate(123L, 456L, "/start");
        ArgumentCaptor<Update> messageCaptor = ArgumentCaptor.forClass(Update.class);

        doNothing().when(botSpy)
                .handleCommand(any());

        botSpy.onUpdateReceived(update);

        verify(botSpy, times(1)).handleCommand(messageCaptor.capture());

        update = messageCaptor.getValue();
        assertEquals("456", update.getMessage()
                .getChatId()
                .toString());
        assertTrue(update.getMessage()
                .getText()
                .contains("/start"));
    }

    @Test
    void testHandleCallback() {
        Update update = createCallbackUpdate(123L, 456L, 789, "menu_settings");
        ArgumentCaptor<Update> messageCaptor = ArgumentCaptor.forClass(Update.class);

        doNothing().when(botSpy)
                .handleCallback(any());

        botSpy.onUpdateReceived(update);

        verify(botSpy, times(1)).handleCallback(messageCaptor.capture());

        update = messageCaptor.getValue();
        assertEquals("456", update.getMessage()
                .getChatId()
                .toString());
    }

    private Update createTextUpdate(Long userId, Long chatId, String text) {
        Update update = mock(Update.class);
        User user = mock(User.class);
        Chat chat = mock(Chat.class);
        Message message = mock(Message.class);

        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);

        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(text);
        when(message.getChat()).thenReturn(chat);
        when(message.getFrom()).thenReturn(user);
        when(message.getChatId()).thenReturn(chatId);
        when(message.getForwardDate()).thenReturn(null);

        when(chat.getId()).thenReturn(chatId);
        when(user.getId()).thenReturn(userId);
        return update;
    }

    private Update createCallbackUpdate(Long userId, Long chatId, Integer messageId, String callbackData) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        User user = mock(User.class);
        Chat chat = mock(Chat.class);
        Message message = mock(Message.class);

        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(update.getMessage()).thenReturn(message);

        when(callbackQuery.getData()).thenReturn(callbackData);
        when(callbackQuery.getId()).thenReturn("test_callback_id");
        when(callbackQuery.getFrom()).thenReturn(user);
        when(callbackQuery.getMessage()).thenReturn(message);

        when(user.getId()).thenReturn(userId);

        when(message.getChat()).thenReturn(chat);
        when(message.getMessageId()).thenReturn(messageId);
        when(message.getChatId()).thenReturn(chatId);
        when(chat.getId()).thenReturn(chatId);

        return update;
    }
}
