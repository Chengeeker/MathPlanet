// Offline integrity check for the packed 16 kHz / PCM16 / mono narration assets.
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "..");
const pcm = fs.readFileSync(path.join(root, "app/src/main/assets/voicepack.pcm"));
const index = fs.readFileSync(path.join(root, "app/src/main/assets/voicepack.tsv"), "utf8")
  .trim().split(/\r?\n/).map(line => {
    const [name, offset, length] = line.split("\t");
    return { name, offset: Number(offset), length: Number(length) };
  });
let peak = 0, clipped = 0, invalid = 0, nearSilent = 0, maxStep = 0, largeSteps = 0;
const stepByClip = [];
const ranges = [];
for (const { name, offset, length } of index) {
  if (!Number.isInteger(offset) || !Number.isInteger(length) || length <= 0 ||
      offset < 0 || length % 2 || offset + length > pcm.length) {
    invalid++;
    continue;
  }
  ranges.push([offset, offset + length, name]);
  let localPeak = 0, localStep = 0, localLargeSteps = 0, last = null, silentSamples = 0;
  for (let i = offset; i < offset + length; i += 2) {
    const sample = pcm.readInt16LE(i);
    const abs = Math.abs(sample);
    localPeak = Math.max(localPeak, abs);
    if (abs >= 32760) clipped++;
    if (abs < 100) silentSamples++;
    if (last !== null) {
      const step = Math.abs(sample - last);
      localStep = Math.max(localStep, step);
      if (step > 12000) { largeSteps++; localLargeSteps++; }
    }
    last = sample;
  }
  peak = Math.max(peak, localPeak);
  maxStep = Math.max(maxStep, localStep);
  if (localLargeSteps) stepByClip.push({ name, largeSteps: localLargeSteps, largest: localStep });
  if (silentSamples / (length / 2) > 0.9) nearSilent++;
}
ranges.sort((a, b) => a[0] - b[0]);
const gaps = ranges.slice(1).filter((r, i) => r[0] !== ranges[i][1]);
const result = {
  clips: index.length, bytes: pcm.length, seconds: pcm.length / 32000,
  invalid, gaps: gaps.length, clipped, peak, maxStep, largeSteps, nearSilent,
  largeStepClips: stepByClip.sort((a, b) => b.largeSteps - a.largeSteps).slice(0, 10)
};
console.log(JSON.stringify(result, null, 2));
if (invalid || gaps.length) process.exitCode = 1;
