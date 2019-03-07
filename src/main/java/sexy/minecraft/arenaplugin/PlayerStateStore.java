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

public class PlayerStateStore {

    private static final int NUM_OF_SLOTS = 41;

    private Map<Player, ItemStackSnapshot[]> savedInventories = new HashMap<>();
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
        Slot[] slots = getInventorySlots(player);

        // stores a snapshot for every slot, from which all items can be restored
        ItemStackSnapshot[] savedSlots = new ItemStackSnapshot[NUM_OF_SLOTS];

        for (int i = 0; i < NUM_OF_SLOTS; i++) {

            savedSlots[i] = slots[i].poll() // takes ItemStack for this slot and removes it
                    .map(ItemStack::createSnapshot)
                    .orElse(ItemStackSnapshot.NONE);
        }

        savedInventories.put(player, savedSlots);
    }


    private void restoreInventory(Player player) {

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
}
