package sexy.minecraft.arenaplugin;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Arena {

    public static final int MAX_PLAYERS = 10;
    public static final int MIN_PLAYERS = 2;
    public static final int DELAY = 60;        // delay before the game actually starts

    private boolean gameStarted = false;       // is set true whenever game is started
    private Set<Player> players = new HashSet<>(MIN_PLAYERS);
    private final ArenaPlugin plugin;

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
        if (players.size() == MAX_PLAYERS) {    // if max number of players, start immediately
            startGame();
            startCountDown.cancel();
        }
    }

    public void removePlayer(Player player) {

        players.remove(player);
        if (players.isEmpty())  // if last player leaves, remove the arena
            die();
    }

    void die() {   //  goes to garbage collector
        startCountDown.cancel();
        plugin.removeArena();
    }

    public void extend(int seconds) {   //  NB: parameter can be negative. Bug or feature?
        startCountDown.extend(seconds);
    }

    private void sendToAllPlayers(String text) {
        players.forEach(p -> p.sendMessage(Text.of(text)));
    }

    private void startGame() {

        gameStarted = true;
        sendToAllPlayers("now fight!");

        // here things start happening
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

}
