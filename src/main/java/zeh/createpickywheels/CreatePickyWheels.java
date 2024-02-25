package zeh.createpickywheels;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import zeh.createpickywheels.common.CommonSetup;
import zeh.createpickywheels.common.Configuration;

import org.slf4j.Logger;

@Mod(zeh.createpickywheels.CreatePickyWheels.MODID)
public class CreatePickyWheels {

    public static final String MODID = "createpickywheels";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreatePickyWheels() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(CommonSetup::init);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Configuration.COMMON_CONFIG);

        MinecraftForge.EVENT_BUS.register(this);
    }

}
