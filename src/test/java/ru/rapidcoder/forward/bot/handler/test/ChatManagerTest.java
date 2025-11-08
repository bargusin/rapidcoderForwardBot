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
        chatManager = ChatManager.getInstance(TEST_DB);
    }

    @Test
    public void testSaveOrUpdateChat() {
        assertThrows(NullPointerException.class, () -> {
            chatManager.saveOrUpdateChat(null);
        });

        assertThrows(NullPointerException.class, () -> {
            MonitorChat failChat = new MonitorChat();
            chatManager.saveOrUpdateChat(failChat);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            MonitorChat failChat = new MonitorChat();
            failChat.setChatId(1L);
            chatManager.saveOrUpdateChat(failChat);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            MonitorChat failChat = new MonitorChat();
            failChat.setChatId(1L);
            failChat.setChatType("channel");
            chatManager.saveOrUpdateChat(failChat);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            MonitorChat failChat = new MonitorChat();
            failChat.setChatId(1L);
            failChat.setChatType("channel");
            failChat.setChatTitle("TestChannel");
            chatManager.saveOrUpdateChat(failChat);
        });

        MonitorChat chat = new MonitorChat();
        chat.setChatId(1L);
        chat.setChatType("channel");
        chat.setChatTitle("TestChannel");
        chat.setBotStatus("administrator");
        chatManager.saveOrUpdateChat(chat);

        chat = chatManager.findChatById(1L);
        assertThat(chat.getChatId()).isEqualTo(1L);
        assertThat(chat.getChatType()).isEqualTo("channel");
        assertThat(chat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(chat.getBotStatus()).isEqualTo("administrator");
        assertThat(chat.getAddedDate()).isNotNull();
        assertThat(chat.getLastActivity()).isNotNull();
        assertThat(chat.toString()).isNotNull();

        chat = chatManager.findChatById(2L);
        assertThat(chat).isNull();
    }

    @Test
    public void testGetAllChats() {
        MonitorChat chat = new MonitorChat();
        chat.setChatId(1L);
        chat.setChatType("channel");
        chat.setChatTitle("TestChannel");
        chat.setBotStatus("administrator");
        chatManager.saveOrUpdateChat(chat);

        List<MonitorChat> chats = chatManager.getAllChats();
        assertThat(chats.size()).isEqualTo(1);

        chat = chats.get(0);
        assertThat(chat.getChatId()).isEqualTo(1L);
        assertThat(chat.getChatType()).isEqualTo("channel");
        assertThat(chat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(chat.getBotStatus()).isEqualTo("administrator");
        assertThat(chat.getAddedDate()).isNotNull();
        assertThat(chat.getLastActivity()).isNotNull();
    }

    @Test
    public void testDeleteChat() {
        MonitorChat chat = new MonitorChat();
        chat.setChatId(1L);
        chat.setChatType("channel");
        chat.setChatTitle("TestChannel");
        chat.setBotStatus("administrator");
        chatManager.saveOrUpdateChat(chat);

        chat = chatManager.findChatById(1L);
        assertThat(chat).isNotNull();

        chatManager.deleteChat(1L);

        chat = chatManager.findChatById(1L);
        assertThat(chat).isNull();
    }

    @Test
    public void testUpdateBotStatus() throws InterruptedException {
        MonitorChat chat = new MonitorChat();
        chat.setChatId(1L);
        chat.setChatType("channel");
        chat.setChatTitle("TestChannel");
        chat.setBotStatus("administrator");
        chatManager.saveOrUpdateChat(chat);

        chat = chatManager.findChatById(1L);
        assertThat(chat).isNotNull();

        chatManager.updateBotStatus(1L, "left");

        MonitorChat modifiedChat = chatManager.findChatById(1L);
        assertThat(modifiedChat.getChatId()).isEqualTo(1L);
        assertThat(modifiedChat.getChatType()).isEqualTo("channel");
        assertThat(modifiedChat.getChatTitle()).isEqualTo("TestChannel");
        assertThat(modifiedChat.getBotStatus()).isEqualTo("left");
        assertThat(modifiedChat.getAddedDate()).isEqualTo(chat.getAddedDate());
    }
}
