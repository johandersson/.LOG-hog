Param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$workspace = (Resolve-Path (Join-Path $scriptDir '..\..')) | Select-Object -ExpandProperty Path
Write-Host "Workspace: $workspace"


$pmdRoot = Join-Path $scriptDir 'pmd'
New-Item -ItemType Directory -Force -Path $pmdRoot | Out-Null

# If an extracted PMD distribution already exists under $pmdRoot, prefer it and skip download.
$existing = Get-ChildItem -Path $pmdRoot -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -like 'pmd*' } | Select-Object -First 1
if ($existing) {
	$pmdDir = $existing
	Write-Host "Using existing PMD at: $($pmdDir.FullName)"
} else {
	$versions = @('6.56.0','6.55.0','6.54.0','6.53.0','6.52.0')
	$zip = $null
	foreach ($v in $versions) {
		$candidate = Join-Path $pmdRoot "pmd-bin-$v.zip"
		$url = "https://github.com/pmd/pmd/releases/download/pmd_releases%2F$v/pmd-bin-$v.zip"
		Write-Host "Trying PMD $v..."
		try {
			Invoke-WebRequest -Uri $url -OutFile $candidate -UseBasicParsing -ErrorAction Stop
			$zip = $candidate
			Write-Host "Downloaded PMD $v to $candidate"
			break
		} catch {
			$msg = $_.Exception.Message -replace '\r|\n',' '
			Write-Host ('Failed to download PMD ' + $v + ': ' + $msg)
		}
	}
	if (-not $zip) { Write-Error 'Could not download any PMD release from candidates'; exit 2 }

	Write-Host 'Extracting PMD...'
	Expand-Archive -Path $zip -DestinationPath $pmdRoot -Force

	$pmdDir = Get-ChildItem -Path $pmdRoot -Directory | Where-Object { $_.Name -like 'pmd*' } | Select-Object -First 1
	if (-not $pmdDir) { Write-Error 'PMD extraction failed'; exit 2 }
	Write-Host "PMD installed at: $($pmdDir.FullName)"
}

$srcDir = Join-Path $workspace 'src'
if (-not (Test-Path $srcDir)) { Write-Error "Source dir not found: $srcDir"; exit 3 }

$outDir = Join-Path $workspace 'build\pmd-report'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$reportXml = Join-Path $outDir 'pmd.xml'
$reportHtml = Join-Path $outDir 'pmd.html'

Write-Host 'Running PMD analysis (security + bestpractices + performance)'
$rules = 'category/java/security.xml,category/java/bestpractices.xml,category/java/performance.xml'
$pmdExe = Join-Path $pmdDir.FullName 'bin\pmd.bat'
if (-not (Test-Path $pmdExe)) { Write-Error "pmd executable not found: $pmdExe"; exit 4 }

Write-Host "Generating XML report: $reportXml"
& "$pmdExe" -d "$srcDir" -R "$rules" -f xml -r "$reportXml" -language java
Write-Host "PMD (XML) exit code: $LASTEXITCODE"
if (Test-Path $reportXml) { Write-Host "XML report written: $reportXml" } else { Write-Host 'No PMD XML report produced' }

Write-Host "Generating HTML report: $reportHtml"
& "$pmdExe" -d "$srcDir" -R "$rules" -f html -r "$reportHtml" -language java
Write-Host "PMD (HTML) exit code: $LASTEXITCODE"
if (Test-Path $reportHtml) { Write-Host "HTML report written: $reportHtml" } else { Write-Host 'No PMD HTML report produced' }

Write-Host 'PMD run complete.'
