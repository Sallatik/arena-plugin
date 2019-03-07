package sexy.minecraft.arenaplugin;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.util.function.Consumer;

@Plugin(
        id = "arena-plugin",
        name = "Arena Plugin",
        version = "1.0-SNAPSHOT",
        description = "Plugin for creating a PvP arena",
        authors = "sorrowloki, Sallatik",
        url = "http://minecraft.sexy"
)
public class ArenaPlugin {

    private static final int DEFAULT_EXTEND_DURATION = 20;

    @Inject
    private Logger logger;

    private ArenaBuilder arenaBuilder = new ArenaBuilder(this);
    private Arena arena = null; // is null until someone uses /arena start
    private ChestManager chestManager = new ChestManager(this);

    @Listener
    public void onGameInit(GameInitializationEvent event) {
        registerCommands();
        Sponge.getEventManager().registerListeners(this, chestManager);
    }

    private void registerCommands() {

        CommandSpec start = CommandSpec.builder()
                .executor(this::arenaStart)
                .build();

        CommandSpec join = CommandSpec.builder()
                .executor(this::arenaJoin)
                .build();

        CommandSpec leave = CommandSpec.builder()
                .executor(this::arenaLeave)
                .build();

        CommandSpec extend = CommandSpec.builder()
                .executor(this::arenaExtend)
                .arguments(
                        GenericArguments.optional(
                                GenericArguments.onlyOne(
                                        GenericArguments.integer(Text.of("duration"))
                                )
                        )
                ).build();

        CommandSpec clear = CommandSpec.builder()
                .executor((src, args) -> {
                    arenaBuilder.clearPoints();
                    return CommandResult.success();
                }).build();

        CommandSpec add = CommandSpec.builder()
                .executor(((src, args) -> arenaPointX(src, arenaBuilder::addSpawnPoint)))
                .build();

        CommandSpec remove = CommandSpec.builder()
                .executor(((src, args) -> arenaPointX(src, arenaBuilder::removeSpawnPoint)))
                .build();

        CommandSpec point = CommandSpec.builder()
                .child(clear, "clear")
                .child(add, "add")
                .child(remove, "remove")
                .build();

        CommandSpec maxPlayers = CommandSpec.builder()
                .executor((src, args) -> arenaSetX(args, "number", arenaBuilder::setMaxPlayers))
                .arguments(
                        GenericArguments.onlyOne(
                                GenericArguments.integer(Text.of("number"))
                        )
                ).build();

        CommandSpec minPlayers = CommandSpec.builder()
                .executor((src, args) -> arenaSetX(args, "number", arenaBuilder::setMinPlayers))
                .arguments(
                        GenericArguments.onlyOne(
                                GenericArguments.integer(Text.of("number"))
                        )
                ).build();

        CommandSpec delay = CommandSpec.builder()
                .executor((src, args) -> arenaSetX(args, "seconds", arenaBuilder::setDelay))
                .arguments(
                        GenericArguments.onlyOne(
                                GenericArguments.integer(Text.of("seconds"))
                        )
                ).build();

        CommandSpec set = CommandSpec.builder()
                .child(minPlayers, "min-players")
                .child(maxPlayers, "max-players")
                .child(delay, "delay")
                .build();

        CommandSpec chestAdd = CommandSpec.builder()
                .executor((src, args) -> {
                    chestManager.arenaChestAddRemove(requirePlayer(src), false);
                    return CommandResult.success();
                }).build();

        CommandSpec chestRemove = CommandSpec.builder()
                .executor((src, args) -> {
                    chestManager.arenaChestAddRemove(requirePlayer(src), true);
                    return CommandResult.success();
                }).build();

        CommandSpec chest = CommandSpec.builder()
                .child(chestAdd, "add")
                .child(chestRemove, "remove")
                .build();

        CommandSpec arena = CommandSpec.builder()
                .child(start, "start")
                .child(join, "join")
                .child(leave, "leave")
                .child(extend, "extend")
                .child(point, "point")
                .child(chest, "chest")
                .child(set, "set")
                .build();

        Sponge.getCommandManager().register(this, arena, "arena");
    }

    private Player requirePlayer(CommandSource src) throws CommandException{
        if (src instanceof Player)
            return (Player) src;
        else
            throw new CommandException(Text.of("You are not a player!"));
    }

    private void requireArenaAvailable() throws CommandException {
        if (arena == null)
            throw new CommandException(Text.of("arena does not exist"));
        else if (arena.isGameStarted())
            throw new CommandException(Text.of("game already started"));
    }

    // an utility method to prevent code duplication.
    private <T> CommandResult arenaSetX(CommandContext args, String key, Consumer<T> setter) throws CommandException {

        T value = args.<T>getOne(key).get();
        try {
            setter.accept(value);
        } catch (IllegalArgumentException e) {
            throw new CommandException(Text.of(e.getMessage()));
        }
        return CommandResult.success();
    }

    @NonnullByDefault
    private CommandResult arenaPointX(CommandSource src, Consumer<Location<World>> consumer) throws CommandException {
        Player player = requirePlayer(src);
        consumer.accept(player.getLocation());
        return CommandResult.success();
    }

    @NonnullByDefault
    private CommandResult arenaStart(CommandSource src, CommandContext args) throws CommandException {

        Player player = requirePlayer(src);

        if(arena != null)
            throw new CommandException(Text.of("Arena already exists"));

        arena = arenaBuilder.build();
        arena.startCountDown();
        arena.addPlayer(player);
        player.sendMessage(Text.of("success"));

        return CommandResult.success();
    }

    @NonnullByDefault
    private CommandResult arenaJoin(CommandSource src, CommandContext args) throws CommandException{

        Player player = requirePlayer(src);
        requireArenaAvailable();
        if (arena.contains(player))
            throw new CommandException(Text.of("You are already in this arena"));

        arena.addPlayer(player);
        player.sendMessage(Text.of("success"));

        return CommandResult.success();
    }

    @NonnullByDefault
    private CommandResult arenaLeave(CommandSource src, CommandContext args) throws CommandException{

        Player player = requirePlayer(src);
        requireArenaAvailable();
        if (!arena.contains(player))
            throw new CommandException(Text.of("you are not in this arena"));

        arena.removePlayer(player);
        player.sendMessage(Text.of("success"));
        return CommandResult.success();

    }

    @NonnullByDefault
    private CommandResult arenaExtend(CommandSource src, CommandContext args) throws CommandException{

        Player player = requirePlayer(src);
        requireArenaAvailable();
        if (!arena.contains(player))
            throw new CommandException(Text.of("you are not in this arena"));

        int duration = args.<Integer>getOne("duration").orElse(DEFAULT_EXTEND_DURATION);
        arena.extend(duration);
        player.sendMessage(Text.of("success"));
        return CommandResult.success();
    }

    void removeArena() {
        arena = null;
    }

    @Listener
    public void onDestructEntityDeath(DestructEntityEvent.Death event) {

        if (arena != null && arena.isGameStarted() && event.getTargetEntity() instanceof Player) {

            Player deadPlayer = (Player) event.getTargetEntity();
            if (arena.contains(deadPlayer)) {
                arena.playerDied(deadPlayer);
                event.setCancelled(true);
            }
        }
    }

    public Logger getLogger() {
        return logger;
    }
}
