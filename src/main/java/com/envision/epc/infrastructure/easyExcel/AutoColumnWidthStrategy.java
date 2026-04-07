package com.envision.epc.infrastructure.easyExcel;

import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.AbstractColumnWidthStyleStrategy;
import org.apache.poi.ss.usermodel.Cell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * easyExcel 生成表格自适应列宽
 * @author gangxiang.guan
 * @date 2025/9/24 11:57
 */
public class AutoColumnWidthStrategy extends AbstractColumnWidthStyleStrategy {
    private Map<Integer, Map<Integer, Integer>> cache = new HashMap<>(); // 用于缓存每列的最大宽度

    @Override
    protected void setColumnWidth(WriteSheetHolder writeSheetHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        int sheetNo = writeSheetHolder.getSheetNo();
        int columnIndex = cell.getColumnIndex();
        Map<Integer, Integer> maxWidthMap = cache.computeIfAbsent(sheetNo, k -> new HashMap<>());

        // 计算当前单元格内容的宽度（可根据内容类型细化）
        int length = cell.getStringCellValue().getBytes().length + 3; // 增加一些缓冲
        // 确保列宽不超过Excel限制（255个字符）
        length = Math.min(length, 120);

        // 如果当前宽度大于该列已缓存的最大宽度，则更新列宽
        if (length > maxWidthMap.getOrDefault(columnIndex, 0)) {
            maxWidthMap.put(columnIndex, length);
            writeSheetHolder.getSheet().setColumnWidth(columnIndex, length * 256);
        }
    }
}