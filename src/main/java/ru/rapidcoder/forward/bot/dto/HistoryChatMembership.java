package ru.rapidcoder.forward.bot.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class HistoryChatMembership extends BaseChatMembership {

    private boolean deleted;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
