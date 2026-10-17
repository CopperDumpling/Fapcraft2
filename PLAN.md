# Fapcraft 26.3 rebuild plan

Ground-up rebuild of the 1.12.2 mod (`../Jenny-Mod-Fapcraft`, ~315 files / ~65k lines,
decompiled) on **Minecraft 26.3 / NeoForge 26.3.0.86 / GeckoLib 5.5.7 / Java 25**.

The 1.12.2 code is the *behavioural reference*, not something to translate line by line:
read what a class does, then write it the 26.3 way. Logic that never touches Minecraft is
copied over nearly verbatim.

---

## Ground rules

- **Mod id stays `sexmod`** so every `sexmod:` resource location, and every community
  reskin pack (`assets/sexmod/geo|textures|animations/...`), keeps resolving.
- **No 1.12 world compatibility.** Modded entities and NBT from 1.12 worlds will not
  survive the upgrade; 26.3 starts from new worlds. This frees the save format.
- **One vertical slice before breadth.** Jenny goes end to end (spawn → render → animate →
  interact → scene → save) before any other girl is started.
- **Server-authoritative, client-safe.** Every client class lives under `client/` and is
  only reached from `FapcraftClient` or `Dist.CLIENT` code. Dedicated-server boot is a
  check at the end of every phase.
- **Feature inventory is the checklist.** Each phase ends by ticking its rows in the
  inventory below. Nothing is "done" until it is compared against the 1.12.2 behaviour.
- The 1.12.2 repo stays maintained on its own branch as the playable release and the
  reference while this one is incomplete.

---

## API mapping (1.12.2 → 26.3)

| Concern | 1.12.2 | 26.3 |
|---|---|---|
| Registration | `RegistryEvent.Register`, `GameRegistry` | `DeferredRegister` (`ENTITY_TYPES`, `ITEMS`, `BLOCKS`, `SOUND_EVENTS`, `MOB_EFFECTS`, `ATTACHMENT_TYPES`, `MENU_TYPES`, `DATA_COMPONENT_TYPES`) |
| Entity sync | `DataParameter` / `EntityDataManager` | `SynchedEntityData` + `EntityDataAccessor` |
| Entity save | `writeEntityToNBT` / NBT | `addAdditionalSaveData` with `ValueOutput`/`ValueInput` + codecs |
| AI | `EntityAITasks` | `GoalSelector` goals (simple mobs) or `Brain` (complex girls) |
| Item data | item NBT | Data components (`DataComponentType` + codec + stream codec) |
| Per-player/world data | `WorldSavedData`, capabilities | `SavedData` with codec; NeoForge data attachments for per-player/per-entity state |
| Networking | `SimpleNetworkWrapper` + `IMessage` (49 packets) | `CustomPacketPayload` records + `StreamCodec`, registered in `RegisterPayloadHandlersEvent` |
| Rendering | `GlStateManager`, `Tessellator`, `RenderLiving` | `EntityRenderer` + render states, `PoseStack`, `MultiBufferSource`, `RenderType`/render pipelines |
| Animation | GeckoLib 3 (`IAnimatable`, `AnimationController`, `GeoEntityRenderer`) | GeckoLib 5 (`GeoEntity`, `AnimatableInstanceCache`, controllers + `RawAnimation`, `GeoEntityRenderer` with render-state data tickets, `GeoRenderLayer`) |
| GUI | `GuiScreen`, `drawRect`, `GuiContainer` | `Screen`, `GuiGraphics`, `AbstractContainerScreen` + `AbstractContainerMenu` |
| HUD | `RenderGameOverlayEvent` | `RegisterGuiLayersEvent` |
| Keybinds | `KeyBinding` + `ClientRegistry` | `KeyMapping` in `RegisterKeyMappingsEvent` |
| Commands | `CommandBase` | Brigadier in `RegisterCommandsEvent` |
| Potion | `Potion` | `MobEffect` (`DeferredRegister.MOB_EFFECTS`) |
| Worldgen | `IWorldGenerator` + `.nbt` templates | Data-driven structures (`data/sexmod/worldgen/structure`, `structure_set`, template pools) + the same `.nbt` templates |
| Lang | `.lang` | `.json` (15 languages to convert) |
| Loot / recipes | `assets/.../loot_tables`, `recipes` | `data/sexmod/loot_table`, `data/sexmod/recipe` (new JSON format) |
| Item models | `models/item` | `assets/sexmod/items/*.json` item model definitions + `models/item` |
| Config | `config/sexmod.json` parsed by offset | `ModConfigSpec`, types `CLIENT` / `SYNCED` (was SERVER) |
| Obfuscation | MCP/SRG + custom MCRepack | None: 26.x is unobfuscated |

