Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Escape-Xml([string]$s) {
    if ($null -eq $s) { return '' }
    return [System.Security.SecurityElement]::Escape($s)
}

function Cell-InlineStr([string]$ref, [string]$text, [int]$style = -1) {
    $sAttr = if ($style -ge 0) { " s=""$style""" } else { '' }
    $t = Escape-Xml $text
    return "<c r=""$ref""$sAttr t=""inlineStr""><is><t>$t</t></is></c>"
}

function Cell-Number([string]$ref, [double]$num, [int]$style = -1) {
    $sAttr = if ($style -ge 0) { " s=""$style""" } else { '' }
    $v = $num.ToString([System.Globalization.CultureInfo]::InvariantCulture)
    return "<c r=""$ref""$sAttr><v>$v</v></c>"
}

function Cell-Formula([string]$ref, [string]$formula, [double]$cached = 0, [int]$style = -1) {
    $sAttr = if ($style -ge 0) { " s=""$style""" } else { '' }
    $f = Escape-Xml $formula
    $v = $cached.ToString([System.Globalization.CultureInfo]::InvariantCulture)
    return "<c r=""$ref""$sAttr><f>$f</f><v>$v</v></c>"
}

function Build-SheetXml([array]$rows, [array]$merges = @()) {
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.Append('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
    [void]$sb.Append('<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">')
    [void]$sb.Append('<sheetData>')
    for ($i = 0; $i -lt $rows.Count; $i++) {
        $rIdx = $i + 1
        [void]$sb.Append("<row r=""$rIdx"">")
        foreach ($cell in $rows[$i]) { [void]$sb.Append($cell) }
        [void]$sb.Append('</row>')
    }
    [void]$sb.Append('</sheetData>')
    if ($merges.Count -gt 0) {
        [void]$sb.Append("<mergeCells count=""$($merges.Count)"">")
        foreach ($m in $merges) { [void]$sb.Append("<mergeCell ref=""$m""/>") }
        [void]$sb.Append('</mergeCells>')
    }
    [void]$sb.Append('</worksheet>')
    return $sb.ToString()
}

function Get-StylesXml() {
@"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="2">
    <numFmt numFmtId="164" formatCode="#,##0.00"/>
    <numFmt numFmtId="165" formatCode="0.00%"/>
  </numFmts>
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF2CC"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="4">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
    <xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
  </cellXfs>
  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>
"@
}

function Write-Entry($zip, [string]$path, [string]$content) {
    $entry = $zip.CreateEntry($path)
    $sw = New-Object IO.StreamWriter($entry.Open(), [Text.Encoding]::UTF8)
    $sw.Write($content)
    $sw.Dispose()
}

function Create-Xlsx([string]$filePath, [array]$sheetDefs) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path $filePath) { Remove-Item $filePath -Force }
    $zip = [IO.Compression.ZipFile]::Open($filePath, [System.IO.Compression.ZipArchiveMode]::Create)

    $ctOverride = New-Object System.Text.StringBuilder
    for ($i = 0; $i -lt $sheetDefs.Count; $i++) {
        $idx = $i + 1
        [void]$ctOverride.Append("<Override PartName=""/xl/worksheets/sheet$idx.xml"" ContentType=""application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml""/>")
    }

    $contentTypes = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
        '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
        '<Default Extension="xml" ContentType="application/xml"/>' +
        '<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>' +
        '<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>' +
        $ctOverride.ToString() + '</Types>'

    $relsRoot = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
"@

    $wbSheets = New-Object System.Text.StringBuilder
    $wbRels = New-Object System.Text.StringBuilder
    for ($i = 0; $i -lt $sheetDefs.Count; $i++) {
        $idx = $i + 1
        $nameEsc = Escape-Xml $sheetDefs[$i].Name
        [void]$wbSheets.Append("<sheet name=""$nameEsc"" sheetId=""$idx"" r:id=""rId$idx""/>")
        [void]$wbRels.Append("<Relationship Id=""rId$idx"" Type=""http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"" Target=""worksheets/sheet$idx.xml""/>")
    }

    $workbook = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">' +
        "<sheets>$($wbSheets.ToString())</sheets></workbook>"

    $workbookRels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
        $wbRels.ToString() +
        '<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>' +
        '</Relationships>'

    Write-Entry $zip '[Content_Types].xml' $contentTypes
    Write-Entry $zip '_rels/.rels' $relsRoot
    Write-Entry $zip 'xl/workbook.xml' $workbook
    Write-Entry $zip 'xl/_rels/workbook.xml.rels' $workbookRels
    Write-Entry $zip 'xl/styles.xml' (Get-StylesXml)

    for ($i = 0; $i -lt $sheetDefs.Count; $i++) {
        $idx = $i + 1
        Write-Entry $zip "xl/worksheets/sheet$idx.xml" $sheetDefs[$i].Xml
    }

    $zip.Dispose()
}

