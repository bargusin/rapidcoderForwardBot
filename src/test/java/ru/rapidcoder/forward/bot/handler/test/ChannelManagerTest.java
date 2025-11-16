package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.*;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import ru.rapidcoder.forward.bot.dto.ChatMembership;
import ru.rapidcoder.forward.bot.dto.HistoryChatMembership;
import ru.rapidcoder.forward.bot.handler.ChannelManager;
import ru.rapidcoder.forward.bot.handler.ChannelStorage;
import ru.rapidcoder.forward.bot.handler.PermissionStorage;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

public class ChannelManagerTest {

    private static final String TEST_DB = "test_chat.db";
    private ChannelManager channelManager;

    @BeforeEach
    void setUp() throws Exception {
        resetChannelStorageSingleton();
        channelManager = new ChannelManager(TEST_DB);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() throws Exception {
        new File(TEST_DB).delete();
    }

    private void resetChannelStorageSingleton() throws Exception {
        Field instanceField = ChannelStorage.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSaveOrUpdateChat() {
        assertThrows(NullPointerException.class, () -> {
            channelManager.save(null, null, null, null, null, null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            channelManager.save(1L, null, null, null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            channelManager.save(1L, 2L, null, null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            channelManager.save(1L, 2L, "userName", null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            channelManager.save(1L, 2L, "userName", "TestChannel", null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            channelManager.save(1L, 2L, "userName", "TestChannel", "channel", null, null);
        });

        channelManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);
        ChatMembership chat = channelManager.get(1L);
        assertThat(chat.getChatId()).isEqualTo(1L);
        assertThat(chat.getUserId()).isEqualTo(2L);
        assertThat(chat.getUserName()).isEqualTo("userName");
        assertThat(chat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(chat.getChatType()).isEqualTo("channel");
        assertThat(chat.getBotNewStatus()).isEqualTo("administrator");
        assertThat(chat.getBotOldStatus()).isNull();
        assertThat(chat.getAddedDate()).isNotNull();
        assertThat(chat.getUpdatedDate()).isNotNull();
        assertThat(chat.toString()).isNotNull();

        channelManager.save(1L, 2L, "userName", "TestChannel", "channel", "left", "administrator");
        chat = channelManager.get(1L);
        assertThat(chat.getChatId()).isEqualTo(1L);
        assertThat(chat.getUserId()).isEqualTo(2L);
        assertThat(chat.getUserName()).isEqualTo("userName");
        assertThat(chat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(chat.getChatType()).isEqualTo("channel");
        assertThat(chat.getBotNewStatus()).isEqualTo("left");
        assertThat(chat.getBotOldStatus()).isEqualTo("administrator");
        assertThat(chat.getAddedDate()).isNotNull();
        assertThat(chat.getUpdatedDate()).isNotNull();
        assertThat(chat.toString()).isNotNull();

        chat = channelManager.get(2L);
        assertThat(chat).isNull();
    }

    @Test
    void testSaveHistorySending() {
        assertThrows(NullPointerException.class, () -> {
            channelManager.saveHistorySending(null, null, null, null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            channelManager.saveHistorySending(1L, null, null, null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            channelManager.saveHistorySending(1L, 2L, null, null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            channelManager.saveHistorySending(1L, 2L, "userName", null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            channelManager.saveHistorySending(1L, 2L, "userName", "TestChannel", null);
        });

        channelManager.saveHistorySending(1L, 2L, "userName", "TestChannel", 3);
        assertThat(channelManager.getHistorySending()
                .size()).isNotZero();

    }

    @Test
    void testGetAllChat() {
        channelManager.save(1L, 2L, "userName", "TestChannel", "channel", "left", "administrator");

        List<ChatMembership> chats = channelManager.getAll();
        assertThat(chats.size()).isNotZero();

        ChatMembership chat = chats.get(0);
        assertThat(chat.getChatId()).isEqualTo(1L);
        assertThat(chat.getUserId()).isEqualTo(2L);
        assertThat(chat.getUserName()).isEqualTo("userName");
        assertThat(chat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(chat.getChatType()).isEqualTo("channel");
        assertThat(chat.getBotNewStatus()).isEqualTo("left");
        assertThat(chat.getBotOldStatus()).isEqualTo("administrator");
        assertThat(chat.getAddedDate()).isNotNull();
        assertThat(chat.getUpdatedDate()).isNotNull();
        assertThat(chat.toString()).isNotNull();
    }

    @Test
    void testDeleteChat() {
        channelManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);

        ChatMembership chat = channelManager.get(1L);
        assertThat(chat).isNotNull();

        channelManager.delete(1L);

        chat = channelManager.get(1L);
        assertThat(chat).isNull();
    }

    @Test
    void testUpdateBotStatus() {
        channelManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);

        ChatMembership chat = channelManager.get(1L);
        assertThat(chat).isNotNull();

        channelManager.updateStatus(1L, "left", "administrator");

        ChatMembership modifiedChat = channelManager.get(1L);
        assertThat(modifiedChat.getChatId()).isEqualTo(chat.getChatId());
        assertThat(modifiedChat.getUserId()).isEqualTo(chat.getUserId());
        assertThat(modifiedChat.getChatTitle()).isEqualTo(chat.getChatTitle());
        assertThat(modifiedChat.getChatType()).isEqualTo(chat.getChatType());
        assertThat(modifiedChat.getBotNewStatus()).isEqualTo("left");
        assertThat(modifiedChat.getBotOldStatus()).isEqualTo("administrator");
        assertThat(modifiedChat.getAddedDate()).isEqualTo(chat.getAddedDate());
        assertThat(modifiedChat.getUpdatedDate()).isNotNull();
        assertThat(modifiedChat.toString()).isNotNull();
    }

    @Test
    void testUploadData() {
        channelManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);

        ChatMembership chat = channelManager.get(1L);
        assertThat(chat).isNotNull();

        SendDocument document = channelManager.uploadData(1L);
        assertThat(document.getChatId()).isEqualTo(chat.getChatId()
                .toString());
        assertThat(document.getDocument()).isNotNull();
    }

    @Test
    void testGetHistory() {
        channelManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);

        ChatMembership chat = channelManager.get(1L);
        assertThat(chat).isNotNull();

        List<HistoryChatMembership> history = channelManager.getHistory();
        assertThat(history.size()).isNotZero();
    }
}
