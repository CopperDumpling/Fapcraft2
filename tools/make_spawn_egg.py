#!/usr/bin/env python3
"""Draw a 16x16 spawn egg (base colour + spots) and its item model/definition.

    python3 -I tools/make_spawn_egg.py jenny 322640 C11A85

The colours are the two egg colours 1.12.2 registered for the entity. 26.3 has no tinted
egg template any more, so each egg gets its own texture.
"""
import json
import struct
import sys
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/sexmod"

# egg silhouette: '#' body, '.' transparent
SHAPE = [
    "......####......",
    ".....######.....",
    "....########....",
    "...##########...",
    "...##########...",
    "..############..",
    "..############..",
    ".##############.",
    ".##############.",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "..############..",
    "...##########...",
    ".....######.....",
]
SPOTS = {(5, 3), (6, 3), (5, 4), (9, 6), (10, 6), (9, 7), (10, 7), (4, 9), (5, 9), (4, 10),
         (11, 11), (12, 11), (11, 12), (7, 13), (8, 13)}


def rgb(hex_colour):
    v = int(hex_colour, 16)
    return (v >> 16) & 255, (v >> 8) & 255, v & 255


def shade(c, f):
    return tuple(max(0, min(255, int(x * f))) for x in c)


def png(pixels):
    raw = b"".join(b"\x00" + b"".join(bytes(p) for p in row) for row in pixels)
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def main():
    name, base, spot = sys.argv[1], rgb(sys.argv[2]), rgb(sys.argv[3])
    pixels = []
    for y, row in enumerate(SHAPE):
        out = []
        for x, ch in enumerate(row):
            if ch != "#":
                out.append((0, 0, 0, 0))
                continue
            edge = x == 0 or x == 15 or SHAPE[y][x - 1] != "#" or SHAPE[y][x + 1] != "#" or y == 15 or SHAPE[y + 1][x] != "#"
            c = spot if (x, y) in SPOTS else base
            if edge:
                c = shade(c, 0.6)
            elif x <= 4 and y <= 6:
                c = shade(c, 1.3)  # highlight
            out.append((*c, 255))
        pixels.append(out)
    item = f"{name}_spawn_egg"
    tex = ASSETS / "textures/item" / f"{item}.png"
    tex.parent.mkdir(parents=True, exist_ok=True)
    tex.write_bytes(png(pixels))
    model = ASSETS / "models/item" / f"{item}.json"
    model.parent.mkdir(parents=True, exist_ok=True)
    model.write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"sexmod:item/{item}"}}, indent=2) + "\n")
    definition = ASSETS / "items" / f"{item}.json"
    definition.parent.mkdir(parents=True, exist_ok=True)
    definition.write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"sexmod:item/{item}"}}, indent=2) + "\n")
    print("wrote", tex.relative_to(ROOT), model.relative_to(ROOT), definition.relative_to(ROOT))


if __name__ == "__main__":
    main()
