#!/usr/bin/env python3
"""Generate original child-friendly audio; pack locally synthesized Mandarin voice.

This script only uses Python's standard library and local WAV clips.
All musical notes, arrangement and sound effects are original.
"""
import array
import math
import os
import random
import struct
import sys
import wave
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "app" / "src" / "main" / "res" / "raw"
ASSETS = ROOT / "app" / "src" / "main" / "assets"
CLIPS = ROOT / "audio" / "voiceclips"
RAW.mkdir(parents=True, exist_ok=True)
ASSETS.mkdir(parents=True, exist_ok=True)

FS = 22050
RNG = random.Random(77)

def note_hz(name):
    if name == "R":
        return 0.0
    n = "C C# D D# E F F# G G# A A# B".split()
    pitch, octv = name[:-1], int(name[-1])
    midi = (octv + 1) * 12 + n.index(pitch)
    return 440.0 * 2 ** ((midi - 69) / 12)

def mallet(t, freq, length, amp=0.14):
    if freq <= 0 or t < 0 or t > length:
        return 0
    # Airy mallet: rounded attack, fast decay and an inharmonic sparkle.
    attack = min(1.0, t * 90)
    env = attack * math.exp(-3.6 * t / max(length, 0.05)) * min(1.0, (length - t) * 35)
    w = 2 * math.pi * freq * t
    return amp * env * (0.77 * math.sin(w) + 0.17 * math.sin(2.02*w) + 0.06 * math.sin(4.1*w))

def bell(t, freq, length, amp=0.18):
    if t < 0 or t >= length: return 0
    env = (1 - math.exp(-80*t)) * math.exp(-3.8*t/max(length,.04)) * min(1., (length-t)*24)
    w = 2*math.pi*freq*t
    return amp*env*(.64*math.sin(w)+.26*math.sin(2*w)+.10*math.sin(3.02*w))

def pad(t, freq, length, amp=0.06):
    if t < 0 or t >= length: return 0
    attack = min(1, t/0.25)
    release = min(1, (length-t)/0.25)
    w = 2*math.pi*freq*t
    return amp*attack*release*(0.82*math.sin(w)+0.18*math.sin(2*w))

