package com.stockexchange.messages;


public class MsgEnvelope implements Message.Command {
    public final String topic;
    public final Object payload;

    public MsgEnvelope(String topic, Object payload) {
        this.topic = topic;
        this.payload = payload;
    }
}