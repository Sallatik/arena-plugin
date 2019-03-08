package sexy.minecraft.arenaplugin;

import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

public class InventorySnapshot {

    private final ItemStackSnapshot [] itemStackSnapshots;

    public InventorySnapshot(Inventory inventory) {

        Slot [] slots = getInventorySlots(inventory);
        ItemStackSnapshot [] itemStackSnapshots = new ItemStackSnapshot[slots.length];

        for (int i = 0; i < slots.length; i++) {

            itemStackSnapshots[i] = slots[i].poll() // takes ItemStack for this slot and removes it
                    .map(ItemStack::createSnapshot)
                    .orElse(ItemStackSnapshot.NONE);
        }

        this.itemStackSnapshots = itemStackSnapshots;
    }

    public void apply(Inventory inventory) {

        Slot [] slots = getInventorySlots(inventory);
        for(int i = 0; i < itemStackSnapshots.length; i++)
            slots[i].set(itemStackSnapshots[i].createStack());
    }

    private Slot[] getInventorySlots(Inventory inventory) {

        List<Slot> slots = new ArrayList<>();
        for (Inventory slot : inventory.slots())
            slots.add((Slot) slot);

        return slots.toArray(new Slot[0]);
    }
}