function Build-Source-PL() {
    $rows = @()
    $rows += ,@(
        (Cell-InlineStr 'A1' '项目编码' 1),
        (Cell-InlineStr 'B1' '项目名称' 1),
        (Cell-InlineStr 'C1' 'PL金额' 1)
    )
    $rows += ,@((Cell-InlineStr 'A2' 'PJT-001'), (Cell-InlineStr 'B2' '项目A'), (Cell-Number 'C2' 120000 2))
    $rows += ,@((Cell-InlineStr 'A3' 'PJT-002'), (Cell-InlineStr 'B3' '项目B'), (Cell-Number 'C3' 80000 2))
    $rows += ,@((Cell-InlineStr 'A4' 'PJT-003'), (Cell-InlineStr 'B4' '项目C'), (Cell-Number 'C4' 50000 2))
    $rows += ,@((Cell-InlineStr 'A5' '合计' 1), (Cell-InlineStr 'B5' ''), (Cell-Formula 'C5' 'SUM(C2:C4)' 250000 2))
    return Build-SheetXml $rows
}

function Build-Source-OutputVAT() {
    $rows = @()
    $rows += ,@((Cell-InlineStr 'A1' '项目编码' 1), (Cell-InlineStr 'B1' '销项税额' 1))
    $rows += ,@((Cell-InlineStr 'A2' 'PJT-001'), (Cell-Number 'B2' 10800 2))
    $rows += ,@((Cell-InlineStr 'A3' 'PJT-002'), (Cell-Number 'B3' 7200 2))
    $rows += ,@((Cell-InlineStr 'A4' 'PJT-003'), (Cell-Number 'B4' 4500 2))
    $rows += ,@((Cell-InlineStr 'A5' '合计' 1), (Cell-Formula 'B5' 'SUM(B2:B4)' 22500 2))
    return Build-SheetXml $rows
}

function Build-Source-InputVAT() {
    $rows = @()
    $rows += ,@((Cell-InlineStr 'A1' '项目编码' 1), (Cell-InlineStr 'B1' '进项税额' 1))
    $rows += ,@((Cell-InlineStr 'A2' 'PJT-001'), (Cell-Number 'B2' 6200 2))
    $rows += ,@((Cell-InlineStr 'A3' 'PJT-002'), (Cell-Number 'B3' 2800 2))
    $rows += ,@((Cell-InlineStr 'A4' 'PJT-003'), (Cell-Number 'B4' 1800 2))
    $rows += ,@((Cell-InlineStr 'A5' '合计' 1), (Cell-Formula 'B5' 'SUM(B2:B4)' 10800 2))
    return Build-SheetXml $rows
}

function Build-Source-Config() {
    $rows = @()
    $rows += ,@((Cell-InlineStr 'A1' '项目编码' 1), (Cell-InlineStr 'B1' '默认税率' 1), (Cell-InlineStr 'C1' '公司代码' 1))
    $rows += ,@((Cell-InlineStr 'A2' 'PJT-001'), (Cell-Number 'B2' 0.09 3), (Cell-InlineStr 'C2' '2320'))
    $rows += ,@((Cell-InlineStr 'A3' 'PJT-002'), (Cell-Number 'B3' 0.09 3), (Cell-InlineStr 'C3' '2320'))
    $rows += ,@((Cell-InlineStr 'A4' 'PJT-003'), (Cell-Number 'B4' 0.09 3), (Cell-InlineStr 'C4' '2320'))
    return Build-SheetXml $rows
}