def export_wav(path, samples, fs=FS):
    ints = array.array("h")
    for x in samples:
        # Gentle limiter for summing instrument parts.
        clipped = math.tanh(1.5*x) / math.tanh(1.5)
        ints.append(int(max(-1.0,min(1.0,clipped))*32760))
    if sys.byteorder != "little": ints.byteswap()
    with wave.open(str(path), "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(fs)
        out.writeframes(ints.tobytes())
    print(f"WAV {path.name}: {path.stat().st_size // 1024} KiB")

def original_music():
    bpm = 112
    beat = 60 / bpm
    measures = 8
    duration = measures * 4 * beat
    count = int(duration * FS)
    sounds = [0.0] * count
    # Bright major-key, calm enough behind Mandarin narration.
    chords = [
        ("C4","E4","G4"), ("G3","B3","D4"),
        ("A3","C4","E4"), ("F3","A3","C4"),
        ("C4","E4","G4"), ("G3","B3","D4"),
        ("F3","A3","C4"), ("G3","B3","D4")
    ]
    melody = [
        ["E5","G5","E5","D5","C5","R","E5","R"],
        ["D5","G5","D5","B4","G4","R","D5","R"],
        ["C5","E5","A5","E5","C5","R","B4","R"],
        ["A4","C5","F5","C5","A4","R","G4","R"],
        ["E5","G5","C6","G5","E5","R","D5","R"],
        ["D5","G5","B5","G5","D5","R","B4","R"],
        ["A4","C5","F5","C5","A4","R","C5","R"],
        ["B4","D5","G5","D5","G4","R","G5","R"]
    ]
    for bar in range(measures):
        start = bar * 4*beat
        # Triad pad, sustained 1.9 seconds; each note is low volume.
        for chord in chords[bar]:
            freq = note_hz(chord)
            for i in range(int(3.95*beat*FS)):
                idx = int(start*FS)+i
                if idx < count:
                    sounds[idx] += pad(i/FS, freq, 3.95*beat, 0.027)
        # Soft arpeggio every beat.
        for b in range(4):
            chord = chords[bar][(b+bar)%3]
            freq=note_hz(chord)*2
            ix=int((start+b*beat)*FS)
            for i in range(int(beat*.91*FS)):
                if ix+i<count:
                    sounds[ix+i] += mallet(i/FS, freq, beat*.91, .12)
        # Main marimba: eighth-note melody.
        for k, pitch in enumerate(melody[bar]):
            hz=note_hz(pitch)
            ix=int((start+k*beat*.5)*FS)
            for i in range(int(beat*.44*FS)):
                if ix+i<count:
                    sounds[ix+i] += bell(i/FS, hz, beat*.44, .165)
        # Tiny rhythmic shaker, very soft and never overpowering.
        for k in (1,3,5,7):
            ix=int((start + k*beat*.5)*FS)
            for i in range(int(.07*FS)):
                if ix+i<count:
                    env=math.exp(-60*i/FS)
                    sounds[ix+i] += RNG.uniform(-.018,.018)*env
    # Near-seamless looping: fade just the last 65ms and first 65ms.
    fade=int(.065*FS)
    for i in range(fade):
        sounds[i] *= i/fade
        sounds[-1-i] *= i/fade
    export_wav(RAW/"adventure_loop.wav", sounds)

def sfx(events, duration, filename):
    sounds=[0.0]*int(FS*duration)
    for start, pitch, length, amp in events:
        freq=note_hz(pitch)
        starti=int(start*FS)
        for i in range(int(length*FS)):
            if starti+i < len(sounds):
                sounds[starti+i] += bell(i/FS,freq,length,amp)
    export_wav(RAW/filename,sounds)

def voice_pack():
    if not CLIPS.exists(): raise FileNotFoundError("Missing audio/voiceclips: run generate_neural_voice.py first")
    combined=bytearray()
    index=[]
    for path in sorted(CLIPS.glob("*.wav")):
        with wave.open(str(path)) as fp:
            rate=fp.getframerate(); nch=fp.getnchannels()
            width=fp.getsampwidth()
            raw=fp.readframes(fp.getnframes())
        if (rate,nch,width)!=(16000,1,2):
            raise ValueError(f"{path.name} format {(rate,nch,width)}")
        arr=array.array("h")
        arr.frombytes(raw)
        if sys.byteorder!="little": arr.byteswap()
        # Trim long synthesis silence; neural clips need shorter joins to avoid robotic gaps.
        peaks=[abs(x) for x in arr]
        active=[i for i, amplitude in enumerate(peaks) if amplitude>140]
        if active:
            left=max(0,active[0]-int(.025*rate))
            right=min(len(arr),active[-1]+int(.040*rate))
            arr=arr[left:right]
        # Fade first and last 10ms to avoid pops between segments.
        fade=min(int(.01*rate),len(arr)//2)
        for i in range(fade):
            arr[i]=int(arr[i]*i/fade)
            arr[-1-i]=int(arr[-1-i]*i/fade)
        off=len(combined)
        combined.extend(arr.tobytes())
        index.append((path.stem,off,len(arr)*2))
    (ASSETS/"voicepack.pcm").write_bytes(combined)
    (ASSETS/"voicepack.tsv").write_text(
        "\n".join(f"{key}\t{offset}\t{length}" for key,offset,length in index)+"\n",
        encoding="utf-8"
    )
    print(f"Voice pack: {len(index)} clips, {len(combined)//1024} KiB (16kHz/16-bit/mono)")

if __name__ == "__main__":
    original_music()
    sfx([(0,"C6",.09,.32)],.13,"tap.wav")
    sfx([(0,"C5",.25,.25),(.15,"E5",.28,.27),(.30,"G5",.30,.28),(.48,"C6",.43,.27)],.95,"success.wav")
    sfx([(0,"A4",.30,.20),(.18,"F4",.40,.16)],.7,"retry.wav")
    voice_pack()
