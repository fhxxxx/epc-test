package com.envision.bunny.demo.integration.sensitive;

import com.github.houbb.sensitive.annotation.Sensitive;
import com.github.houbb.sensitive.annotation.SensitiveEntry;
import com.github.houbb.sensitive.annotation.strategy.*;
import com.github.houbb.sensitive.core.api.strategory.StrategyChineseName;
import com.github.houbb.sensitive.core.api.strategory.StrategyPassword;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class SensitiveTestEntity {

    @SensitiveStrategyChineseName
    private String username;

    @SensitiveStrategyPassword
    @Sensitive(strategy = StrategyPassword.class)
    private String password;

    @SensitiveStrategyPassport
    private String passport;

    @SensitiveStrategyIdNo
    private String idNo;

    @SensitiveStrategyCardId
    private String bandCardId;

    @SensitiveStrategyPhone
    private String phone;

    @SensitiveStrategyEmail
    private String email;

    @SensitiveStrategyAddress
    private String address;

    @SensitiveStrategyBirthday
    private String birthday;

    @SensitiveStrategyGps
    private String gps;

    @SensitiveStrategyIp
    private String ip;

    @SensitiveStrategyMaskAll
    private String maskAll;

    @SensitiveStrategyMaskHalf
    private String maskHalf;

    @SensitiveStrategyMaskRange
    private String maskRange;

    @SensitiveEntry
    @Sensitive(strategy = StrategyChineseName.class)
    private List<String> list;

    @SensitiveEntry
    private SensitiveTest userTest;

    //Getter & Setter
    //toString()
}
