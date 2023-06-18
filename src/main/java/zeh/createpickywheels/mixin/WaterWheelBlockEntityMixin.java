package zeh.createpickywheels.mixin;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour.ChunkNotLoadedException;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import zeh.createpickywheels.common.Configuration;
import zeh.createpickywheels.common.PickyTags;
import zeh.createpickywheels.common.util.BlockPosEntry;

import java.util.*;

@Mixin(value = WaterWheelBlockEntity.class, remap = false)
public abstract class WaterWheelBlockEntityMixin extends GeneratingKineticBlockEntity {

	private WaterWheelBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
	private boolean infinite = false;
	boolean inBiome = false;
	boolean hasValidSource = false;
	boolean isLava = false;

	public BlockPos root;

	private final int searchedPerTick = 640;
	List<BlockPosEntry> frontier = new ArrayList<>();
	Set<BlockPos> visited = new HashSet<>();

	int revalidateIn = 1;

	List<BlockPos> powerSource = new ArrayList<>();

	protected int validationTimer() {
		int maxBlocks = maxBlocks();
		// Allow enough time for the server's infinite block threshold to be reached
		int validationTimerMin = 200;
		return maxBlocks < 0 ? validationTimerMin : Math.max(validationTimerMin, maxBlocks / searchedPerTick + 1);
	}
	protected void setValidationTimer() { revalidateIn = validationTimer(); }
	protected void setLongValidationTimer() { revalidateIn = validationTimer() * 2; }
	protected int maxRange() { return Configuration.WATERWHEELS_RANGE.get(); }
	protected int maxBlocks() { return Configuration.WATERWHEELS_THRESHOLD.get(); }

	public void reset() {
		setValidationTimer();
		frontier.clear();
		visited.clear();
		infinite = false;
		sendData();
	}

	@Override
	public void destroy() { reset(); super.destroy(); }

