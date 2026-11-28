# Shared implementation for the checkout's staged validation runner. Importing it runs no Gradle task.

function Get-ValidationModules {
    param([string]$Root)
    $match = Select-String -LiteralPath (Join-Path $Root 'gradle.properties') -Pattern '^enabled_platforms\s*=\s*(.+)$'
    if ($null -eq $match) { throw 'gradle.properties has no enabled_platforms.' }
    $modules = @('common') + @($match.Matches[0].Groups[1].Value.Split(',') | ForEach-Object { $_.Trim() })
    foreach ($module in $modules) {
        if ($module -notmatch '^[a-z][a-z0-9_-]*$') { throw "Invalid module name: $module" }
    }
    return $modules
}

function Get-ValidationSnapshot {
    param([string]$Root)
    $files = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($file in Get-ChildItem -LiteralPath $Root -File -Force) {
        if ($file.Name -match '^(review-|handoff|port-)' -or $file.Extension -eq '.log') { continue }
        if ($file.Extension -in @('.md', '.gradle', '.properties', '.ps1', '.py', '.json', '.bat') -or
                $file.Name -in @('gradlew', '.gitignore', '.gitattributes')) {
            [void]$files.Add($file.FullName)
        }
    }
    $directories = @('gradle', 'config', 'tools', 'tests')
    foreach ($module in Get-ValidationModules $Root) {
        $directories += "$module/src"
        foreach ($name in @('build.gradle', 'gradle.properties')) {
            $path = Join-Path $Root "$module/$name"
            if (Test-Path -LiteralPath $path -PathType Leaf) { [void]$files.Add($path) }
        }
    }
    # Generated Javadoc is an output, not a review input. Do not traverse that tree.
    $docs = Join-Path $Root 'docs'
    if (Test-Path -LiteralPath $docs) {
        foreach ($item in Get-ChildItem -LiteralPath $docs -Force) {
            if ($item.Name -eq 'javadoc') { continue }
            if ($item.PSIsContainer) { $directories += 'docs/' + $item.Name }
            else { [void]$files.Add($item.FullName) }
        }
    }
    foreach ($directory in $directories) {
        $path = Join-Path $Root $directory
        if (!(Test-Path -LiteralPath $path -PathType Container)) { continue }
        foreach ($file in Get-ChildItem -LiteralPath $path -Recurse -File -Force) {
            if ($file.FullName -match '[\\/](\.cache|__pycache__)[\\/]') { continue }
            [void]$files.Add($file.FullName)
        }
    }
    $paths = [string[]]@($files)
    [Array]::Sort($paths, [StringComparer]::Ordinal)
    $manifest = @($paths | ForEach-Object {
        [pscustomobject]@{
            Path = $_.Substring($Root.Length + 1).Replace('\', '/')
            Hash = (Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash
        }
    })
    $text = ($manifest | ForEach-Object { $_.Path + ':' + $_.Hash }) -join "`n"
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $hash = [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))).Replace('-', '') }
    finally { $sha.Dispose() }
    return [pscustomobject]@{ Hash = $hash; Files = $manifest }
}

