package com.trolmastercard.sexmod.entity.allie;

import com.geckolib.animation.RawAnimation;
import com.trolmastercard.sexmod.client.ClientHooks;
import com.trolmastercard.sexmod.client.scene.AllieSceneEvents;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.PlayerPose;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.entity.SceneOption.Outfit;
import com.trolmastercard.sexmod.item.AlliesLampItem;
import com.trolmastercard.sexmod.registry.ModEntities;
import com.trolmastercard.sexmod.util.ParticleRings;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Allie, the lamp's genie (1.12.2 {@code AllieEntity}). She exists only while summoned
 * ({@link AlliesLampItem}): she floats where she appeared, holds her summoner in place, and is
 * gone once her visit is over, her summoner leaves, or the world is reloaded.
 * <ul>
 *   <li>Summon: her first time a long introduction, later a short "you have N wishes left"; then
 *   the wish menu, which comes back until a wish is picked. On sand she screams and vanishes.</li>
 *   <li>Make me rich: diamonds, emeralds and gold ingots (1–2 each), then she vanishes.</li>
 *   <li>Deepthroat: start, slow ⇄ fast on sneak, climax. Reverse cowgirl: start (the summoner sits
 *   where she floats), slow (three variants) ⇄ fast on sneak (sneak held keeps it going, in three
 *   variants), climax. Both use her {@code boyCam} bone in first person and end her visit.</li>
 * </ul>
 */
public class AllieEntity extends GirlEntity {
    /** Wishes left before the current one is granted: 3 on her first visit (1.12.2 read the lamp's uses). */
    private static final EntityDataAccessor<Integer> DATA_WISHES_LEFT = SynchedEntityData.defineId(AllieEntity.class, EntityDataSerializers.INT);
    /** Which of the three reverse cowgirl fast loops plays (1.12.2 {@code stateCount}). */
    private static final EntityDataAccessor<Integer> DATA_FAST_VARIANT = SynchedEntityData.defineId(AllieEntity.class, EntityDataSerializers.INT);
    /** She is shrinking away (1.12.2 {@code LAMP_SCALE} below 1). */
    private static final EntityDataAccessor<Boolean> DATA_VANISHING = SynchedEntityData.defineId(AllieEntity.class, EntityDataSerializers.BOOLEAN);

    private static final List<SceneOption> OPTIONS = List.of(
        new SceneOption("makemerich", "action.names.makemerichallie", null, 0, GirlAction.RICH_FIRST_TIME, Outfit.ANY, false, false, false, ""),
        new SceneOption("deepthroat", "action.names.deepthroat", null, 0, GirlAction.DEEPTHROAT_START, Outfit.ANY, false, false, false, ""),
        new SceneOption("reverse_cowgirl", "action.names.reversecowgirl", null, 0, GirlAction.REVERSE_COWGIRL_START, Outfit.ANY, false, false, false, ""));

    /** How long she takes to shrink away (1.12.2: 1 % per rendered frame). */
    public static final int VANISH_TICKS = 40;
    /** Keyframe times of the steps timed on the server: {@code summonDone}, {@code summon_normalDone}. */
    private static final int SUMMON_DONE_TICKS = 242;
    private static final int SUMMON_NORMAL_DONE_TICKS = 124;
    /** {@code disappear} and {@code summon_sandMSG2} of the sand summon. */
    private static final int SAND_VANISH_TICKS = 40;
    private static final int SAND_PHOBIA_TICKS = 60;
    /** {@code rich_MSG1} and {@code disappear} of the two wish animations. */
    private static final int RICH_GRANT_TICKS = 1;
    private static final int RICH_VANISH_TICKS = 17;
    private static final int RICH_NORMAL_GRANT_TICKS = 10;
    private static final int RICH_NORMAL_VANISH_TICKS = 18;
    /** {@code deepthroat_prepareDone} of the two preparations. */
    private static final int PREPARE_DONE_TICKS = 67;
    private static final int PREPARE_NORMAL_DONE_TICKS = 32;
    /** The reverse cowgirl steps that hand over on a timer (1.12.2 {@code Action} lengths). */
    private static final int COWGIRL_START_TICKS = 88;
    private static final int COWGIRL_FAST_START_TICKS = 34;
    private static final int COWGIRL_FAST_CONTINUES_TICKS = 39;
    /** Deepthroat: the summoner steps up to her (1.12.2 {@code positionPlayerRelative(0, 0, 1.35, 0, 30)}). */
    private static final PlayerPose DEEPTHROAT_POSE = new PlayerPose(0.0, 0.0, 1.35, 0.0F, 30.0F);
    private static final double CHAT_RANGE = 40.0;
    /** Her summoner's client asks for the next fast loop ({@code fastSwitch} with sneak held). */
    public static final String REQUEST_FAST_SWITCH = "fast_switch";

