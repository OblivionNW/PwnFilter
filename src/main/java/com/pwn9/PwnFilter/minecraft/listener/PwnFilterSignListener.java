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

import com.google.common.collect.Lists;
import com.pwn9.PwnFilter.FilterTask;
import com.pwn9.PwnFilter.minecraft.api.MinecraftPlayer;
import com.pwn9.PwnFilter.minecraft.PwnFilterPlugin;
import com.pwn9.PwnFilter.minecraft.util.ColoredString;
import com.pwn9.PwnFilter.config.SpongeConfig;
import com.pwn9.PwnFilter.rules.RuleManager;
import com.pwn9.PwnFilter.util.LogManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * Listen for Sign Change events and apply the filter to the text.
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class PwnFilterSignListener extends BaseListener implements EventListener<ChangeSignEvent> {

    /**
     * <p>Constructor for PwnFilterSignListener.</p>
     *
     */
    public PwnFilterSignListener() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public String getShortName() {
        return "SIGN";
    }

    /**
     * The sign filter has extra work to do that the chat doesn't:
     * 1. Take lines of sign and aggregate them into one string for processing
     * 2. Feed them into the filter.
     * 3. Re-split the lines so they can be placed on the sign.
     *
     * @param event The SignChangeEvent to be processed.
     */
    @SuppressWarnings("ConstantConditions")
    public void handle(ChangeSignEvent event) {
        if (event.isCancelled()) return;

        Optional<Player> playerOptional = event.getCause().first(Player.class);
        if(!playerOptional.isPresent()) {
            return;
        }

        MinecraftPlayer bukkitPlayer = MinecraftPlayer.getInstance(playerOptional.get());

        // Permissions Check, if player has bypass permissions, then skip everything.
        if (bukkitPlayer.hasPermission("pwnfilter.bypass.signs")) {
            return;
        }

        SignData signData = event.getText();
        if (!signData.getValue(Keys.SIGN_LINES).isPresent()) {
            return;
        }

        // Take the message from the SignListenerEvent and send it through the filter.
        StringBuilder builder = new StringBuilder();

        for (Text l : signData.getValue(Keys.SIGN_LINES).get()) {
            builder.append(Texts.toPlain(l)).append("\t");
        }
        String signLines = builder.toString().trim();

        FilterTask filterTask = new FilterTask(new ColoredString(signLines), bukkitPlayer, this);

        ruleChain.execute(filterTask);

        if (filterTask.messageChanged()){
            // TODO: Can colors be placed on signs?  Wasn't working. Find out why.
            // Break the changed string into new Lines
            List<String> newLines = new ArrayList<>();

            // Global decolor
            if ((SpongeConfig.decolor()) && !(bukkitPlayer.hasPermission("pwnfilter.color"))) {
                Collections.addAll(newLines,filterTask.getModifiedMessage().toString().split("\t"));
            } else {
                Collections.addAll(newLines,filterTask.getModifiedMessage().getRaw().split("\t"));
            }

            String[] outputLines = new String[4];

            // Check if any of the lines are now too long to fit the sign.  Truncate if they are.

            for (int i = 0 ; i < 4 && i < newLines.size() ; i++) {
                if (newLines.get(i).length() > 15) {
                    outputLines[i] = newLines.get(i).substring(0, 15);
                } else {
                    outputLines[i] = newLines.get(i);
                }
            }

            ArrayList<Text> newTexts = Lists.newArrayList();
            for (int i = 0 ; i < 4 ; i++ ) {
                if (outputLines[i] != null) {
                    newTexts.add(Texts.of(outputLines[i]));
                } else {
                    newTexts.add(Texts.of(""));
                }
            }
            event.getText().set(Keys.SIGN_LINES, newTexts);
        }

        if (filterTask.isCancelled()) {
            event.setCancelled(true);
            filterTask.getAuthor().sendMessage("Your sign broke, there must be something wrong with it.");
            filterTask.addLogMessage("SIGN " + filterTask.getAuthor().getName() + " sign text: "
                    + filterTask.getOriginalMessage().getRaw());
        }

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
        setRuleChain(RuleManager.getInstance().getRuleChain("sign.txt"));
        Order priority = SpongeConfig.getSignpriority();
        if (SpongeConfig.signfilterEnabled()) {
            // Now register the listener with the appropriate priority
            Sponge.getGame().getEventManager().registerListener(PwnFilterPlugin.getInstance(), ChangeSignEvent.class, SpongeConfig.getSignpriority(), this);
            LogManager.info("Activated SignListener with Priority Setting: " + priority.toString()
                    + " Rule Count: " + getRuleChain().ruleCount());
            setActive();
        }
    }

}
