package com.envision.bunny.demo.integration.log;

import com.envision.bunny.demo.scenario.service.Person;
import com.yomahub.tlog.core.annotation.TLogAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chaoyue.zhao1
 * @since 2025/12/26-11:19
 */
@Slf4j
@RestController
@RequestMapping("/tlog")
public class TlogTest {


    @GetMapping("/test")
    @TLogAspect(value = {"id","name"},pattern = "<-{}->",joint = "_")
    public String test(String  id,String name){
        log.info("这是一个测试日志");
        return id+name;
    }

    @PostMapping("/test1")
    @TLogAspect(value = {"person.name","person.age"},pattern = "<-{}->",joint = "_")
    public String test1(@RequestBody Person person){
        log.info("这是一个测试日志");
        return person.getName();
    }
}
