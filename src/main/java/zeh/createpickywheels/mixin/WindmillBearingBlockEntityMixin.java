package zeh.createpickywheels.mixin;

import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zeh.createpickywheels.common.Configuration;
import zeh.createpickywheels.common.PickyTags;
import zeh.createpickywheels.common.util.BlockPosEntry;

import java.util.*;

@Mixin(value = WindmillBearingBlockEntity.class, remap = false)
public abstract class WindmillBearingBlockEntityMixin extends MechanicalBearingBlockEntity {

    private WindmillBearingBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    @Unique
    private boolean createPickyWheels$hasFlow = false;
    @Unique
    boolean createPickyWheels$isViable = false;

    @Unique
    public BlockPos createPickyWheels$root;
    @Unique
    float createPickyWheels$flowScore = 0;
    @Unique
    int createPickyWheels$currents = 0;

    @Unique
    private final int createPickyWheels$searchedPerTick = 256;
    @Unique
    List<BlockPosEntry> createPickyWheels$frontier = new ArrayList<>();
    @Unique
    Set<BlockPos> createPickyWheels$visited = new HashSet<>();

    @Unique
    int createPickyWheels$revalidateIn = 1;
    @Unique
    int createPickyWheels$flowCheck = 0;
    @Unique
    protected float createPickyWheels$aboveOf = 0;
    @Unique
    protected float createPickyWheels$boost = 0;

    @Unique
    protected int createPickyWheels$validationTimer() {
        int maxBlocks = createPickyWheels$maxBlocks();
        // Allow enough time for the block threshold to be reached
        int validationTimerMin = 200;
        return maxBlocks < 0 ? validationTimerMin : Math.max(validationTimerMin, maxBlocks / createPickyWheels$searchedPerTick + 1);
    }

    @Unique
    protected void createPickyWheels$setValidationTimer() { createPickyWheels$revalidateIn = createPickyWheels$validationTimer(); }
    @Unique
    protected void createPickyWheels$setLongValidationTimer() { createPickyWheels$revalidateIn = createPickyWheels$validationTimer() * 2; }
    @Unique
    protected int createPickyWheels$maxRange() { return Configuration.WINDMILLS_MAX_RANGE.get(); }
    @Unique
    protected int createPickyWheels$requiredRange() { return Configuration.WINDMILLS_REQUIRED_RANGE.get(); }
    @Unique
    protected int createPickyWheels$requiredRangePoints() { return Configuration.WINDMILLS_REQUIRED_RANGE_POINTS.get(); }
    @Unique
    protected int createPickyWheels$maxBlocks() { return Configuration.WINDMILLS_THRESHOLD.get(); }
    @Unique
    protected boolean createPickyWheels$enabled() { return Configuration.WINDMILLS_ENABLED.get(); }
    @Unique
    protected double createPickyWheels$penalty() { return Configuration.WINDMILLS_PENALTY.get(); }

    @Unique
    public void createPickyWheels$reset() {
        createPickyWheels$setValidationTimer();
        createPickyWheels$currents = 0;
        createPickyWheels$frontier.clear();
        createPickyWheels$visited.clear();
        createPickyWheels$hasFlow = false;
        sendData();
    }

    @Override
    public void destroy() {
        if (createPickyWheels$enabled()) createPickyWheels$reset();
        super.destroy();
    }

