# Produce optional, fully offline shop narration clips using the local Chinese SAPI voice.
# Existing neural clips remain untouched. Output is 16 kHz PCM16 mono WAV.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Speech
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$out = Join-Path $root 'audio\voiceclips'
New-Item -ItemType Directory -Force -Path $out | Out-Null
$phrases = [ordered]@{
  'shop_price' = '商品价格'
  'shop_pay' = '请付钱'
  'shop_paid' = '顾客付了'
  'shop_change' = '应该找回'
  'shop_retry' = '再仔细数一数'
  'shop_success' = '太棒了，你算对啦'
  'shop_more' = '给的钱多啦，仔细看看应该是多少'
  'shop_less' = '还不够哦，再看看差多少'
  'yuan' = '元'
}
$s = New-Object System.Speech.Synthesis.SpeechSynthesizer
try {
  $chinese = $s.GetInstalledVoices() | Where-Object { $_.VoiceInfo.Culture.Name -eq 'zh-CN' } | Select-Object -First 1
  if ($null -eq $chinese) { throw 'No zh-CN local SAPI voice is installed.' }
  $s.SelectVoice($chinese.VoiceInfo.Name)
  $s.Rate = -1
  $format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo -ArgumentList @(
    16000,
    [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen,
    [System.Speech.AudioFormat.AudioChannel]::Mono
  )
  foreach ($entry in $phrases.GetEnumerator()) {
    $name = Join-Path $out ($entry.Key + '.wav')
    if (Test-Path $name) { continue }
    $s.SetOutputToWaveFile($name, $format)
    $s.Speak([string]$entry.Value)
    $s.SetOutputToNull()
    Write-Output "SHOP_SPEECH: $($entry.Key)"
  }
} finally {
  $s.Dispose()
}
