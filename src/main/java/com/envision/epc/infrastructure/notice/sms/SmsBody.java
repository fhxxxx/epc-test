package com.envision.epc.infrastructure.notice.sms;

import com.envision.epc.infrastructure.notice.whitelist.WhitelistChecking;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.hibernate.validator.constraints.Range;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Auto-generated: 2021-04-14 11:29:20
 *
 * @author jingjing.dong
 * @since 2021/4/14-11:28
 */
@Getter
@Setter
@Builder
@ToString
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SmsBody implements WhitelistChecking {
    @Range(min = 1, max = 99, message = "BusinessId 必须是一个两位的数字")
    private String businessId;
    @Singular(value = "addNum")
    @PhoneNum(message = "请至少包含一个格式正确的手机号码")
    private List<String> phoneNums;
    @JsonProperty("message")
    private String content;

    protected void addPrefix() {
        List<String> newList = phoneNums.stream().map(x -> "+86" + x).collect(Collectors.toList());
        this.setPhoneNums(newList);
    }

    @Override
    public List<String> getSendTo() {
        return this.getPhoneNums();
    }

    @Override
    public String getMsgType() {
        return "sms";
    }
}