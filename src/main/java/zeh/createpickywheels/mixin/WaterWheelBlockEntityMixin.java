package zeh.createpickywheels.mixin;

import com.simibubi.create.foundation.item.TooltipHelper;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
	@Unique
	private boolean createPickyWheels$infinite = false;
	@Unique
	boolean createPickyWheels$inBiome = false;
	@Unique
	boolean createPickyWheels$hasValidSource = false;
	@Unique
	boolean createPickyWheels$isLava = false;

	@Unique
	public BlockPos createPickyWheels$root;

	@Unique
	private final int createPickyWheels$searchedPerTick = 640;
	@Unique
	List<BlockPosEntry> createPickyWheels$frontier = new ArrayList<>();
	@Unique
	Set<BlockPos> createPickyWheels$visited = new HashSet<>();

	@Unique
	int createPickyWheels$revalidateIn = 1;

	@Unique
	List<BlockPos> createPickyWheels$powerSource = new ArrayList<>();

	@Unique
	protected int createPickyWheels$validationTimer() {
		int maxBlocks = createPickyWheels$maxBlocks();
		// Allow enough time for the server's infinite block threshold to be reached
		int validationTimerMin = 200;
		return maxBlocks < 0 ? validationTimerMin : Math.max(validationTimerMin, maxBlocks / createPickyWheels$searchedPerTick + 1);
	}
	@Unique
	protected void createPickyWheels$setValidationTimer() { createPickyWheels$revalidateIn = createPickyWheels$validationTimer(); }
	@Unique
	protected void createPickyWheels$setLongValidationTimer() { createPickyWheels$revalidateIn = createPickyWheels$validationTimer() * 2; }
	@Unique
	protected int createPickyWheels$maxRange() { return Configuration.WATERWHEELS_RANGE.get(); }
	@Unique
	protected int createPickyWheels$maxBlocks() { return Configuration.WATERWHEELS_THRESHOLD.get(); }
	@Unique
	protected boolean createPickyWheels$enabled() { return Configuration.WATERWHEELS_ENABLED.get(); }

	@Unique
	public void createPickyWheels$reset() {
		createPickyWheels$setValidationTimer();
		createPickyWheels$frontier.clear();
		createPickyWheels$visited.clear();
		createPickyWheels$infinite = false;
		sendData();
	}

	@Override
	public void destroy() {
		if (createPickyWheels$enabled()) createPickyWheels$reset();
		super.destroy();
	}

	@Unique
	protected String createPickyWheels$canPullFluidsFrom(BlockState blockState, BlockPos pos) {
		if (blockState.hasProperty(BlockStateProperties.WATERLOGGED) && blockState.getValue(BlockStateProperties.WATERLOGGED)) return "SOURCE";
		if (blockState.getBlock() instanceof LiquidBlock) return blockState.getValue(LiquidBlock.LEVEL) == 0 ? "SOURCE" : "FLOWING";
		if (level != null && blockState.getFluidState().getType() != Fluids.EMPTY && blockState.getCollisionShape(level, pos, CollisionContext.empty()).isEmpty()) {
			return "SOURCE";
		}
		return "NONE";
	}

	@Unique
	protected void createPickyWheels$search(Fluid fluid, List<BlockPosEntry> frontier, Set<BlockPos> visited) throws ChunkNotLoadedException {
		int maxBlocks = createPickyWheels$maxBlocks();
		int maxRange = createPickyWheels$maxRange();
		int maxRangeSq = maxRange * maxRange;

		for (int i = 0; i < createPickyWheels$searchedPerTick && !frontier.isEmpty() && (visited.size() <= maxBlocks); i++) {
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
				if (visited.contains(offsetPos) || offsetPos.distSqr(createPickyWheels$root) > maxRangeSq) continue;
				FluidState nextFluidState = level.getFluidState(offsetPos);
				if (nextFluidState.isEmpty()) continue;
				if (Objects.equals(createPickyWheels$canPullFluidsFrom(level.getBlockState(offsetPos), offsetPos), "SOURCE")) {
					frontier.add(new BlockPosEntry(offsetPos, entry.distance() + 1));
				}
			}
		}
	}

	@Unique
	private void createPickyWheels$continueSearch(Fluid fluid) {
		try {
			createPickyWheels$search(fluid, createPickyWheels$frontier, createPickyWheels$visited);
		} catch (ChunkNotLoadedException e) {
			sendData();
			createPickyWheels$frontier.clear();
			createPickyWheels$visited.clear();
			createPickyWheels$setLongValidationTimer();
			return;
		}
		int maxBlocks = createPickyWheels$maxBlocks();
		if (createPickyWheels$visited.size() >= maxBlocks) {
			createPickyWheels$frontier.clear();
			if (!createPickyWheels$infinite) {
				createPickyWheels$infinite = true;
				createPickyWheels$visited.clear();
				sendData();
				determineAndApplyFlowScore();
			}
			createPickyWheels$setLongValidationTimer();
			return;
		}
		if (!createPickyWheels$frontier.isEmpty()) return;
		if (createPickyWheels$infinite) {
			createPickyWheels$reset();
			determineAndApplyFlowScore();
			return;
		}
		createPickyWheels$setValidationTimer();
		sendData();
		createPickyWheels$visited.clear();
	}

	@Override
	public void tick() {
		super.tick();
		if (!createPickyWheels$enabled()) return;
		if (level == null || level.isClientSide()) return;
		if (!createPickyWheels$inBiome || !createPickyWheels$hasValidSource) return;
		if (!createPickyWheels$frontier.isEmpty() && level != null) {
			Fluid fluid = level.getFluidState(createPickyWheels$root).getType();
			if (fluid != Fluids.EMPTY) createPickyWheels$continueSearch(fluid);
			return;
		}
		if (createPickyWheels$revalidateIn > 0) createPickyWheels$revalidateIn--;
		if (createPickyWheels$frontier.isEmpty() && createPickyWheels$revalidateIn == 0) {
			createPickyWheels$visited.clear();
			createPickyWheels$frontier.add(new BlockPosEntry(createPickyWheels$root, 0));
		}
	}

	@Unique
	public boolean createPickyWheels$isPowerSourceViable() {
		if (getSize() == 1 && createPickyWheels$powerSource.size() == 1) return true;
		if (getSize() == 2 && createPickyWheels$powerSource.size() <= 3  && !createPickyWheels$powerSource.isEmpty()) {
			BlockPos first = null;
			int check = 0, count;
			for (BlockPos pos : createPickyWheels$powerSource) {
				count = 0;
				if (first == null) first = pos;
				if (first.getX() == pos.getX()) count++;
				if (first.getY() == pos.getY()) count++;
				if (first.getZ() == pos.getZ()) count++;
				if (count >= 2) check++;
			}
			return check == createPickyWheels$powerSource.size();
		}
		return false;
	}

	@Unique
	public void createPickyWheels$determineViability() {
		if (level == null) return;

		createPickyWheels$inBiome = level.getBiome(worldPosition).is(PickyTags.PICKY_WATERWHEELS);

		createPickyWheels$powerSource.clear();
		for (BlockPos blockPos : getOffsetsToCheck()) {
			BlockPos targetPos = blockPos.offset(worldPosition);
			if (Objects.equals(createPickyWheels$canPullFluidsFrom(level.getBlockState(targetPos), targetPos), "SOURCE")) {
				createPickyWheels$powerSource.add(targetPos);
				createPickyWheels$isLava |= FluidHelper.isLava(level.getFluidState(targetPos).getType());
			}
		}
		createPickyWheels$root = !createPickyWheels$powerSource.isEmpty() ? createPickyWheels$powerSource.get(0) : worldPosition;
		createPickyWheels$hasValidSource = createPickyWheels$isPowerSourceViable();
	}

	@Inject(method = "determineAndApplyFlowScore", at = @At("HEAD"), cancellable = true)
	private void determineAndApplyFlowScoreMixin(CallbackInfo ci) {
		if (!createPickyWheels$enabled()) return;

		createPickyWheels$determineViability();
		setFlowScoreAndUpdate(createPickyWheels$inBiome && createPickyWheels$hasValidSource && createPickyWheels$infinite ? 1 : 0);
		if (level != null && createPickyWheels$inBiome && createPickyWheels$hasValidSource && createPickyWheels$infinite && !level.isClientSide())
			award(createPickyWheels$isLava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);

		ci.cancel();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (!createPickyWheels$enabled()) return addToGoggleTooltip;

		if (!createPickyWheels$inBiome) TooltipHelper.addHint(tooltip, "hint.waterwheel_river");
		if (!createPickyWheels$hasValidSource && createPickyWheels$inBiome) TooltipHelper.addHint(tooltip, "hint.waterwheel_source");
		if (!createPickyWheels$infinite && createPickyWheels$inBiome && createPickyWheels$hasValidSource) TooltipHelper.addHint(tooltip, "hint.waterwheel_infinite");

		return addToGoggleTooltip;
	}
	@Inject(method = "write", at = @At("TAIL"))
	private void write(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
		if (!createPickyWheels$enabled()) return;
		if (createPickyWheels$infinite) NBTHelper.putMarker(nbt, "Infinite");
		if (createPickyWheels$inBiome) NBTHelper.putMarker(nbt, "InBiome");
		if (createPickyWheels$hasValidSource) NBTHelper.putMarker(nbt, "HasValidSource");
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void read(CompoundTag nbt, boolean clientPacket, CallbackInfo info) {
		if (!createPickyWheels$enabled()) return;
		createPickyWheels$infinite = nbt.contains("Infinite");
		createPickyWheels$inBiome = nbt.contains("InBiome");
		createPickyWheels$hasValidSource = nbt.contains("HasValidSource");
	}

	@Shadow
	public abstract void setFlowScoreAndUpdate(int score);

	@Shadow
	protected abstract int getSize();

																																																																																@Shadow
	protected abstract Set<BlockPos> getOffsetsToCheck();

	@Shadow public abstract void determineAndApplyFlowScore();
}
