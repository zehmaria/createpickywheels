package zeh.createpickywheels.mixin;

import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
import com.simibubi.create.foundation.ponder.PonderStoryBoardEntry;
import com.simibubi.create.foundation.ponder.PonderTag;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.simibubi.create.foundation.ponder.PonderStoryBoardEntry.PonderStoryBoard;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PonderRegistrationHelper.class, remap = false)
public abstract class PonderRegistrationHelperMixin {

    @Shadow
    public abstract ResourceLocation asLocation(String path);

    @Shadow
    public abstract PonderStoryBoardEntry addStoryBoard(ItemProviderEntry<?> component,
                                                        ResourceLocation schematicLocation,
                                                        PonderStoryBoard storyBoard,
                                                        PonderTag... tags);

    @Inject(method = "addStoryBoard(Lcom/tterrag/registrate/util/entry/ItemProviderEntry;Ljava/lang/String;Lcom/simibubi/create/foundation/ponder/PonderStoryBoardEntry$PonderStoryBoard;[Lcom/simibubi/create/foundation/ponder/PonderTag;)Lcom/simibubi/create/foundation/ponder/PonderStoryBoardEntry;",
            at = @At("HEAD"), cancellable = true)
    protected void replaceSchematics(ItemProviderEntry<?> component, String schematicPath, PonderStoryBoard storyBoard,
                                  PonderTag[] tags, CallbackInfoReturnable<PonderStoryBoardEntry> cir) {
        if (schematicPath.equals("large_water_wheel") || schematicPath.equals("water_wheel")) {
            cir.setReturnValue(addStoryBoard(component, asLocation(schematicPath + '2'), storyBoard, tags));
        }
    }

}
