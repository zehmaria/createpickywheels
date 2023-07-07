package zeh.createpickywheels.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

public class PickyTags {
    public static final TagKey<Biome> PICKY_WATERWHEELS = create("picky_waterwheels");
    public static final TagKey<Biome> PICKY_WINDMILLS = create("picky_windmills");

    private static TagKey<Biome> create(String id) {
        return TagKey.create(ForgeRegistries.BIOMES.getRegistryKey(), new ResourceLocation(zeh.createpickywheels.CreatePickyWheels.MODID, id));
    }
}