---

## Feature inventory (from the 1.12.2 source)

### Characters
Each NPC girl has a "player" variant (the player transformed into that girl), a GeckoLib
model + NPC model, a renderer, and scene animations.

| Character | NPC | Player variant | Notes |
|---|---|---|---|
| Jenny | `JennyEntity` | `JennyPlayerEntity` | Vertical-slice target |
| Ellie | `EllieEntity` | `ElliePlayerEntity` | |
| Bia | `BiaEntity` | `BiaPlayerEntity` | |
| Luna | `LunaEntity` | `LunaPlayerEntity` | Cat model; fishing (`CatActivateFishingPacket`, eating / throw-away packets), `LunaRodItem` |
| Allie | `AllieEntity` | `AlliePlayerEntity` | Summoned via `AlliesLampItem`, wishes (`MakeRichWishPacket`), `AllieWorldData` |
| Bee | `BeeEntity` | `BeePlayerEntity` | Chest opening, dialogue screen, `BeeWorldData` |
| Slime | `SlimeEntity`, `WildSlimeEntity` | `SlimePlayerEntity` | Wild variant with face layer |
| Kobold | `KoboldEntity` (3.2k lines) | `KoboldPlayerEntity` | Tribes (`KoboldManager`, `TribeState`, claim/follow-mode/UI packets, tribe name screen), eggs (`KoboldEggItem`, `TribeEggItem`, egg entity + projectile), mining / tree-felling tasks, `DragonStaffItem` |
| Goblin | `GoblinEntity` (2.2k) | `GoblinPlayerEntity` | Shoulder riding, throw/pickup gesture pad, first-person renderer, goblin lair (`CommandLocateGoblinLair`) |
| Galath | `GalathEntity` (3.2k) | `GalathPlayerEntity` | Flight (`GalathFlightData`, flight HUD), pounce / back-off, energy-ball particles, coin item, custom damage source |
| Manglelie | `ManglelieEntity` | - | |
| Dragon | `DragonEntity` | - | Breath particle |
| Cummy | `CummyEntity` | - | Effect entity |

Shared base: `BaseGirlEntity` (1.8k lines), `AbstractGirlNpcEntity`, `AbstractPlayerGirlEntity`,
`AbstractNpcOnlyEntity`, `BasicGirlEntity`, `BodyParts`, `BoneType`, `NpcType`, eye/hair/skin
colours, kobold names.

### Systems
- **Scenes**: `SexEntity` / `SexEntityPart` / `SexSceneEntity`, scene renderer + model, player
  camera / movement / velocity / teleport packets, `Action` state machine, "Leave scene" key
  (R-Shift) with per-girl reset, escape minigame HUD, horny meter HUD, `HornyPotion` effect.
- **Girl ownership & AI**: follow / goto / avoid / door / watch / nearest-attackable goals,
  companion home (`SetNewHomePacket`, `SendCompanionHomePacket`), ownership packets,
  combat protection, death handling, bed interaction, name tags.
- **Girl inventory & equipment**: two inventory containers + GUIs, equipment sync, upload
  inventory packets, chest container.
