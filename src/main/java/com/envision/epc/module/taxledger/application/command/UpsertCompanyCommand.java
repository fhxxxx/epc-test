package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

@Data
public class UpsertCompanyCommand {
    private Long id;
    private String companyCode;
    private String companyName;
    private String financeBpAd;
    private String financeBpName;
    private String financeBpEmail;
    private Integer status;
}
