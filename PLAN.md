# Fapcraft 26.3 rebuild plan

Rebuild of the 1.12.2 mod (`../Jenny-Mod-Fapcraft`, ~315 files / ~65k lines, decompiled) on
**Minecraft 26.3 / NeoForge 26.3.0.86 / GeckoLib 5.5.7 / Java 25**. The 1.12.2 code is the
behavioural reference; MC-free logic is copied nearly verbatim.

## Ground rules
- **Mod id stays `sexmod`**, so `sexmod:` resources and community reskin packs keep resolving.
- **No 1.12 world compatibility**; the save format is free.
- **Vertical slice first:** Jenny end to end before any other girl.
- **Server-authoritative, client-safe:** client code under `client/`, reached only from
  `FapcraftClient` / `Dist.CLIENT`. Dedicated-server boot is checked after every phase.
- **The inventory below is the checklist.** Nothing is done until compared against 1.12.2.
- The 1.12.2 repo stays on its own branch as the playable release and reference.

## API mapping (1.12.2 → 26.3)

| Concern | 1.12.2 | 26.3 |
|---|---|---|
| Registration | `RegistryEvent.Register`, `GameRegistry` | `DeferredRegister` |
| Entity sync / save | `DataParameter`, NBT | `SynchedEntityData`; `ValueOutput`/`ValueInput` + codecs |
| AI | `EntityAITasks` | `Goal`s (simple mobs) or `Brain` (complex girls) |
| Item data | item NBT | Data components |
| World / player data | `WorldSavedData`, capabilities | Codec `SavedData`; data attachments |
| Networking | `SimpleNetworkWrapper` (49 packets) | `CustomPacketPayload` + `StreamCodec` |
| Rendering | `GlStateManager`, `RenderLiving` | `EntityRenderer` + render states, `PoseStack`, `RenderType` |
| Animation | GeckoLib 3 | GeckoLib 5 (`GeoEntity`, `RawAnimation`, render-state data tickets, `GeoRenderLayer`) |
| GUI / HUD | `GuiScreen`, `RenderGameOverlayEvent` | `Screen` + `GuiGraphics`, menus; `RegisterGuiLayersEvent` |
| Keybinds / commands | `KeyBinding`; `CommandBase` | `RegisterKeyMappingsEvent`; Brigadier |
| Potion | `Potion` | `MobEffect` |
| Worldgen | `IWorldGenerator` + `.nbt` | Data-driven structures + same `.nbt` templates |
| Lang / loot / recipes / item models | `.lang`, `assets/` | `.json`, `data/sexmod/`, `assets/sexmod/items/` |
| Config | `config/sexmod.json` | `ModConfigSpec` (`CLIENT` / `SYNCED`) |

## Feature inventory (from 1.12.2)

### Characters
Each NPC girl has a player variant, a GeckoLib model, a renderer and scene animations.

| Character | NPC | Player variant | Notes |
|---|---|---|---|
| Jenny | `JennyEntity` | `JennyPlayerEntity` | Vertical slice |
| Ellie | `EllieEntity` | `ElliePlayerEntity` | |
| Bia | `BiaEntity` | `BiaPlayerEntity` | |
| Luna | `LunaEntity` | `LunaPlayerEntity` | Cat model; fishing + eat/throw packets, `LunaRodItem` |
| Allie | `AllieEntity` | `AlliePlayerEntity` | `AlliesLampItem`, wishes, `AllieWorldData` |
| Bee | `BeeEntity` | `BeePlayerEntity` | Chest opening, dialogue screen, `BeeWorldData` |
| Slime | `SlimeEntity`, `WildSlimeEntity` | `SlimePlayerEntity` | Wild variant has a face layer |
| Kobold | `KoboldEntity` (3.2k lines) | `KoboldPlayerEntity` | Tribes (manager, claim/follow/UI packets, name screen), eggs + projectile, mining / tree felling, `DragonStaffItem` |
| Goblin | `GoblinEntity` (2.2k) | `GoblinPlayerEntity` | Shoulder riding, throw/pickup pad, first-person renderer, lair + locate command |
| Galath | `GalathEntity` (3.2k) | `GalathPlayerEntity` | Flight + HUD, pounce / back-off, energy-ball particles, coin, damage source |
| Manglelie | `ManglelieEntity` | - | |
| Dragon | `DragonEntity` | - | Breath particle |
| Cummy | `CummyEntity` | - | Effect entity |
| Pyrocinical | `BasicGirlEntity` | - | Rare ambient Nether wanderer; player hits stop it and remove it 6.25 s later |

