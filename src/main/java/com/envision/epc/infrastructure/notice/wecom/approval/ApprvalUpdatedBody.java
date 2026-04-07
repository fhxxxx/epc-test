package com.envision.epc.infrastructure.notice.wecom.approval;

import lombok.*;

/**
 * 企业微信审批请求同步的请求体
 * @author jingjing.dong
 * @since 2021/4/27-19:36
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprvalUpdatedBody {
    private String approvalId;
    private String approverUserId;
    private String comment;
    private String status;
}

