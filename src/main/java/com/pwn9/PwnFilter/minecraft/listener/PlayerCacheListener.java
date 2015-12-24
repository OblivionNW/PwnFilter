package com.pwn9.PwnFilter.minecraft.listener;

import com.pwn9.PwnFilter.minecraft.PwnFilterPlugin;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;

/**
 * Listeners for the DataCache
 * User: ptoal
 * Date: 13-11-13
 * Time: 10:34 AM
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class PlayerCacheListener {

    /**
     * <p>onPlayerQuit.</p>
     *
     * @param event a {@link org.spongepowered.api.event.network.ClientConnectionEvent.Disconnect} object.
     */
    @Listener(order = Order.LAST)
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        // Cleanup player messages on quit
        if (PwnFilterPlugin.lastMessage.containsKey(event.getTargetEntity().getUniqueId())) {
            PwnFilterPlugin.lastMessage.remove(event.getTargetEntity().getUniqueId());
        }
    }

//    /**
//     * <p>onPlayerJoin.</p>
//     *
//     * @param event a {@link org.bukkit.event.player.PlayerJoinEvent} object.
//     */
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        // In future, might want to load points data?
//    }

}
