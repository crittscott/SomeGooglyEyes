# Integration tests use isolated checkouts and a fake wrapper; no actual Gradle task is run.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot
. (Join-Path $projectRoot 'tools/validation.ps1')
$fixtureRoot = Join-Path $projectRoot ('tmp/validation-tests/' + [guid]::NewGuid().ToString('N'))
[void](New-Item -ItemType Directory -Path $fixtureRoot -Force)
$script:assertions = 0

function Assert-True {
    param([bool]$Condition, [string]$Message)
    $script:assertions++
    if (!$Condition) { throw "Assertion failed: $Message" }
}

function New-Case {
    param([string]$Name)
    $root = Join-Path $fixtureRoot $Name
    foreach ($directory in @('tools', 'common/src/main/java', '.validation', 'docs/javadoc')) {
        [void](New-Item -ItemType Directory -Path (Join-Path $root $directory) -Force)
    }
    Copy-Item -LiteralPath (Join-Path $projectRoot 'gradlews.ps1') -Destination (Join-Path $root 'gradlews.ps1')
    Copy-Item -LiteralPath (Join-Path $projectRoot 'tools/validation.ps1') -Destination (Join-Path $root 'tools/validation.ps1')
    Set-Content -LiteralPath (Join-Path $root 'gradle.properties') -Value 'enabled_platforms = commonfixture'
    Set-Content -LiteralPath (Join-Path $root 'common/src/main/java/Example.java') -Value 'class Example {}'
    Set-Content -LiteralPath (Join-Path $root 'review-Change.md') -Value "Verdict: Ready`nFixture source review."
    Set-Content -LiteralPath (Join-Path $root 'review-Analysis.md') -Value "Verdict: Accepted`nFixture diagnostic assessment."
    @'
@echo off
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0fake.ps1" %*
exit /b %errorlevel%
'@ | Set-Content -LiteralPath (Join-Path $root 'gradlew.bat') -Encoding Ascii
    @'
$stage = if ($args -contains 'build') { 'compile' } elseif ($args -contains 'analysisReview') { 'analysis' } elseif ($args -contains 'runGameTestServer') { 'gametests' } elseif ($args -contains 'generateDocs') { 'docs' } else { 'jars' }
Add-Content -LiteralPath (Join-Path $PSScriptRoot '.validation/calls.log') -Value ($stage + ' ' + ($args -join ' '))
$planPath = Join-Path $PSScriptRoot '.validation/plan.json'
$plan = if (Test-Path -LiteralPath $planPath) { Get-Content -LiteralPath $planPath -Raw | ConvertFrom-Json } else { $null }
if ($null -ne $plan -and $plan.noise) { 1..500 | ForEach-Object { Write-Output "ordinary output $_" } }
if ($null -ne $plan -and $plan.mutate -eq $stage) { Add-Content -LiteralPath (Join-Path $PSScriptRoot 'common/src/main/java/Example.java') -Value '// changed during execution' }
if ($stage -eq 'docs') { Set-Content -LiteralPath (Join-Path $PSScriptRoot 'docs/javadoc/index.html') -Value '<html>generated</html>' }
if ($null -ne $plan -and $plan.junit -and $stage -eq 'gametests') {
    $directory = Join-Path $PSScriptRoot 'commonfixture/build'
    [void](New-Item -ItemType Directory -Path $directory -Force)
    '<testsuite><testcase classname="Example" name="first"><failure message="state changed"/></testcase><testcase classname="Example" name="second"><error message="fixture missing"/></testcase></testsuite>' | Set-Content -LiteralPath (Join-Path $directory 'gametest-report.xml')
}
if ($null -ne $plan -and $plan.fail -eq $stage) {
    if (!$plan.unclassified) {
        Write-Output 'Example.java:12: error: first root cause'
        Write-Output 'Example.java:19: error: second root cause'
        Write-Output 'Example.java:19: error: second root cause'
        Write-Output 'warning: No processor claimed the fixture annotation'
        Write-Output 'error: standalone compiler failure'
        [Console]::Error.WriteLine('native stderr is retained')
    }
    exit 7
}
Write-Output 'BUILD SUCCESSFUL'
exit 0
'@ | Set-Content -LiteralPath (Join-Path $root 'fake.ps1') -Encoding UTF8
    return $root
}

