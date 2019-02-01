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
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

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

    private Arena arena = null; // is null until someone uses /arena start

    @Listener
    public void onGameInit(GameInitializationEvent event) {
        registerCommands();
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
                )
                .build();

        CommandSpec arena = CommandSpec.builder()
                .child(start, "start")
                .child(join, "join")
                .child(leave, "leave")
                .child(extend, "extend")
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

    @NonnullByDefault
    private CommandResult arenaStart(CommandSource src, CommandContext args) throws CommandException {

        Player player = requirePlayer(src);

        if(arena != null)
            throw new CommandException(Text.of("Arena already exists"));

        arena = new Arena(this);
        arena.addPlayer(player);
        arena.startCountDown();
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
}
