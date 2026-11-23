# Build a production APK with the original BKS signing identity, without exposing passwords.
# Usage: powershell -ExecutionPolicy Bypass -File scripts\sign-release.ps1
param([switch]$SkipGradle)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$propertiesFile = Join-Path $root 'key.properties'
if (!(Test-Path -LiteralPath $propertiesFile)) { throw 'Missing local key.properties. Never commit signing passwords.' }
$props = @{}
Get-Content -LiteralPath $propertiesFile | ForEach-Object {
    if ($_ -match '^\s*([^#=]+)\s*=(.*)$') { $props[$matches[1].Trim()] = $matches[2].Trim() }
}
foreach ($key in @('storeFile','storePassword','keyPassword','keyAlias')) {
    if ([string]::IsNullOrWhiteSpace($props[$key])) { throw "Missing signing property: $key" }
}
$store = $props['storeFile']
if (!([IO.Path]::IsPathRooted($store))) { $store = Join-Path $root $store }
if (!(Test-Path -LiteralPath $store)) { throw "Missing signing store: $store" }
$type = if ($props['storeType']) { $props['storeType'] } else { 'BKS' }
$bc = Get-ChildItem (Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1\org.bouncycastle') -Recurse -File -Filter 'bcprov-jdk18on-*.jar' -ErrorAction SilentlyContinue | Sort-Object FullName -Descending | Select-Object -First 1
if ($type -ieq 'BKS' -and $null -eq $bc) { throw 'Missing Bouncy Castle provider JAR (bcprov-jdk18on).' }
$sdk = 'C:\Users\Administrator\AppData\Local\Android\Sdk'
$buildTools = Get-ChildItem (Join-Path $sdk 'build-tools') -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName 'lib\apksigner.jar') } |
    Sort-Object { [version]($_.Name -replace '[^\d\.]','') } -Descending | Select-Object -First 1
