package com.envision.bunny.demo.capability.response;

import com.envision.bunny.demo.scenario.service.Person;
import com.envision.bunny.demo.scenario.service.ServiceDemo;
import com.envision.bunny.infrastructure.response.Echo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统一返回体
 * @author jingjing.dong
 * @since 2021/3/24-20:51
 */
@RestController
@RequestMapping("/response")
public class OneResponseTest {
    @Autowired
    ServiceDemo serviceDemo;

    /**
     * 返回异常
     *
     * @param name 入参|郭问之
     */
    @GetMapping("/fail")
    public String test(@RequestParam String name) {
        serviceDemo.exceptionTest(name);
        return "Success do a fail request";
    }

    /**
     * 正常返回-Echo
     * @param name 入参|董卓
     */
    @GetMapping("/success")
    public Echo test2(@RequestParam String name) {
        return Echo.success(name);
    }

    /**
     * 正常返回String
     *
     * @param name 姓名|邵宗
     */
    @GetMapping("/success2")
    public String test3(@RequestParam String name) {
        return name;
    }

    /**
     * 正常返回-Obj
     * @param name 姓名|倪妮
     */
    @GetMapping("/success3")
    public Person test4(@RequestParam String name) {
        return new Person(name);
    }
}
