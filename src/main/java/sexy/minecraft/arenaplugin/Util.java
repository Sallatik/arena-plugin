package sexy.minecraft.arenaplugin;

import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.TileEntityTypes;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Util {

    public static boolean isSameBlock(Location<World> a, Location<World> b) {
        return
                a.getBlockX() == b.getBlockX() &&
                        a.getBlockY() == b.getBlockY() &&
                        a.getBlockZ() == b.getBlockZ() &&
                        a.getExtent() == b.getExtent();
    }

    public static Optional<Chest> getChest(Location<World> location) {
        if(location == null)
            return Optional.empty();
        Optional<TileEntity> tileEntity = location.getTileEntity();
        if(!tileEntity.isPresent())
            return Optional.empty();
        TileEntity entity = tileEntity.get();
        if(!entity.getType().equals(TileEntityTypes.CHEST))
            return Optional.empty();
        Chest chest = (Chest) entity;
        return Optional.of(chest);
    }
}
