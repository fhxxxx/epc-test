package com.envision.epc.facade.ip;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/12/19-14:10
 */
@Setter
@Getter
@ToString
public class IpResponse {
    private Integer code;
    private List<IpResp> data;

    @Setter
    @Getter
    @ToString
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class IpResp {
        /** 数据湖编号 */
        private String caspianId;
        /** CMDB实例更新时间 */
        private String cmdbLastTime;
        /** CMDB实例创建时间 */
        private String cmdbCreateTime;
        /** 所属区域 */
        private String seczoneArea;
        /** 安全域备注 */
        private String seczoneNote;
        /** CMDB实例ID */
        private String cmdbInstId;
        /** IP地址段 */
        private String seczoneIprange;
        /** 状态 */
        private String seczoneStatus;
        /** 网络安全域VLAN */
        private String seczoneVlan;
        /** CMDB实例名 */
        private String bkInstName;
        /** 网络安全域描述 */
        private String seczoneDescription;
        /** 网络安全域名称 */
        private String seczoneName;
        /** 运维Group */
        private String seczoneOpgroup;
        /** 运维Group负责人 */
        private String seczoneOpgroupOwner;
        /** 运维Group负责人 */
        private String seczoneOpgroupDept;
        /** 运维Group成员 */
        private String seczoneOpgroupMember;
        /** 资产可扫描 */
        private String seczoneScannable;
    }
}
