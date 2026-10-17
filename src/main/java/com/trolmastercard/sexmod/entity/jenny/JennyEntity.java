package com.trolmastercard.sexmod.entity.jenny;

import com.geckolib.animation.RawAnimation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import com.trolmastercard.sexmod.client.scene.JennySceneEvents;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.PlayerPose;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.sound.ModSounds;
import com.trolmastercard.sexmod.util.VectorMath;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Items;
import static com.trolmastercard.sexmod.entity.SceneOption.Outfit;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Jenny: the blonde sword/bow girl (1.12.2 {@code JennyEntity}).
 * <p>
 * Scenes ported: blowjob (payment, intro, suck ⇄ thrust on sneak, climax on a full meter + jump),
 * boobjob (paizuri: start, slow ⇄ fast on sneak, climax), doggy on a bed, strip and dress up,
 * and the companion behaviour every girl shares ({@link GirlEntity}).
 */
public class JennyEntity extends GirlEntity {
    private static final List<SceneOption> OPTIONS = List.of(
        new SceneOption("blowjob", "action.names.blowjob", Items.EMERALD, 3, GirlAction.STARTBLOWJOB, Outfit.ANY, false, false, false, "giveblowjob"),
        new SceneOption("boobjob", "action.names.boobjob", Items.ENDER_PEARL, 2, GirlAction.PAIZURI_START, Outfit.ANY, true, false, false, "givebooba"),
        new SceneOption("doggy", "action.names.doggy", Items.DIAMOND, 2, GirlAction.STARTDOGGY, Outfit.ANY, true, false, true, "givesex"),
        new SceneOption("strip", "action.names.strip", Items.GOLD_INGOT, 1, GirlAction.STRIP, Outfit.DRESSED, false, false, false, "showBobsandveganapls"),
        new SceneOption("dressup", "action.names.dressup", null, 0, GirlAction.STRIP, Outfit.NUDE, false, true, false, ""));

    /** Partner kneels in front of her during the intro (1.12.2 event {@code bjiMSG10}). */
    private static final PlayerPose BLOWJOB_POSE = new PlayerPose(-0.65, -0.8, -0.25, 60.0F, -3.0F);
    /** Partner looks down for the climax (1.12.2 {@code getCumAction}). */
    private static final PlayerPose CLIMAX_POSE = new PlayerPose(0.0, 0.0, 0.0, 0.0F, 70.0F);
    public static final String POSE_BLOWJOB = "blowjob";
    /** Partner's place at the boobjob (1.12.2 event {@code boobjob_camera}); also after each fast stretch. */
    private static final PlayerPose BOOBJOB_POSE = new PlayerPose(-0.7, -0.6, 0.2, 60.0F, -3.0F);
    /** Partner leans in for the fast stretch (1.12.2 {@code getNextAction}). */
    private static final PlayerPose BOOBJOB_FAST_POSE = new PlayerPose(0.0, 0.0, 0.2, 0.0F, 70.0F);
    public static final String POSE_BOOBJOB = "boobjob";

    /** The fast doggy stretch plays its hard variant when the partner holds jump at its midpoint ({@code doggyfastReady}). */
    private static final EntityDataAccessor<Boolean> DATA_HARD = SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String REQUEST_HARD = "hard";

    public JennyEntity(EntityType<? extends JennyEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HARD, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes();
    }

    @Override
    protected String animationPrefix() {
        return "animation.jenny.";
    }

    @Override
    protected String blinkAnimationName() {
        return "fhappy";
    }

