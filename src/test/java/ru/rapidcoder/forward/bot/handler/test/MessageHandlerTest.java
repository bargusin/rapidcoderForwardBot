package ru.rapidcoder.forward.bot.handler.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.rapidcoder.forward.bot.Bot;
import ru.rapidcoder.forward.bot.dto.AccessRequest;
import ru.rapidcoder.forward.bot.dto.ChatMembership;
import ru.rapidcoder.forward.bot.handler.ChannelManager;
import ru.rapidcoder.forward.bot.handler.MessageHandler;
import ru.rapidcoder.forward.bot.handler.PermissionManager;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

public class MessageHandlerTest {
    private static final String TEST_DB = "test_chat.db";
    private final Long adminUserId = 100L;
    private final ChannelManager mockChannelManager = mock(ChannelManager.class);
    private final PermissionManager mockPermissionManager = mock(PermissionManager.class);
    private MessageHandler messageHandler;
    private Bot botSpy;

    @AfterEach
    void cleanup() {
        new File(TEST_DB).delete();
    }

    @BeforeEach
    void setUp() throws IllegalAccessException, NoSuchFieldException, TelegramApiException {
        Bot bot = new Bot("testBot", "testToken", TEST_DB, List.of(adminUserId));
        botSpy = spy(bot);

        doNothing().when(botSpy)
                .sendMessage(any(), any(), any());
        doNothing().when(botSpy)
                .updateMessage(any(), any(), any(), any());
        doNothing().when(botSpy)
                .showNotification(any(), any());

        messageHandler = new MessageHandler(botSpy, TEST_DB, List.of(adminUserId));

        Field channelManagerField = MessageHandler.class.getDeclaredField("channelManager");
        channelManagerField.setAccessible(true);
        channelManagerField.set(messageHandler, mockChannelManager);

        Field permissionManagerField = MessageHandler.class.getDeclaredField("permissionManager");
        permissionManagerField.setAccessible(true);
        permissionManagerField.set(messageHandler, mockPermissionManager);

        when(mockPermissionManager.hasAccess(adminUserId)).thenReturn(true);
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
        callbackQuery.setId("1");

        update.setCallbackQuery(callbackQuery);

        return update;
    }

    @Test
    void testShowMainMenu() {
        Update update = createUpdateWithText(1L, adminUserId, "/start");
        messageHandler.handleCommand(update);
        verify(botSpy).showMainMenu(1L, null, false);
        verify(botSpy, never()).showMainMenu(2L, null, false);

        update = createUpdateWithText(1L, adminUserId, "/help");
        messageHandler.handleCommand(update);
        verify(botSpy).showMainMenu(1L, null, false);
    }

    @Test
    void testShowMainMenuNotAuthorizedUser() {
        Update update = createUpdateWithText(1L, 2L, "/start");
        messageHandler.handleCommand(update);
        verify(botSpy).showRequestAccessMenu(2L);
        verify(botSpy, never()).showMainMenu(1L, null, false);
    }

