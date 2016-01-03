
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
import com.pwn9.PwnFilter.minecraft.PwnFilterPlugin;
import com.pwn9.PwnFilter.minecraft.api.MinecraftPlayer;
import com.pwn9.PwnFilter.minecraft.util.ColoredString;
import com.pwn9.PwnFilter.rules.RuleChain;
import com.pwn9.PwnFilter.rules.RuleManager;
import com.pwn9.PwnFilter.util.LogManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;

import java.util.Optional;

/**
 * Apply the filter to commands.
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class PwnFilterCommandListener extends BaseListener implements EventListener<SendCommandEvent> {

    private RuleChain chatRuleChain;

    /**
     * <p>getShortName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getShortName() { return "COMMAND" ;}

    /**
     * <p>Constructor for PwnFilterCommandListener.</p>
     *
     */
    public PwnFilterCommandListener() {
	    super();
    }

    /** {@inheritDoc} */
    public void activate() {
        if (isActive()) return;

        setRuleChain(RuleManager.getInstance().getRuleChain("command.txt"));
        chatRuleChain = RuleManager.getInstance().getRuleChain("chat.txt");


        Order priority = SpongeConfig.getCmdpriority();
        if (SpongeConfig.cmdfilterEnabled()) {
            Sponge.getGame().getEventManager().registerListener(PwnFilterPlugin.getInstance(), SendCommandEvent.class, priority, this);
            setActive();
            LogManager.info("Activated CommandListener with Priority Setting: " + priority.toString()
                    + " Rule Count: " + getRuleChain().ruleCount() );

            StringBuilder sb = new StringBuilder("Commands to filter: ");
            for (String command : SpongeConfig.getCmdlist()) sb.append(command).append(" ");
            LogManager.getInstance().debugLow(sb.toString().trim());

            sb = new StringBuilder("Commands to never filter: ");
            for (String command : SpongeConfig.getCmdblist()) sb.append(command).append(" ");
            LogManager.getInstance().debugLow(sb.toString().trim());
        }
    }


    /**
     * <p>eventProcessor.</p>
     *
     * @param event a {@link SendCommandEvent} object.
     */
    public void handle(SendCommandEvent event) {

        if (event.isCancelled()) return;

        Optional<Player> playerOptional = event.getCause().first(Player.class);
        if(!playerOptional.isPresent()) {
            return;
        }

        MinecraftPlayer minecraftPlayer = MinecraftPlayer.getInstance(playerOptional.get().getUniqueId());

        if (minecraftPlayer.hasPermission("pwnfilter.bypass.commands")) return;

        String message = event.getCommand()+" "+event.getArguments();

        //Gets the actual command as a string
        String cmdmessage = event.getCommand();


        FilterTask filterTask = new FilterTask(new ColoredString(message), minecraftPlayer, this);

        // Check to see if we should treat this command as chat (eg: /tell)
        if (SpongeConfig.getCmdchat().contains(cmdmessage)) {
            // Global mute
            if ((SpongeConfig.isGlobalMute()) && (!minecraftPlayer.hasPermission("pwnfilter.bypass.mute"))) {
                event.setCancelled(true);
                return;
            }

            // Simple Spam filter
            if (SpongeConfig.commandspamfilterEnabled() && !minecraftPlayer.hasPermission("pwnfilter.bypass.spam")) {
                // Keep a log of the last message sent by this player.  If it's the same as the current message, cancel.
                if (PwnFilterPlugin.lastMessage.containsKey(minecraftPlayer.getID()) && PwnFilterPlugin.lastMessage.get(minecraftPlayer.getID()).equals(message)) {
                    event.setCancelled(true);
                    return;
                }
                PwnFilterPlugin.lastMessage.put(minecraftPlayer.getID(), message);
            }

            chatRuleChain.execute(filterTask);

        } else {

            if (!SpongeConfig.getCmdlist().isEmpty() && !SpongeConfig.getCmdlist().contains(cmdmessage)) return;
            if (SpongeConfig.getCmdblist().contains(cmdmessage)) return;

            // Take the message from the Command Event and send it through the filter.

            ruleChain.execute(filterTask);

        }

        // Only update the message if it has been changed.
        if (filterTask.messageChanged()){
            if (filterTask.getModifiedMessage().toString().isEmpty()) {
                event.setCancelled(true);
                return;
            }
            Sponge.getGame().getCommandManager().process(playerOptional.get(), filterTask.getModifiedMessage().getRaw());
        }

        if (filterTask.isCancelled()) event.setCancelled(true);

    }

}