    /** Server: her first visit from this lamp (1.12.2 {@code hasLampItem}). */
    private boolean firstWish;
    /** Server: the scene picked, played once she has prepared. */
    private GirlAction chosenScene = GirlAction.NULL;
    private int vanishTicks = -1;

    /** Client: the reverse cowgirl's slow variant (1.12.2 {@code stateIndex}). */
    private int slowVariant = 1;
    /** Client: the wish menu has been opened for this visit. */
    private boolean wishMenuOpened;
    /** Client: the summoner closed the wish menu without a wish; it comes back (1.12.2 {@code ac}). */
    private boolean menuDeclined;
    private int clientVanishTicks;
    private boolean fastMoanSounded;
    private int lastTailParticleTick = -1;

    public AllieEntity(EntityType<? extends AllieEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes();
    }

    /** Server: she appears at {@code pos} facing the summoner, who held a lamp with {@code wishesUsed} wishes granted before. */
    public static void summon(ServerLevel level, ServerPlayer summoner, Vec3 pos, int wishesUsed) {
        AllieEntity allie = ModEntities.ALLIE.get().create(level, EntitySpawnReason.TRIGGERED);
        if (allie == null) return;
        float yaw = summoner.getYRot() + 180.0F;
        allie.snapTo(pos.x, pos.y, pos.z, yaw, summoner.getXRot());
        allie.firstWish = wishesUsed == 0;
        allie.entityData.set(DATA_WISHES_LEFT, AlliesLampItem.WISHES - wishesUsed);
        allie.setPartner(summoner);
        allie.anchor(pos, yaw);
        allie.noPhysics = true;
        BlockState below = level.getBlockState(BlockPos.containing(pos).below());
        boolean sand = below.is(Blocks.SAND) || below.is(Blocks.RED_SAND); // 1.12.2's sand block covered both
        allie.setAction(sand ? GirlAction.SUMMON_SAND : allie.firstWish ? GirlAction.SUMMON : GirlAction.SUMMON_NORMAL);
        level.addFreshEntity(allie);
        ParticleRings.ring(level, ParticleTypes.PORTAL, pos, 300, 0.75, 1.5);
    }

    /** Whether an Allie summoned by {@code player} is still out (1.12.2 refused the lamp while its Allie was). */
    public static boolean hasAllieOut(ServerPlayer player) {
        return !player.level().getEntities(ModEntities.ALLIE.get(), allie -> allie.isAlive() && allie.isPartner(player)).isEmpty();
    }