- **Customisation**: clothing screen, body sliders, gender swap (`CommandFuta`, screen), girl
  DNA/data packets, model code (`CommandSetModelCode`, `UploadModelStringPacket`).
- **Custom models (legacy)**: `ServerWhitelistManager` (`.cfg` + `.png` + `.geo.json`, zip
  models), server → client model download, `CommandWhitelistServer`, `CommandReloadCustomModels`.
- **Reskin packs**: scanner (zip central dir + RAR headers, any depth), reader, applier
  (geo/texture/animation overrides), stack browser, per-world persistence, async apply with
  progress bars, GitHub archive downloader, no-models prompt.
- **World data**: `GirlSavedData`, `GirlWorldData`, `AllieWorldData`, `BeeWorldData`, kobold
  tribes, `PlayerIds`.
- **Worldgen**: girl houses from `structures/*.nbt` (alex, bia, ellie, goblin, jenny, luna, ssa)
  via `GirlHouseGenerator` + config.
- **Items / blocks**: Allie's lamp, dragon staff, Galath coin, kobold egg, tribe egg, Luna rod,
  NPC editor wand, Winchester; `SexFireBlock`.
- **Client misc**: keybinds (goblin interact `G`, customisation `K`, leave scene `R-Shift`),
  ribbon / trail renderers, structure markers, in-hand map renderer, debug windows, update
  checker, skin fetcher.
- **Assets**: geo (15 MB), animations (6 MB), textures (14 MB), sounds (21 MB, `sounds.json`
  carries over), models, blockstates, loot tables, recipes, 15 lang files.

---

## Phases

Each phase lists its exit check. Rough sizes assume one developer and are for ordering,
not promises.

### Phase 0: Scaffold (done)
NeoForge 26.3 ModDevGradle project, GeckoLib 5.5.7 + unrar5j (jar-in-jar), mod id `sexmod`,
Java 25. **Exit:** `./gradlew build` succeeds.

### Phase 1: Foundations (small) (done, pending in-game /playsound check)
- Registries skeleton: `ModEntities`, `ModItems`, `ModBlocks`, `ModSounds`, `ModEffects`,
  `ModAttachments`, `ModMenus`, `ModComponents`, `ModPayloads`.
- Copy assets: geo, animations, textures, sounds + `sounds.json`; convert `.lang` → `.json`
  (script); move loot tables / recipes to `data/` in the new format.
- Port the MC-free code verbatim: `PackDownloader`, `ReskinScanner`, `ReskinReader`, `ReskinEntry`,
  `UpdateChecker`, cfg parsing from `ModelData`, maths helpers (`VectorMath`, `TrigMath`, ...).
- `ModConfigSpec` for client + server options.
- **Exit:** client and dedicated server boot; sounds play via `/playsound sexmod:...`.

**Results**
- Assets imported by `tools/import_assets.py`: 26 geo models, 16 animation files, 82 textures,
  719 sounds, 15 lang files, 4 loot tables, 7 structures; recipes + fire blockstate parked in `pending/`.
- GeckoLib 5 loads every 1.12 **model**. *Correction found in Phase 2:* 69 **animations** failed
  to bake. The errors print as bare exception text, which the Phase 1 log check missed. Causes
  and fixes:
  - junk GeckoLib 3 silently read as 0 (easing names pasted into values, `nan`, lone `.`):
    the importer rewrites it (953 values);
  - runtime variables 1.12 set (`size`, `pitch`, `yaw`, `armpitch`, `armyaw`): registered in
    `MolangVariables`, driven per girl when she is ported;
  - **still failing (6, Phase 5):** Kobold `blowjobSlowL/R`, `blowjobFast`, `blowjobCum`,
    `paymentBackpack` (variable `scale`, and expressions ending in an operator like
    `-36.4659+size*`) and Goblin `nelson_slow2` (`0-1*`). 1.12's own log had a "MolangParser
    IndexOutOfBounds" at startup, so these may have been broken there too: check in game first.