Shared base: `BaseGirlEntity` (1.8k lines), `AbstractGirlNpcEntity`, `AbstractPlayerGirlEntity`,
`AbstractNpcOnlyEntity`, `BasicGirlEntity`, `BodyParts`, `BoneType`, `NpcType`, colours, kobold names.

### Systems
- **Scenes:** scene entities, renderer + model, player camera/movement/teleport, `Action` state
  machine, leave key (R-Shift) with per-girl reset, escape minigame HUD, horny meter HUD, `HornyPotion`.
- **Ownership & AI:** follow / goto / avoid / door / watch / attack goals, companion home,
  ownership, combat protection, death, beds, name tags.
- **Inventory & equipment:** two containers + GUIs, equipment sync, chest container.
- **Customisation:** clothing screen, body sliders, gender swap (`CommandFuta` + screen), girl
  DNA, model code (`CommandSetModelCode`, `UploadModelStringPacket`).
- **Custom models (legacy):** `ServerWhitelistManager` (`.cfg`/`.png`/`.geo.json`, zips),
  server → client download, whitelist + reload commands.
- **Reskin packs:** scanner (zip + RAR, any depth), reader, applier (geo/texture/animation),
  stack browser, per-world persistence, async apply + progress bars, GitHub downloader, no-models prompt.
- **World data:** `GirlSavedData`, `GirlWorldData`, `AllieWorldData`, `BeeWorldData`, kobold tribes, `PlayerIds`.
- **Worldgen:** girl houses (alex, bia, ellie, goblin, jenny, luna, ssa) + config.
- **Items / blocks:** Allie's lamp, dragon staff, Galath coin, kobold egg, tribe egg, Luna rod,
  NPC editor wand, Winchester; `SexFireBlock`.
- **Client misc:** keybinds (`G` goblin, `K` customisation, R-Shift leave), ribbon/trail
  renderers, structure markers, in-hand map, debug windows, update checker, skin fetcher.

## Phases

### Phase 0: Scaffold (done)
ModDevGradle project, GeckoLib + unrar5j (jar-in-jar). **Exit:** `./gradlew build` succeeds.

### Phase 1: Foundations (done)
Registries skeleton, asset import, MC-free code, configs. **Exit:** client and dedicated server
boot; `/playsound sexmod:...` works.

- `tools/import_assets.py` imports 26 models, 16 animation files, 82 textures, 719 sounds,
  15 langs, 4 loot tables, 7 structures; recipes + fire blockstate wait in `pending/`.
- Animation fixes in the importer: GeckoLib 3 junk values read as 0 (953 rewritten); runtime
  variables (`size`, `pitch`, `yaw`, `armpitch`, `armyaw`) registered in `MolangVariables`.
  **Still failing (Phase 5):** Kobold `blowjobSlowL/R`, `blowjobFast`, `blowjobCum`,
  `paymentBackpack` and Goblin `nelson_slow2` (variable `scale`, expressions ending in an
  operator). Possibly broken in 1.12 too: check in game first.
