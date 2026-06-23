package com.aihealthcoach.chat.service;

interface ChatStreamEventSink {

    void send(String eventName, Object data);

    void complete();

    void completeWithError(Throwable error);
}
