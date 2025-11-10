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
            chatManager.save(null, null, null, null, null, null, null);
        });

        assertThrows(NullPointerException.class, () -> {
            chatManager.save(1L, null, null, null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            chatManager.save(1L, 2L, null, null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            chatManager.save(1L, 2L, "userName", null, null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            chatManager.save(1L, 2L, "userName", "TestChannel", null, null, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            chatManager.save(1L, 2L, "userName", "TestChannel", "channel", null, null);
        });

        chatManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);
        MonitorChat chat = chatManager.get(1L);
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

        chatManager.save(1L, 2L, "userName", "TestChannel", "channel", "left", "administrator");
        chat = chatManager.get(1L);
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

        chat = chatManager.get(2L);
        assertThat(chat).isNull();
    }

    @Test
    void testGetAll() {
        chatManager.save(1L, 2L, "userName", "TestChannel", "channel", "left", "administrator");

        List<MonitorChat> chats = chatManager.getAll();
        assertThat(chats.size()).isNotZero();

        MonitorChat chat = chats.get(0);
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
    void testDelete() {
        chatManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);

        MonitorChat chat = chatManager.get(1L);
        assertThat(chat).isNotNull();

        chatManager.delete(1L);

        chat = chatManager.get(1L);
        assertThat(chat).isNull();
    }

    @Test
    void testUpdateStatus() {
        chatManager.save(1L, 2L, "userName", "TestChannel", "channel", "administrator", null);

        MonitorChat chat = chatManager.get(1L);
        assertThat(chat).isNotNull();

        chatManager.updateStatus(1L, "left", "administrator");

        MonitorChat modifiedChat = chatManager.get(1L);
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
}
