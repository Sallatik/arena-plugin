package sexy.minecraft.arenaplugin;

import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static sexy.minecraft.arenaplugin.Util.*;

public class PlayerStateStore {

    private Map<Player, InventorySnapshot> savedInventories = new HashMap<>();
    private Set<Player> toRestore = new HashSet<>();

    public void store(Player player) {
        saveInventory(player);
    }

    public void restore(Player player) {

        if(savedInventories.containsKey(player)) {
            toRestore.add(player);
            tryRestore(player);
        }
    }

    @Listener
    public void onPlayerRespawn(RespawnPlayerEvent event) {

        Player player = event.getTargetEntity();
        if(toRestore.contains(player))
            tryRestore(player);
    }

    @Listener
    public void onClientJoin(ClientConnectionEvent.Join event) {

        Player player = event.getTargetEntity();
        if (toRestore.contains(player))
            tryRestore(player);
    }

    private void tryRestore(Player player) {

        if(player.get(Keys.HEALTH).get() > 0 && player.isOnline())
            restoreInventory(player);

    }
    private void saveInventory(Player player) {
        savedInventories.put(player, new InventorySnapshot(player.getInventory()));
    }


    private void restoreInventory(Player player) {
        savedInventories.get(player).apply(player.getInventory());
        savedInventories.remove(player);
    }
}