- `ModSounds` (112 groups) is generated from `sounds.json`, so only real variants exist
  (1.12's hand-sized arrays made some picks silent).
- Importer fixes 1.12 leniency: trailing comma in `stick.json`; 58 subtitle keys moved to `subtitles.sexmod.*`.
- Config: `ServerConfig` (SYNCED: `generateBuildings`, `allowFlying`), `ClientConfig` (`loadOtherSkins`).
- Ported: `PackDownloader`, `ReskinScanner/Reader/Entry`, `UpdateChecker`, `VectorMath` (MC-convention
  rotations only). Trivial vector/geometry types replaced by JOML / vanilla.

### Phase 2: Vertical slice: Jenny
Base entity, GeckoLib 5 renderer, spawning, goals, one full scene, save/load.
**Exit:** Jenny spawns, walks, animates, plays one scene in singleplayer and on a dedicated
server, survives reload.

Design decisions:
- **Slice scene:** paid blowjob (3 emeralds): `PAYMENT` → `STARTBLOWJOB` → `SUCKBLOWJOB` ⇄
  `THRUSTBLOWJOB` (sneak) → `CUMBLOWJOB` (jump, full meter) → reset.
- **Steps are driven by animation keyframe events, validated by the server.** Only the partner's
  client sends `SceneStepPayload(girl, from, to)`; the server accepts only legal successors. Clients
  play sounds and chat locally.
- **Server-authoritative otherwise:** server-side prices and player positioning; the client sends
  only girl id + intent (1.12 trusted client item removal, teleports and entity data).
- **Step timing is per entity** (1.12 shared a mutable tick array across all girls).
- A basic horny meter HUD ships here; parity in Phase 3.

### Phase 3: Shared girl framework + Jenny parity
**Exit:** Jenny is at 1.12.2 parity on the checklist.

**Animation fix:** GeckoLib 5 extrapolates channels before their first keyframe (GeckoLib 3 held
it); the importer pins 1583 such channels at t=0 (`hold_first_keyframes`).

**3a: strip, boobjob, horny potion, transition (done)**
- [x] Strip (paid), dress up (free, no scene lock); outfit toggle and end timed on the server
  (3 s / 5.04 s), since dress up has no partner client.
- [x] Boobjob: start → slow ⇄ fast (sneak) → climax; boobjob/doggy strip her first if dressed.
- [x] Menu shows only options fitting her outfit, with a payment line each.
- [x] Horny potion (`MobEffect` + data-driven brewing: mundane + poppy, + gunpowder, + dragon's
  breath): waives payment and skips its animation. Villager/animal reactions and transformation
  are Phase 6 / 4.
- [x] Black-screen transition (`bjcBlackScreen`).
- Not ported: `drawItems`/`removeItems` (never handled in 1.12) and `*Ready` events.

**3b: doggy + bed (done)**
- [x] Payment → (strip) → walk to nearest bed (22 blocks, ±3), stand on its free side (`BedFinder`)
  → `STARTDOGGY` → `WAITDOGGY` → player walks into her → `DOGGYSTART` → `DOGGYSLOW` ⇄ `DOGGYFAST`
  (sneak; hard variant if jump held at `doggyfastReady`) → `DOGGYCUM`.
- Changed: bed checked before payment; teleport to the bed after 10 s; wait times out after
  5 min; bed-time lines and timing are server-side.

**3c: ownership, home, combat (done)**
- [x] Follow / stop following / go home / set home (`CompanionOrderPayload`; server checks range,
  busy, master). Master is synced data, not saved; she stops following when the master leaves.
  Home (spawn point until set) is saved.
- [x] `FollowMasterGoal`: match pace, run/walk animations, teleport when out of range, wander,
  ride along (boat → `SIT`, horse → `RIDE`), fight attackers and nearby monsters (not creepers):
  sword up close with damage/enchantments/sweeping, bow beyond 6 blocks.
- [x] Go home: pearl thrown at 19 ticks; arrives at the landing point if within 5 blocks of home,
  else at home (58 ticks).
- [x] Health: regen every 4 s (faster with master and no monsters); with a master, lethal damage
  downs her at 1 HP (`DOWNED`) until healed; no damage in scenes, partner protected. Death drops
  weapon, bow and armour; death sigh, no hurt sound.
- [x] `HeldWeaponLayer` on the `weapon` bone with 1.12 angles.
- Changed: attack end and pearl timing are server-side (and attack animation synced); her pearls
  are caught in `ProjectileImpactEvent`; downed/homeward girl says she's busy; exactly-lethal
  damage downs her; void and `/kill` still kill; death drops real stacks; home is the block centre.
- Kept from 1.12: bow never visibly drawn; weapons swing every 3 ticks.
- Not ported: `GirlAiBase` (dead), `InformOfOwnershipPacket` (Phase 5). Equipment button is 3d.

**3d: equipment (done)**
- [x] Equipment menu from her "equipment" order: weapon (sword or tool), bow and four armour slots
  over the player's inventory, `girlinventory.png` layout, shift-click.
- [x] Worn armour on her dressed model: armour bones shown per part, hiding what they cover;
  material by texture column (diamond and others 0, gold 1, iron/chainmail 2, leather 4 + dye).
- Changed: the server menu edits her inventory directly (1.12 uploaded both inventories from the
  client on close). Luna's rod menu and the 27-slot girl chest belong to Luna/Bee/Kobold (Phases 4–5).

**3e: menu and HUD artwork (done)**
- [x] Action menu in the 1.12 layout: options slide in bottom right with their prices, equipment
  preview and icon order buttons that open on hover on the left.
- [x] Horny meter with `hornymeter.png` / `buttons.png`: slide-in, SHIFT and JUMP prompts, fill
  flies off on climax.

### Phase 4: Remaining NPC girls
Ellie, Bia, Slime (+ wild), Manglelie, Luna (fishing), Bee (chest, dialogue, world data), Allie
(lamp, wishes, world data), the pyrocinical, each at parity before the next. **Exit:** all simple
girls at parity.

**4a: Ellie (done)**
- [x] Basics (done): spawn egg, renderer (her armour bones incl. headband, hotpants, slip),
  weapon grip, crouch walk/idle and lower eyes under a block two above her feet, companion lines,
  dress up (reachable once the potion route has undressed her).
- [x] Carried scene, "Face fuck" (done): walk-in, intro (partner lifted at
  110 ticks, loop at 191), four random slow loops ⇄ fast on sneak, climax on a full meter + jump.
  First-person camera on her `boyCam` bone (client `Camera` mixin).
- [x] Horny-potion route (done): dash at the nearest player within 10,
  hug from behind, cowgirl/missionary menu, walk to the end of a bed, sit and wait with head
  tracking; whoever steps within 1.5 gets the scene (black transition), and she ends nude.
- Changed: she faces the player during the dash (1.12 left out the +90 its other turn code has);
  she is put on the seat if she can't reach it in 10 s (as Jenny at her bed).
- Changed: her chat lines are lang keys (1.12 hard-coded some English); the attack swing sound
  (`attackSound`, missing from Jenny's port too) now plays for every girl.

**4b: Bia (done)**
- [x] Spawn egg, renderer (leaves under a helmet, bra and slip under armour), Jenny's weapon grip,
  free strip / dress up, new-home line, brown wool on death.
- [x] Headpat; talk → anal / doggy menu (closing it ends the talk, as 1.12).
- [x] Anal: walk to the nearest bed's free side, undress and wait; a player within 1 block for
  22 ticks gets start, slow ⇄ fast on sneak, climax.
- [x] Prone doggy: walk to a bed's end, sit and wait; same pickup; intro, sneak to insert (42
  ticks), soft, hard stretches (34 ticks, three random variants) while sneak is held, climax.
  Camera on her `boyCam` bone.
- Changed: the post-talk answer, bed preparation and doggy stretch timers run on the server
  (1.12 drove them from client animation events).

**4c: Slime + wild slime (done)**
- [x] Slime girl: not a companion (no menu, orders, goals, weapons or regeneration); spawns in
  swamps; hops every 100 ticks; progress from random hops (1: undress, 2+: hunts the nearest
  player with hearts) or the potion (straight to 2); touch at 2+ → blowjob, at 4+ she waits on
  all fours → doggy; a finished scene makes her pregnant (2400 ticks → wild slime) and she
  dresses again. Slime puddle bones during doggy; hat follows the head.
- [x] Wild slime (`friendly_slime`): a vanilla cube mob that never attacks, tiny, with a tail;
  sparkles from 5880 ticks, clouds from 7980, grows into a slime girl at 8400; tiny-slime drops.
- Changed: undress and getting down are timed on the server; a second player touching her mid-scene
  no longer hijacks it.

**4d: Manglelie, wild (built, awaiting in-game check)**
- [x] Ambient Nether spawn (weight 5), at least 700 blocks from any earlier Galath or Manglelie
  spawn (`NetherSpawns`, 1.12 "galath_spawn_list"); not a companion; base AI plus fleeing players
  within 20 blocks (scared run); immune to potion effects; skirt bone rules (skirted hips, strips
  following cheeks and legs).
- Moved to Galath (Phase 5): adoption, riding Galath's head, the corruption beam and arrow, the
  threesome, damage forwarded to her Galath, the angry face. They all need a Galath.
- Not ported: 1.12's skirt-strip and chest-cloth meshes (never called) and its wing mesh for her
  (cast her to a Galath interface she lacks).

### Phase 5: Complex girls
- **Kobold:** tribes as `SavedData`, eggs, tribe UI, mining / tree felling (likely `Brain`), dragon staff + dragon.
- **Goblin:** shoulder riding (passenger + first-person render), gesture pad, lair + locate command.
- **Galath:** flight + HUD, pounce / back-off, escape minigame HUD, particles, coin, damage source;
  with Manglelie: adoption, riding Galath's head, the corruption beam + arrow, the threesome,
  Mang's damage forwarded to her Galath, the angry face, Mang not fleeing near a mastered Galath.

**Exit:** each at parity; dedicated server boots.

### Phase 6: Player-as-girl
Render the player as a girl (renderer swap), sync model state via attachments, transformation,
gender swap screen + command. **Exit:** every player variant renders and plays scenes in multiplayer.

### Phase 7: Customisation, custom models, reskins
Clothing screen + sliders; legacy custom models on GeckoLib 5 bone hooks (hidden bones,
custom-bone parenting); whitelist + model download as payloads; reskin applier via a reload
listener (map packs' `geo|animations/` to `geckolib/models|animations/`); pack browser, async
apply + progress, GitHub download prompt, per-world persistence.
**Exit:** the existing custom-models archive installs and applies.

### Phase 8: World + polish
Girl houses as data-driven structures; Brigadier commands; Winchester, wand; debug tooling;
update checker for 26.3 releases; translations; docs.
**Exit:** full checklist ticked; multiplayer test with 2+ clients on a dedicated server.

## Risks

| Risk | Mitigation |
|---|---|
| GeckoLib 3 → 5 animation differences | 6 animations still fail (Phase 1); grep logs for `Unable to bake` after asset changes |
| Behaviour hidden in decompiled code | Checklist; side-by-side testing against 1.12.2 |
| Reskin packs poke GeckoLib caches | Rebuild on reload hooks; test the archive early in Phase 7 |
| Player-as-girl rendering is engine-coupled | Prototype the renderer swap during Phase 3 |
| 26.3 / GeckoLib moving targets | Versions pinned in `gradle.properties`; bump between phases |

## Later (out of scope until Phase 8 is done)
Ideas noted during the rebuild. Nothing here gets built before parity.
- Armour colours beyond 1.12's four: the girl textures have an unused fourth column (offset 3)
  that looks like chainmail; netherite and copper have no column and show as diamond.
