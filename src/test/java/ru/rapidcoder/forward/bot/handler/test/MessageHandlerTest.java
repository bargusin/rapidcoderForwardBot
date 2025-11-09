package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.handler.MessageHandler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MessageHandlerTest {

    @InjectMocks
    private MessageHandler messageHandler;
    @Mock
    private Bot mockBot;

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

    @Test
    void testShowMainMenu() {
        Update update = createUpdateWithText(1L, "/start");
        messageHandler.handleCommand(update);
        verify(mockBot).showMainMenu(1L, null);
        verify(mockBot, never()).showMainMenu(2L, null);

        update = createUpdateWithText(1L, "/help");
        messageHandler.handleCommand(update);
        verify(mockBot).showMainMenu(1L, null);
    }

    @Test
    void testShowHelpMenu() {
        Update update = createUpdateWithText(1L, "/help");
        messageHandler.handleCommand(update);
        verify(mockBot, never()).showMainMenu(1L, null);
        verify(mockBot).showHelpMenu(1L, null);
    }

    @Test
    void testSettingsMenu() {
        Update update = createUpdateWithText(1L, "/settings");
        messageHandler.handleCommand(update);
        verify(mockBot, never()).showMainMenu(1L, null);
        //TODO
    }
}
