package zeh.createpickywheels.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class PickyTags {
    public static final TagKey<Biome> WATERWHEELS_WHITELIST = create("waterwheels_whitelist");
    public static final TagKey<Biome> WATERWHEELS_BOOSTED = create("waterwheels_boosted");
    public static final TagKey<Biome> WINDMILLS_WHITELIST = create("windmills_whitelist");
    public static final TagKey<Biome> WINDMILLS_BOOSTED = create("windmills_boosted");

    private static TagKey<Biome> create(String id) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(zeh.createpickywheels.CreatePickyWheels.MODID, id));
    }
}
