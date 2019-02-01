package sexy.minecraft.arenaplugin;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Arena {

    // TODO load those values from configuration file instead
    public static final int MAX_PLAYERS = 10;
    public static final int MIN_PLAYERS = 2;
    public static final int DELAY = 60;        // delay before the game actually starts

    private boolean gameStarted = false;       // is set true whenever game is started
    private Set<Player> players = new HashSet<>(MIN_PLAYERS);
    private final ArenaPlugin plugin;

    // points for players to be placed when the game starts. Currently hard coded
    // TODO load from file instead
    private Set<Location<World>> startingPoints = hardCodedStartingPoints();
    private StartCountDown startCountDown = new StartCountDown();

    public Arena(ArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startCountDown() {
        startCountDown.start();
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean contains(Player player) {
        return players.contains(player);
    }

    public void addPlayer(Player player) {

        players.add(player);

        // if max number of players, start immediately
        if (players.size() == MAX_PLAYERS) {

            startCountDown.cancel();
            startGame();
        }
    }

    public void removePlayer(Player player) {

        players.remove(player);
        // if last player leaves, remove the arena
        if (players.isEmpty())
            die();
    }

    //  go to garbage collector
    void die() {
        // stop countdown
        startCountDown.cancel();
        // set arena field in plugin to null
        plugin.removeArena();
    }

    //  NB: parameter can be negative. Bug or feature?
    public void extend(int seconds) {
        startCountDown.extend(seconds);
    }

    // convenience method for sending simple unformatted text to all players
    private void sendToAllPlayers(String text) {
        players.forEach(p -> p.sendMessage(Text.of(text)));
    }

    private void startGame() {

        gameStarted = true;

        // TODO save inventory for all players
        // TODO restrict fast travel commands usage e.g. /spawn, /home, /warp
        // put players at starting points
        positionPlayers();
        sendToAllPlayers("now fight!");
        // TODO start game (item spawning, restoring losers inventory, determining the winner)
        die();
    }

    private void positionPlayers() {

        // linked list implements Deque interface, thus can be used as a stack
        LinkedList<Location<World>> locations = new LinkedList<>(startingPoints);
        // shuffle locations so that they appear in random order
        Collections.shuffle(locations);
        // teleport every player to location popped from the stack
        players.forEach(p -> p.setLocation(locations.pop()));
    }

    private class StartCountDown {

        Task timer;
        int secondsLeft = DELAY;

        void cancel() {
            timer.cancel();
        }

        void start() {

            timer = Task.builder()
                    .interval(1, TimeUnit.SECONDS)
                    .execute(this::eachSecond)
                    .submit(plugin);
        }

        void eachSecond() {

            if (--secondsLeft <= 0) {
                timer.cancel();
                timeout();
            } else if (secondsLeft % 10 == 0 || secondsLeft < 5) {
                sendToAllPlayers(secondsLeft + ((secondsLeft == 1) ? " second left" : " seconds left"));
            }
        }

        void timeout() {

            if (players.size() >= MIN_PLAYERS)  // if there is enough players
                startGame();
            else {
                die();
                sendToAllPlayers("Can not start game: not enough players!");
            }
        }

        void extend(int seconds) {
            secondsLeft += seconds;
        }
    }

    // TODO remove when not needed
    private Set<Location<World>> hardCodedStartingPoints() {

        World world = Sponge.getServer().getWorld("world").get();
        Set<Location<World>> hardcoded = new HashSet<>();

        hardcoded.add(new Location<>(world, 1, 1, 1));
        hardcoded.add(new Location<>(world, 500, 200, 500));
        hardcoded.add(new Location<>(world, 255, 100, 255));
        // ...

        return hardcoded;
    }
}