    @Test
    void testShowHelpMenu() {
        Update update = createUpdateWithText(1L, adminUserId, "/help");
        messageHandler.handleCommand(update);
        verify(botSpy, never()).showMainMenu(1L, null, true);
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

    @Test
    void testHandleCallbackAccessRequestsMenu() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_access_requests");
        messageHandler.handleCallback(update);
        verify(botSpy).showAccessRequestsMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackGrantedAccessMenu() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_access");
        messageHandler.handleCallback(update);
        verify(botSpy).showGrantedAccessMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackMenuRequestAccess() {
        Update update = createUpdateWithCallbackQuery(1L, 2L, "menu_request_access");
        messageHandler.handleCallback(update);
        verify(mockPermissionManager).saveRequest(any(), any());
        verify(botSpy).sendMessage(any(), any(), any());
    }

    @Test
    void testShowMainMenuAfterGrantedAccess() {
        Update update = createUpdateWithCallbackQuery(1L, 2L, "menu_request_access");
        messageHandler.handleCallback(update);
        verify(mockPermissionManager).saveRequest(any(), any());
        verify(botSpy).sendMessage(any(), any(), any());

        reset(botSpy);

        doNothing().when(botSpy)
                .sendMessage(any(), any(), any());
        when(mockPermissionManager.hasAccess(2L)).thenReturn(true);
        messageHandler.handleCallback(update);
        verify(botSpy).showMainMenu(1L, null, false);
    }

    @Test
    void testHandleCallbackChatToggle() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "chat_toggle_1");
        messageHandler.handleCallback(update);
        verify(botSpy).showSendMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackMenuSendMessage() {
        List<ChatMembership> chats = new ArrayList<>();
        ChatMembership chat = new ChatMembership();
        chat.setChatId(1L);
        chats.add(chat);
        when(mockChannelManager.getAll()).thenReturn(chats);

        Map<Long, List<Message>> map = new HashMap<>();
        map.put(1L, new ArrayList<>());
        when(botSpy.getMessagesForSend()).thenReturn(map);

        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_send_message");
        messageHandler.handleCallback(update);

        verify(botSpy).showNotification(any(), any());
        verify(botSpy).showMainMenu(1L, null, false);
    }

