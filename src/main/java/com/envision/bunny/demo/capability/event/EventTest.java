package com.envision.bunny.demo.capability.event;

import com.envision.bunny.demo.scenario.service.ServiceDemo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring事件
 * @author jingjing.dong
 * @since 2021/4/23-17:05
 */
@RestController
@RequestMapping("/event")
public class EventTest {
    @Autowired
    ServiceDemo serviceDemo;

    /**
     * String类型的事件
     * @param name 姓名|王娟娟
     */
    @GetMapping("/basic")
    public String test(String name) {
        serviceDemo.publishBasicEvent(name);
        return "Success";
    }

    /**
     * 对象类型的事件
     * @param name 入参
     * @author jingjing.dong
     */
    @GetMapping("/obj")
    public String test2(String name) {
        serviceDemo.publishObjEvent(name);
        return "Success";
    }
}