- `ModSounds` is generated from sounds.json: 112 groups. 1.12's hand-sized arrays disagreed with
  the files (e.g. `jenny.hmph` expected 5 variants, 4 exist; `kobold.lightbreathing` has no 8), so
  some random picks were silent; now only real variants are registered.
- 1.12 leniency fixed in the importer: a trailing comma in `stick.json`; 58 untranslated subtitle
  keys moved to `subtitles.sexmod.*` with the same English text.
- Config: `ServerConfig` (SYNCED: `generateBuildings`, `allowFlying`) and `ClientConfig`
  (`loadOtherSkins`), with the NeoForge config screen.
- Ported MC-free code: `PackDownloader`, `ReskinScanner`, `ReskinReader`, `ReskinEntry`,
  `UpdateChecker`; `VectorMath` keeps only the MC-convention rotations (the rest maps to vanilla
  `Vec3`/`Mth`, documented in the class). The trivial `Vector2d/2f/4d`, `Point2D`, `Rectangle`
  were not ported: use JOML / vanilla types.

### Phase 2: Vertical slice: Jenny (large)
- `BaseGirlEntity` redesign: synced data, codec-based save, attachments for ownership.
- `JennyEntity` with GeckoLib 5 (`GeoEntity`, controllers, idle/walk animations from the
  existing `animations/*.json`).
- `GirlRendererBase` / `JennyRenderer` as a `GeoEntityRenderer` with render state.
- Spawn egg, natural spawn rules, basic follow/goto goals.
- One interaction → one scene end to end: prompt payload, `SexSceneEntity`, camera/movement
  control, natural end + R-Shift leave with full reset.
- Save/load round trip on a dedicated server.
- **Exit:** Jenny spawns, walks, animates, plays one full scene in singleplayer and on a
  dedicated server, survives world reload.

**Design (decided at the start of Phase 2)**
- **The slice scene is the blowjob, with payment:** right-click → action menu → pay
  (3 emeralds) → `PAYMENT` → `STARTBLOWJOB` → `SUCKBLOWJOB` ⇄ `THRUSTBLOWJOB` (sneak) →
  `CUMBLOWJOB` (jump with a full meter) → reset. It needs no bed and no strip. Jenny's other
  actions (boobjob, doggy, strip/dress, combat, pearl home, horny-potion bypass) are Phase 3.
- **Scene steps stay driven by animation keyframe events, but the server validates them.**
  In 1.12, events in the animation JSON (`bjiDone`, `paymentMSG2`, ...) fire on the client,
  which advances the step. A dedicated server has no animation files, so it can't time scenes
  itself. Now only the scene partner's client may send `SceneStepPayload(girl, from, to)`, and the
  server applies it only if `to` is a legal successor of `from` in that girl's step graph. Every
  nearby client plays the step's sounds and chat locally, as before.
- **The server is authoritative for everything else.** 1.12 trusted the client with item
  removal (`RemoveItemsPacket` took any player UUID), teleports (`TeleportPlayerPacket`) and
  arbitrary entity data (`ChangeDataParameterPacket`). Now prices are a server-side table,
  the server positions the player, and the client only sends the girl's entity id plus an
  intent.
- **Step timing is per entity.** 1.12's `Action` enum kept a mutable `ticksPlaying` array on
  each constant, shared by every girl at once (a bug). The step's start tick is stored on the girl.
- **Minimal scene HUD in this phase.** The horny meter decides when the scene can end, so a
  basic meter layer ships now; Phase 3 brings it to parity.

### Phase 3: Shared girl framework + Jenny parity (medium)
Extract what Jenny proved into reusable pieces and finish Jenny against 1.12.2. Split into
steps so each one can be played before the next starts.

**3a: strip, boobjob, horny potion, transition (built, awaiting in-game check)**
- [x] Strip (paid) and dress up (free, no scene lock). The outfit toggle and the end are timed
  on the server (3 s / 5.04 s into the animation), because dress up has no partner client to
  report keyframes.
