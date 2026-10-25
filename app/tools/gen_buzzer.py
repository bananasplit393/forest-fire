#!/usr/bin/env python3
"""Generates res/raw/alarm_buzzer.wav: a 2-second loopable two-tone emergency buzzer.

Alternating 960 Hz / 770 Hz square-ish tones, 250 ms each, 16-bit mono 22.05 kHz,
5 ms fades so the loop has no clicks. Generated here, so there is no licence question.
Usage: python3 tools/gen_buzzer.py [output.wav]
"""
import math
import struct
import sys
import wave

RATE = 22050
TONES = [960.0, 770.0] * 4  # 8 x 250 ms = 2 s
SEGMENT = 0.25
FADE = 0.005
AMPLITUDE = 0.9


def square_ish(freq: float, t: float) -> float:
    # First three odd harmonics: harsh like a buzzer, without aliasing a pure square.
    w = 2 * math.pi * freq * t
    return (math.sin(w) + math.sin(3 * w) / 3 + math.sin(5 * w) / 5) * (4 / math.pi) * 0.8


def main(path: str) -> None:
    frames = bytearray()
    per_segment = int(RATE * SEGMENT)
    fade = int(RATE * FADE)
    for freq in TONES:
        for i in range(per_segment):
            envelope = min(1.0, i / fade, (per_segment - 1 - i) / fade)
            sample = max(-1.0, min(1.0, square_ish(freq, i / RATE) * envelope * AMPLITUDE))
            frames += struct.pack("<h", int(sample * 32767))
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(bytes(frames))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "app/src/main/res/raw/alarm_buzzer.wav")
