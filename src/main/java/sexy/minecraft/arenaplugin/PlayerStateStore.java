package sexy.minecraft.arenaplugin;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
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

    private static final int NUM_OF_SLOTS = 41;

    private Map<Player, InventorySnapshot> savedInventories = new HashMap<>();
    private Set<Player> toRestore = new HashSet<>();

    public void store(Player player) {
        saveInventory(player);
    }

    public void restore(Player player) {
        if(savedInventories.containsKey(player))
            toRestore.add(player);
    }

    @Listener
    public void onPlayerRespawn(RespawnPlayerEvent event) {

        Player player = event.getTargetEntity();
        if(toRestore.contains(player))
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
