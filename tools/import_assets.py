#!/usr/bin/env python3
"""Import the 1.12.2 assets into the 26.3 layout (re-runnable; overwrites its outputs).

    python3 -I tools/import_assets.py ../Jenny-Mod-Fapcraft

- geo/        -> assets/sexmod/geckolib/models/      (GeckoLib 5 path)
- animations/ -> assets/sexmod/geckolib/animations/
- textures/, sounds/ (*.ogg) + sounds.json, models/item -> same place
- lang/*.lang -> lang/*.json with 26.3 translation keys, plus tools/lang_extra/<lang>.json
- loot_tables/ -> data/sexmod/loot_table/entities/ (26.3 loot format)
- structures/  -> data/sexmod/structure/ (old templates are datafixed on load)
- recipes/, blockstates/ -> pending/ (moved into resources once their item/block exists)

Skipped: source-only files (*.mp3, *.py, *.mp4, *.txt notes).
"""
import json
import re
import shutil
import sys
from pathlib import Path

SRC = Path(sys.argv[1]).resolve() / "src/main/resources"
OLD = SRC / "assets/sexmod"
ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/sexmod"
DATA = ROOT / "src/main/resources/data/sexmod"
PENDING = ROOT / "pending"

SKIP_SUFFIXES = {".mp3", ".py", ".mp4", ".txt"}


def load_lenient_json(path: Path):
    """1.12's Gson accepted trailing commas; 26.3's parser (and Python's) does not."""
    return json.loads(re.sub(r",(\s*[}\]])", r"\1", path.read_text(encoding="utf-8")))


