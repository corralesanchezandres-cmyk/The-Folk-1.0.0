package com.thefolk;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Comandos para probar el mod: /thefolk spawn <1-3> y /thefolk status */
@Mod.EventBusSubscriber(modid = TheFolkMod.MOD_ID)
public class FolkCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("thefolk")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("phase", IntegerArgumentType.integer(1, 3))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int phase = IntegerArgumentType.getInteger(ctx, "phase");
                                            boolean ok = FolkDirector.trySpawn(player, phase, true);
                                            ctx.getSource().sendSuccess(() -> Component.literal(ok
                                                    ? "The Folk (fase " + phase + ") ha aparecido."
                                                    : "No se encontro un lugar para que aparezca."), false);
                                            return ok ? 1 : 0;
                                        })))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ServerLevel level = ctx.getSource().getLevel();
                                    long day = FolkDirector.currentDay(level);
                                    int phase = FolkDirector.phaseOfDay(day);
                                    int dayInCycle = FolkDirector.dayInCycle(day);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Dia " + day + " del mundo | dia " + dayInCycle + " del ciclo | fase " + phase), false);
                                    return 1;
                                })));
    }
}
