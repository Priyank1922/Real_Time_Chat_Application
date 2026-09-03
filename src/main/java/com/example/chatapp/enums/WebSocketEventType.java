package com.example.chatapp.enums;

public enum WebSocketEventType {
    USER_ONLINE,
    USER_OFFLINE,
    USER_TYPING,
    USER_STOPPED_TYPING,
    NEW_MESSAGE,
    MESSAGE_UPDATED,
    MESSAGE_DELETED,
    ROOM_USER_JOINED,
    ROOM_USER_LEFT,
    IMPORTANT_MESSAGE,
    URGENT_MESSAGE
}
