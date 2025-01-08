package com.stockexchange.messages;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public enum EventTopic {
    PSA("psasystem"),
    ENERGY("energy"),
    ORDERSTATUS("orderstatus"),
    FILLS("fills");

    private static final Map<String, EventTopic> mValueMap;
    private final String topic;

    EventTopic(String s) {
        this.topic = s;
    }


    public String getValue() {
        return topic;
    }


    static EventTopic getInstanceForValue(String inValue) {
        return mValueMap.get(inValue);
    }


    static {
        Map<String, EventTopic> table = new HashMap<>();
        for (EventTopic ot : values()) {
            table.put(ot.getValue(), ot);
        }
        mValueMap = Collections.unmodifiableMap(table);
    }

}
