package com.envision.epc;

import com.envision.epc.infrastructure.util.AsposeUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;


import java.time.ZoneId;
import java.util.TimeZone;

/**
 * @author jingjing.dong
 */
@SpringBootApplication
@EnableRetry
@EnableScheduling
public class BunnyApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(BunnyApplication.class, args);
    }

    /**
     * 系统启动后设置邮件参数，当附件过长时不会截断；否则会导致附件乱码
     */
    @Override
    public void run(ApplicationArguments args) {
        AsposeUtils.loadLicense();
        System.setProperty("mail.mime.splitlongparameters", "false");
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")));
    }
}
