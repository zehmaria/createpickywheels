package zeh.createpickywheels.common;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public class PickyTags {
    public static final TagKey<Biome> PICKY_WATERWHEELS = create("picky_waterwheels");
    public static final TagKey<Biome> PICKY_WINDMILLS = create("picky_windmills");

    private static TagKey<Biome> create(String id) {
        return TagKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(zeh.createpickywheels.CreatePickyWheels.MODID, id));
    }
}
