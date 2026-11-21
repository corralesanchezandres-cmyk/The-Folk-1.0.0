package com.thefolk;

import com.thefolk.entity.TheFolkEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controla el ciclo de fases de The Folk y cuándo aparece.
 * Todo lo editable está en el bloque CONFIGURACIÓN.
 */
@Mod.EventBusSubscriber(modid = TheFolkMod.MOD_ID)
public class FolkDirector {

    // ================= CONFIGURACIÓN =================
    /** Duración de cada fase en días de Minecraft. */
    public static final int PHASE1_DAYS = 5;
    public static final int PHASE2_DAYS = 8;
    public static final int PHASE3_DAYS = 5;

    /** Espera entre apariciones, en ticks (20 ticks = 1 segundo). Índice = fase. */
    private static final int[] COOLDOWN_MIN = {0, 3000, 2400, 6000};
    private static final int[] COOLDOWN_MAX = {0, 6000, 6000, 12000};

    /** Distancia al jugador a la que aparece, en bloques. Índice = fase. */
    private static final int[] DIST_MIN = {0, 28, 22, 14};
    private static final int[] DIST_MAX = {0, 45, 38, 24};
    // =================================================

    private static final long DAY_TICKS = 24000L;
    private static final Map<UUID, Long> NEXT_SPAWN = new HashMap<>();

    public static long currentDay(ServerLevel level) {
        return level.getDayTime() / DAY_TICKS;
    }

    public static int phaseOfDay(long day) {
        int cycle = PHASE1_DAYS + PHASE2_DAYS + PHASE3_DAYS;
        int d = (int) (day % cycle);
        if (d < PHASE1_DAYS) return TheFolkEntity.PHASE_WATCH;
        if (d < PHASE1_DAYS + PHASE2_DAYS) return TheFolkEntity.PHASE_WALK;
        return TheFolkEntity.PHASE_CHASE;
    }

    public static int dayInCycle(long day) {
        return (int) (day % (PHASE1_DAYS + PHASE2_DAYS + PHASE3_DAYS)) + 1;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 40 != 0) return;
        if (player.isCreative() || player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;

        long now = level.getGameTime();
        Long next = NEXT_SPAWN.get(player.getUUID());
        if (next == null) {
            NEXT_SPAWN.put(player.getUUID(), now + 1200 + level.random.nextInt(2400));
            return;
        }
        if (now < next) return;

        int phase = phaseOfDay(currentDay(level));
        if (phase != TheFolkEntity.PHASE_CHASE && !isDark(level, player)) return;

        if (trySpawn(player, phase, false)) {
            int span = COOLDOWN_MAX[phase] - COOLDOWN_MIN[phase] + 1;
            NEXT_SPAWN.put(player.getUUID(), now + COOLDOWN_MIN[phase] + level.random.nextInt(span));
        } else {
            NEXT_SPAWN.put(player.getUUID(), now + 200);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        NEXT_SPAWN.clear();
    }

    private static boolean isDark(ServerLevel level, ServerPlayer player) {
        long t = level.getDayTime() % DAY_TICKS;
        boolean night = t >= 13000L && t <= 23000L;
        return night || level.getMaxLocalRawBrightness(player.blockPosition()) <= 4;
    }

    /** Intenta hacer aparecer a The Folk cerca del jugador. force = ignora límites (para el comando). */
    public static boolean trySpawn(ServerPlayer player, int phase, boolean force) {
        ServerLevel level = player.serverLevel();
        if (!force && !level.getEntitiesOfClass(TheFolkEntity.class, player.getBoundingBox().inflate(128.0D)).isEmpty()) {
            return false;
        }

        RandomSource rnd = level.random;
        Vec3 eye = player.getEyePosition();

        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0D;
            double dist = DIST_MIN[phase] + rnd.nextDouble() * (DIST_MAX[phase] - DIST_MIN[phase]);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);

            BlockPos spot = findStandingSpot(level, x, z, player.blockPosition().getY());
            if (spot == null) continue;

            boolean relax = force && attempt >= 25;
            if (!relax) {
                Vec3 head = new Vec3(spot.getX() + 0.5D, spot.getY() + 2.5D, spot.getZ() + 0.5D);
                boolean visible = level.clip(new ClipContext(eye, head, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
                // Fase 3 aparece escondido; las otras aparecen a la vista.
                if (phase == TheFolkEntity.PHASE_CHASE && visible) continue;
                if (phase != TheFolkEntity.PHASE_CHASE && !visible) continue;
            }

            TheFolkEntity folk = ModEntities.THE_FOLK.get().create(level);
            if (folk == null) return false;
            folk.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, rnd.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(folk)) continue;

            folk.setPhase(phase);
            level.addFreshEntity(folk);
            return true;
        }
        return false;
    }

    private static BlockPos findStandingSpot(ServerLevel level, int x, int z, int aroundY) {
        if (!level.hasChunkAt(new BlockPos(x, aroundY, z))) return null;
        int minY = Math.max(level.getMinBuildHeight() + 1, aroundY - 10);
        int maxY = Math.min(level.getMaxBuildHeight() - 4, aroundY + 10);
        for (int y = maxY; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) continue;
            if (level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.above(2)).isAir()) {
                return pos;
            }
        }
        return null;
    }
}
