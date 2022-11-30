package sswar;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import sswar.capability.IWarMember;
import sswar.capability.WarMember;
import sswar.data.TeamSavedData;
import sswar.data.WarSavedData;
import sswar.util.MessageUtils;
import sswar.war.War;
import sswar.war.WarState;
import sswar.war.recruit.WarRecruit;
import sswar.war.recruit.WarRecruitEntry;
import sswar.war.team.WarTeam;
import sswar.war.team.WarTeamEntry;
import sswar.war.team.WarTeams;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class WarCommands {

    private static final DynamicCommandExceptionType PLAYER_IS_IN_WAR = new DynamicCommandExceptionType(o -> MessageUtils.component("command.exception.player_is_in_war", o));
    private static final SimpleCommandExceptionType PLAYER_SELF_IS_IN_WAR = new SimpleCommandExceptionType(MessageUtils.component("command.exception.player_self_is_in_war"));
    private static final DynamicCommandExceptionType PLAYER_IS_NOT_IN_WAR = new DynamicCommandExceptionType(o -> MessageUtils.component("command.exception.player_is_not_in_war", o));
    private static final SimpleCommandExceptionType PLAYER_SELF_IS_NOT_IN_WAR = new SimpleCommandExceptionType(MessageUtils.component("command.exception.player_self_is_not_in_war"));

    private static final String WAR = "war";

    public static void register(final RegisterCommandsEvent event) {

        // accept
        event.getDispatcher().register(Commands.literal(WAR)
                .then(Commands.literal("accept")
                        .executes(context -> accept(context.getSource()))));

        // deny
        event.getDispatcher().register(Commands.literal(WAR)
                .then(Commands.literal("deny")
                        .executes(context -> deny(context.getSource()))));

        // forfeit (alias ff)
        event.getDispatcher().register(Commands.literal(WAR)
                .then(Commands.literal("ff")
                        .executes(context -> forfeit(context.getSource())))
                .then(Commands.literal("forfeit")
                        .executes(context -> forfeit(context.getSource()))));

        // invite
        event.getDispatcher().register(Commands.literal(WAR)
                .then(Commands.literal("invite")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> invite(context.getSource(), EntityArgument.getPlayer(context, "target"))))));

        // list
        event.getDispatcher().register(Commands.literal(WAR)
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource()))));

        // stats
        event.getDispatcher().register(Commands.literal(WAR)
                .then(Commands.literal("stats")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> stats(context.getSource(), EntityArgument.getPlayers(context, "targets"))))));

        // start
        event.getDispatcher().register(Commands.literal(WAR)
                .then(Commands.literal("declare")
                        .executes(context -> declare(context.getSource(), WarUtils.MAX_PLAYER_COUNT))
                        .then(Commands.argument("max_players", IntegerArgumentType.integer(2))
                                .executes(context -> declare(context.getSource(), IntegerArgumentType.getInteger(context, "max_players"))))));
    }

    private static int accept(final CommandSourceStack context) throws CommandSyntaxException {
        // load target data
        final ServerPlayer target = context.getPlayerOrException();
        final IWarMember targetWar = target.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(targetWar.hasActiveWar()) {
            throw PLAYER_IS_IN_WAR.create(target.getDisplayName().getString());
        }
        // load data
        final WarSavedData data = WarSavedData.get(context.getServer());
        // determine current recruit
        UUID warId = null;
        WarRecruit recruit = null;
        for(Map.Entry<UUID, WarRecruit> entry : data.getRecruits().entrySet()) {
            Optional<WarRecruitEntry> oRecruit = entry.getValue().getEntry(target.getUUID());
            if(oRecruit.isPresent() && !oRecruit.get().getState().isAccepted() && entry.getValue().isFull()) {
                context.sendFailure(MessageUtils.component("command.war.accept.failure.max_players", entry.getValue().getMaxPlayers(), entry.getValue().getMaxPlayers()));
                return 0;
            }
            // attempt to accept recruit request
            if(entry.getValue().accept(target.getUUID())) {
                warId = entry.getKey();
                recruit = entry.getValue();
                // save data
                data.setDirty();
                break;
            }
        }
        // verify recruit request exists
        if(null == warId || null == recruit) {
            context.sendFailure(MessageUtils.component("command.exception.no_recruit_exists"));
            return 0;
        }
        // update war
        WarUtils.onPlayerAcceptRecruit(target, data, warId);
        // send feedback to target
        context.sendSuccess(MessageUtils.component("command.war.accept.success"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int deny(final CommandSourceStack context) throws CommandSyntaxException {
        // load target data
        final ServerPlayer target = context.getPlayerOrException();
        final IWarMember targetWar = target.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(targetWar.hasActiveWar()) {
            throw PLAYER_IS_IN_WAR.create(target.getDisplayName().getString());
        }
        // load data
        final WarSavedData data = WarSavedData.get(context.getServer());
        // determine current recruit
        UUID warId = null;
        WarRecruit recruit = null;
        for(Map.Entry<UUID, WarRecruit> entry : data.getRecruits().entrySet()) {
            // attempt to deny recruit request
            if(entry.getValue().deny(target.getUUID())) {
                warId = entry.getKey();
                recruit = entry.getValue();
                // save data
                data.setDirty();
                break;
            }
        }
        // verify recruit request exists
        if(null == warId || null == recruit) {
            context.sendFailure(MessageUtils.component("command.exception.no_recruit_exists"));
            return 0;
        }
        // update war
        WarUtils.onPlayerDenyRecruit(target, data, warId);
        // send feedback to target
        context.sendSuccess(MessageUtils.component("command.war.deny.success"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int forfeit(final CommandSourceStack context) throws CommandSyntaxException {
        // determine current war
        final ServerPlayer player = context.getPlayerOrException();
        final IWarMember playerWar = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(!playerWar.hasActiveWar()) {
            throw PLAYER_SELF_IS_NOT_IN_WAR.create();
        }
        final UUID warId = playerWar.getActiveWar();
        // load war team data
        final TeamSavedData teamData = TeamSavedData.get(context.getServer());
        final Optional<WarTeams> oTeams = teamData.getTeams(warId);
        if(oTeams.isEmpty()) {
            SSWar.LOGGER.error("[WarCommands#forfeit] Failed to locate war teams instance for war with ID " + warId);
            return 0;
        }
        // load war team and entry
        final WarTeams teams = oTeams.get();
        final Optional<Pair<WarTeam, WarTeamEntry>> oEntry = teams.getTeamAndEntryForPlayer(player.getUUID());
        if(oEntry.isEmpty()) {
            SSWar.LOGGER.error("[WarCommands#forfeit] Failed to determine player team for war with ID " + warId);
            return 0;
        }
        final WarTeam team = oEntry.get().getFirst();
        final WarTeamEntry entry = oEntry.get().getSecond();
        // check if player is already forfeit
        if(entry.isForfeit()) {
            context.sendFailure(MessageUtils.component("command.war.forfeit.failure"));
            return 0;
        }
        entry.setForfeit(true);
        // save data
        teamData.setDirty();
        // update war
        WarUtils.onPlayerForfeit(player, teamData, teams, team, entry);
        // send feedback
        context.sendSuccess(MessageUtils.component("command.war.forfeit.success"), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int invite(final CommandSourceStack context, final ServerPlayer target) throws CommandSyntaxException {
        // determine current war
        final ServerPlayer player = context.getPlayerOrException();
        final IWarMember playerWar = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(!playerWar.hasActiveWar()) {
            throw PLAYER_SELF_IS_NOT_IN_WAR.create();
        }
        final UUID warId = playerWar.getActiveWar();
        // load target data
        final IWarMember targetWar = target.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(targetWar.hasActiveWar()) {
            throw PLAYER_IS_IN_WAR.create(target.getDisplayName().getString());
        }
        // load data
        final WarSavedData data = WarSavedData.get(context.getServer());
        // determine current recruit
        Optional<WarRecruit> oRecruit = data.getRecruit(warId);
        if(oRecruit.isEmpty()) {
            SSWar.LOGGER.error("[WarCommands#invite] Failed to locate war recruit instance for war with ID " + warId);
            return 0;
        }
        WarRecruit recruit = oRecruit.get();
        // check for player count
        if(recruit.isFull()) {
            context.sendFailure(MessageUtils.component("command.war.invite.failure.max_players", recruit.getMaxPlayers(), recruit.getMaxPlayers()));
            return 0;
        }
        // check for existing recruit request
        final UUID targetId = target.getUUID();
        for(WarRecruit r : data.getRecruits().values()) {
            Optional<WarRecruitEntry> entry = r.getEntry(targetId);
            if(entry.isPresent() && !entry.get().getState().isRejected()) {
                context.sendFailure(MessageUtils.component("command.exception.player_has_pending_recruit", target.getDisplayName().getString()));
                return 0;
            }
        }
        // add recruit request
        recruit.getInvitedPlayers().put(targetId, new WarRecruitEntry(context.getLevel().getGameTime()));
        // save data
        data.setDirty();
        // send recruit message
        target.displayClientMessage(WarUtils.createRecruitComponent(), false);
        // send feedback
        context.sendSuccess(MessageUtils.component("command.war.invite.single", target.getDisplayName().getString()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int list(final CommandSourceStack context) {
        // load wars and teams
        final WarSavedData warData = WarSavedData.get(context.getServer());
        final TeamSavedData teamData = TeamSavedData.get(context.getServer());
        final Map<UUID, War> warMap = warData.getWars();
        // create feedback component
        Component feedback = Component.empty();
        feedback.getSiblings().add(MessageUtils.component("command.war.list").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
        feedback.getSiblings().add(Component.literal("\n"));
        // add count
        String key = "command.war.list.count." + (warMap.size() == 1 ? "single" : "multiple");
        feedback.getSiblings().add(MessageUtils.component(key, warMap.size())
                .withStyle(warMap.isEmpty() ? ChatFormatting.RED : ChatFormatting.GREEN));
        feedback.getSiblings().add(Component.empty().withStyle(ChatFormatting.RESET));
        // iterate over wars and teams
        for(Map.Entry<UUID, War> entry : warMap.entrySet()) {
            // get teams from team data
            teamData.getTeams(entry.getKey()).ifPresent(team -> {
                // add war component to feedback
                feedback.getSiblings().add(Component.literal("\n"));
                feedback.getSiblings().add(createWarComponent(context.getServer(), entry.getKey(), entry.getValue(), team));
                feedback.getSiblings().add(Component.literal("\n"));
            });
        }
        context.sendSuccess(feedback, false);
        return warMap.size();
    }

    private static int stats(final CommandSourceStack context, final Collection<ServerPlayer> targets) {
        Component feedback = Component.empty();
        feedback.getSiblings().add(MessageUtils.component("command.war.stats").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
        for(ServerPlayer player : targets) {
            feedback.getSiblings().add(Component.literal("\n"));
            feedback.getSiblings().add(createStatsComponent(player));
        }
        context.sendSuccess(feedback, false);
        return targets.size();
    }

    private static int declare(final CommandSourceStack context, final int maxPlayers) throws CommandSyntaxException {
        // ensure player is not currently in a war
        ServerPlayer player = context.getPlayerOrException();
        IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).orElse(WarMember.EMPTY);
        if(iWarMember.hasActiveWar()) {
            throw PLAYER_SELF_IS_IN_WAR.create();
        }
        // opens war GUI
        if(WarUtils.openWarMenu(player, maxPlayers)) {
            return Command.SINGLE_SUCCESS;
        }
        // failure
        return 0;
    }

    private static Component createStatsComponent(final ServerPlayer player) {
        final String playerId = player.getStringUUID();
        final Component message = Component.empty();
        message.getSiblings().add(MessageUtils.component("command.war.stats.player", player.getDisplayName().getString())
                .withStyle(ChatFormatting.AQUA)
                .withStyle(a -> a
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(playerId)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, playerId))));
        IWarMember iWarMember = player.getCapability(SSWar.WAR_MEMBER).resolve().orElse(WarMember.EMPTY);
        message.getSiblings().add(Component.literal("\n"));
        message.getSiblings().add(MessageUtils.component("command.war.stats.wins_and_losses", iWarMember.getWins(), iWarMember.getLosses()));
        //message.getSiblings().add(MessageUtils.component("command.war.stats.wins", iWarMember.getWins()));
        //message.getSiblings().add(Component.literal("\n"));
        //message.getSiblings().add(MessageUtils.component("command.war.stats.losses", iWarMember.getLosses()));
        return message;
    }

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static Component createWarComponent(final MinecraftServer server, final UUID warId, final War war, final WarTeams warTeams) {
        final String sWarid = warId.toString();
        Component message = Component.empty();
        // add war name
        message.getSiblings().add(MessageUtils.component("command.war.list.war", war.getName())
                .withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE)
                .withStyle(a -> a
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(sWarid)))
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, sWarid))));
        // add war state
        if(war.getState() != WarState.ACTIVE) {
            message.getSiblings().add(Component.literal(" "));
            Component warState = MessageUtils.component(war.getState().getTranslationKey());
            message.getSiblings().add(MessageUtils.component("command.war.list.state", warState.getString()).withStyle(ChatFormatting.GRAY));
        }
        // determine date
        LocalDateTime createdDate = war.getCreatedDate();
        DateTimeFormatter formatter;
        // check if the war has been active for more than 1 day
        if(LocalDateTime.now().minusDays(1).isAfter(createdDate)) {
            // use date-only format
            formatter = DATE_FORMATTER;
        } else {
            formatter = DATE_TIME_FORMATTER;
        }
        // add date
        message.getSiblings().add(Component.literal("\n").withStyle(ChatFormatting.RESET));
        message.getSiblings().add(MessageUtils.component("command.war.list.start", formatter.format(createdDate)));
        // add team A
        message.getSiblings().add(Component.literal("\n"));
        message.getSiblings().add(createTeamComponent(server, warTeams.getTeamA(), "  "));
        // add team B
        message.getSiblings().add(Component.literal("\n"));
        message.getSiblings().add(createTeamComponent(server, warTeams.getTeamB(), "  "));
        return message;
    }

    private static Component createTeamComponent(final MinecraftServer server, final WarTeam team, final String prefix) {
        Component message = Component.literal(prefix);
        // add team name
        Component teamName = MessageUtils.component("command.war.list.team", team.getName())
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE);
        message.getSiblings().add(teamName);
        // add player list
        message.getSiblings().add(Component.literal("\n" + prefix).withStyle(ChatFormatting.RESET));
        for(String playerName : team.getSortedPlayerNames(server)) {
            message.getSiblings().add(Component.literal(playerName));
            message.getSiblings().add(Component.literal(", "));
        }
        // remove trailing comma
        message.getSiblings().remove(message.getSiblings().size() - 1);
        return message;
    }
}
