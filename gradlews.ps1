[CmdletBinding()]
param(
    [ValidateSet('Begin', 'Ready', 'Analyze', 'Assess', 'Finish', 'Status')]
    [string]$Action = 'Status',
    [string]$Scope,
    [string]$Ownership,
    [string]$Reviewer,
    [string]$Report,
    [string]$Reason,
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'tools/validation.ps1')

try {
    Invoke-ChangeValidation -Root $PSScriptRoot -Action $Action -Scope $Scope -Ownership $Ownership `
        -Reviewer $Reviewer -Report $Report -Reason $Reason -Clean:$Clean
    exit 0
} catch {
    Write-Host ('FAIL  ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