function Build-Template-Main() {
    $rows = @()
    $rows += ,@(
        (Cell-InlineStr 'A1' 'Demo台账输出（由数据源映射生成）' 1),
        (Cell-InlineStr 'B1' ''), (Cell-InlineStr 'C1' ''), (Cell-InlineStr 'D1' ''), (Cell-InlineStr 'E1' ''), (Cell-InlineStr 'F1' ''), (Cell-InlineStr 'G1' '')
    )
    $rows += ,@(
        (Cell-InlineStr 'A2' '项目编码' 1), (Cell-InlineStr 'B2' '项目名称' 1), (Cell-InlineStr 'C2' 'PL金额' 1),
        (Cell-InlineStr 'D2' '销项税额' 1), (Cell-InlineStr 'E2' '进项税额' 1), (Cell-InlineStr 'F2' '应纳税额(同表公式)' 1), (Cell-InlineStr 'G2' '交叉校验(跨Sheet公式)' 1)
    )

    $projectCodes = @('PJT-001', 'PJT-002', 'PJT-003')
    $names = @('项目A', '项目B', '项目C')
    $pl = @(120000, 80000, 50000)
    $outv = @(10800, 7200, 4500)
    $inv = @(6200, 2800, 1800)

    for ($i = 0; $i -lt 3; $i++) {
        $r = $i + 3
        $rows += ,@(
            (Cell-InlineStr "A$r" $projectCodes[$i]),
            (Cell-InlineStr "B$r" $names[$i]),
            (Cell-Number "C$r" $pl[$i] 2),
            (Cell-Number "D$r" $outv[$i] 2),
            (Cell-Number "E$r" $inv[$i] 2),
            (Cell-Formula "F$r" "D$r-E$r" ($outv[$i] - $inv[$i]) 2),
            (Cell-Formula "G$r" "F$r-'参数区'!`$B`$2" (($outv[$i] - $inv[$i]) - 2000) 2)
        )
    }

    $rows += ,@(
        (Cell-InlineStr 'A6' '合计' 1),
        (Cell-InlineStr 'B6' ''),
        (Cell-Formula 'C6' 'SUM(C3:C5)' 250000 2),
        (Cell-Formula 'D6' 'SUM(D3:D5)' 22500 2),
        (Cell-Formula 'E6' 'SUM(E3:E5)' 10800 2),
        (Cell-Formula 'F6' 'SUM(F3:F5)' 11700 2),
        (Cell-Formula 'G6' 'SUM(G3:G5)' 5700 2)
    )

    return Build-SheetXml $rows @('A1:G1')
}

function Build-Template-Param() {
    $rows = @()
    $rows += ,@((Cell-InlineStr 'A1' '参数项' 1), (Cell-InlineStr 'B1' '参数值' 1))
    $rows += ,@((Cell-InlineStr 'A2' '风险阈值(税额)'), (Cell-Number 'B2' 2000 2))
    $rows += ,@((Cell-InlineStr 'A3' '备注'), (Cell-InlineStr 'B3' '用于跨Sheet公式校验'))
    return Build-SheetXml $rows
}

function Build-Template-Readme() {
    $rows = @()
    $rows += ,@((Cell-InlineStr 'A1' '说明' 1), (Cell-InlineStr 'B1' '内容' 1))
    $rows += ,@((Cell-InlineStr 'A2' '用途'), (Cell-InlineStr 'B2' '用于验证多源取数、公式、样式、跨sheet计算'))
    $rows += ,@((Cell-InlineStr 'A3' '对应数据源'), (Cell-InlineStr 'B3' 'demo_source_pl/demo_source_vat_output/demo_source_vat_input/demo_source_config'))
    return Build-SheetXml $rows
}

$base = 'C:\projects\epc-test\demo-data'

Create-Xlsx -filePath (Join-Path $base 'demo_source_pl.xlsx') -sheetDefs @(
    @{ Name = 'PL附表-2320、2355'; Xml = (Build-Source-PL) }
)

Create-Xlsx -filePath (Join-Path $base 'demo_source_vat_output.xlsx') -sheetDefs @(
    @{ Name = '增值税销项 '; Xml = (Build-Source-OutputVAT) }
)

Create-Xlsx -filePath (Join-Path $base 'demo_source_vat_input.xlsx') -sheetDefs @(
    @{ Name = '增值税进项认证清单'; Xml = (Build-Source-InputVAT) }
)

Create-Xlsx -filePath (Join-Path $base 'demo_source_config.xlsx') -sheetDefs @(
    @{ Name = '公司代码配置表'; Xml = (Build-Source-Config) }
)

Create-Xlsx -filePath (Join-Path $base 'demo_ledger_template.xlsx') -sheetDefs @(
    @{ Name = '模板说明'; Xml = (Build-Template-Readme) },
    @{ Name = '参数区'; Xml = (Build-Template-Param) },
    @{ Name = 'Demo_台账输出'; Xml = (Build-Template-Main) }
)

Write-Output 'DONE'



