
/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.minecraft.listener;

import com.pwn9.PwnFilter.minecraft.DeathMessages;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.text.Texts;

/**
 * Catch Death events to rewrite them with a custom message.
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class PwnFilterEntityListener {

    /**
     * <p>onEntityDeath.</p>
     *
     * @param event a {@link DestructEntityEvent.Death} object.
     */
    @Listener(order= Order.LAST)
    public void onEntityDeath(DestructEntityEvent.Death event) {
        if(!(event.getTargetEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player)event.getTargetEntity();

        if (DeathMessages.killedPlayers.containsKey(player.getUniqueId())) {
            event.setMessage(Texts.of(DeathMessages.killedPlayers.remove(player.getUniqueId())));
        }

    }

}
