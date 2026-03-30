package com.envision.bunny.mvc;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * @author jingjing.dong
 * @since 2021/4/17-15:21
 */
@SpringBootTest
@Slf4j
@AutoConfigureMockMvc
public class MvcTest {
    @Autowired
    MockMvc mockMvc;
    @Test
    @Rollback
    public void test() throws Exception {
        mockMvc.perform(get("/mail/test")
                .contentType(MediaType.APPLICATION_JSON)
                //.param("arg","value")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                //.andExpect(MockMvcResultMatchers.content().string("Su"))
                .andExpect(MockMvcResultMatchers.jsonPath("data").value("Su"))
                .andDo(result -> {
                            String json = result.getResponse().getContentAsString();
                            log.info("获取响应信息为：\n" + json);
                        });
    }
}