    @Unique
    protected void createPickyWheels$search(List<BlockPosEntry> frontier, Set<BlockPos> visited) throws FluidManipulationBehaviour.ChunkNotLoadedException {
        if (level == null) return;
        int maxBlocks = createPickyWheels$maxBlocks();
        int maxRange = createPickyWheels$maxRange();
        int maxRangeSq = maxRange * maxRange;
        int requiredRange = createPickyWheels$requiredRange();
        int requiredRangeSq = requiredRange * requiredRange;

        Axis axis = movedContraption.getRotationAxis();
        Direction dirA = Iterate.directionsInAxis(axis)[0];
        Direction dirB = Iterate.directionsInAxis(axis)[1];
        int sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks();
        float rh = Mth.sqrt(sails);

        for (int i = 0; i < createPickyWheels$searchedPerTick && !frontier.isEmpty() && (visited.size() <= maxBlocks || createPickyWheels$currents <= createPickyWheels$requiredRangePoints()); i++) {
            BlockPosEntry entry = frontier.remove(0);
            BlockPos currentPos = entry.pos();

            if (currentPos.distSqr(createPickyWheels$root) > requiredRangeSq) createPickyWheels$currents++;
            if (visited.contains(currentPos)) continue; else visited.add(currentPos);
            if (!level.isLoaded(currentPos)) throw new FluidManipulationBehaviour.ChunkNotLoadedException();

            BlockState blockState = level.getBlockState(currentPos);
            if (!blockState.isAir()) continue;
            for (Direction side : Iterate.directions) {
                if (dirA == side || dirB == side) continue;
                int k = 0;
                BlockPos offsetPos = currentPos.relative(side);

                if (!level.isLoaded(offsetPos)) throw new FluidManipulationBehaviour.ChunkNotLoadedException();
                if (visited.contains(offsetPos) || offsetPos.distSqr(createPickyWheels$root) > maxRangeSq) continue;
                if (level.getBlockState(offsetPos).isAir()) k++;
                for (int j = 0; j <= rh; j++) {
                    BlockPos offsetA = offsetPos.relative(dirA);
                    if (!level.isLoaded(offsetA)) throw new FluidManipulationBehaviour.ChunkNotLoadedException();
                    if (level.getBlockState(offsetA).isAir()) k++;
                    offsetPos = offsetA;
                }
                offsetPos = currentPos.relative(side);
                for (int j = 0; j <= rh; j++) {
                    BlockPos offsetB = offsetPos.relative(dirB);
                    if (!level.isLoaded(offsetB)) throw new FluidManipulationBehaviour.ChunkNotLoadedException();
                    if (level.getBlockState(offsetB).isAir()) k++;
                    offsetPos = offsetB;
                }

                if (k >= rh)  {
                    offsetPos = currentPos.relative(side);
                    frontier.add(new BlockPosEntry(offsetPos, entry.distance() + 1));
                }
            }
        }
    }

    @Unique
    private void createPickyWheels$continueSearch() {
        try {
            createPickyWheels$search(createPickyWheels$frontier, createPickyWheels$visited);
        } catch (FluidManipulationBehaviour.ChunkNotLoadedException e) {
            sendData();
            createPickyWheels$currents = 0;
            createPickyWheels$frontier.clear();
            createPickyWheels$visited.clear();
            createPickyWheels$setLongValidationTimer();
            return;
        }

        if (createPickyWheels$currents > createPickyWheels$requiredRangePoints() && createPickyWheels$visited.size() > createPickyWheels$maxBlocks()) {
            createPickyWheels$currents = 0;
            createPickyWheels$frontier.clear();
            if (!createPickyWheels$hasFlow) {
                createPickyWheels$hasFlow = true;
                createPickyWheels$visited.clear();
                sendData();
                createPickyWheels$determineAndApplyFlowScore();
            }
            createPickyWheels$setLongValidationTimer();
            return;
        }
        if (!createPickyWheels$frontier.isEmpty()) return;
        if (createPickyWheels$hasFlow) {
            createPickyWheels$reset();
            createPickyWheels$determineAndApplyFlowScore();
            return;
        }
        createPickyWheels$setValidationTimer();
        sendData();
        createPickyWheels$visited.clear();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (!createPickyWheels$enabled()) return;
        ci.cancel();

        super.tick();
        if (level != null && level.isClientSide()) return;
        createPickyWheels$tickToo();

        if (movedContraption == null) return;
        if (createPickyWheels$flowCheck == 0) createPickyWheels$determineAndApplyFlowScore();
        else if (createPickyWheels$flowCheck > 0) createPickyWheels$flowCheck--;

        if (!createPickyWheels$isViable) return;
        if (!createPickyWheels$frontier.isEmpty() && level != null) {
            createPickyWheels$continueSearch();
            return;
        }

        if (createPickyWheels$revalidateIn > 0) createPickyWheels$revalidateIn--;
        if (createPickyWheels$frontier.isEmpty() && createPickyWheels$revalidateIn == 0) {
            createPickyWheels$currents = 0;
            createPickyWheels$visited.clear();
            for (Direction side : Iterate.directions) {
                if (Iterate.directionsInAxis(movedContraption.getRotationAxis())[0] == side) continue;
                if (Iterate.directionsInAxis(movedContraption.getRotationAxis())[1] == side) continue;
                createPickyWheels$frontier.add(new BlockPosEntry(createPickyWheels$root.relative(side), 0));
            }

            int r = createPickyWheels$requiredRange(), x = createPickyWheels$root.getX(), z = createPickyWheels$root.getZ(), n = 0, h = 0;
            for (int i = x - r; i <= x + r; i++) {
                for (int j = z - r; j <= z + r; j++) {
                    if (level != null) {
                        h += level.getHeight(Heightmap.Types.WORLD_SURFACE, i, j);
                        n++;
                    }
                }
            }
            int above = Configuration.WINDMILLS_ABOVE.get();
            createPickyWheels$aboveOf = (float) Mth.clamp(createPickyWheels$root.getY() - h / n, 0, above) / above;
        }

    }

