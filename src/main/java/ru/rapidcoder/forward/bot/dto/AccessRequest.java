package ru.rapidcoder.forward.bot.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;

public class AccessRequest {

    private Long userId;
    private String userName;
    private RequestStatus status;
    private LocalDateTime addedDate;

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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(LocalDateTime addedDate) {
        this.addedDate = addedDate;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public enum RequestStatus {
        PENDING, APPROVED, REJECT
    }
}
