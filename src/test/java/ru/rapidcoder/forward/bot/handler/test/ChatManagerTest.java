package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.rapidcoder.forward.bot.component.MonitorChat;
import ru.rapidcoder.forward.bot.handler.ChatManager;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;

public class ChatManagerTest {

    private static final String TEST_DB = "test_chat.db";
    private ChatManager chatManager;

    @BeforeAll
    static void cleanup() {
        new File(TEST_DB).delete();
    }

    @BeforeEach
    void setUp() {
        chatManager = new ChatManager(TEST_DB);
    }

    @Test
    void testSaveOrUpdate() {
        assertThrows(NullPointerException.class, () -> {
            chatManager.save(null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            chatManager.save(1L, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            chatManager.save(1L, "TestChannel", null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            chatManager.save(1L, "TestChannel", "channel", null);
        });

        chatManager.save(1L, "TestChannel", "channel", "administrator");

        MonitorChat chat = chatManager.get(1L);
        assertThat(chat.getChatId()).isEqualTo(1L);
        assertThat(chat.getChatType()).isEqualTo("channel");
        assertThat(chat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(chat.getBotStatus()).isEqualTo("administrator");
        assertThat(chat.getAddedDate()).isNotNull();
        assertThat(chat.getLastActivity()).isNotNull();
        assertThat(chat.toString()).isNotNull();

        chat = chatManager.get(2L);
        assertThat(chat).isNull();
    }

    @Test
    void testGetAll() {
        chatManager.save(1L, "TestChannel", "channel", "administrator");

        List<MonitorChat> chats = chatManager.getAll();
        assertThat(chats.size()).isNotZero();

        MonitorChat chat = chats.get(0);
        assertThat(chat.getChatId()).isEqualTo(1L);
        assertThat(chat.getChatType()).isEqualTo("channel");
        assertThat(chat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(chat.getBotStatus()).isEqualTo("administrator");
        assertThat(chat.getAddedDate()).isNotNull();
        assertThat(chat.getLastActivity()).isNotNull();
    }

    @Test
    void testDelete() {
        chatManager.save(1L, "TestChannel", "channel", "administrator");

        MonitorChat chat = chatManager.get(1L);
        assertThat(chat).isNotNull();

        chatManager.delete(1L);

        chat = chatManager.get(1L);
        assertThat(chat).isNull();
    }

    @Test
    void testUpdateStatus() {
        chatManager.save(1L, "TestChannel", "channel", "administrator");

        MonitorChat chat = chatManager.get(1L);
        assertThat(chat).isNotNull();

        chatManager.updateStatus(1L, "left");

        MonitorChat modifiedChat = chatManager.get(1L);
        assertThat(modifiedChat.getChatId()).isEqualTo(1L);
        assertThat(modifiedChat.getChatType()).isEqualTo("channel");
        assertThat(modifiedChat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(modifiedChat.getBotStatus()).isEqualTo("left");
        assertThat(modifiedChat.getAddedDate()).isEqualTo(chat.getAddedDate());
    }
}
