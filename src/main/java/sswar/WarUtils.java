package sswar;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;
import sswar.capability.IWarMember;
import sswar.capability.WarMember;
import sswar.data.TeamSavedData;
import sswar.data.WarSavedData;
import sswar.menu.DeclareWarMenu;
import sswar.menu.WarCompassMenu;
import sswar.util.MessageUtils;
import sswar.war.War;
import sswar.war.WarState;
import sswar.war.recruit.WarRecruit;
import sswar.war.team.WarTeam;
import sswar.war.team.WarTeamEntry;
import sswar.war.team.WarTeams;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WarUtils {

    public static final int MAX_PLAYER_COUNT = 1024;
    public static final int WAR_NAME_MAX_LENGTH = 24;
    public static final String WAR_NAME_REGEX = "[a-zA-Z0-9() _-]{0," + WAR_NAME_MAX_LENGTH + "}";

    public static final String WAR_NAME = "War";
    public static final String TEAM_A_NAME = "Team A";
    public static final String TEAM_B_NAME = "Team B";

    private static final String KEY_BLOCK_ENTITY_TAG = "BlockEntityTag";
    private static final String KEY_LOOT_TABLE = "LootTable";
    private static final String VICTORY_LOOT_TABLE = new ResourceLocation(SSWar.MODID, "gameplay/war_victory").toString();
    private static final String KEY_WAR_COMPASS = "WarCompass";
    private static final String KEY_TARGET = "Target";
    private static final String KEY_TARGET_FRIENDLY = "Friendly";

    /**
     * Selects a random player from the server, excluding players with the given UUIDs
     * @param server the server
     * @param blacklist the players to exclude
     * @return a random valid player, or empty if none is found
     */
    public static Optional<ServerPlayer> findValidPlayer(final MinecraftServer server, final Collection<UUID> blacklist) {
        final List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        // remove blacklisted players
        players.removeIf(p -> blacklist.contains(p.getUUID()));
        // randomize order
        Collections.shuffle(players);
        // return the first valid player
        for(ServerPlayer player : players) {
            if(isValidPlayer(player)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    /**
     * @param server the server
     * @param blacklist the players to exclude
     * @return a list of valid players, excluding the ones in the blacklist
     */
    public static List<ServerPlayer> listValidPlayers(final MinecraftServer server, final Collection<UUID> blacklist) {
        final List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        // remove blacklisted players
        players.removeIf(p -> blacklist.contains(p.getUUID()) || !isValidPlayer(p));
        return players;
    }

    /**
     * @param player the player
     * @return true if the player is not currently or recently in a war
     */
    public static boolean isValidPlayer(final ServerPlayer player) {
        Optional<IWarMember> oWarMember = player.getCapability(SSWar.WAR_MEMBER).resolve();
        if(oWarMember.isPresent()) {
            IWarMember iWarMember = oWarMember.get();
            return !iWarMember.hasActiveWar() && (iWarMember.getWarEndedTimestamp() <= 0 || player.level.getGameTime() - iWarMember.getWarEndedTimestamp() > SSWar.CONFIG.getPlayerWarCooldownTicks());
        }
        return false;
    }

    /**
     * @param server the server
     * @param uuid the player UUID
     * @return true if the player is online and valid according to {@link #isValidPlayer(ServerPlayer)}
     */
    public static boolean isValidPlayer(final MinecraftServer server, final UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if(null == player) {
            return false;
        }
        return isValidPlayer(player);
    }

    /**
     * Called when a player forfeits
     * @param player the player that forfeited
     * @param teamData the war team data
     * @param teams the war teams
     * @param team the player war team
     * @param entry the player war team entry
     */
    public static void onPlayerForfeit(final ServerPlayer player, final TeamSavedData teamData, final WarTeams teams, final WarTeam team, final WarTeamEntry entry) {
        // create message
        final int remainingForfeits = team.getTeam().size() / 2 - team.countForfeits();
        Component message = MessageUtils.component("message.war.event.forfeit", player.getDisplayName().getString(), remainingForfeits, team.getName()).withStyle(ChatFormatting.YELLOW);
        // end messages to players in the same war
        for(WarTeam t : teams) {
            for(UUID playerId : t) {
                ServerPlayer p = player.getServer().getPlayerList().getPlayer(playerId);
                if(p != null) {
                    p.displayClientMessage(message, false);
                }
            }
        }
    }

    /**
     * Called when a player dies to update death count
     * @param player the player that died
     * @param warId the ID of the active war
     */
    public static void onPlayerDeath(final ServerPlayer player, final UUID warId) {
        // load war data and active war
        final WarSavedData warData = WarSavedData.get(player.getServer());
        final Optional<War> oWar = warData.getWar(warId);
        if(oWar.isEmpty()) {
            return;
        }
        final War war = oWar.get();
        // verify war is active
        if(war.getState() != WarState.ACTIVE) {
            return;
        }
        // load war teams
        final TeamSavedData teamData = TeamSavedData.get(player.getServer());
        final Optional<WarTeams> oTeams = teamData.getTeams(warId);
        if(oTeams.isEmpty()) {
            warData.invalidateWar(warId);
            return;
        }
        final WarTeams teams = oTeams.get();
        // load player team
        final Optional<WarTeam> oTeam = teams.getTeamForPlayer(player.getUUID());
        if(oTeam.isEmpty()) {
            return;
        }
        final WarTeam team = oTeam.get();
        // load player entry
        final Optional<WarTeamEntry> oEntry = team.getEntry(player.getUUID());
        if(oEntry.isEmpty()) {
            return;
        }
        // add death count
        oEntry.get().addDeath();
        teamData.setDirty();
    }


    public static void onPlayerAcceptRecruit(final ServerPlayer player, final WarSavedData data, final UUID warId) {
        Optional<War> oWar = data.getWar(warId);
        // send feedback to war owner, if present
        oWar.ifPresent(war -> {
            if(war.hasOwner()) {
                ServerPlayer owner = player.getServer().getPlayerList().getPlayer(war.getOwner());
                if(owner != null) {
                    owner.displayClientMessage(MessageUtils.component("command.war.accept.feedback", player.getDisplayName().getString())
                            .withStyle(ChatFormatting.GREEN), false);
                }
            }
        });
    }

    public static void onPlayerDenyRecruit(final ServerPlayer player, final WarSavedData data, final UUID warId) {
        Optional<War> oWar = data.getWar(warId);
        // send feedback to war owner, if present
        oWar.ifPresent(war -> {
            if(war.hasOwner()) {
                ServerPlayer owner = player.getServer().getPlayerList().getPlayer(war.getOwner());
                if(owner != null) {
                    owner.displayClientMessage(MessageUtils.component("command.war.deny.feedback", player.getDisplayName().getString())
                            .withStyle(ChatFormatting.RED), false);
                }
            }
        });
    }

    /**
     * Loads the player if they are online and updates stats and items
     * @param server the server
     * @param playerId the player
     * @param isWin true if the player wins
     * @return true if the player was rewarded
     */
    public static boolean reward(final MinecraftServer server, final UUID playerId, final boolean isWin) {
        // load the player
        final ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if(null == player) {
            return false;
        }
        // load capability
        IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(isWin) {
            // add stats
            iWarMember.addWin();
            // create reward item
            final ItemStack itemStack = Items.CHEST.getDefaultInstance();
            CompoundTag blockEntityTag = new CompoundTag();
            blockEntityTag.putString(KEY_LOOT_TABLE, VICTORY_LOOT_TABLE);
            itemStack.getOrCreateTag().put(KEY_BLOCK_ENTITY_TAG, blockEntityTag);
            itemStack.setHoverName(MessageUtils.component("item.sswar.victory_chest").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            if(!player.addItem(itemStack)) {
                player.drop(itemStack, false);
            }
            // add firework particles
            sendFireworks(player);
        } else {
            // add stats
            iWarMember.addLoss();
        }
        // reward was successful
        return true;
    }

    private static void sendFireworks(final ServerPlayer player) {
        // create itemstack
        final ItemStack rocket = Items.FIREWORK_ROCKET.getDefaultInstance();
        // create firework compound tag
        CompoundTag rocketTag = new CompoundTag();
        rocketTag.putByte("Flight", (byte)1);
        ListTag explosions = new ListTag();
        explosions.add(createExplosionTag(FireworkRocketItem.Shape.LARGE_BALL, DyeColor.LIME));
        explosions.add(createExplosionTag(FireworkRocketItem.Shape.SMALL_BALL, DyeColor.YELLOW));
        rocketTag.put("Explosions", explosions);
        rocket.getOrCreateTag().put("Fireworks", rocketTag);
        // this method would spawn cosmetic-only firework particles, but only works client-side
        // instead, we have to create an entity to spawn the fireworks
        // player.level.createFireworks(player.getX(), player.getY() + 2.0D, player.getZ(), 0, 0, 0, tag);
        // spawn firework entity
        FireworkRocketEntity entity = new FireworkRocketEntity(player.level, player, player.getX(), player.getY() + 1.0D, player.getZ(), rocket);
        player.level.addFreshEntity(entity);
    }

    private static CompoundTag createExplosionTag(final FireworkRocketItem.Shape shape, final DyeColor color) {
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) shape.getId());
        explosion.putIntArray("Colors", List.of(color.getFireworkColor()));
        return explosion;
    }

    /**
     * @param server the server
     * @param player the player UUID
     * @return the player name if it is known, otherwise the UUID as a string
     */
    public static String getPlayerName(final MinecraftServer server, final UUID player) {
        Optional<GameProfile> oProfile = server.getProfileCache().get(player);
        if(oProfile.isPresent()) {
            return oProfile.get().getName();
        }
        return player.toString();
    }

    public static boolean openWarCompassMenu(final ServerPlayer player, final ItemStack compass) {
        // verify player is in war
        IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(!iWarMember.hasActiveWar()) {
            return false;
        }
        // load team
        TeamSavedData teamData = TeamSavedData.get(player.getServer());
        Optional<WarTeams> oTeams = teamData.getTeams(iWarMember.getActiveWar());
        if(oTeams.isEmpty()) {
            return false;
        }
        // load player and enemy team
        WarTeam playerTeam;
        WarTeam enemyTeam;
        if(oTeams.get().getTeamA().getTeam().containsKey(player.getUUID())) {
            playerTeam = oTeams.get().getTeamA();
            enemyTeam = oTeams.get().getTeamB();
        } else if(oTeams.get().getTeamB().getTeam().containsKey(player.getUUID())) {
            playerTeam = oTeams.get().getTeamB();
            enemyTeam = oTeams.get().getTeamA();
        } else {
            return false;
        }
        // create list of players to track
        List<UUID> playerList = new ArrayList<>();
        if(player.isShiftKeyDown()) {
            playerList.addAll(playerTeam.getTeam().keySet());
            playerList.remove(player.getUUID());
        } else {
            playerList.addAll(enemyTeam.getTeam().keySet());
        }
        // create list of online players
        final List<ServerPlayer> validPlayers = new ArrayList<>();
        for(UUID playerId : playerList) {
            ServerPlayer serverPlayer = player.getServer().getPlayerList().getPlayer(playerId);
            if(serverPlayer != null) {
                validPlayers.add(serverPlayer);
            }
        }
        // send message if there are no online players
        if(validPlayers.isEmpty()) {
            player.displayClientMessage(MessageUtils.component("item.sswar.war_compass.use.no_players").withStyle(ChatFormatting.RED), false);
            return false;
        }
        // create container with player heads
        final SimpleContainer playerHeads = createPlayerHeads(player, validPlayers, true);
        // open menu
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) ->
                        new WarCompassMenu(id, playerHeads), MessageUtils.component("gui.sswar.war_compass")),
                buf -> {
                    buf.writeInt(playerHeads.getContainerSize());
                    CompoundTag tag = new CompoundTag();
                    tag.put(DeclareWarMenu.KEY_VALID_PLAYER_ITEMS, playerHeads.createTag());
                    buf.writeNbt(tag);
                });
        return true;
    }

    public static boolean openWarMenu(final ServerPlayer player, final int maxPlayers) {
        // create container with player heads
        final SimpleContainer playerHeads = createPlayerHeads(player, listValidPlayers(player.getServer(), List.of(player.getUUID())), false);
        // open menu
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, p) ->
                        new DeclareWarMenu(id, playerHeads, maxPlayers), MessageUtils.component("gui.sswar.declare_war")),
                buf -> {
                    buf.writeInt(playerHeads.getContainerSize());
                    buf.writeInt(maxPlayers);
                    CompoundTag tag = new CompoundTag();
                    tag.put(DeclareWarMenu.KEY_VALID_PLAYER_ITEMS, playerHeads.createTag());
                    buf.writeNbt(tag);
                });
        return true;
    }

    public static boolean isWarCompass(final ItemStack itemStack) {
        return itemStack.is(Items.COMPASS) && itemStack.hasTag() && itemStack.getTag().getBoolean(KEY_WAR_COMPASS);
    }

    /**
     * Checks the player inventory for a war compass and updates the first one found
     * @param player the player
     */
    public static void tryUpdateWarCompass(final ServerPlayer player) {
        for(int i = 0, n = player.getInventory().getContainerSize(); i < n; i++) {
            ItemStack itemStack = player.getInventory().getItem(i);
            if(isWarCompass(itemStack)) {
                updateWarCompass(player, itemStack);
                return;
            }
        }
    }

    public static void updateWarCompassTarget(final ServerPlayer player, final ServerPlayer target) {
        ItemStack compass = player.getItemInHand(player.getUsedItemHand());
        if(isWarCompass(compass)) {
            compass.getTag().putUUID(KEY_TARGET, target.getUUID());
            updateWarCompass(player, compass);
        }
    }

    public static ItemStack makeWarCompass() {
        ItemStack itemStack = Items.COMPASS.getDefaultInstance();
        CompoundTag tag = itemStack.getOrCreateTag();
        // add tag data
        tag.putBoolean(KEY_WAR_COMPASS, true);
        tag.putBoolean("LodestoneTracked", false);
        tag.put("LodestonePos", NbtUtils.writeBlockPos(BlockPos.ZERO));
        tag.put("LodestoneDimension", new CompoundTag());
        // add display name
        itemStack.setHoverName(MessageUtils.component("item.sswar.war_compass"));
        CompoundTag display = itemStack.getOrCreateTagElement("display");
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(MessageUtils.component("item.sswar.war_compass.status_bar", "-", "-", "-"))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(MessageUtils.component("item.sswar.war_compass.tooltip.description"))));
        display.put("Lore", lore);
        return itemStack;
    }

    private static void updateWarCompass(final ServerPlayer player, final ItemStack compass) {
        // update name and lore
        // determine if this is a war compass
        if(!compass.hasTag() || !compass.getTag().contains(KEY_TARGET)) {
            return;
        }
        // determine if player is in war
        IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(!iWarMember.hasActiveWar()) {
            // remove target from tag
            compass.getTag().remove(KEY_TARGET);
            // update tag
            compass.getTag().put("LodestonePos", NbtUtils.writeBlockPos(player.blockPosition()));
            compass.getTag().put("LodestoneDimension", new CompoundTag());
        }
        // determine if target is online
        final UUID targetId = compass.getTag().getUUID(KEY_TARGET);
        final ServerPlayer target = player.getServer().getPlayerList().getPlayer(targetId);
        if(null == target) {
            // target is offline, do nothing
            return;
        }
        // target is online, determine location
        final ResourceKey<Level> targetLevel = target.level.dimension();
        final BlockPos targetPos = target.blockPosition();
        final boolean friendly = compass.getTag().getBoolean(KEY_TARGET_FRIENDLY);
        // determine whether to track target
        boolean tracked = targetLevel.equals(player.level.dimension()) && (friendly || !player.blockPosition().closerThan(targetPos, SSWar.CONFIG.COMPASS_UNCERTAINTY_DISTANCE.get()));
        if(tracked) {
            // update tag
            compass.getTag().put("LodestonePos", NbtUtils.writeBlockPos(targetPos));
            Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, targetLevel).resultOrPartial(SSWar.LOGGER::error).ifPresent((encoded) -> {
                compass.getTag().put("LodestoneDimension", encoded);
            });
        } else {
            // update tag
            compass.getTag().put("LodestonePos", NbtUtils.writeBlockPos(player.blockPosition()));
            compass.getTag().put("LodestoneDimension", new CompoundTag());
        }
        // update player status bar
        final Component message = MessageUtils.component("item.sswar.war_compass.status_bar", target.getDisplayName().getString(), targetLevel.location().getPath(), (tracked ? targetPos.getY() : "-"));
        player.displayClientMessage(message, true);
        // update lore text
        CompoundTag display = compass.getOrCreateTagElement("display");
        ListTag lore = display.getList("Lore", Tag.TAG_STRING);
        if(lore.size() > 0) {
            lore.remove(0);
        }
        lore.add(0, StringTag.valueOf(Component.Serializer.toJson(message)));
    }

    public static SimpleContainer createPlayerHeads(final ServerPlayer required, final List<ServerPlayer> validPlayers, final boolean showDeathCount) {
        // create container
        SimpleContainer container = new SimpleContainer(validPlayers.size() + 1);
        // add non-required players
        for(int i = 0, n = validPlayers.size(); i < n; i++) {
            ItemStack playerHead = createPlayerHead(validPlayers.get(i), false, showDeathCount);
            // add head item to container
            container.setItem(i, playerHead);
        }
        // required player is last
        container.addItem(createPlayerHead(required, true, showDeathCount));
        return container;
    }


    /**
     * @param player the player
     * @param required true if the player is marked as required
     * @param showDeathCount true to show the player death count
     * @return an ItemStack with a player head for the given player
     */
    public static ItemStack createPlayerHead(final ServerPlayer player, final boolean required, final boolean showDeathCount) {
        ItemStack itemStack = Items.PLAYER_HEAD.getDefaultInstance();
        GameProfile profile = player.getGameProfile();
        // write game profile
        CompoundTag tag = new CompoundTag();
        NbtUtils.writeGameProfile(tag, profile);
        itemStack.getOrCreateTag().put(DeclareWarMenu.KEY_SKULL_OWNER, tag);
        if(required) {
            itemStack.getTag().putBoolean(DeclareWarMenu.KEY_REQUIRED_PLAYER, true);
        }
        // write death count
        if(showDeathCount) {
            IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
            // TODO
        }
        return itemStack;
    }

    /**
     * Checks if either team is empty switches player teams until they are both non-empty
     * @param server the server
     * @param data the team data
     * @param teamA the first team
     * @param teamB the second team
     * @return true if the teams are valid
     */
    public static boolean tryBalanceTeams(final MinecraftServer server, final TeamSavedData data, final WarTeam teamA, final WarTeam teamB) {
        int sizeA = teamA.getTeam().size();
        int sizeB = teamB.getTeam().size();
        // do not process teams that are too small to balance
        if((sizeA <= 1 && sizeB == 0) || (sizeB <= 1 && sizeA == 0)) {
            return false;
        }
        // prepare to balance
        WarTeam from;
        WarTeam to;
        List<UUID> keySet = new ArrayList<>();
        UUID player;
        // move random players from one team to the other until sizes are roughly equal
        while(sizeA > 0 && sizeB > 0 && Mth.ceil(sizeA / 2.0F) != Mth.ceil(sizeB / 2.0F)) {
            // determine larger team
            if (sizeA > sizeB) {
                from = teamA;
                to = teamB;
            } else {
                from = teamB;
                to = teamA;
            }
            // add UUIDs to list
            keySet.clear();
            keySet.addAll(from.getTeam().keySet());
            // determine player to move
            player = Util.getRandom(keySet, server.overworld().getRandom());
            // move the player
            WarTeamEntry entry = from.getTeam().remove(player);
            to.getTeam().put(player, entry);
            data.setDirty();
            // update sizes
            sizeA = teamA.getTeam().size();
            sizeB = teamB.getTeam().size();
        }
        // teams are now balanced
        return true;
    }

    /**
     * Attempts to create war and war recruit objects from the given data
     * @param owner the player who created the war, or null for server events
     * @param warName the war name
     * @param nameA the name of team A
     * @param nameB the name of team B
     * @param listA the list of players to include in team A, can be empty
     * @param listB the list of players to include in team B, can be empty
     * @param maxPlayers the maximum number of players to participate
     * @return the war ID if it was successfully created
     */
    public static Optional<UUID> tryCreateWar(@Nullable final UUID owner, final String warName, final String nameA, final String nameB,
                                              final List<UUID> listA, final List<UUID> listB, final int maxPlayers, final boolean hasPrepPeriod) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if(null == server) {
            return Optional.empty();
        }
        // ensure all players are online and valid
        final List<UUID> teamA = new ArrayList<>(listA);
        teamA.removeIf(uuid -> !isValidPlayer(server, uuid));
        final List<UUID> teamB = new ArrayList<>(listB);
        teamB.removeIf(uuid -> !isValidPlayer(server, uuid));
        // ensure there is at least 1 player per team
        // EDIT: teams can be empty for server-created wars
        /*if(teamA.isEmpty() || teamB.isEmpty()) {
            return false;
        }*/
        // determine game time
        final long gameTime = server.getLevel(Level.OVERWORLD).getGameTime();
        // load war data
        final WarSavedData warData = WarSavedData.get(server);
        // create war
        final Pair<UUID, War> pair = warData.createWar(owner, warName, gameTime, maxPlayers);
        // load WarRecruit
        final Optional<WarRecruit> oRecruit = warData.getRecruit(pair.getFirst());
        oRecruit.ifPresent(recruit -> {
            recruit.addAll(server, warData, pair.getSecond(), teamA, gameTime);
            recruit.addAll(server, warData, pair.getSecond(), teamB, gameTime);
            // update war state
            pair.getSecond().setState(WarState.RECRUITING);
            warData.setDirty();
        });
        // load team data
        final TeamSavedData teamData = TeamSavedData.get(server);
        teamData.addTeams(pair.getFirst(), nameA, nameB, teamA, teamB);
        return Optional.of(pair.getFirst());
    }





    public static Component createRecruitComponent() {
        // create components
        Component feedback = Component.empty().withStyle(ChatFormatting.BOLD);
        feedback.getSiblings().add(MessageUtils.component("message.war.lifecycle.recruit.prompt"));
        Component yes = MessageUtils.component("message.war.lifecycle.recruit.prompt.yes")
                .withStyle(ChatFormatting.GREEN)
                .withStyle(a -> a
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, MessageUtils.component("message.war.lifecycle.recruit.prompt.yes.tooltip")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/war accept")));
        Component no = MessageUtils.component("message.war.lifecycle.recruit.prompt.no")
                .withStyle(ChatFormatting.RED)
                .withStyle(a -> a
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, MessageUtils.component("message.war.lifecycle.recruit.prompt.no.tooltip")))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/war deny")));
        // combine components
        feedback.getSiblings().add(Component.literal(" "));
        feedback.getSiblings().add(yes);
        feedback.getSiblings().add(Component.literal(" "));
        feedback.getSiblings().add(no);
        return feedback;
    }
}
