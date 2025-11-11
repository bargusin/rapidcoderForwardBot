package ru.rapidcoder.forward.bot.handler;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.rapidcoder.forward.bot.dto.ChatMembership;
import ru.rapidcoder.forward.bot.dto.HistoryChatMembership;

import java.io.File;
import java.util.List;

public class ChannelManager {

    private final ChannelStorage storage;
    private final String storageFile;

    public ChannelManager(String storageFile) {
        this.storageFile = storageFile;
        this.storage = ChannelStorage.getInstance(storageFile);
    }

    /**
     * Сохранить информацию о канале/группе
     *
     * @param chatId    идентификатор чата
     * @param userId    идетификатор пользователя
     * @param userName  имя пользователя
     * @param title     название чата
     * @param type      тип чата
     * @param newStatus новый статус бота в чате
     * @param oldStatus текущий статус бота в чате
     */
    public void save(Long chatId, Long userId, String userName, String title, String type, String newStatus, String oldStatus) {
        ChatMembership chat = new ChatMembership();
        chat.setChatId(chatId);
        chat.setUserId(userId);
        chat.setUserName(userName);
        chat.setChatTitle(title);
        chat.setChatType(type);
        chat.setBotNewStatus(newStatus);
        chat.setBotOldStatus(oldStatus);
        storage.saveOrUpdateChat(chat);
    }

    /**
     * Получить информацию о канале/группе
     *
     * @param chatId идентификатор чата
     * @return информация о канале/группе
     */
    public ChatMembership get(Long chatId) {
        return storage.findChatById(chatId);
    }

    /**
     * Удалить канал/группу
     *
     * @param chatId идентификатор чата
     */
    public void delete(Long chatId) {
        storage.deleteChat(chatId);
    }

    /**
     * Обновить статус бота в канале/группе
     *
     * @param chatId идентификатор чата
     * @param newStatus новый статус бота
     * @param oldStatus текущий статус бота
     */
    public void updateStatus(Long chatId, String newStatus, String oldStatus) {
        storage.updateBotStatus(chatId, newStatus, oldStatus);
    }

    /**
     * Получить список всех каналов/групп, на которые подписан бот
     *
     * @return список каналов/групп подписки бота
     */
    public List<ChatMembership> getAll() {
        return storage.getAllChats();
    }

    /**
     * Получить историю подписок бота
     *
     * @return история подписок бота
     */
    public List<HistoryChatMembership> getHistory() {
        return storage.getHistoryChats();
    }

    public SendDocument uploadData(Long chatId) {
        File file = new File(storageFile);
        SendDocument document = new SendDocument();
        document.setChatId(chatId);
        document.setDocument(new InputFile(file, file.getName()));
        return document;
    }
}
