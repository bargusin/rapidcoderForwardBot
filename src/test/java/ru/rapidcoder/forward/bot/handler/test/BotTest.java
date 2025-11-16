package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.handler.ChannelStorage;
import ru.rapidcoder.forward.bot.handler.MessageHandler;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BotTest {

    @Mock
    private Bot bot;

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Mock
    private User user;

    @Mock
    private CallbackQuery callbackQuery;

    @Mock
    private ChatMemberUpdated chatMember;

    private static final String TEST_DB = "test_chat.db";

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() {
        new File(TEST_DB).delete();
    }

    @BeforeEach
    void setUp() {
        bot = spy(new Bot("", "", TEST_DB, List.of(100L)));

        doNothing().when(bot)
                .handleCommand(update);
        doNothing().when(bot)
                .handleForwardMessage(update);
        doNothing().when(bot)
                .handleCallback(update);
        doNothing().when(bot)
                .handleChatMember(update);
    }

    @Test
    void testTextMessageHandling() {
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(2L);
        when(message.getChatId()).thenReturn(1L);
        when(message.getForwardDate()).thenReturn(null);

        bot.onUpdateReceived(update);

        verify(bot, times(1)).handleCommand(update);
        verify(bot, never()).handleForwardMessage(update);
        verify(bot, never()).handleCallback(update);
        verify(bot, never()).handleChatMember(update);
    }

    @Test
    void testForwardedMessageHandling() {
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(123L);
        when(message.getChatId()).thenReturn(456L);
        when(message.getForwardDate()).thenReturn(123456789); // Пересланное сообщение
        when(message.hasText()).thenReturn(true); // Может иметь текст

        bot.onUpdateReceived(update);

        verify(bot, times(1)).handleForwardMessage(update);
        verify(bot, never()).handleCommand(update);
        verify(bot, never()).handleCallback(update);
        verify(bot, never()).handleChatMember(update);
    }

    @Test
    void testCallbackQueryHandling() {
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.hasMessage()).thenReturn(false);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);

        bot.onUpdateReceived(update);

        verify(bot, times(1)).handleCallback(update);
        verify(bot, never()).handleCommand(update);
        verify(bot, never()).handleForwardMessage(update);
        verify(bot, never()).handleChatMember(update);
    }

    @Test
    void testChatMemberUpdateHandling() {
        when(update.hasMyChatMember()).thenReturn(true);
        when(update.hasMessage()).thenReturn(false);
        when(update.hasCallbackQuery()).thenReturn(false);
        when(update.getMyChatMember()).thenReturn(chatMember);

        bot.onUpdateReceived(update);

        verify(bot, times(1)).handleChatMember(update);
        verify(bot, never()).handleCommand(update);
        verify(bot, never()).handleForwardMessage(update);
        verify(bot, never()).handleCallback(update);
    }

    @Test
    void testMessageWithoutText() {
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getFrom()).thenReturn(user);
        when(user.getId()).thenReturn(123L);
        when(message.getChatId()).thenReturn(456L);
        when(message.getForwardDate()).thenReturn(null);
        when(message.hasText()).thenReturn(false); // Сообщение без текста

        bot.onUpdateReceived(update);

        verify(bot, never()).handleCommand(update);
        verify(bot, never()).handleForwardMessage(update);
        verify(bot, never()).handleCallback(update);
        verify(bot, never()).handleChatMember(update);
    }
}