def copy_tree(src: Path, dst: Path, only=None):
    # overwrite only: hand-authored 26.3 files (spawn eggs, ...) live in the same folders
    n = 0
    for f in sorted(src.rglob("*")):
        if not f.is_file() or f.suffix.lower() in SKIP_SUFFIXES:
            continue
        if only and f.suffix.lower() not in only:
            continue
        out = dst / f.relative_to(src)
        out.parent.mkdir(parents=True, exist_ok=True)
        if f.suffix.lower() == ".json" and f.stat().st_size < 1_000_000:
            out.write_text(json.dumps(load_lenient_json(f), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        else:
            shutil.copy2(f, out)
        n += 1
    return n


# 1.12 key -> 26.3 key. Custom keys (jenny.*, action.*, window.*, ...) are read by the
# mod's own code and keep their names.
def convert_key(key: str) -> str:
    parts = key.split(".")
    if parts[0] in ("entity", "item", "tile") and len(parts) == 3 and parts[2] == "name":
        kind = "block" if parts[0] == "tile" else parts[0]
        return f"{kind}.sexmod.{parts[1]}"
    if parts[0] == "effect" and len(parts) == 2:
        return f"effect.sexmod.{parts[1]}"
    if parts[0] in ("potion", "splash_potion", "lingering_potion", "tipped_arrow") and len(parts) == 3 and parts[1] == "effect":
        return f"item.minecraft.{parts[0]}.effect.{parts[2]}"
    return key


def convert_lang(src: Path, dst: Path):
    out = {}
    for line in src.read_text(encoding="utf-8-sig").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[convert_key(k.strip())] = v
    # strings added in the 26.3 rebuild (config screen etc.) live in tools/lang_extra/
    extra = Path(__file__).resolve().parent / "lang_extra" / dst.name
    if extra.is_file():
        out.update(json.loads(extra.read_text(encoding="utf-8")))
    dst.write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return len(out)


# GeckoLib 3 evaluated unknown Molang names as 0. The 1.12 exports contain junk that relied on
# that: easing names pasted into values ("easeinoutsine", "-97.03+easeinoutsine", "linear"),
# "nan", and lone dots ("50+.").
# GeckoLib 5 rejects the whole animation, so rewrite them to what GeckoLib 3 computed.
# Real variables (size, pitch, ...) are left alone; MolangVariables registers them.
JUNK_NAME = re.compile(r"(?i)\b(ease[a-z]+|linear|nan)\b")
LONE_DOT = re.compile(r"(?<![\w.])\.(?![\w.])")  # "50+." but not ".5"


def fix_value(value):
    if not isinstance(value, str):
        return value, False
    fixed = LONE_DOT.sub("0", JUNK_NAME.sub("0", value))
    return fixed, fixed != value


def fix_animation_values(folder: Path) -> int:
    count = 0
    def walk(node):
        nonlocal count
        items = node.items() if isinstance(node, dict) else enumerate(node) if isinstance(node, list) else ()
        for key, value in list(items):
            if isinstance(value, (dict, list)):
                walk(value)
            elif key != "easing" and key != "effect":
                fixed, changed = fix_value(value)
                if changed:
                    node[key] = fixed
                    count += 1
    for f in sorted(folder.rglob("*.json")):
        data = json.loads(f.read_text(encoding="utf-8"))
        for animation in data.get("animations", {}).values():
            walk(animation.get("bones", {}))
        f.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return count


def convert_number(n):
    if isinstance(n, dict) and "type" not in n and {"min", "max"} <= n.keys():
        return {"type": "minecraft:uniform", "min": n["min"], "max": n["max"]}
    return n


def convert_loot(src: Path, dst: Path):
    old = json.loads(src.read_text(encoding="utf-8"))
    pools = []
    for p in old.get("pools", []):
        entries = []
        for e in p.get("entries", []):
            entry = {"type": "minecraft:" + e["type"].removeprefix("minecraft:"), "name": e["name"]}
            if "weight" in e:
                entry["weight"] = e["weight"]
            fns = []
            for fn in e.get("functions", []):
                fn = dict(fn)
                fn["function"] = "minecraft:" + fn["function"].removeprefix("minecraft:")
                if "count" in fn:
                    fn["count"] = convert_number(fn["count"])
                fns.append(fn)
            if fns:
                entry["functions"] = fns
            entries.append(entry)
        pools.append({"rolls": convert_number(p.get("rolls", 1)), "entries": entries})
    dst.write_text(json.dumps({"type": "minecraft:entity", "pools": pools}, indent=2) + "\n", encoding="utf-8")


def convert_recipe(src: Path, dst: Path):
    old = json.loads(src.read_text(encoding="utf-8"))
    new = {"type": old["type"]}
    for k in ("pattern", "category"):
        if k in old:
            new[k] = old[k]
    if "key" in old:
        new["key"] = {c: v["item"] for c, v in old["key"].items()}
    if "ingredients" in old:
        new["ingredients"] = [i["item"] for i in old["ingredients"]]
    res = old["result"]
    new["result"] = {"id": res["item"], "count": res.get("count", 1)}
    dst.write_text(json.dumps(new, indent=2) + "\n", encoding="utf-8")


# Before a channel's first keyframe, GeckoLib 3 held that keyframe's value (Bedrock semantics).
# GeckoLib 5 instead extrapolates the first segment backwards, and with an easing curve that
# reaches thousands of degrees: bones spin wildly until the first keyframe, then settle.
# Pin each such channel with a copy of its first value at t=0.
def hold_first_keyframes(folder: Path) -> int:
    count = 0
    for f in sorted(folder.rglob("*.json")):
        data = json.loads(f.read_text(encoding="utf-8"))
        for animation in data.get("animations", {}).values():
            for bone in animation.get("bones", {}).values():
                for channel, frames in list(bone.items()):
                    if not isinstance(frames, dict) or "vector" in frames or not frames:
                        continue
                    try:
                        times = {float(t): t for t in frames}
                    except ValueError:
                        continue
                    first = min(times)
                    if first <= 0:
                        continue
                    frame = frames[times[first]]
                    if isinstance(frame, dict):
                        value = frame.get("pre", frame.get("vector", frame.get("post")))
                        if isinstance(value, dict):  # "pre": {"vector": [...]}
                            value = value.get("vector")
                    else:
                        value = frame
                    if value is None:
                        continue
                    bone[channel] = {"0.0": {"vector": value}, **frames}
                    count += 1
        f.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return count


def main():
    report = {}
    report["geckolib/models"] = copy_tree(OLD / "geo", ASSETS / "geckolib/models")
    report["geckolib/animations"] = copy_tree(OLD / "animations", ASSETS / "geckolib/animations")
    report["animation values fixed"] = fix_animation_values(ASSETS / "geckolib/animations")
    report["late first keyframes held"] = hold_first_keyframes(ASSETS / "geckolib/animations")
    report["textures"] = copy_tree(OLD / "textures", ASSETS / "textures")
    report["sounds"] = copy_tree(OLD / "sounds", ASSETS / "sounds", only={".ogg"})
    report["models/item"] = copy_tree(OLD / "models/item", ASSETS / "models/item")
    report["structure"] = copy_tree(OLD / "structures", DATA / "structure")
    shutil.copy2(SRC / "logo.png", ROOT / "src/main/resources/logo.png")

    (ASSETS / "lang").mkdir(parents=True, exist_ok=True)
    for f in (ASSETS / "lang").glob("*.json"):
        f.unlink()
    report["lang"] = 0
    for f in sorted((OLD / "lang").glob("*.lang")):
        convert_lang(f, ASSETS / "lang" / (f.stem.lower() + ".json"))
        report["lang"] += 1

    loot = DATA / "loot_table/entities"
    if loot.exists():
        shutil.rmtree(loot)
    loot.mkdir(parents=True)
    for f in sorted((OLD / "loot_tables").glob("*.json")):
        convert_loot(f, loot / f.name)
    report["loot_table"] = len(list(loot.iterdir()))

    if PENDING.exists():
        shutil.rmtree(PENDING)
    rec = PENDING / "data/sexmod/recipe"
    rec.mkdir(parents=True)
    for f in sorted((OLD / "recipes").glob("*.json")):
        # 26.3 recipe ids are lowercase snake_case
        name = "".join("_" + c.lower() if c.isupper() else c for c in f.stem)
        convert_recipe(f, rec / (name + ".json"))
    report["pending recipes"] = len(list(rec.iterdir()))
    report["pending blockstates"] = copy_tree(OLD / "blockstates", PENDING / "assets/sexmod/blockstates")

    # sounds.json: 1.12 used bare subtitle keys ("ahh") with no translation, so the raw key was
    # shown. Keep that text, but as a real translation key so 26.3 doesn't log 719 errors.
    sounds = load_lenient_json(OLD / "sounds.json")
    subtitles = {}
    for ev in sounds.values():
        sub = ev.get("subtitle")
        if sub and "." not in sub:
            key = "subtitles.sexmod." + sub
            subtitles[key] = sub
            ev["subtitle"] = key
    (ASSETS / "sounds.json").write_text(json.dumps(sounds, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    en_us = ASSETS / "lang/en_us.json"
    lang = json.loads(en_us.read_text(encoding="utf-8"))
    for key, text in subtitles.items():
        lang.setdefault(key, text)
    en_us.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    report["subtitle keys"] = len(subtitles)

    # sounds.json must only point at files that were imported
    missing = [s["name"] for ev in sounds.values() for s in ev["sounds"]
               if not (ASSETS / "sounds" / (s["name"].split(":", 1)[1] + ".ogg")).is_file()]
    report["sound events"] = len(sounds)
    report["sounds.json missing files"] = len(missing)
    for m in missing[:10]:
        print("  missing:", m)

    report["sound groups (ModSounds)"] = generate_mod_sounds(sounds)

    for k, v in report.items():
        print(f"{k:28} {v}")


SOUNDS_JAVA = ROOT / "src/main/java/com/trolmastercard/sexmod/sound/ModSounds.java"


def generate_mod_sounds(sounds: dict) -> int:
    """Writes ModSounds.java: one SoundGroup constant per numbered sound family.

    Event ids look like ``girls.jenny.ahh.ahh3``: group ``girls.jenny.ahh``, stem ``ahh``,
    variant 3. Only variants that exist in sounds.json are registered (1.12.2's hand-sized
    arrays disagreed with the files in a few groups, e.g. kobold lightbreathing has no 8).
    """
    groups = {}
    for event_id in sounds:
        group, _, last = event_id.rpartition(".")
        stem = last.rstrip("0123456789")
        index = last[len(stem):]
        if not group or not stem or not index:
            raise SystemExit(f"sound event doesn't follow <group>.<stem><n>: {event_id}")
        g = groups.setdefault(group, {"stem": stem, "indices": set()})
        if g["stem"] != stem:
            raise SystemExit(f"mixed stems in sound group {group}: {g['stem']} / {stem}")
        g["indices"].add(int(index))
    lines = []
    for name in sorted(groups):
        g = groups[name]
        indices = sorted(g["indices"])
        const = name.upper().replace(".", "_")
        if indices == list(range(len(indices))):
            lines.append(f'    public static final SoundGroup {const} = group("{name}", "{g["stem"]}", {len(indices)});')
        else:
            listed = ", ".join(map(str, indices))
            lines.append(f'    public static final SoundGroup {const} = group("{name}", "{g["stem"]}", new int[] {{{listed}}});')
    SOUNDS_JAVA.parent.mkdir(parents=True, exist_ok=True)
    SOUNDS_JAVA.write_text(f'''package com.trolmastercard.sexmod.sound;

import com.trolmastercard.sexmod.Fapcraft;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Every sound event in {{@code assets/sexmod/sounds.json}}, grouped into numbered variants.
 * <p>
 * <b>Generated by {{@code tools/import_assets.py}} — do not edit.</b> Re-run the script after
 * changing sounds.json.
 */
public final class ModSounds {{
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Fapcraft.MODID);
    private static final Map<String, SoundGroup> BY_NAME = new LinkedHashMap<>();

{chr(10).join(lines)}

    private ModSounds() {{
    }}

    /** Every group by name (e.g. {{@code "girls.jenny.ahh"}}). */
    public static Map<String, SoundGroup> all() {{
        return Collections.unmodifiableMap(BY_NAME);
    }}

    private static SoundGroup group(String name, String stem, int count) {{
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) indices[i] = i;
        return group(name, stem, indices);
    }}

    private static SoundGroup group(String name, String stem, int[] indices) {{
        List<Holder<SoundEvent>> variants = new ArrayList<>(indices.length);
        for (int i : indices) {{
            String id = name + "." + stem + i;
            variants.add(SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Fapcraft.id(id))));
        }}
        SoundGroup group = new SoundGroup(name, indices.clone(), List.copyOf(variants));
        BY_NAME.put(name, group);
        return group;
    }}
}}
''', encoding="utf-8")
    return len(groups)


if __name__ == "__main__":
    main()
