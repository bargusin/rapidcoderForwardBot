package ru.rapidcoder.forward.bot.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;

public class PermissionUser {

    private Long userId;
    private String userName;
    private UserStatus status;
    private UserRole role;

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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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

    public enum UserStatus {
        ACTIVE, BLOCKED
    }

    public enum UserRole {
        ADMIN, MEMBER
    }
}
