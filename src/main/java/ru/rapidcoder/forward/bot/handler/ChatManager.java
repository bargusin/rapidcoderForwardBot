package ru.rapidcoder.forward.bot.handler;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import ru.rapidcoder.forward.bot.component.MonitorChat;

import java.io.File;
import java.util.List;

public class ChatManager {

    private final ChatStorage storage;
    private String storageFile;

    public ChatManager(String storageFile) {
        this.storageFile = storageFile;
        this.storage = ChatStorage.getInstance(storageFile);
    }

    /**
     * Сохранить информацию о канале/группе
     *
     * @param chatId идентификатор чата
     * @param title название чата
     * @param type тип чата
     * @param status статус бота в чате
     */
    public void save(Long chatId, String title, String type, String status) {
        MonitorChat chat = new MonitorChat();
        chat.setChatId(chatId);
        chat.setChatTitle(title);
        chat.setChatType(type);
        chat.setBotStatus(status);
        storage.saveOrUpdateChat(chat);
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
        storage.deleteChat(chatId);
    }

    /**
     * Обновить статус бота в канале/группе
     *
     * @param chatId идентификатор чата
     * @param status новый статус бота
     */
    public void updateStatus(Long chatId, String status) {
        storage.updateBotStatus(chatId, status);
    }

    /**
     * Получить список всех каналов/групп, на которые подписан бот
     *
     * @return список каналов/групп подписки бота
     */
    public List<MonitorChat> getAll() {
        return storage.getAllChats();
    }

    public SendDocument uploadData(Long chatId) {
        File file = new File(storageFile);
        SendDocument document = new SendDocument();
        document.setChatId(chatId);
        document.setDocument(new InputFile(file, file.getName()));
        return document;
    }
}
