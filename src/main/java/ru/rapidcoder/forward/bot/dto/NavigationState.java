package ru.rapidcoder.forward.bot.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;

public class NavigationState {

    private long chatId;
    private String state;
    private String context;
    private LocalDateTime lastUpdated;

    public NavigationState() {
    }

    public NavigationState(long chatId, String state, String context) {
        this.chatId = chatId;
        this.state = state;
        this.context = context;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
