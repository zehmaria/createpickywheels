package zeh.createpickywheels;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import zeh.createpickywheels.common.Configuration;

@Mod(CreatePickyWheels.MODID)
public class CreatePickyWheels {

    public static final String MODID = "createpickywheels";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final BooleanProperty PICKY = BooleanProperty.create("picky");

    public CreatePickyWheels(ModContainer modContainer, IEventBus modEventBus) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Configuration.COMMON_CONFIG);
    }
    
}
