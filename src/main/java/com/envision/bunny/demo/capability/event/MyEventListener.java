package com.envision.bunny.demo.capability.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author jingjing.dong
 * @since 2021/4/23-16:59
 */
@Component
@Slf4j
public class MyEventListener {
    @org.springframework.context.event.EventListener
    public void handle(String name) throws InterruptedException {
        log.info("first begin say hi");
        TimeUnit.SECONDS.sleep(5);
        log.info("first say hello to " + name);
    }
    @org.springframework.context.event.EventListener
    public void handle(EventDemo eventDemo) throws InterruptedException {
        log.info("first begin say hi");
        TimeUnit.SECONDS.sleep(5);
        log.info("first say hi to " + eventDemo.getName());
    }
}
