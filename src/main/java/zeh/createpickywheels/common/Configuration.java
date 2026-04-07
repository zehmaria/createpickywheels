package zeh.createpickywheels.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class Configuration {

    public static ForgeConfigSpec COMMON_CONFIG;

	public static ForgeConfigSpec.BooleanValue WATERWHEELS_ENABLED;
	public static ForgeConfigSpec.BooleanValue WATERWHEELS_LOSDOS;
	public static ForgeConfigSpec.BooleanValue WATERWHEELS_PICKY;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_BIOME_RPM_BOOST;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_BIOME_RPM_PENALTY;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_BIOME_STRESS_BOOST;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_BIOME_STRESS_PENALTY;
	public static ForgeConfigSpec.BooleanValue WATERWHEELS_SOURCE_FLOW;
	public static ForgeConfigSpec.BooleanValue WATERWHEELS_ACCEPTS_FLOWING;
	public static ForgeConfigSpec.IntValue WATERWHEELS_MINIMUM_SHALLOWNESS;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_SOURCE_RPM_BOOST;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_SOURCE_RPM_PENALTY;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_SOURCE_STRESS_BOOST;
	public static ForgeConfigSpec.DoubleValue WATERWHEELS_SOURCE_STRESS_PENALTY;
	public static ForgeConfigSpec.IntValue WATERWHEELS_SOURCE_THRESHOLD;
	public static ForgeConfigSpec.IntValue WATERWHEELS_SOURCE_RANGE;

	public static ForgeConfigSpec.BooleanValue WINDMILLS_ENABLED;

    public static ForgeConfigSpec.DoubleValue WINDMILLS_BASE_BOOST;
	public static ForgeConfigSpec.DoubleValue WINDMILLS_PENALTY;
	public static ForgeConfigSpec.IntValue WINDMILLS_THRESHOLD;
	public static ForgeConfigSpec.IntValue WINDMILLS_REQUIRED_RANGE;

	public static ForgeConfigSpec.IntValue WINDMILLS_REQUIRED_RANGE_POINTS;
	public static ForgeConfigSpec.IntValue WINDMILLS_MAX_RANGE;
	public static ForgeConfigSpec.DoubleValue WINDMILLS_ABOVE_PENALTY;

	public static ForgeConfigSpec.IntValue WINDMILLS_ABOVE;

    static {

		ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

		COMMON_BUILDER.comment("Waterwheel requirements").push("waterwheels");

		WATERWHEELS_ENABLED = COMMON_BUILDER.comment("Enable waterwheels modifications?")
				.define("waterwheelsEnabled", true);

		WATERWHEELS_PICKY = COMMON_BUILDER.comment("Only enable pickyness onPlacement, if the water wheel was instead generated during worldgen, it works as default.")
				.define("waterwheelsPickyOnPlacement", true);

		COMMON_BUILDER.comment("Biome impact on waterwheel").push("biome efficiency");

		WATERWHEELS_BIOME_RPM_BOOST = COMMON_BUILDER.comment("RPM Boost for Waterwheels placed on boosted biomes tag.")
				.defineInRange("waterwheelBiomeRPMBoost", 1.0, 0, Double.MAX_VALUE);

		WATERWHEELS_BIOME_RPM_PENALTY = COMMON_BUILDER.comment("RPM Penalty for Waterwheels placed on whitelisted but not on preferential biomes [waterwheel_boosted biome tag].")
				.defineInRange("waterwheelBiomeRPMPenalty", 0.75, 0, Double.MAX_VALUE);

		WATERWHEELS_BIOME_STRESS_BOOST = COMMON_BUILDER.comment("STRESS Boost for Waterwheels placed on boosted Biomes.")
				.defineInRange("waterwheelBiomeSTRESSBoost", 1.25, 0, Double.MAX_VALUE);

		WATERWHEELS_BIOME_STRESS_PENALTY = COMMON_BUILDER.comment("STRESS Penalty for Waterwheels placed on whitelisted but not on preferential biomes [waterwheel_boosted biome tag].")
				.defineInRange("waterwheelBiomeSTRESSPenalty", 0.5, 0, Double.MAX_VALUE);


		COMMON_BUILDER.pop();

		COMMON_BUILDER.comment("Body of water requirement impact (similar to infinite fluid extraction from pulley)").push("power source condition");

		WATERWHEELS_LOSDOS = COMMON_BUILDER.comment("When enabled waterwheels also work when under create's default conditions but at a penalty")
				.define("waterwheelsPorqueNoLosDos", true);

		WATERWHEELS_SOURCE_RPM_BOOST = COMMON_BUILDER.comment("Boost for Waterwheels placed on optimal conditions.")
				.defineInRange("waterwheelSourceRPMBoost", 1.5, 0, Double.MAX_VALUE);

		WATERWHEELS_SOURCE_RPM_PENALTY = COMMON_BUILDER.comment("Penalty for Waterwheels placed under create's default conditions.")
				.defineInRange("waterwheelSourceRPMPenalty", 0.5, 0, Double.MAX_VALUE);

		WATERWHEELS_SOURCE_STRESS_BOOST = COMMON_BUILDER.comment("Boost for Waterwheels placed on optimal conditions.")
				.defineInRange("waterwheelSourceSTRESSBoost", 1.25, 0, Double.MAX_VALUE);

		WATERWHEELS_SOURCE_STRESS_PENALTY = COMMON_BUILDER.comment("Penalty for Waterwheels placed under create's default conditions.")
				.defineInRange("waterwheelSourceSTRESSPenalty", 0.5, 0, Double.MAX_VALUE);

		WATERWHEELS_SOURCE_THRESHOLD = COMMON_BUILDER.comment("The minimum amount of fluid blocks the waterwheel needs to find before rotation begins.")
				.defineInRange("waterwheelSourceThreshold", 512, 1, Integer.MAX_VALUE);

		WATERWHEELS_SOURCE_RANGE = COMMON_BUILDER.comment("The maximum distance a waterwheel can consider fluid blocks from.")
				.defineInRange("waterwheelSourceRange", 64, 1, Integer.MAX_VALUE);

		WATERWHEELS_SOURCE_FLOW = COMMON_BUILDER.comment("When enabled boosted waterwheels require correct flow (like in TerraFirmaCraft's water river)")
				.define("waterwheelsSourceRequiresFlow", false);

		WATERWHEELS_ACCEPTS_FLOWING = COMMON_BUILDER.comment("When enabled waterwheels accepts flowing water in source calculation")
				.define("waterwheelsAcceptsFlowing", false);

		WATERWHEELS_MINIMUM_SHALLOWNESS = COMMON_BUILDER.comment("Minimum level of shallowness required to accept flowing water in source calculation")
				.defineInRange("waterwheelsMinimumShallowness", 3, 1, 7);

		COMMON_BUILDER.pop();

		COMMON_BUILDER.pop();

		COMMON_BUILDER.comment("#Windmill requirements").push("windmills");

		WINDMILLS_ENABLED = COMMON_BUILDER.comment("Enable windmills modifications?")
				.define("windmillsEnabled", true);

        WINDMILLS_BASE_BOOST = COMMON_BUILDER.comment("Penalty for Windmills placed on whitelisted but not on preferential biomes [windmills_boosted biome tag].")
                .defineInRange("windmillBaseBoost", 1.0, 0, Double.MAX_VALUE);

		WINDMILLS_PENALTY = COMMON_BUILDER.comment("Penalty for Windmills placed on whitelisted but not on preferential biomes [windmills_boosted biome tag].")
				.defineInRange("windmillPenalty", 0.75, 0, Double.MAX_VALUE);

		WINDMILLS_THRESHOLD = COMMON_BUILDER.comment("The minimum floor area required. Default: 1/4 of the max area [PI * 32 ^ 2]. Set this and windmillRequiredRangePoints to zero, to disable area check.")
				.defineInRange("windmillThreshold", 804, 0, Integer.MAX_VALUE);

		WINDMILLS_REQUIRED_RANGE = COMMON_BUILDER.comment("The minimum distance for it to consider a valid air current source.")
				.defineInRange("windmillRequiredRange", 24, 1, Integer.MAX_VALUE);

		WINDMILLS_REQUIRED_RANGE_POINTS = COMMON_BUILDER.comment("The minimum amount of points that must hit windmillRequiredRange. Set this and windmillThreshold to zero, to disable area check.")
				.defineInRange("windmillRequiredRangePoints", 128, 0, Integer.MAX_VALUE);

		WINDMILLS_MAX_RANGE = COMMON_BUILDER.comment("The maximum distance a windmill can consider air blocks from.")
				.defineInRange("windmillMaxRange", 32, 1, Integer.MAX_VALUE);

		WINDMILLS_ABOVE_PENALTY = COMMON_BUILDER.comment("The percentage of Generated Speed only given if windmills are raised enough.")
				.defineInRange("windmillAbovePenalty", 0.5, 0, 1);

		WINDMILLS_ABOVE = COMMON_BUILDER.comment("The height required for the full benefit from windmillAbovePenalty.")
				.defineInRange("windmillAbove", 12, 1, Integer.MAX_VALUE);


		COMMON_BUILDER.pop();

		COMMON_CONFIG = COMMON_BUILDER.build();

	}
}
