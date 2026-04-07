package com.envision.epc.infrastructure.easyExcel;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.util.List;
import java.util.Objects;

/**
 * @author gangxiang.guan
 * @date 2025/10/27 15:55
 */
public class CustomHeaderColorHandler implements CellWriteHandler {

    // 目标字段的列索引（从0开始）
    private List<Integer> indexList;

    private Integer headerIndex = 0;

    public CustomHeaderColorHandler(List<Integer> indexList, Integer headerIndex) {
        this.indexList = indexList;
        this.headerIndex = headerIndex;
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        // 判断是否为表头单元格
        if (Objects.equals(context.getRowIndex(), headerIndex) && context.getHead() &&
                CollUtil.isNotEmpty(indexList) && indexList.contains(context.getColumnIndex())) {
            WriteCellData<?> cellData = context.getFirstCellData();
            // 确保单元格数据类型，便于设置样式
            if (cellData.getType() == CellDataTypeEnum.EMPTY) {
                cellData.setType(CellDataTypeEnum.STRING);
            }
            // 获取或创建单元格样式
            WriteCellStyle writeCellStyle = cellData.getOrCreateStyle();
            // 设置黄色背景
            writeCellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex()); // 设置为黄色
            writeCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND); // 设置填充模式
        }

    }
}