    @Unique
    public void createPickyWheels$tickToo() {
        if (!queuedReassembly)  return;
        queuedReassembly = false;
        if (!running)  assembleNextTick = true;
    }

    @Unique
    public boolean createPickyWheels$determineViability() {
        createPickyWheels$boost = 0;
        if (level != null && !level.getBiome(worldPosition).is(PickyTags.WINDMILLS_WHITELIST)) return false;
        createPickyWheels$boost = level.getBiome(worldPosition).is(PickyTags.WINDMILLS_BOOSTED) ? 1.0F : (float) createPickyWheels$penalty();
        createPickyWheels$root = worldPosition;
        return true;
    }

    @Unique
    public void createPickyWheels$determineAndApplyFlowScore() {
        createPickyWheels$flowCheck = createPickyWheels$validationTimer();
        createPickyWheels$isViable = createPickyWheels$determineViability();
        createPickyWheels$setFlowScoreAndUpdate(createPickyWheels$hasFlow && createPickyWheels$isViable ?
                ((0.5F + createPickyWheels$aboveOf) * createPickyWheels$boost) : 0);
    }

    @Unique
    public void createPickyWheels$setFlowScoreAndUpdate(float score) {
        if (createPickyWheels$flowScore == score)  return;
        createPickyWheels$flowScore = score;
        updateGeneratedRotation();
        setChanged();
    }

    @Inject(method = "getGeneratedSpeed", at = @At("HEAD"), cancellable = true)
    private void getGeneratedSpeed(CallbackInfoReturnable<Float> cir) {
        if (!createPickyWheels$enabled()) return;

        if (!running) { cir.setReturnValue(0F); return; }
        if (!createPickyWheels$hasFlow) { cir.setReturnValue(0F); return; }
        if (movedContraption == null) { cir.setReturnValue(lastGeneratedSpeed); return; }

        int sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks()
                / AllConfigs.server().kinetics.windmillSailsPerRPM.get();
        double abovePenalty = Configuration.WINDMILLS_ABOVE_PENALTY.get();
        cir.setReturnValue((float) ((Mth.clamp(sails, 1, 16) * (1F - abovePenalty * (1 - createPickyWheels$aboveOf)))
                * getAngleSpeedDirection() * createPickyWheels$boost));

        cir.cancel();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (!createPickyWheels$enabled()) return addToGoggleTooltip;

        Lang.number(createPickyWheels$boost)
                .style(ChatFormatting.AQUA)
                .space()
                .add(Lang.translate("hint.picky_biome_boost")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        double abovePenalty = Configuration.WINDMILLS_ABOVE_PENALTY.get();
        Lang.number((1F - abovePenalty * (1 - createPickyWheels$aboveOf)))
                .style(ChatFormatting.AQUA)
                .space()
                .add(Lang.translate("hint.picky_height_boost")
                        .style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);

        if (!createPickyWheels$isViable && running) TooltipHelper.addHint(tooltip, "hint.windmill_biome");
        if (!createPickyWheels$hasFlow && running && createPickyWheels$isViable) TooltipHelper.addHint(tooltip, "hint.windmill_flow");

        return addToGoggleTooltip;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void write(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
        if (!createPickyWheels$enabled()) return;
        if (createPickyWheels$hasFlow) NBTHelper.putMarker(nbt, "HasFlow");
        if (createPickyWheels$isViable) NBTHelper.putMarker(nbt, "IsViable");
        nbt.putFloat("FlowScore", createPickyWheels$flowScore);
        nbt.putFloat("AboveOf", createPickyWheels$aboveOf);
        nbt.putFloat("Boosted", createPickyWheels$boost);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void read(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
        if (!createPickyWheels$enabled()) return;
        createPickyWheels$hasFlow = nbt.contains("HasFlow");
        createPickyWheels$isViable = nbt.contains("IsViable");
        createPickyWheels$flowScore = nbt.getInt("FlowScore");
        createPickyWheels$aboveOf = nbt.getFloat("AboveOf");
        createPickyWheels$boost = nbt.getFloat("Boosted");
    }

    @Shadow protected boolean queuedReassembly;
    @Shadow protected abstract float getAngleSpeedDirection();
    @Shadow protected float lastGeneratedSpeed;
    @Shadow public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);

}
