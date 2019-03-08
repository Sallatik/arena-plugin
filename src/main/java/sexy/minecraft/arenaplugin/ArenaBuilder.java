package sexy.minecraft.arenaplugin;

import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static sexy.minecraft.arenaplugin.Util.isSameBlock;

public class ArenaBuilder {

    private final ArenaPlugin plugin;
    private int maxPlayers = 2;
    private int minPlayers = 2;
    private int delay = 0;
    private int chestInterval = 10;

    private Set<Location<World>> spawnPoints = new HashSet<>();

    public void clearPoints() {
        spawnPoints.clear();
    }

    public void setMaxPlayers(int maxPlayers) {
        if (maxPlayers <= 0)
            throw new IllegalArgumentException("max players number has to be positive");
        this.maxPlayers = maxPlayers;
    }

    public void setChestInterval(int chestInterval) {
        if (chestInterval < 0)
            throw new IllegalArgumentException("chest interval has to be positive");
        this.chestInterval = chestInterval;
    }

    public void setMinPlayers(int minPlayers) {
        if (minPlayers <= 0)
            throw new IllegalArgumentException("min players number has to be positive");
        this.minPlayers = minPlayers;
    }

    public void setDelay(int delay) {
        if (delay < 0)
            throw new IllegalArgumentException("delay has to be positive");
        this.delay = delay;
    }

    public void addSpawnPoint(Location<World> spawnPoint) {
        this.spawnPoints.add(spawnPoint);
    }

    public void removeSpawnPoint(Location<World> spawnPoint) {

        spawnPoints = spawnPoints.stream()
                .filter(point -> isSameBlock(point, spawnPoint))
                .collect(Collectors.toSet());
    }


    public Arena build() throws IllegalStateException {

        if (maxPlayers < minPlayers)
            throw new IllegalStateException("Bad configuration: max players number is " + maxPlayers + " while min players number is " + minPlayers);

        if (spawnPoints.size() < maxPlayers)
            throw new IllegalStateException("Not enough spawn points: " + spawnPoints.size() + " of " + maxPlayers);

        return new Arena(plugin, maxPlayers, minPlayers, spawnPoints, delay, chestInterval);

    }

    public ArenaBuilder(ArenaPlugin plugin) {
        this.plugin = plugin;
    }
}
