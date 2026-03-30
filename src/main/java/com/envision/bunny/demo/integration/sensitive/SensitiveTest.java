package com.envision.bunny.demo.integration.sensitive;

import com.github.houbb.sensitive.annotation.strategy.SensitiveStrategyChineseName;
import com.github.houbb.sensitive.annotation.strategy.SensitiveStrategyEmail;
import com.github.houbb.sensitive.annotation.strategy.SensitiveStrategyPassword;
import com.github.houbb.sensitive.annotation.strategy.SensitiveStrategyPhone;
import lombok.Data;
import lombok.ToString;

/**
 * @author chaoyue.zhao1
 * @since 2026/02/26-15:16
 */
@Data
@ToString
public class SensitiveTest {
    @SensitiveStrategyChineseName
    private String name;
    @SensitiveStrategyPassword
    private String password;
    @SensitiveStrategyPhone
    private String phone;

    @SensitiveStrategyEmail
    private String email;
}
