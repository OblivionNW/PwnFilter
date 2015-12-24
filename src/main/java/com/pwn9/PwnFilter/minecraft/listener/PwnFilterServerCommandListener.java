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
import com.pwn9.PwnFilter.minecraft.api.MinecraftConsole;
import com.pwn9.PwnFilter.minecraft.util.ColoredString;
import com.pwn9.PwnFilter.rules.RuleManager;
import com.pwn9.PwnFilter.util.LogManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;

/**
 * Apply the filter to commands.
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class PwnFilterServerCommandListener extends BaseListener implements EventListener<SendCommandEvent> {

    /**
     * <p>Constructor for PwnFilterServerCommandListener.</p>
     *
     */
    public PwnFilterServerCommandListener() {
	    super();
    }

    /** {@inheritDoc} */
    @Override
    public String getShortName() {
        return "CONSOLE";
    }

    /**
     * <p>onServerCommandEvent.</p>
     *
     * @param event a {@link SendCommandEvent} object.
     */
    public void handle(SendCommandEvent event) {
        if(!event.getCause().contains(Sponge.getGame().getServer().getConsole())) {
            return;
        }

        String command = event.getCommand()+" "+event.getArguments();

        //Gets the actual command as a string
        String cmdmessage = event.getCommand();

        if (!SpongeConfig.getCmdlist().isEmpty() && !SpongeConfig.getCmdlist().contains(cmdmessage)) return;
        if (SpongeConfig.getCmdblist().contains(cmdmessage)) return;

        FilterTask state = new FilterTask(new ColoredString(command), MinecraftConsole.getInstance(), this);

        // Take the message from the Command Event and send it through the filter.

        ruleChain.execute(state);

        // Only update the message if it has been changed.
        if (state.messageChanged()){
            Sponge.getGame().getCommandManager().process(Sponge.getGame().getServer().getConsole(), state.getModifiedMessage().getRaw());
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

        setRuleChain(RuleManager.getInstance().getRuleChain("console.txt"));

        if (SpongeConfig.consolefilterEnabled()) {
            Order priority = SpongeConfig.getCmdpriority();
            Sponge.getGame().getEventManager().registerListener(PwnFilterPlugin.getInstance(), SendCommandEvent.class, priority, this);
            LogManager.info("Activated ServerCommandListener with Priority Setting: " + priority.toString()
                    + " Rule Count: " + getRuleChain().ruleCount() );
            setActive();
        }
    }

}