    @Override
    public String dialoguePrefix() {
        return "jenny.dialogue.";
    }

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return switch (action) {
            case NULL -> loop("null");
            case PAYMENT -> once("payment");
            case STARTBLOWJOB -> once("blowjobintro");
            case SUCKBLOWJOB -> loop("blowjobsuck");
            case THRUSTBLOWJOB -> loop("blowjobthrust");
            case CUMBLOWJOB -> once("blowjobcum");
            case STRIP -> once("strip");
            case PAIZURI_START -> once("paizuri_start");
            case PAIZURI_SLOW -> loop("paizuri_slow");
            case PAIZURI_FAST -> loop("paizuri_fast");
            case PAIZURI_CUM -> once("paizuri_cum");
            case WALK_TO_BED -> loop("null"); // the movement controller walks her
            case STARTDOGGY -> once("doggygoonbed");
            case WAITDOGGY -> loop("doggywait");
            case DOGGYSTART -> once("doggystart");
            case DOGGYSLOW -> loop("doggyslow");
            case DOGGYFAST -> loop(this.entityData.get(DATA_HARD) ? "doggyfast_hard" : "doggyfast_soft");
            case DOGGYCUM -> once("doggycum");
            case ATTACK, RIDE, SIT, THROW_PEARL, DOWNED -> companionAnimation(action);
            default -> null;
        };
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return OPTIONS;
    }

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return switch (action) {
            case SUCKBLOWJOB -> GirlAction.THRUSTBLOWJOB;
            case PAIZURI_SLOW -> GirlAction.PAIZURI_FAST;
            case DOGGYSLOW -> GirlAction.DOGGYFAST;
            default -> null;
        };
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return switch (action) {
            case SUCKBLOWJOB, THRUSTBLOWJOB -> GirlAction.CUMBLOWJOB;
            case PAIZURI_SLOW, PAIZURI_FAST -> GirlAction.PAIZURI_CUM;
            case DOGGYSLOW, DOGGYFAST -> GirlAction.DOGGYCUM;
            default -> null;
        };
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        if (to == sneakStep(from) || to == climaxStep(from)) return true;
        return switch (from) {
            case STARTBLOWJOB -> to == GirlAction.SUCKBLOWJOB;   // bjiDone
            case THRUSTBLOWJOB -> to == GirlAction.SUCKBLOWJOB;  // bjtDone
            case CUMBLOWJOB, PAIZURI_CUM, DOGGYCUM -> to == GirlAction.NULL; // bjcDone, doggyCumDone
            case DOGGYSTART -> to == GirlAction.DOGGYSLOW;       // doggystartDone
            case DOGGYFAST -> to == GirlAction.DOGGYSLOW;        // doggyfastDone
            case PAIZURI_START -> to == GirlAction.PAIZURI_SLOW; // paizuri_startDone
            case PAIZURI_FAST -> to == GirlAction.PAIZURI_SLOW;  // paizuri_fastDone
            default -> false;
        };
    }

    @Override
    protected void onStep(ServerPlayer partner, GirlAction from, GirlAction to) {
        this.entityData.set(DATA_HARD, false);
        if (from == GirlAction.STARTBLOWJOB || from == GirlAction.PAIZURI_START) {
            // 1.12.2: step the partner 0.2 closer once the intro ends
            Vec3 offset = VectorMath.rotateByYaw(new Vec3(0.0, 0.0, 0.2), this.getYRot() + 180.0F);
            partner.connection.teleport(partner.getX() + offset.x, partner.getY(), partner.getZ() + offset.z, partner.getYRot(), partner.getXRot());
        }
        if (to == GirlAction.CUMBLOWJOB) {
            positionPartner(partner, CLIMAX_POSE);
        }
        if (to == GirlAction.PAIZURI_FAST) positionPartner(partner, BOOBJOB_FAST_POSE);
        if (from == GirlAction.PAIZURI_FAST && to == GirlAction.PAIZURI_SLOW) positionPartner(partner, BOOBJOB_POSE);
    }

    @Override
    protected @Nullable PlayerPose scenePose(String name, GirlAction action) {
        if (POSE_BLOWJOB.equals(name) && action == GirlAction.STARTBLOWJOB) return BLOWJOB_POSE;
        if (POSE_BOOBJOB.equals(name) && action == GirlAction.PAIZURI_START) return BOOBJOB_POSE;
        return null;
    }

    @Override
    protected int attackTicks(int animation) {
        return animation == 0 ? 7 : 10; // attackDone at 0.32 s / 0.48 s
    }

    @Override
    public void onNewHome(ServerPlayer player) {
        sayTo(player, "newhome");
        this.level().playSound(null, this, ModSounds.GIRLS_JENNY_HAPPYOH.variant(1).value(), this.getSoundSource(), 1.0F, 1.0F);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GIRLS_JENNY_SIGH.random(this.random).value();
    }

    @Override
    protected void refuseSound() {
        this.level().playSound(null, this, ModSounds.GIRLS_JENNY_HMPH.variant(2).value(), this.getSoundSource(), 1.0F, 1.0F);
    }

    @Override
    public void requestNamed(ServerPlayer player, String name) {
        if (REQUEST_HARD.equals(name) && getAction() == GirlAction.DOGGYFAST) this.entityData.set(DATA_HARD, true);
        else super.requestNamed(player, name);
    }

    @Override
    protected int stripEndTicks() {
        return 101; // stripDone at 5.04 s
    }

    @Override
    protected void tickAction(GirlAction action, int ticks) {
        switch (action) {
            // no partner is in the room yet, so the server speaks and times these (events doggyGoOnBedMSG2/3/Done)
            case STARTDOGGY -> {
                if (ticks == 50) sayNearby("doggytext1");
                if (ticks == 83) sayNearby("doggytext2");
                if (ticks >= 130) setAction(GirlAction.WAITDOGGY);
            }
            default -> super.tickAction(action, ticks);
        }
    }

    @Override
    protected void onAnimationEvent(String event) {
        if (this.level().isClientSide()) JennySceneEvents.handle(this, event);
    }
}
