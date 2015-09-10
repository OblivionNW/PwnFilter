/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2014 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.rules.action.minecraft;

import com.pwn9.PwnFilter.FilterTask;
import com.pwn9.PwnFilter.bukkit.PwnFilterPlugin;
import com.pwn9.PwnFilter.rules.action.Action;
import com.pwn9.PwnFilter.util.tags.TagRegistry;
import org.bukkit.ChatColor;

import java.util.ArrayList;

/**
 * Responds to the user with the string provided.
 *
 * @author ptoal
 * @version $Id: $Id
 */
@SuppressWarnings("UnusedDeclaration")
public class Broadcast implements Action {
    ArrayList<String> messageStrings = new ArrayList<String>();

    /** {@inheritDoc} */
    public void init(String s)
    {
        for ( String message : s.split("\n") ) {
            messageStrings.add(ChatColor.translateAlternateColorCodes('&',message));
        }
    }

    /** {@inheritDoc} */
    public boolean execute(final FilterTask filterTask ) {

        final ArrayList<String> preparedMessages = new ArrayList<String>();

        for (String message : messageStrings) {
            preparedMessages.add(TagRegistry.replaceTags(message, filterTask));
        }

        filterTask.addLogMessage("Broadcasted: " + preparedMessages.get(0) + (preparedMessages.size() > 1 ? "..." : ""));

        PwnFilterPlugin.sendBroadcast(preparedMessages);

        return true;
    }
}