- [x] Boobjob: start → slow ⇄ fast (sneak) → climax; partner poses; boobjob/doggy strip her first
  if she is dressed (`SceneOption.needsNude`, `followUp`).
- [x] Menu lists only what fits her outfit (`SceneOption.Outfit`); payment line per option.
- [x] Horny potion as a `MobEffect` + `Potion`; brewing is data-driven in 26.3
  (`data/sexmod/recipe/brewing`: mundane + poppy, + gunpowder, + dragon's breath). On her it waives
  payment and skips the payment animation. Villager/animal reactions and the player
  transformation are Phase 6 / 4.
- [x] Black-screen transition overlay (`bjcBlackScreen`).
- Not ported, deliberately: `drawItems`/`removeItems` (1.12.2 never handled them, so it never drew
  items) and the `*Ready` events.

**Animation fix found in 3a/3b testing:** GeckoLib 5 extrapolates a channel backwards before its
first keyframe (GeckoLib 3 held the first value), so bones whose first key is late spun wildly
and then settled. The importer now pins those 1583 channels at t=0 (`hold_first_keyframes`).

**3b: doggy + bed (built, awaiting in-game check)**
- [x] Doggy: payment → (strip) → she walks to the nearest bed (22 blocks, ±3) and stands on its
  free side (`BedFinder`, 1.12 tables) → `STARTDOGGY` → `WAITDOGGY` → the ordering player walks into
  her → `DOGGYSTART` → `DOGGYSLOW` ⇄ `DOGGYFAST` (sneak; hard variant if jump is held at
  `doggyfastReady`) → `DOGGYCUM`.
- Changed from 1.12: the bed is checked *before* payment is taken (1.12 took it, then refused);
  she gets teleported to the bed if she can't reach it in 10 s; the wait ends after 5 minutes;
  her bed-time lines and timing are server-side because no partner client is in the scene yet.

**3c: ownership, home, combat (built, awaiting in-game check)**
- [x] Follow me / stop following me / go home / set new home on the menu (`CompanionOrderPayload`;
  the server checks range, busy and that only her master gives orders). Her master is synced
  data, not an attachment: 1.12.2 never saved it, and she stops following when the master leaves
  the level. Her home (spawn point until set) is saved.
- [x] `FollowMasterGoal` (1.12 `GirlFollowGoal`): follow and match the master's pace, run/walk
  animations, teleport back when out of path range, wander near the master, ride along (boat
  passenger → `SIT`; behind the master on a horse → `RIDE`), fight attackers of either of them and
  nearby monsters (not creepers): sword up close with the weapon's damage, enchantments and
  sweeping; bow beyond 6 blocks.
- [x] Go home: she faces home, throws an ender pearl (19 ticks in), and arrives at the landing point
  if it is within 5 blocks of home, otherwise at home (58 ticks in).
- [x] Health: regeneration every 4 s (faster with a master and no monsters nearby, with hearts);
  with a master, lethal damage downs her at 1 HP (`DOWNED`) until she is fully healed; no damage
  during scenes; a scene partner standing at her is protected too. Death drops her weapon, bow and
  armour slots; Jenny's death sigh, no hurt sound.
- [x] Held weapon/bow drawn on the `weapon` bone with 1.12's per-girl angles (`HeldWeaponLayer`).
  GeckoLib 3 and 5 bone spaces may differ: check the grip in game.
- Changed from 1.12: the attack animation's end and the pearl's throw/arrival are timed on the
  server (a dedicated server has no animations; 1.12 relied on every client reporting
  `attackDone`, and each client cycled its own attack animation, now synced); vanilla 26.3 pearls
  teleport any owner, so hers are caught in `ProjectileImpactEvent`; a downed or homeward girl
  says she's busy instead of opening the menu; exactly-lethal damage downs her too (1.12: only
  overkill); the void and `/kill` still kill a mastered girl (1.12 cancelled her death, leaving her
  at 0 HP); death drops the real stacks (1.12 dropped one plain item per slot); the new home is the
  centre of her block (1.12: its corner).