function Invoke-Action {
    param([string]$Root, [string]$Action, [hashtable]$Options = @{}, [bool]$Success = $true)
    $failure = $null
    $output = & {
        try { Invoke-ChangeValidation -Root $Root -Action $Action @Options }
        catch { $script:actionFailure = $_.Exception.Message }
    } 6>&1 | Out-String
    $failure = $script:actionFailure
    $script:actionFailure = $null
    Assert-True (($null -eq $failure) -eq $Success) "$Action expected success=$Success; failure=$failure"
    return $output + $failure
}

function Read-State {
    param([string]$Root)
    return Get-Content -LiteralPath (Join-Path $Root '.validation/change.json') -Raw | ConvertFrom-Json
}

function Calls {
    param([string]$Root)
    $path = Join-Path $Root '.validation/calls.log'
    if (Test-Path -LiteralPath $path) { Get-Content -LiteralPath $path }
}

function Prepare-Case {
    param([string]$Root)
    [void](Invoke-Action $Root Begin @{ Scope = 'fixture change'; Ownership = 'fixture source owner' })
    [void](Invoke-Action $Root Ready @{ Reviewer = 'independent fixture reviewer'; Report = 'review-Change.md' })
}

function Accept-Analysis {
    param([string]$Root)
    [void](Invoke-Action $Root Assess @{ Report = 'review-Analysis.md'; Reason = 'All fixture diagnostics assessed.' })
}

$root = New-Case 'success'
[void](Invoke-Action $root Analyze @{} $false)
[void](Invoke-Action $root Begin @{ Scope = 'fixture'; Ownership = 'existing owner' })
[void](Invoke-Action $root Analyze @{} $false)
[void](Invoke-Action $root Ready @{ Reviewer = 'reviewer'; Report = 'review-Change.md' })
[void](Invoke-Action $root Finish @{} $false)
[void](Invoke-Action $root Analyze)
Assert-True (@(Calls $root).Count -eq 2) 'Analyze invokes exactly compile and analysis'
[void](Invoke-Action $root Analyze)
Assert-True (@(Calls $root).Count -eq 2) 'Successful identical analysis is reused'
[void](Invoke-Action $root Finish @{} $false)
Accept-Analysis $root
[void](Invoke-Action $root Finish)
Assert-True (((Calls $root | ForEach-Object { ($_ -split ' ')[0] }) -join ',') -eq 'compile,analysis,gametests,docs,jars') 'Stages execute in order'
Assert-True (@(Calls $root | Where-Object { $_ -notmatch '-Pquality=nullaway --continue --console=plain' }).Count -eq 0) 'Compiler mode is consistent'
Assert-True (@(Calls $root | Where-Object { $_ -match '\bclean\b' }).Count -eq 0) 'Ordinary validation never cleans'
Assert-True ((Read-State $root).status -eq 'ready-for-manual-testing') 'Generated docs do not invalidate readiness'
[void](Invoke-Action $root Finish)
Assert-True (@(Calls $root).Count -eq 5) 'Completed validation is reused'
Add-Content -LiteralPath (Join-Path $root 'common/src/main/java/Example.java') -Value '// later source edit'
$output = Invoke-Action $root Status
Assert-True ($output -match 'State: review-required') 'Status reports current source review as stale'
[void](Invoke-Action $root Finish @{} $false)
Assert-True (@(Calls $root).Count -eq 5) 'Source edits reject stale validation'

$root = New-Case 'compiler failure'
Prepare-Case $root
'{"fail":"compile","noise":true}' | Set-Content -LiteralPath (Join-Path $root '.validation/plan.json')
$output = Invoke-Action $root Analyze @{} $false
Assert-True (@(Calls $root).Count -eq 1) 'Compiler failure stops analysis'
Assert-True ($output.Length -lt 3000) 'Console output is bounded despite 500 log lines'
$stage = (Read-State $root).attempts[0].stages[0]
$diagnostics = @((Get-Content -LiteralPath $stage.diagnostics -Raw | ConvertFrom-Json) | ForEach-Object { $_ })
Assert-True ($diagnostics.Count -eq 4) 'All distinct errors and standalone diagnostics are indexed; repeated errors deduplicated'
Assert-True (@($diagnostics | Where-Object { $_.kind -eq 'warning' }).Count -eq 1) 'Standalone compiler warning is indexed'
Assert-True ((Get-Content -LiteralPath $stage.log -Raw) -match 'ordinary output 500') 'Full stdout is retained'
Assert-True ((Get-Content -LiteralPath $stage.log -Raw) -match 'native stderr is retained') 'Native stderr is retained'
[void](Invoke-Action $root Analyze @{} $false)
[void](Invoke-Action $root Begin @{ Scope = 'same change'; Ownership = 'same owner' } $false)
Assert-True (@(Calls $root).Count -eq 1) 'Neither retry nor new Begin bypasses the correction reason'
'{}' | Set-Content -LiteralPath (Join-Path $root '.validation/plan.json')
[void](Invoke-Action $root Analyze @{ Reason = 'Fixture dependency restored; both causes assessed.' })
Assert-True ((Read-State $root).attempts.Count -eq 2) 'Retry preserves failed attempt history'

