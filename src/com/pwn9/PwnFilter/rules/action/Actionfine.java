/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.rules.action;

import com.pwn9.PwnFilter.FilterState;
import com.pwn9.PwnFilter.PwnFilter;
import com.pwn9.PwnFilter.util.DefaultMessages;
import com.pwn9.PwnFilter.util.LogManager;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Fine the user by extracting money from his economy account.
 */
@SuppressWarnings("UnusedDeclaration")
public class Actionfine implements Action {

    String messageString; // Message to apply to this action
    double fineAmount; // How much to fine the player.

    public void init(String s)
    {
        String[] parts = s.split("\\s",2);

        try {
            fineAmount = Double.parseDouble(parts[0]);
        } catch (NumberFormatException e ) {
            fineAmount = 0.00;
        }

        String message = (parts.length > 1)?parts[1]:"";
        messageString = DefaultMessages.prepareMessage(parts[1], "finemsg");
        if (PwnFilter.economy == null) {
            LogManager.logger.warning("Parsed rule requiring an Economy, but one was not detected. " +
                    "Check Vault configuration, or remove 'then fine' rules.");
        }

    }

    public boolean execute(final FilterState state ) {

        if (state.getPlayer() == null) return false;

        if (PwnFilter.economy != null ) {
            EconomyResponse resp = PwnFilter.economy.withdrawPlayer(state.playerName,fineAmount);
            if (resp.transactionSuccess()) {
                state.addLogMessage(String.format("Fined %s : %f",state.playerName,resp.amount));
            } else {
                state.addLogMessage(String.format("Failed to fine %s : %f. Error: %s",
                        state.playerName,resp.amount,resp.errorMessage));
                return false;
            }
            Bukkit.getScheduler().runTask(state.plugin, new BukkitRunnable() {
                @Override
                public void run() {
                    state.getPlayer().sendMessage(messageString);
                }
            });

            return true;

        } else return false;
    }
}
