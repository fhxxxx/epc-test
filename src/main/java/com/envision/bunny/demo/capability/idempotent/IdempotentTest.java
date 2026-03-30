package com.envision.bunny.demo.capability.idempotent;

import com.envision.bunny.demo.scenario.service.Person;
import com.envision.bunny.infrastructure.idempotent.Idempotent;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 幂等性
 * @author jingjing.dong
 * @since 2021/3/25-19:40
 */
@RestController
@RequestMapping("/ide")
public class IdempotentTest {
    @Autowired
    HttpSession httpSession;

    /**
     * 从对象中取一个属性
     * @param person 对象
     */
    @PostMapping("/test")
    @Idempotent(prefix = "my-test", spelKey = "你好#{#person.name}", expireSeconds = 40)
    public Person test(@RequestBody Person person) {
        return person;
    }

    /**
     * 幂等性测试2 使用SpEL解析
     *
     * @param name   姓名|陈家豪
     * @param person 对象Person
     * @return com.envision.bunny.demo.Person
     * @author jingjing.dong
     * @since 2021/5/7 18:23
     */
    @PostMapping("/test2")
    @Idempotent(prefix = "my-test", spelKey = "test#{#name + #person.name + @eventTest.test(#name)}", expireSeconds = 40)
    public Person test2(@RequestParam String name, @RequestBody Person person) {
        System.out.println(person.getName());
        return new Person(name);
    }

    /**
     * 幂等测试3 SpEL表达式
     *
     * @param name 姓名|董博之
     * @param age  年龄|18
     * @return com.envision.bunny.demo.Person
     * @author jingjing.dong
     * @since 2021/5/7 18:24
     */
    @GetMapping("/test3")
    @Idempotent(prefix = "my-test", spelKey = "#name + #age", expireSeconds = 40)
    public Person test3(@RequestParam String name, @RequestParam String age) {
        return new Person(name);
    }
}