$root = New-Case 'test failure'
Prepare-Case $root
[void](Invoke-Action $root Analyze)
Accept-Analysis $root
'{"junit":true}' | Set-Content -LiteralPath (Join-Path $root '.validation/plan.json')
[void](Invoke-Action $root Finish @{} $false)
Assert-True (@(Calls $root).Count -eq 3) 'Fresh JUnit failures stop docs and jars despite zero native exit'
$stage = (Read-State $root).attempts[-1].stages[0]
$diagnostics = @((Get-Content -LiteralPath $stage.diagnostics -Raw | ConvertFrom-Json) | ForEach-Object { $_ })
Assert-True (@($diagnostics | Where-Object { $_.kind -eq 'test-failure' }).Count -eq 2) 'Both failed test names are retained'
'{}' | Set-Content -LiteralPath (Join-Path $root '.validation/plan.json')
[void](Invoke-Action $root Finish @{ Reason = 'Fixture state and missing fixture both corrected.' })
Assert-True ((Read-State $root).status -eq 'ready-for-manual-testing') 'Stale failure XML does not poison the corrected run'

$root = New-Case 'docs failure'
Prepare-Case $root
[void](Invoke-Action $root Analyze)
Accept-Analysis $root
'{"fail":"docs"}' | Set-Content -LiteralPath (Join-Path $root '.validation/plan.json')
[void](Invoke-Action $root Finish @{} $false)
Assert-True (@(Calls $root).Count -eq 4) 'Docs failure prevents jar collection'
'{}' | Set-Content -LiteralPath (Join-Path $root '.validation/plan.json')
[void](Invoke-Action $root Finish @{ Reason = 'Documentation output destination restored; inputs unchanged.' })
Assert-True (@(Calls $root | Where-Object { $_ -like 'gametests *' }).Count -eq 1) 'Same-input successful GameTests are reused after a docs failure'

$root = New-Case 'changing inputs'
Prepare-Case $root
'{"mutate":"compile"}' | Set-Content -LiteralPath (Join-Path $root '.validation/plan.json')
[void](Invoke-Action $root Analyze @{} $false)
Assert-True (@(Calls $root).Count -eq 1) 'Input mutation during a stage stops the pipeline'
Assert-True ($null -eq (Read-State $root).analysis) 'Changed inputs never receive an analysis receipt'
[void](Invoke-Action $root Analyze @{ Reason = 'Must still obtain review.' } $false)

$root = New-Case 'review and clean'
Prepare-Case $root
Add-Content -LiteralPath (Join-Path $root 'review-Change.md') -Value 'Revised reviewer evidence.'
[void](Invoke-Action $root Analyze @{} $false)
[void](Invoke-Action $root Ready @{ Reviewer = 'reviewer'; Report = 'review-Change.md' })
[void](Invoke-Action $root Analyze @{ Clean = $true } $false)
[void](Invoke-Action $root Analyze @{ Clean = $true; Reason = 'Explicit stale-output diagnostic.' })
Assert-True ((Calls $root)[0] -match '^compile clean build ') 'Explicit Clean reaches only the requested compile stage'
Accept-Analysis $root
Add-Content -LiteralPath (Join-Path $root 'review-Analysis.md') -Value 'Changed disposition.'
$output = Invoke-Action $root Status
Assert-True ($output -match 'State: analysis-assessment-required') 'Status reports an edited diagnostic assessment as stale'
[void](Invoke-Action $root Finish @{} $false)
Set-Content -LiteralPath (Join-Path $root 'review-Change.md') -Value 'Verdict: Changes required'
[void](Invoke-Action $root Ready @{ Reviewer = 'reviewer'; Report = 'review-Change.md' } $false)

Write-Output ("PASS {0} assertions; fake Gradle only. Fixtures: {1}" -f $script:assertions, $fixtureRoot)
