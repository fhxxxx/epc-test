package com.envision.bunny.demo.capability.event;

import org.springframework.context.ApplicationEvent;

/**
 * @author jingjing.dong
 * @since 2021/4/23-16:53
 */
public class EventDemo extends ApplicationEvent {
    String name;
    public EventDemo(Object source) {
        super(source);
    }

    public EventDemo(Object source, String name) {
        super(source);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
