package zeh.createpickywheels.mixin;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.waterwheel.LargeWaterWheelBlock;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zeh.createpickywheels.CreatePickyWheels;

@Mixin(value = LargeWaterWheelBlock.class)
public abstract class LargeWaterWheelBlockMixin extends RotatedPillarKineticBlock {

    private LargeWaterWheelBlockMixin(Properties properties) {
        super(properties);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/waterwheel/LargeWaterWheelBlock;registerDefaultState(Lnet/minecraft/world/level/block/state/BlockState;)V"),
            index = 0)
    private BlockState injectedNewPickyBlockstateMixin(BlockState defaultState) {
        return defaultState.setValue(CreatePickyWheels.PICKY, false);
    }

    @ModifyArg(method = "createBlockStateDefinition", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/base/RotatedPillarKineticBlock;createBlockStateDefinition(Lnet/minecraft/world/level/block/state/StateDefinition$Builder;)V"),
            index = 0)
     private Builder<Block, BlockState> pickyWheels_CreateBlockStateDefinitionMixin(Builder<Block, BlockState> builder) {
        return builder.add(CreatePickyWheels.PICKY);
    }

    @Inject(method = "getStateForPlacement", at = @At("TAIL"), cancellable = true)
    private void pickyWheels_getStateForPlacementMixin(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        cir.setReturnValue(cir.getReturnValue().setValue(CreatePickyWheels.PICKY, true));
    }

}