package zeh.createpickywheels.mixin;

import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.contraptions.bearing.WindmillBearingBlockEntity;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
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
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zeh.createpickywheels.common.Configuration;
import zeh.createpickywheels.common.PickyTags;
import zeh.createpickywheels.common.util.BlockPosEntry;

import java.util.*;

@Mixin(value = WindmillBearingBlockEntity.class, remap = false)
public abstract class WindmillBearingBlockEntityMixin extends MechanicalBearingBlockEntity {

    @Shadow public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);

    private WindmillBearingBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    private boolean hasFlow = false;
    boolean isViable = false;

    public BlockPos root;
    float flowScore = 0;
    int currents = 0;

    private final int searchedPerTick = 320;
    List<BlockPosEntry> frontier = new ArrayList<>();
    Set<BlockPos> visited = new HashSet<>();

    int revalidateIn = 1;
    int flowCheck = 1;

    protected int validationTimer() {
        int maxBlocks = maxBlocks();
        // Allow enough time for the block threshold to be reached
        int validationTimerMin = 200;
        return maxBlocks < 0 ? validationTimerMin : Math.max(validationTimerMin, maxBlocks / searchedPerTick + 1);
    }

    protected void setValidationTimer() { revalidateIn = validationTimer(); }
    protected void setLongValidationTimer() { revalidateIn = validationTimer() * 2; }
    protected int maxRange() { return Configuration.WINDMILLS_MAX_RANGE.get(); }
    protected int requiredRange() { return Configuration.WINDMILLS_REQUIRED_RANGE.get(); }
    protected int requiredRangePoints() { return Configuration.WINDMILLS_REQUIRED_RANGE_POINTS.get(); }
    protected int maxBlocks() { return Configuration.WINDMILLS_THRESHOLD.get(); }
    protected float aboveOf = 0;

    public void reset() {
        setValidationTimer();
        currents = 0;
        frontier.clear();
        visited.clear();
        hasFlow = false;
        sendData();
    }

    @Override
    public void destroy() { reset(); super.destroy(); }

    protected void search(List<BlockPosEntry> frontier, Set<BlockPos> visited) throws FluidManipulationBehaviour.ChunkNotLoadedException {
        if (level == null) return;
        int maxBlocks = maxBlocks();
        int maxRange = maxRange();
        int maxRangeSq = maxRange * maxRange;
        int requiredRange = requiredRange();
        int requiredRangeSq = requiredRange * requiredRange;

        Axis axis = movedContraption.getRotationAxis();
        Direction dirA = Iterate.directionsInAxis(axis)[0];
        Direction dirB = Iterate.directionsInAxis(axis)[1];
        int sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks();
        float rh = Mth.sqrt(sails);

        for (int i = 0; i < searchedPerTick && !frontier.isEmpty() && (visited.size() <= maxBlocks || currents <= requiredRangePoints()); i++) {
            BlockPosEntry entry = frontier.remove(0);
            BlockPos currentPos = entry.pos();

            if (currentPos.distSqr(root) > requiredRangeSq) currents++;
            if (visited.contains(currentPos)) continue; else visited.add(currentPos);
            if (!level.isLoaded(currentPos)) throw new FluidManipulationBehaviour.ChunkNotLoadedException();

            BlockState blockState = level.getBlockState(currentPos);
            if (!blockState.isAir()) continue;
            for (Direction side : Iterate.directions) {
                if (dirA == side || dirB == side) continue;
                int k = 0;
                BlockPos offsetPos = currentPos.relative(side);

                if (!level.isLoaded(offsetPos)) throw new FluidManipulationBehaviour.ChunkNotLoadedException();
                if (visited.contains(offsetPos) || offsetPos.distSqr(root) > maxRangeSq) continue;
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

    private void continueSearch() {
        try {
            search(frontier, visited);
        } catch (FluidManipulationBehaviour.ChunkNotLoadedException e) {
            sendData();
            currents = 0;
            frontier.clear();
            visited.clear();
            setLongValidationTimer();
            return;
        }

        if (currents > requiredRangePoints() && visited.size() > maxBlocks()) {
            currents = 0;
            frontier.clear();
            if (!hasFlow) {
                hasFlow = true;
                visited.clear();
                sendData();
                determineAndApplyFlowScore();
            }
            setLongValidationTimer();
            return;
        }
        if (!frontier.isEmpty()) return;
        if (hasFlow) {
            reset();
            determineAndApplyFlowScore();
            return;
        }
        setValidationTimer();
        sendData();
        visited.clear();
    }

    /**
     * @author ZehMaria
     * @reason Changing Wind Mills to require enough space for air current to flow.
     */
    @Overwrite
    public float getGeneratedSpeed() {
        if (!running) return 0;
        if (!hasFlow) return 0.0001F;
        if (movedContraption == null) return lastGeneratedSpeed;
        int sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks()
                / AllConfigs.server().kinetics.windmillSailsPerRPM.get();
        int aboveX = Configuration.WINDMILLS_ABOVEX.get();
        return ((Mth.clamp(sails, 1, 16) + aboveX * aboveOf) * getAngleSpeedDirection());
    }

    /**
     * @author ZehMaria
     * @reason Changing Wind Mills to require enough space for air current to flow.
     */
    @Overwrite
    public void tick() {
        super.tick();
        if (level != null && level.isClientSide()) return;
        tickToo();

        if (movedContraption == null) return;
        if (flowCheck > 0) flowCheck--;
        if (flowCheck == 0) determineAndApplyFlowScore();

        if (!isViable) return;
        if (!frontier.isEmpty() && level != null) {
            continueSearch();
            return;
        }

        if (revalidateIn > 0) revalidateIn--;
        if (frontier.isEmpty() && revalidateIn == 0) {
            currents = 0;
            visited.clear();
            for (Direction side : Iterate.directions) {
                if (Iterate.directionsInAxis(movedContraption.getRotationAxis())[0] == side) continue;
                if (Iterate.directionsInAxis(movedContraption.getRotationAxis())[1] == side) continue;
                frontier.add(new BlockPosEntry(root.relative(side), 0));
            }

            int r = requiredRange(), x = root.getX(), z = root.getZ(), n = 0, h = 0;
            for (int i = x - r; i <= x + r; i++) {
                for (int j = z - r; j <= z + r; j++) {
                    if (level != null) {
                        h += level.getHeight(Heightmap.Types.WORLD_SURFACE, i, j);
                        n++;
                    }
                }
            }
            int above = Configuration.WINDMILLS_ABOVE.get();
            aboveOf = (float) Mth.clamp(root.getY() - h / n, 0, above) / above;
        }
    }

    public void tickToo() {
        if (!queuedReassembly)  return;
        queuedReassembly = false;
        if (!running)  assembleNextTick = true;
    }

    public boolean determineViability() {
        if (level != null && !level.getBiome(worldPosition).is(PickyTags.PICKY_WINDMILLS)) return false;
        root = worldPosition;
        return true;
    }

    public void determineAndApplyFlowScore() {
        flowCheck = validationTimer();
        isViable = determineViability();
        setFlowScoreAndUpdate(hasFlow && isViable ? (0.5F + aboveOf) : 0);
    }

    public void setFlowScoreAndUpdate(float score) {
        if (flowScore == score)  return;
        flowScore = score;
        setSpeed(score);
        updateGeneratedRotation();
        setChanged();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        if (!isViable && running) TooltipHelper.addHint(tooltip, "hint.waterwheel_biome");
        if (!hasFlow && running && isViable) TooltipHelper.addHint(tooltip, "hint.windmill_flow");
        return addToGoggleTooltip;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void write(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
        if (hasFlow) NBTHelper.putMarker(nbt, "HasFlow");
        if (isViable) NBTHelper.putMarker(nbt, "IsViable");
        nbt.putFloat("FlowScore", flowScore);
        nbt.putFloat("AboveOf", aboveOf);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void read(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
        hasFlow = nbt.contains("HasFlow");
        isViable = nbt.contains("IsViable");
        flowScore = nbt.getInt("FlowScore");
        aboveOf = nbt.getFloat("AboveOf");
    }

    @Shadow
    protected boolean queuedReassembly;

    @Shadow
    protected abstract float getAngleSpeedDirection();

    @Shadow
    protected float lastGeneratedSpeed;
}
