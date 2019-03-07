package sexy.minecraft.arenaplugin;

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

    private final int delay;       // delay before the game actually starts
    private final int maxPlayers;
    private final int minPlayers;

    private boolean gameStarted = false;       // is set true whenever game is started
    private Set<Player> players;
    private final ArenaPlugin plugin;

    // points for players to be placed when the game starts. Currently hard coded
    // TODO load from file instead
    private Set<Location<World>> spawnPoints;

    private StartCountDown startCountDown = new StartCountDown();

    public Arena(ArenaPlugin plugin, int maxPlayers, int minPlayers, Set<Location<World>> spawnPoints, int delay) {
        this.plugin = plugin;
        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.spawnPoints = new HashSet<>(spawnPoints);
        this.players = new HashSet<>(minPlayers);
        this.delay = delay;
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
        if (players.size() == maxPlayers) {

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

        PlayerStateStore playerStateStore = plugin.getPlayerStateStore();
        players.forEach(playerStateStore::store);
        // TODO restrict fast travel commands usage e.g. /spawn, /home, /warp
        // put players at starting points
        positionPlayers();
        sendToAllPlayers("now fight!");
        // TODO start game (item spawning, restoring losers inventory, determining the winner)
    }

    private void positionPlayers() {

        // linked list implements Deque interface, thus can be used as a stack
        LinkedList<Location<World>> locations = new LinkedList<>(spawnPoints);
        // shuffle locations so that they appear in random order
        Collections.shuffle(locations);
        // teleport every player to location popped from the stack
        players.forEach(p -> p.setLocation(locations.pop()));
    }

    void playerDied(Player player) {

        plugin.getPlayerStateStore().restore(player);
        players.remove(player);

        if (players.size() <= 1) {
            awardWinner();
            die();
        }
    }

    void awardWinner() {
        PlayerStateStore playerStateStore = plugin.getPlayerStateStore();
        players.forEach(playerStateStore::restore);
    }

    private class StartCountDown {

        Task timer;
        int secondsLeft;

        void cancel() {
            timer.cancel();
        }

        void start() {

            secondsLeft = delay;
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

            if (players.size() >= minPlayers)  // if there is enough players
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
}