if ($null -eq $buildTools) { throw 'Android build-tools not installed.' }
$apksigner = Join-Path $buildTools.FullName 'lib\apksigner.jar'
$java = 'D:\Java\bin\java.exe'
$zipalign = Join-Path $buildTools.FullName 'zipalign.exe'
$aapt2 = Join-Path $buildTools.FullName 'aapt2.exe'
$keytool = 'D:\Java\bin\keytool.exe'
if (!(Test-Path $java) -or !(Test-Path $keytool)) { throw 'JDK tools were not found.' }
if (!$SkipGradle) {
    Push-Location $root
    try {
        & (Join-Path $root 'gradlew.bat') ':app:testDebugUnitTest' ':app:assembleRelease' '--console=plain'
        if ($LASTEXITCODE -ne 0) { throw 'Gradle build failed.' }
    } finally { Pop-Location }
}
$unsigned = Join-Path $root 'app\build\outputs\apk\release\app-release-unsigned.apk'
if (!(Test-Path $unsigned)) { throw 'Unsigned release APK not found.' }
$badging = & $aapt2 dump badging $unsigned 2>&1 | Out-String
$packageMatch = [regex]::Match($badging, "package: name='com\.game\.maths' versionCode='([0-9]+)' versionName='([^']+)'")
if ($LASTEXITCODE -ne 0 -or !$packageMatch.Success) {
    throw 'Package identity or version missing (expected com.game.maths).'
}
$envNameStore = 'MATHS_BUILD_STORE_PASS'
$envNameKey = 'MATHS_BUILD_KEY_PASS'
$oldStore = [Environment]::GetEnvironmentVariable($envNameStore,'Process')
$oldKey = [Environment]::GetEnvironmentVariable($envNameKey,'Process')
[Environment]::SetEnvironmentVariable($envNameStore, $props['storePassword'], 'Process')
[Environment]::SetEnvironmentVariable($envNameKey, $props['keyPassword'], 'Process')
$temp = Join-Path $env:TEMP ('MathPlanet-' + [guid]::NewGuid().ToString('N') + '.p12')
$aligned = Join-Path $env:TEMP ('MathPlanet-' + [guid]::NewGuid().ToString('N') + '-aligned.apk')
$target = Join-Path $root ("MathPlanet-v" + $packageMatch.Groups[2].Value + "-signed.apk")
try {
    $providerArgs = @()
    if ($type -ieq 'BKS') {
        $providerArgs = @('-providerclass', 'org.bouncycastle.jce.provider.BouncyCastleProvider', '-providerpath', $bc.FullName)
    }
    $checkArgs = @('-list', '-storetype', $type, '-keystore', $store, '-storepass:env', $envNameStore, '-alias', $props['keyAlias']) + $providerArgs
    & $keytool @checkArgs | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'BKS store validation failed.' }
    # This is a temporary container of the SAME private key/certificate, not a new identity.
    $convertArgs = @(
        '-importkeystore', '-noprompt',
        '-srckeystore', $store, '-srcstoretype', $type, '-srcalias', $props['keyAlias'],
        '-srcstorepass:env', $envNameStore, '-srckeypass:env', $envNameKey,
        '-destkeystore', $temp, '-deststoretype', 'PKCS12', '-destalias', $props['keyAlias'],
        '-deststorepass:env', $envNameStore, '-destkeypass:env', $envNameKey
    ) + $providerArgs
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $keytool @convertArgs 2>&1 | Out-Null
        $convertExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    if ($convertExit -ne 0) { throw 'BKS -> PKCS12 temporary conversion failed.' }
    & $zipalign -P 16 -f 4 $unsigned $aligned
    if ($LASTEXITCODE -ne 0) { throw 'zipalign failed.' }
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        # Invoke the JAR directly; the batch launcher can stall in PowerShell automation.
        & $java -jar $apksigner sign --ks $temp --ks-type PKCS12 --ks-key-alias $props['keyAlias'] --ks-pass "env:$envNameStore" --key-pass "env:$envNameKey" --out $target $aligned 2>&1 | Out-Null
        $signExit = $LASTEXITCODE
        if ($signExit -ne 0) { throw 'APK signing failed.' }
        $certs = & $java -jar $apksigner verify --print-certs --verbose $target 2>&1 | Out-String
        $verifyExit = $LASTEXITCODE
        if ($verifyExit -ne 0) { throw 'Signature verification failed.' }
    } finally { $ErrorActionPreference = $previousPreference }
    if ($certs -notmatch '(?m)^Verified using v2 scheme .*: true\s*$') { throw 'APK v2 signing verification failed.' }
    if ($certs -notmatch '(?m)^Verified using v3 scheme .*: true\s*$') { throw 'APK v3 signing verification failed.' }
    if ($certs -notmatch "certificate SHA-256 digest:") { throw 'Missing signature certificate.' }
    & $zipalign -c -P 16 -v 4 $target | Select-Object -Last 2
    if ($LASTEXITCODE -ne 0) { throw 'Signed APK alignment check failed.' }
    # Android's optional v4 .idsig is not needed for manual installation; keep root tidy.
    Remove-Item -LiteralPath ($target + '.idsig') -Force -ErrorAction SilentlyContinue
    Write-Output 'SIGNING_SUCCEEDED'
    Write-Output ('APK='+$target)
    $hashBytes = [System.Security.Cryptography.SHA256]::Create().ComputeHash([System.IO.File]::ReadAllBytes($target))
    Write-Output ('SHA256=' + [System.BitConverter]::ToString($hashBytes).Replace('-', '').ToLowerInvariant())
    $certs -split "`r?`n" | Where-Object { $_ -match 'Verified using v2|Verified using v3|certificate SHA-256 digest' }
} finally {
    Remove-Item -LiteralPath $temp,$aligned -Force -ErrorAction SilentlyContinue
    [Environment]::SetEnvironmentVariable($envNameStore,$oldStore,'Process')
    [Environment]::SetEnvironmentVariable($envNameKey,$oldKey,'Process')
}
