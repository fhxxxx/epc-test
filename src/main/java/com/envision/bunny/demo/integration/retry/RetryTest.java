package com.envision.bunny.demo.integration.retry;

import com.envision.bunny.demo.scenario.service.ServiceDemo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * rest 请求
 *
 * @author jingjing.dong
 * @since 2021/4/8-18:30
 */
@RestController
@RequestMapping("/retry")
public class RetryTest {
    @Autowired
    ServiceDemo serviceDemo;



    /**
     * say 你好
     * @author jingjing.dong
     * @since 2021/5/7 18:29
     * @param name 姓名
     * @return java.lang.String
     */
    @GetMapping("/hello")
    public String hello(String name) {
        return serviceDemo.sayHi(name);
    }

}
