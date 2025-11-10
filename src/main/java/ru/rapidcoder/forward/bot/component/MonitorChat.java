package ru.rapidcoder.forward.bot.component;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;

public class MonitorChat {

    private Long chatId;
    private Long userId;
    private String userName;
    private String chatTitle;
    private String chatType;
    private String botNewStatus;
    private String botOldStatus;
    private LocalDateTime addedDate;
    private LocalDateTime updatedDate;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getChatTitle() {
        return chatTitle;
    }

    public void setChatTitle(String chatTitle) {
        this.chatTitle = chatTitle;
    }

    public String getChatType() {
        return chatType;
    }

    public void setChatType(String chatType) {
        this.chatType = chatType;
    }

    public String getBotNewStatus() {
        return botNewStatus;
    }

    public void setBotNewStatus(String botNewStatus) {
        this.botNewStatus = botNewStatus;
    }

    public String getBotOldStatus() {
        return botOldStatus;
    }

    public void setBotOldStatus(String botOldStatus) {
        this.botOldStatus = botOldStatus;
    }

    public LocalDateTime getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
