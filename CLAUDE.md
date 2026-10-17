# Fapcraft 26.3

Ground-up rebuild of the 1.12.2 Fapcraft mod for Minecraft 26.3. **[PLAN.md](PLAN.md) is the
source of truth** for phases, the feature inventory and the 1.12 → 26.3 API mapping.

## Stack (pinned in `gradle.properties`, bump only deliberately)
Minecraft 26.3 · NeoForge 26.3.0.86 (ModDevGradle) · GeckoLib 5.5.7 · Java 25

- Build: `JAVA_HOME=~/.jdks/temurin-25.0.4.1 ./gradlew build` · Dev client: `./gradlew runClient`
- Reference code (read-only): `../Jenny-Mod-Fapcraft/src/main/java/com/trolmastercard/sexmod/`

## Scope lock
- **Goal is 1.12.2 parity, nothing more.** No new features, mechanics, content or
  "improvements" until Phase 8 is complete. Note ideas in PLAN.md under a *Later* heading
  instead of building them.
- **Work the phases in order.** Don't start a phase until the previous phase's exit check
  passes. Within a phase, only touch what that phase lists.
- **Port behaviour, not code.** Read the 1.12 class, then write it idiomatically for 26.3.
  Never translate decompiled code line by line (no `a`/`b`/`c` names, label blocks, raw types).
  Code with no Minecraft imports (pack scanner/reader, downloader, maths, cfg parsing) may be
  copied nearly verbatim.
- **No 1.12 world compatibility.** Don't write migration code for old NBT or saves.
- When a feature is done, tick it in PLAN.md's inventory after checking it against 1.12 behaviour.

## Fixed decisions
- Mod id is **`sexmod`** (packs target `assets/sexmod/...`). Package `com.trolmastercard.sexmod`.
- Client-only code lives under `client/` and is reached only from `FapcraftClient` or
  `Dist.CLIENT` paths. A dedicated server must boot after every phase.
- Use NeoForge-native systems: `DeferredRegister`, `CustomPacketPayload` + `StreamCodec`,
  data components, data attachments, codec-based `SavedData`, `ModConfigSpec`.

## 26.3 gotchas
- `ResourceLocation` is now **`Identifier`**. Game code is unobfuscated (no SRG/MCP).
- GeckoLib 5 adds `GeoRenderState` to vanilla render states; `interfaces/geckolib.json` (copied from
  its jar) makes that visible to javac. Re-copy it when bumping GeckoLib.
- GeckoLib 5 loads models from `assets/sexmod/geckolib/models/` and animations from
  `assets/sexmod/geckolib/animations/` (not `geo/` / `animations/`). Load errors are logged
  as `Error loading resource:` at client resource reload; animation bake failures print as bare
  `Unable to bake ActorAnimation` exception lines (no log prefix): grep for both.
- Config types: `ModConfig.Type.SYNCED` (was SERVER; per-world override in `syncedconfig/`), `CLIENT`.
- Lang files are JSON; registry keys are `entity.sexmod.<id>`, `item.sexmod.<id>`, etc.
- Data files live under `data/sexmod/` with singular folders: `loot_table/`, `recipe/`,
  `structure/`.

## Assets
- Imported from 1.12.2 by `python3 -I tools/import_assets.py ../Jenny-Mod-Fapcraft` (re-runnable;
  converts lang/loot/recipes, generates `sound/ModSounds.java`). Never hand-edit its outputs: fix
  the script, or add new strings to `tools/lang_extra/<lang>.json`.
- `pending/` holds converted data whose item/block isn't registered yet (recipes, blockstates).
  Move a file into `src/main/resources` in the same change that registers its owner.
