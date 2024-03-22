package zeh.createpickywheels;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;
import zeh.createpickywheels.common.Configuration;

@Mod(zeh.createpickywheels.CreatePickyWheels.MODID)
public class CreatePickyWheels {

    public static final String MODID = "createpickywheels";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final BooleanProperty PICKY = BooleanProperty.create("picky");

    public CreatePickyWheels() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Configuration.COMMON_CONFIG);
        MinecraftForge.EVENT_BUS.register(this);
    }

}
