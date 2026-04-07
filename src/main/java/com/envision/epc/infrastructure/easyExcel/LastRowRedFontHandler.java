package com.envision.epc.infrastructure.easyExcel;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.*;

/**
 * @author gangxiang.guan
 * @date 2025/10/13 16:52
 */
public class LastRowRedFontHandler implements CellWriteHandler {

    // 记录数据总行数，注意：不包括表头
    private int totalRows;

    public LastRowRedFontHandler(int totalRows) {
        this.totalRows = totalRows;
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        // 判断当前单元格是否为表头，我们只处理数据部分
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }

        // 获取当前行索引（从0开始），表头不算在内
        // 数据行的索引范围是 0 到 (totalRows - 1)
        int currentRowIndex = context.getRowIndex();

        // 检查是否为最后一行数据
        if (currentRowIndex == totalRows - 1) {
            Cell cell = context.getCell();
            Workbook workbook = context.getWriteWorkbookHolder().getWorkbook();

            // 创建新字体并设置为红色
            Font redFont = workbook.createFont();
            redFont.setColor(IndexedColors.RED.getIndex()); // 设置字体颜色为红色:cite[1]:cite[6]

            // 获取或创建单元格样式
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setFont(redFont); // 应用红色字体

            // 将样式应用到单元格
            cell.setCellStyle(cellStyle);

            // 重要：清空WriteCellData中的样式，防止被后续操作覆盖:cite[1]
            if (context.getFirstCellData() != null) {
                context.getFirstCellData().setWriteCellStyle(null);
            }
        }
    }
}
