package com.envision.epc.mokito;

import com.envision.epc.idempotent.TestService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

/**
 * @author jingjing.dong
 * @since 2021/4/17-16:37
 */
@SpringBootTest
public class MokitoTest {
    @SpyBean
    TestService service;
    @Test
    public void test(){
        Mockito.when(service.test3(Mockito.anyString(),Mockito.anyString()))
                .thenReturn("return1").thenReturn("return2");
        System.out.println(service.test2());
        System.out.println(service.test3("123234","bb"));
    }
}
