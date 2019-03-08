package sexy.minecraft.arenaplugin;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityTypes;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static sexy.minecraft.arenaplugin.Util.getChest;
import static sexy.minecraft.arenaplugin.Util.isSameBlock;

public class ChestManager {

    private final ArenaPlugin plugin;
    private Map<Player, Boolean> addingRemovingChests = new HashMap<>();
    private Set<Chest> chests = new HashSet<>();
    private List<InventorySnapshot> sets = new ArrayList<>();
    private Set<Player> addingSets = new HashSet<>();
    private Task populator;

    public ChestManager(ArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public void arenaChestAddRemove(Player player, boolean removing) {
        addingRemovingChests.put(player, removing);
    }

    public void addInventory(Player player) { addingSets.add(player); }

    private void removeChest(Chest chest) {
        chests = chests.stream()
                .filter(c -> !isSameBlock(c.getLocation(), chest.getLocation()))
                .collect(Collectors.toSet());
    }

    public void startPopulatingChests(long interval) {

        populator = Task.builder()
                .execute(this::populateChests)
                .interval(interval, TimeUnit.SECONDS)
                .submit(plugin);
    }

    public void stopPopulatingChests() {

        if (populator != null)
            populator.cancel();
    }

    public void populateChests() {
        Random random = new Random();
        chests.forEach(chest -> {
            InventorySnapshot snapshot = sets.get(random.nextInt(sets.size()));
            snapshot.apply(chest.getInventory());
        });
    }

    public void clearSets() {
        sets.clear();
    }

    public void clearChests() {
        chests.clear();
    }

    private void addChest(Chest chest) {
        removeChest(chest);
        chests.add(chest);
    }

    @Listener
    public void onBlockInteract(InteractBlockEvent.Primary event) {

        Optional<Chest> optionalChest = getChest(event.getTargetBlock().getLocation().orElse(null));

        if(event.getSource() instanceof Player && optionalChest.isPresent()) {

            Player player = (Player) event.getSource();
            Chest chest = optionalChest.get();

            if (addingSets.contains(player)) {
                sets.add(new InventorySnapshot(chest.getInventory()));
                addingSets.remove(player);
                player.sendMessage(Text.of("inventory saved"));
            }

            if (addingRemovingChests.containsKey(player)) {
                if (addingRemovingChests.get(player)) {
                    removeChest(chest);
                    player.sendMessage(Text.of("chest removed"));
                } else {
                    addChest(chest);
                    player.sendMessage(Text.of("chest added"));
                }

                addingRemovingChests.remove(player);
            }

        }
    }
}
