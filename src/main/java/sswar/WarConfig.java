package sswar;

import net.minecraftforge.common.ForgeConfigSpec;

public class WarConfig {

    /*
    -RANDOM_WAR_INTERVAL (default 30 min)
    -PLAYER_WAR_COOLDOWN (default 30 min)
    -RECRUIT_TIMEOUT (default 2 min)
    -RECRUIT_DURATION (default 4 min)
    -PREPARATION_DURATION (default 10 min)
    -COMPASS_UNCERTAINTY_DISTANCE (default 30 blocks)
     */

    public final ForgeConfigSpec.IntValue RANDOM_WAR_INTERVAL;
    public final ForgeConfigSpec.IntValue PLAYER_WAR_COOLDOWN;

    public WarConfig(final ForgeConfigSpec.Builder builder) {
        builder.push("options");
        RANDOM_WAR_INTERVAL = builder.comment("Number of minutes between server war events")
                .defineInRange("random_war_interval", 30, 0, 10_080);
        PLAYER_WAR_COOLDOWN = builder.comment("Number of minutes until a player can participate in another war")
                        .defineInRange("player_war_cooldown", 30, 0, 10_080);

        builder.pop();
    }

    public long getRandomWarIntervalTicks() {
        return RANDOM_WAR_INTERVAL.get() * 60 * 20;
    }

    public long getPlayerWarCooldownTicks() {
        return PLAYER_WAR_COOLDOWN.get() * 60 * 20;
    }

}
