package zeh.createpickywheels.mixin;

import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour.ChunkNotLoadedException;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zeh.createpickywheels.CreatePickyWheels;
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
	boolean createPickyWheels$biomeBoosted = false;
	@Unique
	boolean createPickyWheels$hasValidSource = false;

	@Unique
	public BlockPos createPickyWheels$root;

	@Unique
	private final int createPickyWheels$searchedPerTick = 256;
	@Unique
	List<BlockPosEntry> createPickyWheels$frontier = new ArrayList<>();
	@Unique
	Set<BlockPos> createPickyWheels$visited = new HashSet<>();

	@Unique
	int createPickyWheels$revalidateIn = 1;

	@Unique
	List<BlockPos> createPickyWheels$powerSource = new ArrayList<>();

	@Unique
	protected float createPickyWheels$biomeRPMMulti = 0;

	@Unique
	protected float createPickyWheels$optimalRPMMulti = 0;

	@Unique
	protected float createPickyWheels$biomeSTRESSMulti = 0;

	@Unique
	protected float createPickyWheels$optimalSTRESSMulti = 0;

	@Unique
	protected int createPickyWheels$validationTimer() {
		int maxBlocks = createPickyWheels$maxBlocks();
		// Allow enough time for the server's infinite block threshold to be reached
		int validationTimerMin = 320;
		return maxBlocks < 0 ? validationTimerMin : Math.max(validationTimerMin, maxBlocks / createPickyWheels$searchedPerTick + 1);
	}
	@Unique
	protected void createPickyWheels$setValidationTimer() { createPickyWheels$revalidateIn = createPickyWheels$validationTimer(); }
	@Unique
	protected void createPickyWheels$setLongValidationTimer() { createPickyWheels$revalidateIn = createPickyWheels$validationTimer() * 2; }
	@Unique
	protected int createPickyWheels$maxRange() { return Configuration.WATERWHEELS_SOURCE_RANGE.get(); }
	@Unique
	protected int createPickyWheels$maxBlocks() { return Configuration.WATERWHEELS_SOURCE_THRESHOLD.get(); }
	@Unique
	protected boolean createPickyWheels$enabled() {
		return Configuration.WATERWHEELS_ENABLED.get() &&
				(!Configuration.WATERWHEELS_PICKY.get() || getBlockState().getValue(CreatePickyWheels.PICKY));
	}

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
		if (!createPickyWheels$frontier.isEmpty()) {
			Fluid fluid = level.getFluidState(createPickyWheels$root).getType();
			if (fluid != Fluids.EMPTY) createPickyWheels$continueSearch(fluid);
			return;
		}
		if (createPickyWheels$revalidateIn > 0) createPickyWheels$revalidateIn--;
		if (createPickyWheels$revalidateIn == 0) {
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
	public boolean createPickyWheels$determineViability() {
		if (level == null) return false;
		createPickyWheels$inBiome = level.getBiome(worldPosition).is(PickyTags.WATERWHEELS_WHITELIST);
		createPickyWheels$biomeBoosted = level.getBiome(worldPosition).is(PickyTags.WATERWHEELS_BOOSTED);

		createPickyWheels$biomeRPMMulti = createPickyWheels$biomeBoosted ? Configuration.WATERWHEELS_BIOME_RPM_BOOST.get().floatValue() :
				(createPickyWheels$inBiome ? Configuration.WATERWHEELS_BIOME_RPM_PENALTY.get().floatValue() : 0);
		createPickyWheels$biomeSTRESSMulti = createPickyWheels$biomeBoosted ? Configuration.WATERWHEELS_BIOME_STRESS_BOOST.get().floatValue() :
				(createPickyWheels$inBiome ? Configuration.WATERWHEELS_BIOME_STRESS_PENALTY.get().floatValue() : 0);

		createPickyWheels$powerSource.clear();
        Vec3 wheelPlane = Vec3.atLowerCornerOf(new Vec3i(1, 1, 1).subtract(Direction.get(AxisDirection.POSITIVE, getAxis()).getNormal()));
        int flowS = 0;
		boolean lava = false;

        for (BlockPos blockPos : getOffsetsToCheck()) {
            BlockPos targetPos = blockPos.offset(worldPosition);
            if (Objects.equals(createPickyWheels$canPullFluidsFrom(level.getBlockState(targetPos), targetPos), "SOURCE")) {
                if(Configuration.WATERWHEELS_SOURCE_FLOW.get()) {
                    Vec3 flowAtPos = getFlowVectorAtPosition(targetPos).multiply(wheelPlane);
                    if (flowAtPos.lengthSqr() == 0) continue;
                    flowAtPos = flowAtPos.normalize();
                    Vec3 normal = Vec3.atLowerCornerOf(blockPos).normalize();
                    Vec3 positiveMotion = VecHelper.rotate(normal, 90, getAxis());
                    double dot = flowAtPos.dot(positiveMotion);
                    if (Math.abs(dot) > .10) flowS += (int) Math.signum(dot);
                } else flowS += 1;

                createPickyWheels$powerSource.add(targetPos);
				lava |= FluidHelper.isLava(level.getFluidState(targetPos).getType());
            }
        }

		createPickyWheels$root = !createPickyWheels$powerSource.isEmpty() ? createPickyWheels$powerSource.get(0) : worldPosition;
		createPickyWheels$hasValidSource = createPickyWheels$isPowerSourceViable();

		if (createPickyWheels$isOptimal()) {
			createPickyWheels$optimalRPMMulti = Configuration.WATERWHEELS_SOURCE_RPM_BOOST.get().floatValue();
			createPickyWheels$optimalSTRESSMulti = Configuration.WATERWHEELS_SOURCE_STRESS_BOOST.get().floatValue();
			setFlowScoreAndUpdate(flowS);
			if (!level.isClientSide()) award(lava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
		} else {
			createPickyWheels$optimalRPMMulti = Configuration.WATERWHEELS_SOURCE_RPM_PENALTY.get().floatValue();
			createPickyWheels$optimalSTRESSMulti = Configuration.WATERWHEELS_SOURCE_STRESS_PENALTY.get().floatValue();
		}

		return createPickyWheels$isOptimal();
	}

	@Unique
	private boolean createPickyWheels$isOptimal() {
		return createPickyWheels$inBiome && createPickyWheels$hasValidSource && createPickyWheels$infinite;
	}

	@Unique
	private boolean createPickyWheels$isSubOptimal() {
		if (createPickyWheels$isOptimal()) return false;
		return createPickyWheels$biomeRPMMulti > 0 && createPickyWheels$optimalRPMMulti > 0 && flowScore != 0;
	}

	@Unique
	private boolean createPickyWheels$isSubOptimalBiome() {
		return createPickyWheels$biomeRPMMulti > 0 && createPickyWheels$biomeRPMMulti < Configuration.WATERWHEELS_BIOME_RPM_BOOST.get();
	}

	@Inject(method = "determineAndApplyFlowScore", at = @At("HEAD"), cancellable = true)
	private void determineAndApplyFlowScoreMixin(CallbackInfo ci) {
		if (!createPickyWheels$enabled()) return;
		if (createPickyWheels$determineViability() || !Configuration.WATERWHEELS_LOSDOS.get()) ci.cancel();
	}

	@Inject(method = "getGeneratedSpeed", at = @At("HEAD"), cancellable = true)
	public void getGeneratedSpeedMixin(CallbackInfoReturnable<Float> cir) {
		if (!createPickyWheels$enabled()) return;
		cir.setReturnValue((createPickyWheels$biomeRPMMulti * createPickyWheels$optimalRPMMulti) *
				Mth.clamp(flowScore, -1, 1) * 8 / getSize());
	}

	@Override
	public float calculateAddedStressCapacity() {
		return (createPickyWheels$biomeSTRESSMulti * createPickyWheels$optimalSTRESSMulti) * super.calculateAddedStressCapacity();
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		boolean addToGoggleTooltip = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		if (!createPickyWheels$enabled()) return addToGoggleTooltip;

		CreateLang.translate("hint.picky_efficiency_pre").style(ChatFormatting.DARK_GRAY).space()
				.add(CreateLang.number(createPickyWheels$biomeRPMMulti * createPickyWheels$biomeSTRESSMulti *
								createPickyWheels$optimalRPMMulti * createPickyWheels$optimalSTRESSMulti * Math.signum(Mth.abs(flowScore)))
						.text("x")
						.style(ChatFormatting.AQUA).space())
				.add(CreateLang.translate("hint.picky_efficiency").style(ChatFormatting.DARK_GRAY))
				.forGoggles(tooltip);

		CreateLang.number(createPickyWheels$biomeRPMMulti * createPickyWheels$biomeSTRESSMulti)
				.text("x")
				.style(ChatFormatting.DARK_AQUA).space()
				.add(CreateLang.translate("hint.picky_biome_boost").style(ChatFormatting.DARK_GRAY))
				.forGoggles(tooltip, 2);
		CreateLang.number(createPickyWheels$optimalRPMMulti * createPickyWheels$optimalSTRESSMulti * Math.signum(Mth.abs(flowScore)))
				.text("x")
				.style(ChatFormatting.DARK_AQUA).space()
				.add(CreateLang.translate("hint.picky_optimal_boost").style(ChatFormatting.DARK_GRAY))
				.forGoggles(tooltip, 2);

		if (!createPickyWheels$inBiome) {
			CreateLang.translate("hint.picky_biome_error").style(ChatFormatting.RED).forGoggles(tooltip);
		} else if (createPickyWheels$biomeBoosted) {
			CreateLang.translate("hint.picky_biome_notice").style(ChatFormatting.GREEN).forGoggles(tooltip);
		} else {
			CreateLang.translate("hint.picky_biome_warn").style(ChatFormatting.YELLOW).forGoggles(tooltip);
		}

		if (createPickyWheels$isOptimal() && flowScore != 0) {
			CreateLang.translate("hint.picky_source_notice").style(ChatFormatting.GREEN).forGoggles(tooltip);
		} else if (createPickyWheels$isSubOptimal()) {
			CreateLang.translate("hint.picky_source_warn").style(ChatFormatting.YELLOW).forGoggles(tooltip);
		} else {
			CreateLang.translate("hint.picky_source_error").style(ChatFormatting.RED).forGoggles(tooltip);
		}

		if (!createPickyWheels$hasValidSource) TooltipHelper.addHint(tooltip, "hint.waterwheel_source");
		if (!createPickyWheels$infinite && createPickyWheels$hasValidSource) TooltipHelper.addHint(tooltip, "hint.waterwheel_infinite");

		return true;
	}

	@Inject(method = "write", at = @At("TAIL"))
	private void write(CompoundTag compound, boolean clientPacket, CallbackInfo info) {
		if (!createPickyWheels$enabled()) return;
		compound.putFloat("biomeRPMMulti", createPickyWheels$biomeRPMMulti);
		compound.putFloat("optimalRPMMulti", createPickyWheels$optimalRPMMulti);
		compound.putFloat("biomeSTRESSMulti", createPickyWheels$biomeSTRESSMulti);
		compound.putFloat("optimalSTRESSMulti", createPickyWheels$optimalSTRESSMulti);
		compound.putBoolean("Infinite", createPickyWheels$infinite);
		compound.putBoolean("InBiome", createPickyWheels$inBiome);
		compound.putBoolean("BiomeBoosted", createPickyWheels$biomeBoosted);
		compound.putBoolean("HasValidSource", createPickyWheels$hasValidSource);
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void read(CompoundTag compound, boolean clientPacket, CallbackInfo info) {
		if (!createPickyWheels$enabled()) return;
		createPickyWheels$biomeRPMMulti = compound.getFloat("biomeRPMMulti");
		createPickyWheels$optimalRPMMulti = compound.getFloat("optimalRPMMulti");
		createPickyWheels$biomeSTRESSMulti = compound.getFloat("biomeSTRESSMulti");
		createPickyWheels$optimalSTRESSMulti = compound.getFloat("optimalSTRESSMulti");
		createPickyWheels$infinite = compound.getBoolean("Infinite");
		createPickyWheels$inBiome = compound.getBoolean("InBiome");
		createPickyWheels$biomeBoosted = compound.getBoolean("BiomeBoosted");
		createPickyWheels$hasValidSource = compound.getBoolean("HasValidSource");
	}

	@Shadow public int flowScore;
	@Shadow public abstract void setFlowScoreAndUpdate(int score);
	@Shadow protected abstract int getSize();
    @Shadow protected abstract Direction.Axis getAxis();
    @Shadow public abstract Vec3 getFlowVectorAtPosition(BlockPos targetPos);
	@Shadow protected abstract Set<BlockPos> getOffsetsToCheck();
	@Shadow public abstract void determineAndApplyFlowScore();
}
