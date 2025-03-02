package zeh.createpickywheels.mixin;

import com.simibubi.create.infrastructure.ponder.AllCreatePonderScenes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import zeh.createpickywheels.common.Configuration;

@Mixin(value = AllCreatePonderScenes.class, remap = false)
public abstract class AllCreatePonderScenesMixin {

    @ModifyArg(method = "register",
            index = 1,
            at = @At(value = "INVOKE",
                ordinal = 2,
                target = "Lnet/createmod/ponder/api/registration/PonderSceneRegistrationHelper;addStoryBoard(Ljava/lang/Object;Ljava/lang/String;Lnet/createmod/ponder/api/scene/PonderStoryBoard;[Lnet/minecraft/resources/ResourceLocation;)Lnet/createmod/ponder/api/registration/StoryBoardEntry;")
    )
    private static String replaceSchematicsWaterWheel(String schematicPath) {
        if (schematicPath.equals("large_water_wheel") || schematicPath.equals("water_wheel")) {
            if (!Configuration.WATERWHEELS_ENABLED.get()) return schematicPath;
            return schematicPath + '2';
        }
        return schematicPath;
    }
    @ModifyArg(method = "register",
            index = 1,
            at = @At(value = "INVOKE",
                    ordinal = 3,
                    target = "Lnet/createmod/ponder/api/registration/PonderSceneRegistrationHelper;addStoryBoard(Ljava/lang/Object;Ljava/lang/String;Lnet/createmod/ponder/api/scene/PonderStoryBoard;[Lnet/minecraft/resources/ResourceLocation;)Lnet/createmod/ponder/api/registration/StoryBoardEntry;")
    )
    private static String replaceSchematicsLargeWaterWheel(String schematicPath) {
        if (schematicPath.equals("large_water_wheel") || schematicPath.equals("water_wheel")) {
            if (!Configuration.WATERWHEELS_ENABLED.get()) return schematicPath;
            return schematicPath + '2';
        }
        return schematicPath;
    }

}
