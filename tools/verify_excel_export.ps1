param(
    [string]$InputWorkbook = 'tmp/engineering/export-validation.xlsx',
    [string]$OutputReport = 'tmp/engineering/excel-verification.json',
    [string]$CellManifest = ''
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$target = [IO.Path]::GetFullPath((Join-Path $root $InputWorkbook))
if (-not $target.StartsWith($root + [IO.Path]::DirectorySeparatorChar) -or -not (Test-Path -LiteralPath $target)) { throw 'Invalid validation target' }
$reportPath = [IO.Path]::GetFullPath((Join-Path $root $OutputReport))
if (-not $reportPath.StartsWith($root + [IO.Path]::DirectorySeparatorChar)) { throw 'Invalid report target' }
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
    if ($CellManifest) {
        $manifest = [IO.Path]::GetFullPath((Join-Path $root $CellManifest))
        if (-not $manifest.StartsWith($root + [IO.Path]::DirectorySeparatorChar)) { throw 'Invalid cell manifest' }
        $checks = @(Get-Content -Raw -LiteralPath $manifest -Encoding utf8 | ConvertFrom-Json)
        $failures = @()
        foreach ($check in $checks) {
            $actual = $book.Worksheets.Item($check.sheet).Range($check.cell).Value2
            $number = 0.0
            if ([double]::TryParse([string]$check.expected, [Globalization.NumberStyles]::Float, [Globalization.CultureInfo]::InvariantCulture, [ref]$number)) {
                $ok = $null -ne $actual -and [Math]::Abs([double]$actual - $number) -lt 0.000001
            } else { $ok = [string]$actual -ceq [string]$check.expected }
            if (-not $ok) { $failures += [ordered]@{sheet=$check.sheet;cell=$check.cell;expected=$check.expected;actual=$actual} }
        }
        $result.cellChecks = $checks.Count
        $result.cellFailures = $failures
        if ($failures.Count) { throw ($failures | ConvertTo-Json -Depth 5) }
    }
    $formulaErrors = @()
    foreach ($worksheet in $book.Worksheets) {
        try { $errorCells = $worksheet.UsedRange.SpecialCells(-4123,16) } catch { $errorCells = $null }
        if ($null -ne $errorCells) {
            foreach ($cell in $errorCells) { $formulaErrors += [ordered]@{sheet=$worksheet.Name;cell=$cell.Address();value=$cell.Text} }
        }
    }
    $result.formulaErrors = $formulaErrors
    $result | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $reportPath -Encoding utf8
    $result | ConvertTo-Json -Depth 5
} finally {
    if ($null -ne $book) { $book.Close($false) }
    if ($null -ne $excel) { $excel.Quit(); [void][Runtime.InteropServices.Marshal]::FinalReleaseComObject($excel) }
}
