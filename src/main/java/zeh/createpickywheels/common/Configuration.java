package zeh.createpickywheels.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Configuration {

    public static ModConfigSpec COMMON_CONFIG;

	public static ModConfigSpec.BooleanValue WATERWHEELS_ENABLED;
	public static ModConfigSpec.BooleanValue WATERWHEELS_LOSDOS;
    public static ModConfigSpec.BooleanValue WATERWHEELS_PICKY;
	public static ModConfigSpec.DoubleValue WATERWHEELS_BIOME_RPM_BOOST;
	public static ModConfigSpec.DoubleValue WATERWHEELS_BIOME_RPM_PENALTY;
	public static ModConfigSpec.DoubleValue WATERWHEELS_BIOME_STRESS_BOOST;
	public static ModConfigSpec.DoubleValue WATERWHEELS_BIOME_STRESS_PENALTY;
	public static ModConfigSpec.BooleanValue WATERWHEELS_OPTIMAL_FLOW;
	public static ModConfigSpec.DoubleValue WATERWHEELS_OPTIMAL_RPM_BOOST;
	public static ModConfigSpec.DoubleValue WATERWHEELS_OPTIMAL_RPM_PENALTY;
	public static ModConfigSpec.DoubleValue WATERWHEELS_OPTIMAL_STRESS_BOOST;
	public static ModConfigSpec.DoubleValue WATERWHEELS_OPTIMAL_STRESS_PENALTY;
	public static ModConfigSpec.IntValue WATERWHEELS_OPTIMAL_THRESHOLD;
	public static ModConfigSpec.IntValue WATERWHEELS_OPTIMAL_RANGE;

	public static ModConfigSpec.BooleanValue WINDMILLS_ENABLED;

    public static ModConfigSpec.DoubleValue WINDMILLS_BASE_BOOST;
	public static ModConfigSpec.DoubleValue WINDMILLS_PENALTY;
	public static ModConfigSpec.IntValue WINDMILLS_THRESHOLD;
	public static ModConfigSpec.IntValue WINDMILLS_REQUIRED_RANGE;

	public static ModConfigSpec.IntValue WINDMILLS_REQUIRED_RANGE_POINTS;
	public static ModConfigSpec.IntValue WINDMILLS_MAX_RANGE;
	public static ModConfigSpec.DoubleValue WINDMILLS_ABOVE_PENALTY;

	public static ModConfigSpec.IntValue WINDMILLS_ABOVE;

    static {

		ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();

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

		COMMON_BUILDER.comment("Body of water requirement impact (similar to infinite fluid extraction from pulley)").push("optimal condition");

		WATERWHEELS_LOSDOS = COMMON_BUILDER.comment("When enabled waterwheels also work when under create's default conditions but at a penalty")
				.define("waterwheelsPorqueNoLosDos", true);

		WATERWHEELS_OPTIMAL_RPM_BOOST = COMMON_BUILDER.comment("Boost for Waterwheels placed on optimal conditions.")
				.defineInRange("waterwheelOptimalRPMBoost", 1.5, 0, Double.MAX_VALUE);

		WATERWHEELS_OPTIMAL_RPM_PENALTY = COMMON_BUILDER.comment("Penalty for Waterwheels placed under create's default conditions.")
				.defineInRange("waterwheelOptimalRPMPenalty", 0.5, 0, Double.MAX_VALUE);

		WATERWHEELS_OPTIMAL_STRESS_BOOST = COMMON_BUILDER.comment("Boost for Waterwheels placed on optimal conditions.")
				.defineInRange("waterwheelOptimalSTRESSBoost", 1.25, 0, Double.MAX_VALUE);

		WATERWHEELS_OPTIMAL_STRESS_PENALTY = COMMON_BUILDER.comment("Penalty for Waterwheels placed under create's default conditions.")
				.defineInRange("waterwheelOptimalSTRESSPenalty", 0.5, 0, Double.MAX_VALUE);

		WATERWHEELS_OPTIMAL_THRESHOLD = COMMON_BUILDER.comment("The minimum amount of fluid blocks the waterwheel needs to find before rotation begins.")
				.defineInRange("waterwheelOptimalThreshold", 512, 1, Integer.MAX_VALUE);

		WATERWHEELS_OPTIMAL_RANGE = COMMON_BUILDER.comment("The maximum distance a waterwheel can consider fluid blocks from.")
				.defineInRange("waterwheelOptimalRange", 64, 1, Integer.MAX_VALUE);

		WATERWHEELS_OPTIMAL_FLOW = COMMON_BUILDER.comment("When enabled boosted waterwheels require correct flow (like in TerraFirmaCraft's water river)")
				.define("waterwheelsOptimalRequiresFlow", false);

		COMMON_BUILDER.pop();

		COMMON_BUILDER.pop();

		COMMON_BUILDER.comment("#Windmill requirements").push("windmills");

		WINDMILLS_ENABLED = COMMON_BUILDER.comment("Enable windmills modifications?")
				.define("windmillsEnabled", true);

        WINDMILLS_BASE_BOOST = COMMON_BUILDER.comment("Penalty for Windmills placed on whitelisted but not on preferential biomes [windmills_boosted biome tag].")
                .defineInRange("windmillBaseBoost", 1.0, 0, Double.MAX_VALUE);

		WINDMILLS_PENALTY = COMMON_BUILDER.comment("Penalty for Windmills placed on whitelisted but not on preferential biomes [windmills_boosted biome tag].")
				.defineInRange("windmillPenalty", 0.75, 0, Double.MAX_VALUE);

		WINDMILLS_THRESHOLD = COMMON_BUILDER.comment("The minimum floor area required. Default: 1/4 of the max area [PI * 32 ^ 2].")
				.defineInRange("windmillThreshold", 804, 1, Integer.MAX_VALUE);

		WINDMILLS_REQUIRED_RANGE = COMMON_BUILDER.comment("The minimum length of air current required.")
				.defineInRange("windmillRequiredRange", 24, 1, Integer.MAX_VALUE);

		WINDMILLS_REQUIRED_RANGE_POINTS = COMMON_BUILDER.comment("The minimum amount of points that must hit windmillRequiredRange.")
				.defineInRange("windmillRequiredRangePoints", 128, 1, Integer.MAX_VALUE);

		WINDMILLS_MAX_RANGE = COMMON_BUILDER.comment("The maximum distance a waterwheel can consider air blocks from.")
				.defineInRange("windmillMaxRange", 32, 1, Integer.MAX_VALUE);

		WINDMILLS_ABOVE_PENALTY = COMMON_BUILDER.comment("The percentage of Generated Speed only given if windmills are raised enough.")
				.defineInRange("windmillAbovePenalty", 0.5, 0, 1);

		WINDMILLS_ABOVE = COMMON_BUILDER.comment("The height required for the full benefit from windmillAbovePenalty.")
				.defineInRange("windmillAbove", 12, 1, Integer.MAX_VALUE);


		COMMON_BUILDER.pop();

		COMMON_CONFIG = COMMON_BUILDER.build();

	}
}
