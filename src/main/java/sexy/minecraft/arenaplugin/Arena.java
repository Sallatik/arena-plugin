package sexy.minecraft.arenaplugin;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.*;
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

    private Map<Player, ItemStackSnapshot[]> savedInventories = new HashMap<>();

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
        saveInventories();
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

    private static final int NUM_OF_SLOTS = 41;

    private void saveInventories() {

        players.forEach(player -> {

            Slot[] slots = getInventorySlots(player);

            // stores a snapshot for every slot, from which all items can be restored
            ItemStackSnapshot[] savedSlots = new ItemStackSnapshot[NUM_OF_SLOTS];

            for(int i = 0; i < NUM_OF_SLOTS; i++) {

                savedSlots[i] = slots[i].poll() // takes ItemStack for this slot and removes it
                        .map(ItemStack::createSnapshot)
                        .orElse(ItemStackSnapshot.NONE);
            }

            savedInventories.put(player, savedSlots);
        });
    }


    void restoreInventory(Player player) {

        Slot [] slots = getInventorySlots(player);
        ItemStackSnapshot [] savedSlots = savedInventories.get(player);

        for(int i = 0; i < NUM_OF_SLOTS; i++)
            slots[i].set(savedSlots[i].createStack());

        savedInventories.remove(player);
    }

    private Slot [] getInventorySlots(Player player) {

        Slot [] slots = new Slot[NUM_OF_SLOTS];

        int i = 0;
        for (Inventory slot : player.getInventory().slots())
            slots[i++] = (Slot) slot;

        return slots;
    }

    void playerDied(Player player) {

        restoreInventory(player);
        players.remove(player);

        if (players.size() <= 1) {
            awardWinner();
            die();
        }
    }

    void awardWinner() {
        // TODO implement
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
