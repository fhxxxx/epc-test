# Demo实施说明（Aspose版）

## 1. 实现位置
- 代码入口：`src/main/java/com/envision/bunny/demo/ledgerdemo/DemoLedgerRunner.java`
- 数据目录：`demo-data`

## 2. 已实现能力
1. 读取4个数据源Excel + 1个模板Excel。
2. 按 `项目编码` 做多源关联取数。
3. 写入 `Demo_台账输出` sheet。
4. 生成同表公式：`F列 = D-E`。
5. 生成跨sheet公式：`G列 = F-'参数区'!$B$2`。
6. 生成合计行公式（C~G列）。
7. 执行公式重算（`workbook.calculateFormula()`）。
8. 内置校验：
- 合并单元格 `A1:G1` 存在
- F/G列为公式单元格
- 汇总行不出现 `#REF!/#DIV/0!`
- 关键结果断言（`F6=11700`、`G6=5700`）

## 3. 运行方式
默认读取目录：`C:\projects\epc-test\demo-data`

可执行入口（示例，按你本地构建方式调整）：
- main class: `com.envision.epc.demo.ledgerdemo.DemoLedgerRunner`
- 可选参数：`args[0]` 指定数据目录绝对路径

执行成功后输出：
- `demo-data/demo_ledger_generated.xlsx`

## 4. 注意事项
1. 依赖 Aspose Cells，且建议确保 license 正常加载（项目中已调用 `AsposeUtils.loadLicense()`）。
2. `增值税销项 ` sheet 名末尾有空格，需保持精确匹配。
3. 当前环境未提供 Maven 命令（`mvn` 不可用），本次未在终端完成编译执行验证。
