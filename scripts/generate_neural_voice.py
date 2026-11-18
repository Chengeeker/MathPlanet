#!/usr/bin/env python3
"""Regenerate offline Mandarin clips with a warm Microsoft Edge neural voice.

This is a build-time helper only; the APK never needs online TTS.
The Microsoft speech service is external. Review service usage and redistribution
rights before public/commercial distribution of the generated audio.
"""
import asyncio
import shutil
import subprocess
import wave
from pathlib import Path

import edge_tts
import imageio_ffmpeg

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "audio" / "voiceclips"
TEMP = ROOT / "build" / "voice_generation_temp"
VOICE = "zh-CN-XiaoyiNeural"
RATE = "-7%"
PITCH = "+2Hz"

def phrases():
    digits = "零一二三四五六七八九"
    names = {}
    for n in range(100):
        if n < 10:
            literal = digits[n]
        else:
            tens, ones = divmod(n, 10)
            literal = ("十" if tens == 1 else digits[tens] + "十")
            if ones: literal += digits[ones]
        names["n_" + str(n)] = literal
    names.update({
        "unit_100": "百",
        "unit_1000": "千",
        "unit_10000": "万",
        "unit_zero": "零",
        "negative": "负",
        "q_begin": "第",
        "q_after_index": "题，听好啦！",
        "q_equal": "等于多少呢？",
        "q_plus": "加",
        "q_minus": "减",
        "q_multiply": "乘以",
        "q_divide": "除以",
        "q_lparen": "左括号",
        "q_rparen": "右括号",
        "welcome": "欢迎来到数学星球！和小恐龙一起，快乐地学数学吧！",
        "good": "答对啦！你真棒！又收获了一颗星星！",
        "good_after_retry": "太好了！你认真思考找到了正确答案！",
        "retry": "差一点点啦，再试一次吧！你一定行！",
        "show_answer": "没关系，我们一起看看。正确答案是",
        "explain": "我们一步一步算。",
        "is": "等于",
        "then": "再算",
        "finish": "闯关成功！小恐龙为你鼓掌！",
        "tap": "你选的是",
        "test": "你好呀，我是数学星球的小恐龙！我们一起学数学吧！",
        "now": "下一题！",
        "question_hint": "数数看，有多少个小星星？",
        "ask_again": "再听一次题目。",
        "shop_price": "商品价格",
        "shop_pay": "请付钱",
        "shop_paid": "顾客付了",
        "shop_change": "应该找回",
        "shop_retry": "再仔细数一数",
        "shop_success": "太棒了，你算对啦",
        "shop_more": "给的钱多啦，仔细看看应该是多少",
        "shop_less": "还不够哦，再看看差多少",
        "yuan": "元",
    })
    if len(names) != 137:
        raise ValueError("Unexpected number of Mandarin source phrases: " + str(len(names)))
    return names

async def generate_one(key, literal, semaphore):
    mp3 = TEMP / (key + ".mp3")
    wav = OUT / (key + ".wav")
    if wav.exists() and wav.stat().st_size > 350:
        return
    async with semaphore:
        for attempt in range(4):
            try:
                if not mp3.exists() or mp3.stat().st_size < 500:
                    partial = mp3.with_suffix(".download")
                    await edge_tts.Communicate(
                        literal, VOICE, rate=RATE, pitch=PITCH
                    ).save(str(partial))
                    if partial.stat().st_size < 500:
                        raise RuntimeError("Empty neural output: " + key)
                    partial.replace(mp3)
                result = subprocess.run([
                    imageio_ffmpeg.get_ffmpeg_exe(), "-v", "error", "-y",
                    "-i", str(mp3), "-ac", "1", "-ar", "16000",
                    "-acodec", "pcm_s16le", str(wav)
                ], capture_output=True, text=True)
                if result.returncode != 0:
                    raise RuntimeError(result.stderr)
                with wave.open(str(wav), "rb") as audio:
                    if (audio.getnchannels(), audio.getsampwidth(),
                        audio.getframerate()) != (1,2,16000):
                        raise ValueError("Incompatible WAV format")
                    if audio.getnframes() < 1600:
                        raise ValueError("Too few audio samples")
                break
            except Exception as ex:
                if wav.exists(): wav.unlink()
                print("RETRY", key, attempt+1, str(ex)[:120], flush=True)
                if attempt == 3: raise
                await asyncio.sleep(1.5 * (attempt + 1))

async def main():
    OUT.mkdir(parents=True, exist_ok=True)
    TEMP.mkdir(parents=True, exist_ok=True)
    try:
        phrases_by_id = phrases()
        semaphore = asyncio.Semaphore(6)
        jobs = [generate_one(key, value, semaphore) for key,value in phrases_by_id.items()]
        await asyncio.gather(*jobs)
        ready = list(OUT.glob("*.wav"))
        if len(ready) != len(phrases_by_id):
            raise ValueError("Incomplete voice source library")
        print("NEURAL_VOICE_GENERATED", VOICE, len(jobs), flush=True)
        print("VOICE_CLIPS_READY", len(ready), flush=True)
    finally:
        # Intermediate MP3s are disposable; the 16kHz WAV source library is permanent.
        shutil.rmtree(TEMP, ignore_errors=True)

if __name__ == "__main__":
    asyncio.run(main())
