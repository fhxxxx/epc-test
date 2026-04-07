package com.envision.epc.module.extract.application.query;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * @author gangxiang.guan
 * @date 2025/9/26 11:16
 */
@Data
public class InvoiceQuery {
    /**
     * 项目ID
     */
    @NotNull
    private Long projectId;
    /**
     * 公司代码
     * "单选
     * 示例：2025"
     */
    private Set<String> companyCodeList;
    /**
     * 会计年度/期间
     * "会计年度单选、会计期间多选，由会计年度&""0""&会计期间=fiscal_year_period
     * 会计期间选择范围：01-16
     * 示例：2025009、2025010"
     */
    private String fiscalYearPeriodStart;
    private String fiscalYearPeriodEnd;
    /**
     * 过账日期
     * "时间段，该时间段需属于所选的会计期间（如果期间选了13-16期视同12期处理）
     * 示例：如期间选择09、10 ，则此字段可选择20250901到20251031中间的阶段"
     */
    private String postingDateInTheDocumentStart;
    private String postingDateInTheDocumentEnd;

    /**
     * 根据period取可选日期范围段
     *
     * @return
     */
    public List<LocalDate> getDateRange() {
        YearMonth start = YearMonth.of(Integer.parseInt(this.fiscalYearPeriodStart.substring(0, 4)),
                Math.min(Integer.parseInt(this.fiscalYearPeriodStart.substring(5)), 12));
        YearMonth end = YearMonth.of(Integer.parseInt(this.fiscalYearPeriodEnd.substring(0, 4)),
                Math.min(Integer.parseInt(this.fiscalYearPeriodEnd.substring(5)), 12));
        return List.of(start.atDay(1), end.atEndOfMonth());
    }

}
