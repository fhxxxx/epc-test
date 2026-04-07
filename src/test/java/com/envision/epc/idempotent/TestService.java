package com.envision.epc.idempotent;

import com.envision.epc.infrastructure.idempotent.Idempotent;
import org.springframework.stereotype.Service;

/**
 * @author jingjing.dong
 * @since 2021/4/17-14:59
 */
@Service
public class TestService {
    @Idempotent(prefix = "test",spelKey = "#p0",expireSeconds = 40)
    public String test(String arg1,String arg2){
        return "test";
    }

    public String test2(){
        return "test2";
    }

    public String test3(String arg1,String arg2){
        return "test";
    }
}
