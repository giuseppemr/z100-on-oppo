package com.vuzix.ultralite.sample;

import java.util.Objects;

public class Message {
    private String text;
    private String timestamp;
    private String photo;
    private MessageType type;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (!Objects.equals(text, message.text)) return false;
        if (!Objects.equals(timestamp, message.timestamp))
            return false;
        if (!Objects.equals(photo, message.photo)) return false;
        return type == message.type;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (photo != null ? photo.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
