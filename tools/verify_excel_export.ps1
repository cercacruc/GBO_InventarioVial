param([string]$InputWorkbook = 'tmp/engineering/export-validation.xlsx')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$target = [IO.Path]::GetFullPath((Join-Path $root $InputWorkbook))
if (-not $target.StartsWith($root + [IO.Path]::DirectorySeparatorChar) -or -not (Test-Path -LiteralPath $target)) { throw 'Invalid validation target' }
$excel = $null
$book = $null
try {
    $excel = New-Object -ComObject Excel.Application
    if ($excel.Workbooks.Count -ne 0) { throw 'Validation requires an isolated Excel instance with no open workbooks' }
    $excel.Visible = $false
    $excel.DisplayAlerts = $false
    $excel.AskToUpdateLinks = $false
    $excel.AutomationSecurity = 3
    # Read-only, without external-link updates. Never save/reserialize the template or export.
    $book = $excel.Workbooks.Open($target, 0, $true)
    $excel.CalculateFullRebuild()
    $sheet = $book.Worksheets.Item('G.- CONDICION ESTADISTICA')
    $result = [ordered]@{
        file = $InputWorkbook
        engine = 'Microsoft Excel'
        version = $excel.Version
        sheets = $book.Worksheets.Count
        elementCount = $sheet.Range('E186').Value2
        maximumContribution = $sheet.Range('E187').Value2
        globalCondition = $sheet.Range('E191').Value2
        globalConditionText = $sheet.Range('E191').Text
        classification = $sheet.Range('M12').Text
        name = $book.Worksheets.Item(1).Range('J9').Text
        recalculatedReadOnly = $true
    }
    $result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'tmp/engineering/excel-verification.json') -Encoding utf8
    $result | ConvertTo-Json
} finally {
    if ($null -ne $book) { $book.Close($false) }
    if ($null -ne $excel) { $excel.Quit(); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel) }
}
