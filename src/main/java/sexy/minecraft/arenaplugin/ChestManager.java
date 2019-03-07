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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.plugin.meta.util.NonnullByDefault;

import java.util.*;
import java.util.stream.Collectors;

public class ChestManager {

    private final ArenaPlugin plugin;
    private Map<Player, Boolean> addingRemovingChests = new HashMap<>();
    private Set<Chest> chests = new HashSet<>();

    public ChestManager(ArenaPlugin plugin) {
        this.plugin = plugin;
    }

    public void arenaChestAddRemove(Player player, boolean removing) {
        addingRemovingChests.put(player, removing);
    }

    private void removeChest(Chest chest) {
        chests = chests.stream()
                .filter(c -> !locationEquals(c.getLocation(), chest.getLocation()))
                .collect(Collectors.toSet());
    }

    private boolean locationEquals(Location<World> a, Location<World> b) {
        return
                a.getBlockX() == b.getBlockX() &&
                        a.getBlockY() == b.getBlockY() &&
                        a.getBlockZ() == b.getBlockZ() &&
                        a.getExtent() == b.getExtent();
    }

    private void addChest(Chest chest) {
        removeChest(chest);
        chests.add(chest);
    }

    @Listener
    public void onBlockInteract(InteractBlockEvent.Primary event) {
        if (event.getSource() instanceof Player) {
			
            Player player = (Player) event.getSource();

            if (addingRemovingChests.containsKey(player)) {

                Optional<TileEntity> block = event.getTargetBlock().getLocation().get().getTileEntity();

                if (block.isPresent() && block.get().getType().equals(TileEntityTypes.CHEST)) {

                    Chest chest = (Chest) block.get();
                    if (addingRemovingChests.get(player))
                        removeChest(chest);
                    else
                        addChest(chest);
                } else
                    player.sendMessage(Text.of("not a chest!"));

                addingRemovingChests.remove(player);
				player.sendMessage(Text.of(String.valueOf(chests.size())));
            }
        }
    }
}
