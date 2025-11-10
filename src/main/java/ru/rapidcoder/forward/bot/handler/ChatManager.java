package ru.rapidcoder.forward.bot.handler;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.rapidcoder.forward.bot.component.MonitorChat;

import java.io.File;
import java.util.List;

public class ChatManager {

    private final ChatStorage storage;
    private final String storageFile;

    public ChatManager(String storageFile) {
        this.storageFile = storageFile;
        this.storage = ChatStorage.getInstance(storageFile);
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
        MonitorChat chat = new MonitorChat();
        chat.setChatId(chatId);
        chat.setUserId(userId);
        chat.setUserName(userName);
        chat.setChatTitle(title);
        chat.setChatType(type);
        chat.setBotNewStatus(newStatus);
        chat.setBotOldStatus(oldStatus);
        storage.saveOrUpdate(chat);
    }

    /**
     * Получить информацию о канале/группе
     *
     * @param chatId идентификатор чата
     * @return информация о канале/группе
     */
    public MonitorChat get(Long chatId) {
        return storage.findChatById(chatId);
    }

    /**
     * Удалить канал/группу
     *
     * @param chatId идентификатор чата
     */
    public void delete(Long chatId) {
        storage.delete(chatId);
    }

    /**
     * Обновить статус бота в канале/группе
     *
     * @param chatId идентификатор чата
     * @param newStatus новый статус бота
     * @param oldStatus текущий статус бота
     */
    public void updateStatus(Long chatId, String newStatus, String oldStatus) {
        storage.updateStatus(chatId, newStatus, oldStatus);
    }

    /**
     * Получить список всех каналов/групп, на которые подписан бот
     *
     * @return список каналов/групп подписки бота
     */
    public List<MonitorChat> getAll() {
        return storage.getAll();
    }

    public SendDocument uploadData(Long chatId) {
        File file = new File(storageFile);
        SendDocument document = new SendDocument();
        document.setChatId(chatId);
        document.setDocument(new InputFile(file, file.getName()));
        return document;
    }
}
