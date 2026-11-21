package com.thefolk;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TheFolkMod.MOD_ID)
public class TheFolkMod {
    public static final String MOD_ID = "thefolk";

    public TheFolkMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(modBus);
    }
}