    @Test
    void testHandleCallbackGrantAccessBlocked() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "grant_access_blocked_2");
        messageHandler.handleCallback(update);
        verify(mockPermissionManager).blockedUser(2L);
        verify(botSpy).showGrantedAccessMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackGrantAccessActive() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "grant_access_active_2");
        messageHandler.handleCallback(update);
        verify(mockPermissionManager).activeUser(2L);
        verify(botSpy).showGrantedAccessMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackAccessRequestAccept() {
        AccessRequest request = new AccessRequest();
        request.setUserName("John Smith");
        when(mockPermissionManager.findRequestById(3L)).thenReturn(request);

        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "access_request_accept_3");
        messageHandler.handleCallback(update);
        verify(mockPermissionManager).approvedRequest(3L);
        verify(mockPermissionManager).saveUser(3L, "John Smith");
        verify(botSpy).showAccessRequestsMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackAccessRequestAcceptByNotFoundRequest() {
        AccessRequest request = new AccessRequest();
        request.setUserName("John Smith");
        when(mockPermissionManager.findRequestById(10000L)).thenReturn(request);

        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "access_request_accept_3");
        assertThrows(NullPointerException.class, () -> {
            messageHandler.handleCallback(update);
        });
        verify(mockPermissionManager, never()).approvedRequest(10000L);
        verify(mockPermissionManager, never()).saveUser(3L, "John Smith");
        verify(botSpy, never()).showAccessRequestsMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackAccessRequestReject() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "access_request_reject_3");
        messageHandler.handleCallback(update);
        verify(mockPermissionManager).rejectRequest(3L);
        verify(botSpy).showAccessRequestsMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackBackToMain() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "back_to_main");
        messageHandler.handleCallback(update);
        verify(botSpy).showMainMenu(1L, null, false);
    }

    @Test
    void testHandleCallbackMenuSettings() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_settings");
        messageHandler.handleCallback(update);
        verify(botSpy).showSettingsMenu(1L, null);
    }

    @Test
    void testHandleCallbackMenuChatsUpload() throws TelegramApiException {
        doThrow(new RuntimeException()).when(botSpy)
                .execute(any(SendDocument.class));

        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_chats_upload");
        messageHandler.handleCallback(update);
        verify(mockChannelManager).uploadData(1L);
    }

    @Test
    void testHandleCallbackMenuSend() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_send");
        messageHandler.handleCallback(update);
        verify(botSpy).showSendMenu(any(), any(), any());
    }

    @Test
    void testHandleCallbackMenuSendMessageClear() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "menu_send_message_clear");
        messageHandler.handleCallback(update);
        verify(botSpy).showMainMenu(1L, null, false);
    }

    @Test
    void testHandleCallbackSettingsReset() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "settings_reset");
        messageHandler.handleCallback(update);
        verify(botSpy).showNotification(any(), any());
    }

    @Test
    void testHandleCallbackSettingsSave() {
        Update update = createUpdateWithCallbackQuery(1L, adminUserId, "settings_save");
        messageHandler.handleCallback(update);
        verify(botSpy).showNotification(any(), any());
    }

    @Test
    void testHandleForwardMessage() {
        Update update = createUpdateWithText(1L, adminUserId, "TEST");
        messageHandler.handleForwardMessage(update);
        verify(mockPermissionManager).hasAccess(adminUserId);
    }

    @Test
    void testHandleChatMember() {
        Update update = mock(Update.class);
        ChatMemberUpdated chatMemberUpdated = mock(ChatMemberUpdated.class);
        ChatMember newChatMember = mock(ChatMember.class);
        ChatMember oldChatMember = mock(ChatMember.class);
        Chat chat = mock(Chat.class);
        User user = mock(User.class);

        when(update.getMyChatMember()).thenReturn(chatMemberUpdated);
        when(chatMemberUpdated.getChat()).thenReturn(chat);
        when(chatMemberUpdated.getNewChatMember()).thenReturn(newChatMember);
        when(chatMemberUpdated.getOldChatMember()).thenReturn(oldChatMember);
        when(chatMemberUpdated.getNewChatMember()
                .getStatus()).thenReturn("administrator");

        when(chat.isChannelChat()).thenReturn(true);
        when(chat.getId()).thenReturn(1L);
        when(chat.getTitle()).thenReturn("TestChannel");

        messageHandler.handleChatMember(update);
        verify(mockChannelManager).save(1L, -1L, "not defined", "TestChannel", "channel", "administrator", "not defined");

        when(chat.isChannelChat()).thenReturn(false);
        when(chat.isGroupChat()).thenReturn(true);
        messageHandler.handleChatMember(update);
        verify(mockChannelManager).save(1L, -1L, "not defined", "TestChannel", "group", "administrator", "not defined");

        when(chat.isChannelChat()).thenReturn(false);
        when(chat.isGroupChat()).thenReturn(false);
        when(chat.isSuperGroupChat()).thenReturn(true);
        messageHandler.handleChatMember(update);
        verify(mockChannelManager).save(1L, -1L, "not defined", "TestChannel", "supergroup", "administrator", "not defined");

        when(chat.isChannelChat()).thenReturn(false);
        when(chat.isGroupChat()).thenReturn(false);
        when(chat.isSuperGroupChat()).thenReturn(false);
        when(chat.isUserChat()).thenReturn(true);
        messageHandler.handleChatMember(update);
        verify(mockChannelManager).save(1L, -1L, "not defined", "TestChannel", "private", "administrator", "not defined");

        when(chat.isChannelChat()).thenReturn(false);
        when(chat.isGroupChat()).thenReturn(false);
        when(chat.isSuperGroupChat()).thenReturn(false);
        when(chat.isUserChat()).thenReturn(false);
        messageHandler.handleChatMember(update);
        verify(mockChannelManager).save(1L, -1L, "not defined", "TestChannel", "unknown", "administrator", "not defined");


        when(chatMemberUpdated.getFrom()).thenReturn(user);
        when(chat.isChannelChat()).thenReturn(true);
        when(chatMemberUpdated.getFrom()
                .getId()).thenReturn(2L);
        when(chatMemberUpdated.getFrom()
                .getUserName()).thenReturn("John Smith");
        when(chatMemberUpdated.getOldChatMember()
                .getStatus()).thenReturn("left");
        messageHandler.handleChatMember(update);
        verify(mockChannelManager).save(1L, 2L, "John Smith", "TestChannel", "channel", "administrator", "left");

        when(chatMemberUpdated.getNewChatMember()
                .getStatus()).thenReturn("left");
        messageHandler.handleChatMember(update);
        verify(mockChannelManager).delete(1L);
    }
}
