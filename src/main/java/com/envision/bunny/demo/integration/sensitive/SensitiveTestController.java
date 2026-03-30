package com.envision.bunny.demo.integration.sensitive;

import com.github.houbb.sensitive.core.api.SensitiveUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2026/02/27-11:09
 */
@Slf4j
@RestController
@RequestMapping("/sensitive")
public class SensitiveTestController {

    private static final String TEST_LOG = "mobile:13088887777; bankCard:6217004470007335024, email:mahuateng@qq.com, amount:123.00, " +
            "IdNo:340110199801016666, name1:李明, name2:李晓明, name3:李泽明天, name4:山东小栗旬" +
            ", birthday:20220517, GPS:120.882222, IPV4:127.0.0.1, address:中国上海市徐汇区888号, password=123456;";

    @GetMapping("/test1")
    public void test1() {
        log.info(TEST_LOG);
    }

    @GetMapping("/test2")
    public void test2() {
        SensitiveTestEntity bean  = new SensitiveTestEntity();
        bean.setUsername("张三");
        bean.setPassword("zcy010620");
        bean.setPassport("CN1234567");
        bean.setPhone("13066668888");
        bean.setAddress("中国上海市浦东新区外滩18号");
        bean.setEmail("whatanice@code.com");
        bean.setBirthday("20220831");
        bean.setGps("66.888888");
        bean.setIp("127.0.0.1");
        bean.setMaskAll("可恶啊我会被全部掩盖");
        bean.setMaskHalf("还好我只会被掩盖一半");
        bean.setMaskRange("我比较灵活指定掩盖范围");
        bean.setBandCardId("666123456789066");
        bean.setIdNo("360123202306018888");
        bean.setList(List.of("张三","李四","王五"));
        SensitiveTest userTest = new SensitiveTest();
        userTest.setPassword("zcy010620");
        userTest.setName("张三");
        userTest.setPhone("13066668888");
        userTest.setEmail("whatanice@code.com");
        bean.setUserTest(userTest);
        log.info("log:[{}]", SensitiveUtil.desCopy(bean));
        log.info("log:[{}]", SensitiveUtil.desJson(bean));
    }
}
