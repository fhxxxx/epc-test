package com.envision.epc.infrastructure.notice.wecom.approval;

import lombok.Getter;
import lombok.Setter;

/**
 * 创建企业微信审批的请求体
 * @author jingjing.dong
 * @since 2021/4/27-19:29
 */
@Getter
@Setter
public class ApprovalCreatedBody {
    private String approvalId;
    private String type;
    private Long createTime;
    private String approverUserId;
    private String applicantUserId;
    private String applicantName;
    private String applicantDepartment;
    private String status;
    private String summary;
    private Content content;
    private String approvalSubmitUrl;

    /**
     * 传入一个Position对象，然后构造一个请求体；可以从SecurityContext中获取到登陆人的各种信息
     * 然后将Position中的细节信息构造detail即可
     */
    public static ApprovalCreatedBody buildWithPosition(){
        return new ApprovalCreatedBody();
    }
    /**
     * 传入一个Offer对象，然后构造一个请求体；可以从SecurityContext中获取到登陆人的各种信息
     * 然后将Offer中的细节信息构造detail即可
     */
    public static ApprovalCreatedBody buildWithOffer(){
        return new ApprovalCreatedBody();
    }
}
