package zeh.createpickywheels.mixin;

import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
import com.simibubi.create.foundation.utility.Pointing;
import com.simibubi.create.infrastructure.ponder.scenes.KineticsScenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import zeh.createpickywheels.common.Configuration;

@Mixin(value = KineticsScenes.class, remap = false)
public abstract class WaterWheelScenesMixin {

	@Inject(method = "waterWheel", at = @At("HEAD"), cancellable = true)
	private static void waterWheelMixin(SceneBuilder scene, SceneBuildingUtil util, CallbackInfo ci) {
		if (!Configuration.WATERWHEELS_ENABLED.get()) return;
		scene.title("water_wheel", "Generating Rotational Force using Water Wheels");
		scene.configureBasePlate(0, 0, 5);
		scene.world.showSection(util.select.layer(0), Direction.UP);
		scene.idle(5);
		scene.world.showSection(util.select.fromTo(0, 1, 0, 4, 2, 4), Direction.DOWN);
		scene.world.setKineticSpeed(util.select.everywhere(), 0);
		BlockPos gaugePos = util.grid.at(1, 2, 2);
		scene.world.setKineticSpeed(util.select.everywhere(), -8);
		scene.effects.indicateSuccess(gaugePos);
		BlockPos wheel = util.grid.at(3, 2, 2);
		scene.effects.rotationSpeedIndicator(wheel);
		scene.overlay.showText(60).text("Water Wheels draw force from adjacent Water Source, if the body of water is large enough and within a River Biomes").placeNearTarget().pointAt(util.vector.topOf(wheel));
		scene.idle(10);
		AABB bb = new AABB(wheel).inflate(1 / 16f, 0, 0);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, 1, 0).contract(0, .75, 0), 80);
		scene.idle(5);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, 0, -1).contract(0, 0, -.75), 75);
		scene.idle(5);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, -1, 0).contract(0, -.75, 0), 70);
		scene.idle(5);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, 0, 1).contract(0, 0, .75), 65);
		scene.idle(75);
		scene.addKeyframe();
		scene.idle(10);
		scene.overlay.showText(50).text("Covering additional sides, or submerging it fully, will disable its kinetic output further").colored(PonderPalette.RED).placeNearTarget().pointAt(util.vector.blockSurface(wheel, Direction.NORTH));
		scene.idle(80);
		scene.addKeyframe();
		scene.idle(5);
		ItemStack crimsonPlanks = new ItemStack(Items.CRIMSON_PLANKS);
		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(wheel), Pointing.DOWN).rightClick().withItem(crimsonPlanks), 20);
		scene.idle(7);
		scene.world.modifyBlockEntity(wheel, WaterWheelBlockEntity.class, be -> be.applyMaterialIfValid(crimsonPlanks));
		scene.overlay.showText(50).text("Use wood planks on the wheel to change its appearance").colored(PonderPalette.BLUE).placeNearTarget().pointAt(util.vector.blockSurface(wheel, Direction.WEST));
		scene.idle(40);
		ItemStack birchPlanks = new ItemStack(Items.BIRCH_PLANKS);
		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(wheel), Pointing.DOWN).rightClick().withItem(birchPlanks), 20);
		scene.idle(7);
		scene.world.modifyBlockEntity(wheel, WaterWheelBlockEntity.class, be -> be.applyMaterialIfValid(birchPlanks));
		scene.idle(40);
		ItemStack junglePlanks = new ItemStack(Items.JUNGLE_PLANKS);
		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(wheel), Pointing.DOWN).rightClick().withItem(junglePlanks), 20);
		scene.idle(7);
		scene.world.modifyBlockEntity(wheel, WaterWheelBlockEntity.class, be -> be.applyMaterialIfValid(junglePlanks));
		scene.idle(20);
		scene.effects.indicateSuccess(gaugePos);

		ci.cancel();
	}

	@Inject(method = "largeWaterWheel", at = @At("HEAD"), cancellable = true)
	private static void largeWaterWheelMixin(SceneBuilder scene, SceneBuildingUtil util, CallbackInfo ci) {
		if (!Configuration.WATERWHEELS_ENABLED.get()) return;
		scene.title("large_water_wheel", "Generating Rotational Force using Large Water Wheels");
		scene.configureBasePlate(0, 0, 5);
		scene.world.showSection(util.select.layer(0), Direction.UP);
		scene.idle(5);
		scene.world.showSection(util.select.fromTo(0, 1, 0, 4, 2, 4), Direction.DOWN);
		scene.idle(10);
		scene.world.setKineticSpeed(util.select.everywhere(), 0);
		BlockPos gaugePos = util.grid.at(1, 1, 2);
		scene.idle(10);
		scene.world.setKineticSpeed(util.select.everywhere(), -4);
		scene.effects.indicateSuccess(gaugePos);
		BlockPos wheel = util.grid.at(3, 2, 2);
		scene.effects.rotationSpeedIndicator(wheel);
		scene.overlay.showText(60)
			.text("Large Water Wheels draw force from adjacent Water Source, if the body of water is large enough and within a River Biomes").placeNearTarget().pointAt(util.vector.topOf(wheel));
		scene.idle(10);
		AABB bb = new AABB(wheel).inflate(.125, 1, 1);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, 3, 0).contract(0, 2.75, 0), 80);
		scene.idle(5);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, 0, -3).contract(0, 0, -2.75), 75);
		scene.idle(5);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, -3, 0).contract(0, -2.75, 0), 70);
		scene.idle(5);
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.MEDIUM, new Object(), bb.move(0, 0, 3).contract(0, 0, 2.75), 65);
		scene.idle(75);
		scene.addKeyframe();
		scene.idle(10);
		scene.overlay.showText(50).text("Covering additional sides, or submerging it fully, will disable its kinetic output further").colored(PonderPalette.RED).placeNearTarget().pointAt(util.vector.blockSurface(wheel, Direction.NORTH));
		scene.idle(80);
		scene.idle(10);
		scene.overlay.showText(70).attachKeyFrame().text("These rotate only at half the speed of regular water wheels...").colored(PonderPalette.WHITE).placeNearTarget().pointAt(util.vector.blockSurface(gaugePos, Direction.NORTH));
		scene.idle(78);
		scene.overlay.showText(60).text("...but provide a substantially higher stress capacity").colored(PonderPalette.WHITE).placeNearTarget().pointAt(util.vector.blockSurface(gaugePos, Direction.WEST));
		scene.idle(80);
		scene.addKeyframe();
		scene.idle(5);
		BlockPos target = wheel.south().above();
		scene.idle(5);
		ItemStack crimsonPlanks = new ItemStack(Items.CRIMSON_PLANKS);
		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(target), Pointing.DOWN).rightClick().withItem(crimsonPlanks), 20);
		scene.idle(7);
		scene.world.modifyBlockEntity(wheel, WaterWheelBlockEntity.class, be -> be.applyMaterialIfValid(crimsonPlanks));
		scene.overlay.showText(50).text("Use wood planks on the wheel to change its appearance").colored(PonderPalette.BLUE).placeNearTarget().pointAt(util.vector.blockSurface(target, Direction.WEST));
		scene.idle(40);
		ItemStack birchPlanks = new ItemStack(Items.BIRCH_PLANKS);
		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(target), Pointing.DOWN).rightClick().withItem(birchPlanks), 20);
		scene.idle(7);
		scene.world.modifyBlockEntity(wheel, WaterWheelBlockEntity.class, be -> be.applyMaterialIfValid(birchPlanks));
		scene.idle(40);
		ItemStack junglePlanks = new ItemStack(Items.JUNGLE_PLANKS);
		scene.overlay.showControls(new InputWindowElement(util.vector.topOf(target), Pointing.DOWN).rightClick().withItem(junglePlanks), 20);
		scene.idle(7);
		scene.world.modifyBlockEntity(wheel, WaterWheelBlockEntity.class, be -> be.applyMaterialIfValid(junglePlanks));

		ci.cancel();
	}
}
