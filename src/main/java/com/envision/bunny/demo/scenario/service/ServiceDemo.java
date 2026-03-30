package com.envision.bunny.demo.scenario.service;

import com.envision.bunny.demo.capability.event.EventDemo;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;


/**
 * @author jingjing.dong
 * @since 2021/4/21-18:18
 */
@Component(value="serviceDemo")
@Slf4j
public class ServiceDemo {
    @Autowired
    ApplicationEventPublisher publisher;
    public void cronTest(String param, String param2){
        System.out.println("hello " + param);
    }
    public void exceptionTest(String param){
        log.error("you get a error");
        throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, param);
    }
    public void publishObjEvent(String param){
        log.info("publish begin");
        publisher.publishEvent(new EventDemo(this,param));
        log.info("publish done");
    }
    /**
     *
     * @author jingjing.dong
     * @since 2021/5/7 18:00
     * @param param 入参|test-param
     */
    public void publishBasicEvent(String param){
        log.info("publish begin");
        publisher.publishEvent(param);
        log.info("publish done");
    }

    /**
     * 注意：注解中的recover属性的值默认会使用同一个类中添加了@Recover的方法
     * 若类中存在多个@Recover注解方法，则需要指定recover属性的值为对应的方法名称
     *
     * @param name 参数
     * @return 结果
     */
    @Retryable(retryFor = BizException.class, backoff = @Backoff(delay = 500L, multiplier = 1.5), recover = "recover")
    public String sayHi(String name){
        log.info("your name {}",name);
        if (name == "abc") {
            return "hi " + name;
        } else {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 注意：自定义保底方法时，该方法中的第一个参数必须为异常类型，且该异常类型需要主方法抛出的异常类型或其父类
     * 其他参数需要与主方法的参数类型与名称一致，否则，该方法将不会生效
     * 且该方法的返回值类型需要与主方法相同，否则，该方法将不会生效
     *
     * @param e 异常类型
     * @param name 参数
     * @return
     */
    @Recover
    private String recover(BizException e, String name){
        log.info("recover");
        return "abc";
    }
}
