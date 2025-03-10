package com.vuzix.ultralite.sample;

import java.util.Objects;

public class Message {
    private final String text;
    private final String timestamp;
    private final String photo;
    private final MessageType type;

    private String action;

    public Message(String text, String timestamp, String photo, MessageType type) {
        this.text = text;
        this.timestamp = timestamp;
        this.photo = photo;
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPhoto() {
        return photo;
    }

    public MessageType getType() {
        return type;
    }

    public String getAction() {
        return action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(text, message.text) && Objects.equals(timestamp, message.timestamp) && Objects.equals(photo, message.photo) && type == message.type && Objects.equals(action, message.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, timestamp, photo, type, action);
    }
}