	protected String canPullFluidsFrom(BlockState blockState, BlockPos pos) {
		if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED)) return "SOURCE";
		if (blockState.getBlock() instanceof LiquidBlock) return blockState.getValue(LiquidBlock.LEVEL) == 0 ? "SOURCE" : "FLOWING";
		if (level != null && blockState.getFluidState().getType() != Fluids.EMPTY && blockState.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty()) {
			return "SOURCE";
		}
		return "NONE";
	}

	protected void search(Fluid fluid, List<BlockPosEntry> frontier, Set<BlockPos> visited) throws ChunkNotLoadedException {
		int maxBlocks = maxBlocks();
		int maxRange = maxRange();
		int maxRangeSq = maxRange * maxRange;

		for (int i = 0; i < searchedPerTick && !frontier.isEmpty() && (visited.size() <= maxBlocks); i++) {
			BlockPosEntry entry = frontier.remove(0);
			BlockPos currentPos = entry.pos();
			if (visited.contains(currentPos)) continue; else visited.add(currentPos);
			if (level != null && !level.isLoaded(currentPos)) throw new ChunkNotLoadedException();

			FluidState fluidState = level != null ? level.getFluidState(currentPos) : null;
			if (fluidState == null || fluidState.isEmpty()) continue;
			Fluid currentFluid = FluidHelper.convertToStill(fluidState.getType());
			if (!currentFluid.isSame(fluid)) continue;

			for (Direction side : Iterate.directions) {
				BlockPos offsetPos = currentPos.relative(side);
				if (!level.isLoaded(offsetPos)) throw new ChunkNotLoadedException();
				if (visited.contains(offsetPos) || offsetPos.distSqr(root) > maxRangeSq) continue;
				FluidState nextFluidState = level.getFluidState(offsetPos);
				if (nextFluidState.isEmpty()) continue;
				if (Objects.equals(canPullFluidsFrom(level.getBlockState(offsetPos), offsetPos), "SOURCE")) {
					frontier.add(new BlockPosEntry(offsetPos, entry.distance() + 1));
				}
			}
		}
	}

	private void continueSearch(Fluid fluid) {
		try {
			search(fluid, frontier, visited);
		} catch (ChunkNotLoadedException e) {
			sendData();
			frontier.clear();
			visited.clear();
			setLongValidationTimer();
			return;
		}
		int maxBlocks = maxBlocks();
		if (visited.size() >= maxBlocks) {
			frontier.clear();
			if (!infinite) {
				infinite = true;
				visited.clear();
				sendData();
				determineAndApplyFlowScore();
			}
			setLongValidationTimer();
			return;
		}
		if (!frontier.isEmpty()) return;
		if (infinite) {
			reset();
			determineAndApplyFlowScore();
			return;
		}
		setValidationTimer();
		sendData();
		visited.clear();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide()) return;
		if (!inBiome || !hasValidSource) return;
		if (!frontier.isEmpty() && level != null) {
			Fluid fluid = level.getFluidState(root).getType();
			if (fluid != Fluids.EMPTY) continueSearch(fluid);
			return;
		}
		if (revalidateIn > 0) revalidateIn--;
		if (frontier.isEmpty() && revalidateIn == 0) {
			visited.clear();
			frontier.add(new BlockPosEntry(root, 0));
		}
	}

	public boolean isPowerSourceViable() {
		if (getSize() == 1 && powerSource.size() == 1) return true;
		if (getSize() == 2 && powerSource.size() <= 3  && powerSource.size() > 0) {
			BlockPos first = null;
			int check = 0, count;
			for (BlockPos pos : powerSource) {
				count = 0;
				if (first == null) first = pos;
				if (first.getX() == pos.getX()) count++;
				if (first.getY() == pos.getY()) count++;
				if (first.getZ() == pos.getZ()) count++;
				if (count >= 2) check++;
			}
			return check == powerSource.size();
		}
		return false;
	}

	public void determineViability() {
		if (level == null) return;

		inBiome = level.getBiome(worldPosition).is(PickyTags.PICKY_WATERWHEELS);

		powerSource.clear();
		for (BlockPos blockPos : getOffsetsToCheck()) {
			BlockPos targetPos = blockPos.offset(worldPosition);
			if (Objects.equals(canPullFluidsFrom(level.getBlockState(targetPos), targetPos), "SOURCE")) {
				powerSource.add(targetPos);
				isLava |= FluidHelper.isLava(level.getFluidState(targetPos).getType());
			}
		}
		root = powerSource.size() > 0 ? powerSource.get(0) : worldPosition;
		hasValidSource = isPowerSourceViable();
	}

	/**
	* @author ZehMaria
	* @reason Changing Water Wheel to require a big body of water [like the Hose Pulley] and a River Biomes.
	*/
	@Overwrite
	public void determineAndApplyFlowScore() {
		determineViability();
		setFlowScoreAndUpdate(inBiome && hasValidSource && infinite ? 1 : 0);
		if (level != null && inBiome && hasValidSource && infinite && !level.isClientSide())
			award(isLava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (!inBiome) TooltipHelper.addHint(tooltip, "hint.waterwheel_river");
		if (!hasValidSource && inBiome) TooltipHelper.addHint(tooltip, "hint.waterwheel_source");
		if (!infinite && inBiome && hasValidSource) TooltipHelper.addHint(tooltip, "hint.waterwheel_infinite");

		return addToGoggleTooltip;
	}
	@Inject(method = "write", at = @At("TAIL"))
	private void write(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
		if (infinite) NBTHelper.putMarker(nbt, "Infinite");
		if (inBiome) NBTHelper.putMarker(nbt, "InBiome");
		if (hasValidSource) NBTHelper.putMarker(nbt, "HasValidSource");
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void read(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
		infinite = nbt.contains("Infinite");
		inBiome = nbt.contains("InBiome");
		hasValidSource = nbt.contains("HasValidSource");
	}

	@Shadow
	public abstract void setFlowScoreAndUpdate(int score);

	@Shadow
	protected abstract int getSize();

	@Shadow
	protected abstract Set<BlockPos> getOffsetsToCheck();

	@Shadow
	public int flowScore;
}
