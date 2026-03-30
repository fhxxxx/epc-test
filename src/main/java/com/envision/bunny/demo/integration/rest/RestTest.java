package com.envision.bunny.demo.integration.rest;

import com.envision.bunny.infrastructure.filter.upload.UploadFileType;
import com.envision.bunny.infrastructure.opslog.OperationLogAnnotation;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * rest 请求
 *
 * @author jingjing.dong
 * @since 2021/4/8-18:30
 */
@RestController
@RequestMapping("/rest")
@Slf4j
public class RestTest {
    @Autowired
    RestTemplate restTemplate;

    /**
     * 日志
     */
    @GetMapping("/log")
    public JsonNode test() {
        return restTemplate.getForObject("http://localhost:8080/bunny/rest/hello?name=robert",
                JsonNode.class);
    }

    /**
     * say 你好
     * @author jingjing.dong
     * @since 2021/5/7 18:29
     * @param name 姓名
     * @return java.lang.String
     */
    @GetMapping("/hello")
    public String hello(String name) {
        return "Hello " + name;
    }


}
