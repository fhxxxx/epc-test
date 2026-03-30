package com.envision.bunny.demo.integration.excel;


import com.alibaba.excel.EasyExcelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * EasyExcel 测试
 *
 * @author jingjing.dong
 * @since 2021/5/6-11:35
 */
@RestController
public class EasyExcelTest {
    @Value("classpath:Account.xlsx")
    Resource excel;

    /**
     * 读取Excel中行数
     *
     * @return 行数
     */
    @GetMapping("/excel/read")
    public int getLineNum() throws IOException {
        List<Account> data = EasyExcelFactory.read(excel.getFile()).head(Account.class).sheet().doReadSync();
        return data.size();
    }
}
