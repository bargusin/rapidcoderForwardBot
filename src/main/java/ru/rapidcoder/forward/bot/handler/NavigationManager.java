package ru.rapidcoder.forward.bot.handler;

import ru.rapidcoder.forward.bot.dto.NavigationState;

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
        NavigationState navigationState = new NavigationState(chatId, state, null);
        storage.saveNavigationState(navigationState);
    }

    /**
     * Сохранить состояния меню бота
     *
     * @param chatId идентификатор чата
     * @param state состояние
     * @param context контекст
     */
    public void setState(Long chatId, String state, String context) {
        NavigationState navigationState = new NavigationState(chatId, state, context);
        storage.saveNavigationState(navigationState);
    }

    /**
     * Получить состояние меню бота
     *
     * @param chatId идентификатор чата
     * @return состояние
     */
    public Optional<String> getState(Long chatId) {
        return storage.getNavigationState(chatId)
                .map(NavigationState::getState);
    }

    /**
     * Получить контекст меню бота
     *
     * @param chatId идентификатор чата
     * @return контекст
     */
    public Optional<String> getContext(Long chatId) {
        return storage.getNavigationState(chatId)
                .map(NavigationState::getContext);
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
