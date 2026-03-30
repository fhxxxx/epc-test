package com.envision.bunny.demo.capability.locale;

import com.envision.bunny.infrastructure.util.MsgUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 更改Locale
 * @author jingjing.dong
 * @since 2021/3/22-20:24
 */
@RestController
@RequestMapping("/locale")
public class LocaleTest {
    /**
     * 改变Locale
     *
     * @param lang 语言设置|en_US
     * @return java.lang.String
     * @author jingjing.dong
     */
    @GetMapping("/change")
    public String test(@RequestParam String lang) {
        return MsgUtils.getMessage("INTERNAL_SERVER_ERROR", new Object[]{lang});
    }
}