- Kept from 1.12: the bow is held but never visibly drawn (1.12 set and cleared `BOW` in the same
  tick, so no client ever saw it); every weapon swings every 3 ticks (attack speed clamped to 0.5).
- Not ported: `GirlAiBase` (dead code), `InformOfOwnershipPacket` (Galath, Phase 5). The equipment
  button belongs to 3d.

**3d: inventory + equipment.** Two containers, menu, screen, equipment sync.

**3e: HUDs and screens.** 1.12 menu layout and meter artwork (`hornymeter.png`,
`girlinventory.png`, `buttons.png`), escape minigame HUD.

**Exit:** Jenny is fully at 1.12.2 parity on the checklist.

### Phase 4: Remaining NPC girls (large)
In order of independence: Ellie, Bia, Slime (+ wild slime), Manglelie, Luna (fishing), Bee
(chest, dialogue, world data), Allie (lamp, wishes, world data). Each one is ticked off on the
checklist against 1.12.2 behaviour before the next starts.
**Exit:** all simple girls at parity.

### Phase 5: Complex girls (very large)
- **Kobold**: tribes as `SavedData`, eggs, tribe UI, mining / tree-felling tasks (likely `Brain`),
  dragon staff + dragon.
- **Goblin**: shoulder riding (passenger + first-person render), gesture pad, lair structure +
  locate command.
- **Galath**: flight physics + HUD, pounce / back-off, particles, coin, damage source.
**Exit:** each at parity; dedicated-server boot.

### Phase 6: Player-as-girl (large)
The `*PlayerEntity` variants: render the player as a girl (`RenderPlayerEvent` / renderer
swap), sync player model state via attachments, transformation flow, gender swap screen +
command, per-girl player renderers.
**Exit:** every player variant renders and plays its scenes in multiplayer.

### Phase 7: Customisation, custom models, reskins (medium)
Clothing screen + body sliders on `GuiGraphics`; legacy custom-model loader rebuilt on
GeckoLib 5 bone hooks (bones to hide, custom-bone parenting); server whitelist + model
download as payloads; reskin applier against GeckoLib 5's model/animation caches (resource
reload listener instead of cache poking; packs still use the 1.12 layout `assets/sexmod/geo|animations/`,
so map those to GeckoLib 5's `geckolib/models|animations/`); pack browser, async apply + progress bars, GitHub
download prompt, per-world persistence.
**Exit:** the existing custom-models archive installs and applies.

### Phase 8: World + polish (medium)
Girl houses as data-driven structures from the existing `.nbt` templates; commands on
Brigadier; remaining items (Winchester, wand); debug tooling; update checker against a 26.3
release stream; translations; README / docs.
**Exit:** full checklist ticked; multiplayer test with 2+ clients on a dedicated server.

---

## Risks

| Risk | Mitigation |
|---|---|
| GeckoLib 3 → 5 animation/molang differences | Phase 2: everything bakes except 6 Kobold/Goblin animations (see Phase 1 results). Check client logs for `Unable to bake` after every asset change |
| Behaviour hidden in decompiled code (scene timing, edge cases) | Inventory checklist; side-by-side testing against the 1.12.2 build |
| Reskin packs poke GeckoLib caches directly | Rebuild on GeckoLib 5's reload hooks; test with the archive early in Phase 7 |
| Player-as-girl rendering is the most engine-coupled feature | Prototype the renderer swap during Phase 3, before committing to a design |
| 26.3 / GeckoLib moving targets | Pin versions in `gradle.properties`; bump deliberately between phases |

---

## Later (out of scope until Phase 8 is done)
Ideas noted during the rebuild. Nothing here gets built before parity.
