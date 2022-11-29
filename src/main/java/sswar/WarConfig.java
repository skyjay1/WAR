package sswar;

import net.minecraftforge.common.ForgeConfigSpec;

public class WarConfig {

    public final ForgeConfigSpec.IntValue RANDOM_WAR_INTERVAL;
    public final ForgeConfigSpec.IntValue PLAYER_WAR_COOLDOWN;
    public final ForgeConfigSpec.IntValue RECRUIT_TIMEOUT;
    public final ForgeConfigSpec.IntValue RECRUIT_DURATION;
    public final ForgeConfigSpec.IntValue PREPARATION_DURATION;
    public final ForgeConfigSpec.IntValue COMPASS_UNCERTAINTY_DISTANCE;
    public final ForgeConfigSpec.IntValue COMPASS_UPDATE_INTERVAL;

    public WarConfig(final ForgeConfigSpec.Builder builder) {
        builder.push("options");
        RANDOM_WAR_INTERVAL = builder.comment("Number of minutes between server war events")
                .defineInRange("random_war_interval", 30, 0, 10_080);
        PLAYER_WAR_COOLDOWN = builder.comment("Number of minutes until a player can participate in another war")
                        .defineInRange("player_war_cooldown", 30, 0, 10_080);
        RECRUIT_TIMEOUT = builder.comment("Number of seconds until the recruit invitation expires")
                        .defineInRange("recruit_timeout", 120, 0, 21_600);
        RECRUIT_DURATION = builder.comment("Number of seconds until the recruitment period ends")
                .defineInRange("recruit_duration", 240, 0, 21_600);
        PREPARATION_DURATION = builder.comment("Number of minutes until the preparation period ends")
                .defineInRange("recruit_duration", 10, 0, 21_600);
        COMPASS_UNCERTAINTY_DISTANCE = builder.comment("Minimum distance to the target (in blocks) that causes the compass to spin")
                .defineInRange("compass_uncertainty_distance", 30, 0, 256);
        COMPASS_UPDATE_INTERVAL = builder.comment("Number of ticks between compass updates (increase to reduce lag)")
                .defineInRange("compass_update_interval", 20, 1, 12_000);
        builder.pop();
    }

    public long getRandomWarIntervalTicks() {
        return RANDOM_WAR_INTERVAL.get() * 60 * 20;
    }

    public long getPlayerWarCooldownTicks() {
        return PLAYER_WAR_COOLDOWN.get() * 60 * 20;
    }

    public long getRecruitTimeoutTicks() {
        return RECRUIT_TIMEOUT.get() * 20;
    }

    public long getRecruitDurationTicks() {
        return RECRUIT_DURATION.get() * 20;
    }

    public long getPreparationDurationTicks() {
        return PREPARATION_DURATION.get() * 60 * 20;
    }

}
