package com.envision.bunny.idempotent;


import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author jingjing.dong
 * @since 2021/4/17-14:55
 */
@SpringBootTest
@DisplayName("幂等性测试")
@Disabled
@TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
public class IdempotentTest {
    @Autowired
    TestService service;

    @Test
    @Order(2)
    public void test(){
        String result =service.test("test","test2");
        Assertions.assertEquals(result,"test");
    }
    @Test
    @Order(4)
    public void test2(){
        String result =service.test("test2","test3");
        Assertions.assertEquals(result,"test");
    }
    @Test
    @Order(3)
    public void test3(){
        String result =service.test("test3","test4");
        Assertions.assertEquals(result,"test");
    }

}