function Save-ValidationRecord {
    param([string]$Root, $Record)
    $directory = Join-Path $Root '.validation'
    $json = $Record | ConvertTo-Json -Depth 16
    $temporary = Join-Path $directory ([guid]::NewGuid().ToString('N') + '.json')
    [IO.File]::WriteAllText($temporary, $json, [Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temporary -Destination (Join-Path $directory 'change.json') -Force
    $archive = Join-Path $directory ('changes/' + $Record.id + '.json')
    [IO.File]::WriteAllText($archive, $json, [Text.UTF8Encoding]::new($false))
}

function Get-ValidationReport {
    param([string]$Root, [string]$Report, [string]$Verdict)
    if ([string]::IsNullOrWhiteSpace($Report)) { throw 'Supply -Report with the assessment file.' }
    $path = [IO.Path]::GetFullPath((Join-Path $Root $Report))
    if (!$path.StartsWith($Root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Review reports must be inside this checkout.'
    }
    if (!(Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing review report: $path" }
    if ((Get-Content -LiteralPath $path -Raw) -notmatch ('(?m)^Verdict:\s*' + $Verdict + '\s*$')) {
        throw "The report must contain a standalone 'Verdict: $Verdict' line."
    }
    return [pscustomobject]@{
        path = $path.Substring($Root.Length + 1).Replace('\', '/')
        hash = (Get-FileHash -LiteralPath $path).Hash
    }
}

function Assert-ValidationReady {
    param([string]$Root, $Record, [string]$Fingerprint)
    if ($null -eq $Record.review -or $Record.review.fingerprint -ne $Fingerprint) {
        throw 'The current inputs need focused review and Ready before validation.'
    }
    $report = Get-ValidationReport $Root $Record.review.report.path 'Ready'
    if ($report.hash -ne $Record.review.report.hash) { throw 'The focused review report changed; record Ready again.' }
}

function Get-ValidationDiagnostics {
    param([string]$Root, [string]$Log, [datetime]$Started)
    $diagnostics = [System.Collections.Generic.List[object]]::new()
    $seen = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    $lineNumber = 0
    foreach ($line in [IO.File]::ReadLines($Log)) {
        $lineNumber++
        $plain = $line -replace '\x1b\[[0-?]*[ -/]*[@-~]', ''
        $kind = $null
        if ($plain -match '\.java:\d+:\s*warning:|^\s*warning:') { $kind = 'warning' }
        elseif ($plain -match '\.java:\d+:\s*error:|^\s*error:|Execution failed for task|^\s*Caused by:|^\s*> Task .* FAILED|^\s*FAILURE:|^\s*\* What went wrong:') { $kind = 'error' }
        elseif ($plain -match '^\s*> .+' -and $plain -notmatch '^\s*> Task ') { $kind = 'detail' }
        if ($null -ne $kind -and $seen.Add($plain.Trim())) {
            $diagnostics.Add([pscustomobject]@{ kind = $kind; line = $lineNumber; message = $plain.Trim(); source = $Log })
        }
    }
    foreach ($module in Get-ValidationModules $Root) {
        $testFiles = @()
        $results = Join-Path $Root "$module/build/test-results"
        if (Test-Path -LiteralPath $results) { $testFiles += @(Get-ChildItem -LiteralPath $results -Recurse -File -Filter '*.xml') }
        $gameReport = Join-Path $Root "$module/build/gametest-report.xml"
        if (Test-Path -LiteralPath $gameReport) { $testFiles += Get-Item -LiteralPath $gameReport }
        foreach ($file in $testFiles | Where-Object { $_.LastWriteTimeUtc -ge $Started.ToUniversalTime() }) {
            try {
                $xml = [xml]([IO.File]::ReadAllText($file.FullName))
                foreach ($test in $xml.SelectNodes('//testcase[failure or error]')) {
                    $message = $test.GetAttribute('classname') + '.' + $test.GetAttribute('name')
                    foreach ($failure in $test.SelectNodes('failure|error')) { $message += ': ' + $failure.GetAttribute('message') }
                    $diagnostics.Add([pscustomobject]@{ kind = 'test-failure'; line = 0; message = $message; source = $file.FullName })
                }
            } catch {
                $diagnostics.Add([pscustomobject]@{ kind = 'report-error'; line = 0; message = $_.Exception.Message; source = $file.FullName })
            }
        }
    }
    return $diagnostics.ToArray()
}

function Invoke-ValidationStages {
    param([string]$Root, $Record, [string]$Phase, [string]$Fingerprint, [string]$Reason, [switch]$Clean)
    $previous = @($Record.attempts | Where-Object { $_.phase -eq $Phase })
    if ($previous.Count -gt 0 -and [string]::IsNullOrWhiteSpace($Reason)) {
        throw 'Supply -Reason describing the complete correction batch or the specific diagnostic hypothesis before another attempt.'
    }
    if ($Clean -and [string]::IsNullOrWhiteSpace($Reason)) { throw '-Clean requires a specific -Reason.' }
    if ($Phase -eq 'Analyze') {
        $compile = @('build', 'compileGametestJava')
        if ($Clean) { $compile = @('clean') + $compile }
        $stages = @(@{ name = 'compile'; tasks = $compile }, @{ name = 'analysis'; tasks = @('analysisReview') })
    } else {
        $stages = @(
            @{ name = 'gametests'; tasks = @('runGameTestServer') },
            @{ name = 'docs'; tasks = @('generateDocs') },
            @{ name = 'jars'; tasks = @('collectJars') }
        )
    }
    $id = [datetime]::UtcNow.ToString('yyyyMMddTHHmmssfffZ') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
    $directory = Join-Path $Root ('.validation/runs/' + $id)
    [void](New-Item -ItemType Directory -Path $directory -Force)
    $epoch = if ($Phase -eq 'Finish') { $Record.analysis.attempt } else { $id }
    $attempt = [pscustomobject]@{
        id = $id; phase = $Phase; fingerprint = $Fingerprint; epoch = $epoch
        reason = $Reason; started = [datetime]::UtcNow.ToString('o'); status = 'running'; stages = @()
    }
    $Record.attempts = @($Record.attempts) + $attempt
    if ($Phase -eq 'Analyze') { $Record.analysis = $null; $Record.assessment = $null }
    $Record.status = 'validating'
    Save-ValidationRecord $Root $Record
    foreach ($stage in $stages) {
        $receipt = @($previous | Where-Object { $_.fingerprint -eq $Fingerprint -and $_.epoch -eq $epoch } |
                ForEach-Object { $_.stages } | Where-Object { $_.name -eq $stage.name -and $_.exitCode -eq 0 })
        if ($receipt.Count -gt 0) {
            $attempt.stages += $receipt[-1]
            Write-Host ('REUSE ' + $stage.name)
            continue
        }
        $arguments = @($stage.tasks) + @('-Pquality=nullaway', '--continue', '--console=plain')
        $log = Join-Path $directory ($stage.name + '.log')
        $started = [datetime]::UtcNow
        Write-Host ('=== gradlew ' + ($arguments -join ' ') + ' ===') -ForegroundColor Cyan
        $nativePreference = $ErrorActionPreference
        $nativeErrors = $PSNativeCommandUseErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $PSNativeCommandUseErrorActionPreference = $false
            Push-Location -LiteralPath $Root
            try {
                & (Join-Path $Root 'gradlew.bat') @arguments 2>&1 | Out-File -LiteralPath $log -Encoding utf8
                $code = $LASTEXITCODE
            } finally { Pop-Location }
        } finally {
            $ErrorActionPreference = $nativePreference
            $PSNativeCommandUseErrorActionPreference = $nativeErrors
        }
        if ($null -eq $code) { $code = 1 }
        $diagnostics = @(Get-ValidationDiagnostics $Root $log $started)
        if (@($diagnostics | Where-Object { $_.kind -in @('test-failure', 'report-error') }).Count -gt 0) { $code = 1 }
        $indexPath = Join-Path $directory ($stage.name + '-diagnostics.json')
        ConvertTo-Json -InputObject $diagnostics -Depth 5 | Set-Content -LiteralPath $indexPath -Encoding UTF8
        $result = [pscustomobject]@{
            name = $stage.name; arguments = $arguments; exitCode = $code
            seconds = [math]::Round(([datetime]::UtcNow - $started).TotalSeconds, 3)
            log = $log; diagnostics = $indexPath; diagnosticCount = $diagnostics.Count
        }
        $attempt.stages += $result
        $unchanged = (Get-ValidationSnapshot $Root).Hash -eq $Fingerprint
        if (!$unchanged) { $code = 1; $result.exitCode = 1 }
        Save-ValidationRecord $Root $Record
        $label = if ($code -eq 0) { 'PASS' } else { 'FAIL' }
        Write-Host ("$label  {0}; {1}s; {2} diagnostic entries" -f $stage.name, $result.seconds, $diagnostics.Count)
        $diagnostics | Where-Object { $_.kind -ne 'warning' } | Select-Object -First 6 |
                ForEach-Object { Write-Host ('  ' + $_.message) }
        Write-Host ("Details: {0} ; {1}" -f $log, $indexPath)
        if ($code -ne 0) {
            $attempt.status = 'failed'
            $Record.status = 'corrections-required'
            Save-ValidationRecord $Root $Record
            if (!$unchanged) { throw 'Review inputs changed during validation; results are stale. Review the completed edits before retrying.' }
            throw "Stage $($stage.name) failed. Assess all diagnostics and finish the correction batch before retrying."
        }
    }
    $attempt.status = 'passed'
    if ($Phase -eq 'Analyze') {
        $Record.analysis = [pscustomobject]@{ fingerprint = $Fingerprint; attempt = $id }
        $Record.status = 'analysis-assessment-required'
    } else { $Record.status = 'ready-for-manual-testing' }
    Save-ValidationRecord $Root $Record
    Write-Host ('State: ' + $Record.status)
}

function Invoke-ChangeValidation {
    param([string]$Root, [string]$Action, [string]$Scope, [string]$Ownership,
          [string]$Reviewer, [string]$Report, [string]$Reason, [switch]$Clean)
    $Root = [IO.Path]::GetFullPath($Root).TrimEnd('\', '/')
    if ($Clean -and $Action -ne 'Analyze') { throw '-Clean is only available for Analyze.' }
    $directory = Join-Path $Root '.validation'
    [void](New-Item -ItemType Directory -Path (Join-Path $directory 'changes') -Force)
    $lock = [IO.File]::Open((Join-Path $directory 'runner.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    try {
        $statePath = Join-Path $directory 'change.json'
        $record = if (Test-Path -LiteralPath $statePath) { Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json } else { $null }
        if ($Action -eq 'Begin') {
            if ([string]::IsNullOrWhiteSpace($Scope) -or [string]::IsNullOrWhiteSpace($Ownership)) { throw 'Begin requires -Scope and -Ownership.' }
            if ($null -ne $record -and $record.status -ne 'ready-for-manual-testing' -and [string]::IsNullOrWhiteSpace($Reason)) {
                throw 'Finish the active change first, or supply -Reason for a deliberate owner-directed scope replacement. Begin is not a retry mechanism.'
            }
            $snapshot = Get-ValidationSnapshot $Root
            $record = [pscustomobject]@{
                schema = 1; id = [guid]::NewGuid().ToString('N'); scope = $Scope; ownership = $Ownership
                started = [datetime]::UtcNow.ToString('o'); baseline = $snapshot.Files; replacementReason = $Reason
                review = $null; analysis = $null; assessment = $null; attempts = @(); status = 'draft'
            }
            Save-ValidationRecord $Root $record
            Write-Host ('Change: ' + $record.id)
            return
        }
        if ($null -eq $record) {
            if ($Action -eq 'Status') { Write-Host 'No change record. Begin a coherent change before validation.'; return }
            throw 'Begin a change with its scope and ownership before validation.'
        }
        $snapshot = Get-ValidationSnapshot $Root
        if ($Action -eq 'Status') {
            Write-Host ('Change: ' + $record.id + '; ' + $record.scope)
            $status = $record.status
            $staleReason = $null
            if ($null -ne $record.review) {
                try { Assert-ValidationReady $Root $record $snapshot.Hash }
                catch { $status = 'review-required'; $staleReason = $_.Exception.Message }
            }
            if ($null -eq $staleReason -and $null -ne $record.assessment) {
                try {
                    $assessment = Get-ValidationReport $Root $record.assessment.report.path 'Accepted'
                    if ($assessment.hash -ne $record.assessment.report.hash) { throw 'The diagnostic assessment changed; record Assess again.' }
                } catch { $status = 'analysis-assessment-required'; $staleReason = $_.Exception.Message }
            }
            Write-Host ('State: ' + $status + '; attempts: ' + @($record.attempts).Count)
            if ($null -ne $staleReason) { Write-Host ('Recorded state: ' + $record.status + '; stale: ' + $staleReason) }
            Write-Host ('Current fingerprint: ' + $snapshot.Hash)
            $before = @{}; foreach ($file in $record.baseline) { $before[$file.Path] = $file.Hash }
            $after = @{}; foreach ($file in $snapshot.Files) { $after[$file.Path] = $file.Hash }
            foreach ($path in @(@($before.Keys) + @($after.Keys) | Sort-Object -Unique)) {
                if ($before[$path] -ne $after[$path]) { Write-Host ('Changed: ' + $path) }
            }
            return
        }
        if ($Action -eq 'Ready') {
            if ([string]::IsNullOrWhiteSpace($Reviewer)) { throw 'Ready requires the independent -Reviewer identity.' }
            $reviewReport = Get-ValidationReport $Root $Report 'Ready'
            $record.review = [pscustomobject]@{ fingerprint = $snapshot.Hash; reviewer = $Reviewer; report = $reviewReport }
            $record.status = 'ready-for-analysis'
            Save-ValidationRecord $Root $record
            Write-Host ('Ready: ' + $snapshot.Hash)
            return
        }
        Assert-ValidationReady $Root $record $snapshot.Hash
        if ($Action -eq 'Analyze') {
            if ($null -ne $record.analysis -and $record.analysis.fingerprint -eq $snapshot.Hash -and !$Clean -and [string]::IsNullOrWhiteSpace($Reason)) {
                Write-Host ('REUSE completed analysis: ' + $record.analysis.attempt); return
            }
            Invoke-ValidationStages $Root $record 'Analyze' $snapshot.Hash $Reason -Clean:$Clean
            return
        }
        if ($null -eq $record.analysis -or $record.analysis.fingerprint -ne $snapshot.Hash) { throw 'Analyze must pass for the current inputs first.' }
        if ($Action -eq 'Assess') {
            if ([string]::IsNullOrWhiteSpace($Reason)) { throw 'Assess requires -Reason summarizing diagnostic dispositions.' }
            $assessmentReport = Get-ValidationReport $Root $Report 'Accepted'
            $record.assessment = [pscustomobject]@{ attempt = $record.analysis.attempt; report = $assessmentReport; reason = $Reason }
            $record.status = 'ready-for-gametests'
            Save-ValidationRecord $Root $record
            Write-Host 'Analysis assessed; Finish may run GameTests, documentation and jar collection.'
            return
        }
        if ($Clean) { throw '-Clean is only available for Analyze.' }
        if ($null -eq $record.assessment -or $record.assessment.attempt -ne $record.analysis.attempt) { throw 'Assess the advisory diagnostics before Finish.' }
        $assessment = Get-ValidationReport $Root $record.assessment.report.path 'Accepted'
        if ($assessment.hash -ne $record.assessment.report.hash) { throw 'The diagnostic assessment changed; record Assess again.' }
        if ($record.status -eq 'ready-for-manual-testing' -and [string]::IsNullOrWhiteSpace($Reason)) { Write-Host 'REUSE completed validation; ready for manual testing.'; return }
        Invoke-ValidationStages $Root $record 'Finish' $snapshot.Hash $Reason
    } finally { $lock.Dispose() }
}