    @Override
    protected boolean isCompanion() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_WISHES_LEFT, AlliesLampItem.WISHES);
        builder.define(DATA_VANISHING, false);
        builder.define(DATA_FAST_VARIANT, 1);
    }

    public int wishesLeft() {
        return this.entityData.get(DATA_WISHES_LEFT);
    }

    public boolean isVanishing() {
        return this.entityData.get(DATA_VANISHING);
    }

    /** Client: how big she is drawn, from 1 down to 0 as she vanishes. */
    public float vanishScale(float partialTick) {
        if (!isVanishing()) return 1.0F;
        return Math.max(0.0F, 1.0F - (this.clientVanishTicks + partialTick) / VANISH_TICKS);
    }

    @Override
    protected String animationPrefix() {
        return "animation.allie.";
    }

    @Override
    protected String blinkAnimationName() {
        return "null";
    }

    @Override
    protected boolean blinks(GirlAction action) {
        return false; // 1.12.2 never blinked her
    }

    @Override
    protected @Nullable String constantMovementAnimation() {
        return "tail";
    }

    @Override
    public String dialoguePrefix() {
        return "allie.dialogue.";
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return OPTIONS;
    }

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return switch (action) {
            case SUMMON -> once("summon");
            case SUMMON_WAIT -> loop("summon_wait");
            case SUMMON_NORMAL -> once("summon_normal");
            case SUMMON_NORMAL_WAIT -> loop("summon_normal_wait");
            case SUMMON_SAND -> once("summon_sand");
            case RICH_FIRST_TIME -> once("rich");
            case RICH_NORMAL -> once("rich_normal");
            case ALLIE_PREPARE_FIRST_TIME -> once("deepthroat_prepare");
            case ALLIE_PREPARE_NORMAL -> once("deepthroat_normal_prepare");
            case DEEPTHROAT_START -> once("deepthroat_start");
            case DEEPTHROAT_SLOW -> loop("deepthroat_slow");
            case DEEPTHROAT_FAST -> loop("deepthroat_fast");
            case DEEPTHROAT_CUM -> once("deepthroat_cum");
            case REVERSE_COWGIRL_START -> loop("reverse_cowgirl_start");
            case REVERSE_COWGIRL_SLOW -> loop("reverse_cowgirl_slow" + this.slowVariant);
            case REVERSE_COWGIRL_FAST_START -> loop("reverse_cowgirl_fasts");
            case REVERSE_COWGIRL_FAST_CONTINUES -> loop("reverse_cowgirl_fastc" + this.entityData.get(DATA_FAST_VARIANT));
            case REVERSE_COWGIRL_CUM -> once("reverse_cowgirl_cum");
            default -> null;
        };
    }

    /* ------------------------------------------------------------------ */
    /* Ticking                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (isVanishing()) this.clientVanishTicks++;
            GirlAction action = getAction();
            // the server may end the summon before this client reaches the menu's keyframe
            if (action == GirlAction.SUMMON_WAIT || action == GirlAction.SUMMON_NORMAL_WAIT) openWishMenu();
            if (this.menuDeclined && (!awaitsChoice() || ClientHooks.reopenWishMenu(this))) this.menuDeclined = false;
        } else if (getAction() == GirlAction.NULL || (this.vanishTicks >= 0 && ++this.vanishTicks >= VANISH_TICKS)) {
            // her visit is over (or the world was reloaded); 1.12.2 removed her as soon as she was idle
            discard();
        }
    }

    @Override
    protected void tickAction(GirlAction action, int ticks) {
        switch (action) {
            case SUMMON -> {
                if (ticks >= SUMMON_DONE_TICKS) setAction(GirlAction.SUMMON_WAIT);
            }
            case SUMMON_NORMAL -> {
                if (ticks >= SUMMON_NORMAL_DONE_TICKS) setAction(GirlAction.SUMMON_NORMAL_WAIT);
            }
            case SUMMON_SAND -> {
                if (ticks == SAND_VANISH_TICKS) vanish();
                if (ticks == SAND_PHOBIA_TICKS) tellNearby(Component.translatable("allie.dialogue.phobia"));
            }
            case RICH_FIRST_TIME -> {
                if (ticks == RICH_GRANT_TICKS) grantRiches();
                if (ticks == RICH_VANISH_TICKS) vanish();
            }
            case RICH_NORMAL -> {
                if (ticks == RICH_NORMAL_GRANT_TICKS) grantRiches();
                if (ticks == RICH_NORMAL_VANISH_TICKS) vanish();
            }
            case ALLIE_PREPARE_FIRST_TIME -> {
                if (ticks >= PREPARE_DONE_TICKS) startChosenScene();
            }
            case ALLIE_PREPARE_NORMAL -> {
                if (ticks >= PREPARE_NORMAL_DONE_TICKS) startChosenScene();
            }
            case REVERSE_COWGIRL_START -> {
                if (ticks >= COWGIRL_START_TICKS) setAction(GirlAction.REVERSE_COWGIRL_SLOW);
            }
            case REVERSE_COWGIRL_FAST_START -> {
                if (ticks >= COWGIRL_FAST_START_TICKS) setAction(GirlAction.REVERSE_COWGIRL_SLOW);
            }
            case REVERSE_COWGIRL_FAST_CONTINUES -> {
                if (ticks >= COWGIRL_FAST_CONTINUES_TICKS) setAction(GirlAction.REVERSE_COWGIRL_SLOW);
            }
            default -> super.tickAction(action, ticks);
        }
    }

    @Override
    protected int stripEndTicks() {
        return 0; // she never strips
    }

    /** Server: she starts to shrink away; she is gone {@link #VANISH_TICKS} later. */
    private void vanish() {
        if (this.vanishTicks >= 0) return;
        this.vanishTicks = 0;
        this.entityData.set(DATA_VANISHING, true);
    }

    /** "Make me rich!" (1.12.2 {@code MakeRichWishPacket}). */
    private void grantRiches() {
        for (net.minecraft.world.item.Item item : List.of(Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT)) {
            ItemStack stack = new ItemStack(item, this.random.nextInt(2) + 1);
            this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), stack));
        }
    }

    /** A line with no speaker to everyone nearby. */
    private void tellNearby(Component line) {
        for (ServerPlayer nearby : ((ServerLevel) this.level()).players()) {
            if (nearby.distanceToSqr(this) <= CHAT_RANGE * CHAT_RANGE) nearby.sendSystemMessage(line);
        }
    }

    /* ------------------------------------------------------------------ */
    /* The wish menu and the scenes                                       */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean awaitsChoice() {
        GirlAction action = getAction();
        return action == GirlAction.SUMMON || action == GirlAction.SUMMON_WAIT || action == GirlAction.SUMMON_NORMAL || action == GirlAction.SUMMON_NORMAL_WAIT;
    }

    @Override
    public void chooseMidScene(ServerPlayer player, SceneOption option) {
        if (option.firstStep() == GirlAction.RICH_FIRST_TIME) {
            setAction(this.firstWish ? GirlAction.RICH_FIRST_TIME : GirlAction.RICH_NORMAL);
        } else {
            this.chosenScene = option.firstStep();
            setAction(this.firstWish ? GirlAction.ALLIE_PREPARE_FIRST_TIME : GirlAction.ALLIE_PREPARE_NORMAL);
        }
    }

    /** Server: prepared, she starts the scene picked; the summoner is put in place (1.12.2 {@code deepthroat_prepareDone}). */
    private void startChosenScene() {
        if (!(getPartner() instanceof ServerPlayer partner)) {
            endScene();
            return;
        }
        if (this.chosenScene == GirlAction.REVERSE_COWGIRL_START) {
            // 1.12.2 handleAllieOwner: onto the spot she floats at
            partner.connection.teleport(this.getX(), this.getY(), this.getZ(), partner.getYRot(), partner.getXRot());
        } else {
            setSceneFrame(partner.position(), this.getYRot() + 180.0F);
            positionPartner(partner, DEEPTHROAT_POSE);
        }
        setAction(this.chosenScene);
    }

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return switch (action) {
            case DEEPTHROAT_SLOW -> GirlAction.DEEPTHROAT_FAST;
            case REVERSE_COWGIRL_SLOW -> GirlAction.REVERSE_COWGIRL_FAST_START;
            default -> null;
        };
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return switch (action) {
            case DEEPTHROAT_SLOW, DEEPTHROAT_FAST -> GirlAction.DEEPTHROAT_CUM;
            case REVERSE_COWGIRL_SLOW, REVERSE_COWGIRL_FAST_START, REVERSE_COWGIRL_FAST_CONTINUES -> GirlAction.REVERSE_COWGIRL_CUM;
            default -> null;
        };
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        if (to == sneakStep(from) || to == climaxStep(from)) return true;
        return switch (from) {
            case DEEPTHROAT_START, DEEPTHROAT_FAST -> to == GirlAction.DEEPTHROAT_SLOW;                      // deepthroat_startDone, deepthroat_fastDone
            case REVERSE_COWGIRL_FAST_START -> to == GirlAction.REVERSE_COWGIRL_FAST_CONTINUES;              // fastSwitch with sneak held
            case DEEPTHROAT_CUM, REVERSE_COWGIRL_CUM -> to == GirlAction.NULL;                               // *_cumDone
            default -> false;
        };
    }

    @Override
    protected void onAnimationEvent(String event) {
        if (this.level().isClientSide()) AllieSceneEvents.handle(this, event);
    }

    /* ------------------------------------------------------------------ */
    /* Client                                                             */
    /* ------------------------------------------------------------------ */

    @Override
    public void onChoiceDeclined() {
        this.menuDeclined = true;
    }

    /** Client {@code cowgirlSlowDone}: the next slow loop is another of the three. */
    public void rerollSlowVariant() {
        int previous = this.slowVariant;
        do {
            this.slowVariant = this.random.nextInt(3) + 1;
        } while (this.slowVariant == previous);
    }

    /** Client: her summoner's wish menu opens, once per visit (later only when closed without a wish). */
    public void openWishMenu() {
        if (this.wishMenuOpened) return;
        this.wishMenuOpened = true;
        ClientHooks.openWishMenu(this);
    }

    /**
     * Server {@code fastSwitch} with sneak held: another of the three fast loops, from its start, and
     * its timer starts over, so the fast loop goes on while sneak is held (1.12.2 also reset the timer).
     */
    @Override
    public void requestNamed(ServerPlayer player, String name) {
        if (!REQUEST_FAST_SWITCH.equals(name)) {
            super.requestNamed(player, name);
            return;
        }
        if (!isPartner(player) || getAction() != GirlAction.REVERSE_COWGIRL_FAST_CONTINUES) return;
        int previous = this.entityData.get(DATA_FAST_VARIANT);
        int next;
        do {
            next = this.random.nextInt(3) + 1;
        } while (next == previous);
        this.entityData.set(DATA_FAST_VARIANT, next);
        restartActionTimer();
    }

    /** Client {@code fastMoan}: she moans on every other one (1.12.2 {@code stateFlag}). */
    public boolean nextFastMoanSounds() {
        this.fastMoanSounded = !this.fastMoanSounded;
        return this.fastMoanSounded;
    }

    /** Client: a portal particle drifts off her tail every half second; true once per such tick. */
    public boolean tailParticleDue() {
        if (this.tickCount % 10 != 0 || this.lastTailParticleTick == this.tickCount) return false;
        this.lastTailParticleTick = this.tickCount;
        return true;
    }
}
