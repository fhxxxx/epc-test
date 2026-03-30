package com.envision.bunny.demo.capability.cache;

import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * spring cache 缓存
 *
 * @author jingjing.dong
 * @since 2022/2/11-16:38
 */
@RestController
@RequestMapping("/cache")
public class CacheTest {

    @GetMapping("/sync")
    @Cacheable(cacheNames = "user",key = "#p0",sync = true)
    public String syncTest(String name,int age) {
        System.out.println("update load a file and renamed :[{" + name + "}]");
        return name + 1;
    }

    @GetMapping("/transaction")
    @Transactional
    public String transactionTest(String name,int age) {
        System.out.println("main method");
        final CacheTest cacheTest = (CacheTest)ApplicationContextUtils.getBean("cacheTest");
        final String newName = cacheTest.cacheTest(name, age);
        happen();
        return newName;
    }

    @CachePut(cacheNames = "user",key = "#p0")
    public String cacheTest(String name,int age) {
        System.out.println("update load a file and renamed :[{" + name + "}]");
        return name + age;
    }

    private void happen(){
        throw new RuntimeException("sometimes things happen");
    }
}
