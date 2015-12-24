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

import com.pwn9.PwnFilter.FilterTask;
import com.pwn9.PwnFilter.config.SpongeConfig;
import com.pwn9.PwnFilter.helpers.ChatColor;
import com.pwn9.PwnFilter.minecraft.PwnFilterPlugin;
import com.pwn9.PwnFilter.minecraft.api.MinecraftPlayer;
import com.pwn9.PwnFilter.minecraft.util.ColoredString;
import com.pwn9.PwnFilter.rules.RuleManager;
import com.pwn9.PwnFilter.util.LogManager;
import com.pwn9.PwnFilter.util.SimpleString;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.MessageSinkEvent;
import org.spongepowered.api.text.Texts;

import java.util.Optional;

/**
 * Listen for Chat events and apply the filter.
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class PwnFilterPlayerListener extends BaseListener implements EventListener<MessageSinkEvent.Chat> {

    /**
     * <p>getShortName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getShortName() {
        return "CHAT";
    }

	/**
	 * <p>Constructor for PwnFilterPlayerListener.</p>
	 *
	 */
	public PwnFilterPlayerListener(){}

    /**
     * <p>onPlayerChat.</p>
     *
     * @param event a {@link MessageSinkEvent.Chat} object.
     */
    public void handle(MessageSinkEvent.Chat event) {

        if (event.isCancelled()) return;

        Optional<Player> playerOptional = event.getCause().first(Player.class);
        if(!playerOptional.isPresent()) {
            return;
        }


        MinecraftPlayer bukkitPlayer = MinecraftPlayer.getInstance(playerOptional.get());


        // Permissions Check, if player has bypass permissions, then skip everything.
        if (bukkitPlayer.hasPermission("pwnfilter.bypass.chat")) return;

        String message = Texts.legacy().to(event.getMessage());

        // Global mute
        if ((SpongeConfig.isGlobalMute()) && (!bukkitPlayer.hasPermission("pwnfilter.bypass.mute"))) {
            event.setCancelled(true);
            return; // No point in continuing.
        }

        if (SpongeConfig.spamfilterEnabled() && !bukkitPlayer.hasPermission("pwnfilter.bypass.spam")) {
            // Keep a log of the last message sent by this player.  If it's the same as the current message, cancel.
            if (PwnFilterPlugin.lastMessage.containsKey(bukkitPlayer.getID()) && PwnFilterPlugin.lastMessage.get(bukkitPlayer.getID()).equals(message)) {
                event.setCancelled(true);
                return;
            }
            PwnFilterPlugin.lastMessage.put(bukkitPlayer.getID(), message);

        }

        FilterTask state = new FilterTask(new ColoredString(message), bukkitPlayer, this);

        // Global decolor
        if ((SpongeConfig.decolor()) && !(bukkitPlayer.hasPermission("pwnfilter.color"))) {
            // We are changing the state of the message.  Let's do that before any rules processing.
            state.setModifiedMessage(new SimpleString(state.getModifiedMessage().toString()));
        }

        // Take the message from the ChatEvent and send it through the filter.
        LogManager.getInstance().debugHigh("Applying '" + ruleChain.getConfigName() + "' to message: " + state.getModifiedMessage());
        ruleChain.execute(state);

        // Only update the message if it has been changed.
        if (state.messageChanged()){
            event.setMessage(Texts.of(ChatColor.translateAlternateColorCodes(state.getModifiedMessage().getRaw())));
        }
        if (state.isCancelled()) event.setCancelled(true);
    }

    /**
     * {@inheritDoc}
     *
     * Activate this listener.  This method can be called either by the owning plugin
     * or by PwnFilter.  PwnFilter will call the shutdown / activate methods when PwnFilter
     * is enabled / disabled and whenever it is reloading its config / rules.
     * <p/>
     * These methods could either register / deregister the listener with Bukkit, or
     * they could just enable / disable the use of the filter.
     */
    @Override
    public void activate() {

        if (isActive()) return;

        setRuleChain(RuleManager.getInstance().getRuleChain("chat.txt"));

        /* Hook up the Listener for PlayerChat events */
        Order chatPriority = SpongeConfig.getChatpriority();
        Sponge.getGame().getEventManager().registerListener(PwnFilterPlugin.getInstance(), MessageSinkEvent.Chat.class, chatPriority, this);
        LogManager.info("Activated PlayerListener with Priority Setting: " + SpongeConfig.getChatpriority().toString()
                + " Rule Count: " + getRuleChain().ruleCount() );
        setActive();

    }

}


