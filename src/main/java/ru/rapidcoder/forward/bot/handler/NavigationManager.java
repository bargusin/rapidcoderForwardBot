package ru.rapidcoder.forward.bot.handler;

import ru.rapidcoder.forward.bot.component.ChatState;

import java.util.Optional;

public class NavigationManager {

    private final NavigationStorage storage;

    public NavigationManager(String storageFile) {
        this.storage = NavigationStorage.getInstance(storageFile);
    }

    /**
     * Сохранить состояния меню бота
     *
     * @param chatId идентификатор чата
     * @param state состояние
     */
    public void setState(Long chatId, String state) {
        ChatState chatState = new ChatState(chatId, state, null);
        storage.saveNavigationState(chatState);
    }

    /**
     * Сохранить состояния меню бота
     *
     * @param chatId идентификатор чата
     * @param state состояние
     * @param context контекст
     */
    public void setState(Long chatId, String state, String context) {
        ChatState chatState = new ChatState(chatId, state, context);
        storage.saveNavigationState(chatState);
    }

    /**
     * Получить состояние меню бота
     *
     * @param chatId идентификатор чата
     * @return состояние
     */
    public Optional<String> getState(Long chatId) {
        return storage.getNavigationState(chatId)
                .map(ChatState::getState);
    }

    /**
     * Получить контекст меню бота
     *
     * @param chatId идентификатор чата
     * @return контекст
     */
    public Optional<String> getContext(Long chatId) {
        return storage.getNavigationState(chatId)
                .map(ChatState::getContext);
    }

    /**
     * Сбросить состояние меню бота
     *
     * @param chatId идентификатор бота
     */
    public void clearState(Long chatId) {
        storage.clearNavigationState(chatId);
    }

    /**
     * Проверить, есть ли сохраненное состояние меню бота
     *
     * @param chatId идентификатор чата
     * @return признак наличия сохраненного состояния
     */
    public boolean hasState(Long chatId) {
        return storage.getNavigationState(chatId)
                .isPresent();
    }
}
