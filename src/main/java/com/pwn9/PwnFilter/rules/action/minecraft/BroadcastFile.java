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
import com.pwn9.PwnFilter.helpers.ChatColor;
import com.pwn9.PwnFilter.minecraft.api.MinecraftConsole;
import com.pwn9.PwnFilter.minecraft.util.FileUtil;
import com.pwn9.PwnFilter.config.FilterConfig;
import com.pwn9.PwnFilter.rules.action.Action;
import com.pwn9.PwnFilter.util.LogManager;
import com.pwn9.PwnFilter.util.tags.TagRegistry;

import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Broadcasts the contents of the named file to all users.
 *
 * @author ptoal
 * @version $Id: $Id
 */
@SuppressWarnings("UnusedDeclaration")
public class BroadcastFile implements Action {
    final ArrayList<String> messageStrings = new ArrayList<>();

    /** {@inheritDoc} */
    public void init(String s)
    {
        try {
            BufferedReader br = FileUtil.getBufferedReader(FilterConfig.getInstance().getTextDir(),s);
            String message;
            while ( (message = br.readLine()) != null ) {
                messageStrings.add(ChatColor.translateAlternateColorCodes('&',message));
            }
        } catch (FileNotFoundException ex) {
            LogManager.warn("File not found while trying to add Action: " + ex.getMessage());
        } catch (IOException ex) {
            LogManager.warn("Error reading: " + s);
        }
    }

    /** {@inheritDoc} */
    public void execute(final FilterTask filterTask) {
        final ArrayList<String> preparedMessages = messageStrings.stream().map(message -> TagRegistry.replaceTags(message, filterTask)).collect(Collectors.toCollection(ArrayList::new));

        filterTask.addLogMessage("Broadcasted: " + preparedMessages.get(0) + (preparedMessages.size() > 1 ? "..." : ""));

        MinecraftConsole.getInstance().sendBroadcast(preparedMessages);
    }
}

