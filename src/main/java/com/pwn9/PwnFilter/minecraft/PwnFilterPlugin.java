/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.minecraft;

import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.pwn9.PwnFilter.FilterEngine;
import com.pwn9.PwnFilter.api.FilterClient;
import com.pwn9.PwnFilter.config.SpongeConfig;
import com.pwn9.PwnFilter.minecraft.api.SpongeAPI;
import com.pwn9.PwnFilter.minecraft.api.MinecraftAPI;
import com.pwn9.PwnFilter.minecraft.api.MinecraftServer;
import com.pwn9.PwnFilter.minecraft.listener.PlayerCacheListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterBookListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterCommandListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterEntityListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterInvListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterPlayerListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterServerCommandListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterSignListener;
import com.pwn9.PwnFilter.minecraft.util.Tracker;
import com.pwn9.PwnFilter.rules.RuleChain;
import com.pwn9.PwnFilter.util.LogManager;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.mcstats.Metrics;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * A Regular Expression (REGEX) Chat Filter For Bukkit with many great features
 *
 * @author tremor77
 * @version $Id: $Id
 */

// TODO: Add support for Books
// TODO: Enable configuration management /pfset /pfsave
// TODO: It's powerful.  Now, make it easier.
// TODO: Make 'base' files that users can pull in to get started quickly (eg: swearing.txt, hate.txt, etc.)
// TODO: Multiverse-support? (Different configs for different worlds)
@Plugin(name = "PwnFilter", id = "pwnfilter", version = "1.0")
public class PwnFilterPlugin {

    private static PwnFilterPlugin _instance;
    private static MinecraftAPI minecraftAPI;
    private Metrics metrics;
    public static Tracker matchTracker;
    private Metrics.Graph eventGraph;
    //public static Economy economy = null;

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    @Inject
    @ConfigDir(sharedRoot = false)
    private File dataFolder;//TODO nio

    public static final ConcurrentMap<UUID, String> lastMessage = new MapMaker().concurrencyLevel(2).weakKeys().makeMap();

    /**
     * <p>Constructor for PwnFilter.</p>
     */
    public PwnFilterPlugin() {

        _instance = this;

    }


    /**
     * <p>getInstance.</p>
     *
     * @return a {@link PwnFilterPlugin} object.
     */
    public static PwnFilterPlugin getInstance() {
        return _instance;
    }

    /** {@inheritDoc} */
    @Listener
    public void onLoad(GameInitializationEvent event) {

        // Set up the Log manager.
        LogManager.getInstance(logger, dataFolder);

        // We're all Sponge here, so let's set the API appropriately
        MinecraftServer.setAPI(SpongeAPI.getInstance(this));

        // Initialize Configuration
        //TODO sponge
        // saveDefaultConfig();

        // Set up a Vault economy for actions like "fine" (optional)
        setupEconomy();

        // Now get our configuration
        configurePlugin();

        // Activate Plugin Metrics
        activateMetrics();

        //Load up our listeners
        FilterEngine filterEngine = FilterEngine.getInstance();
        filterEngine.registerClient(new PwnFilterCommandListener(), this);
        filterEngine.registerClient(new PwnFilterInvListener(), this);
        filterEngine.registerClient(new PwnFilterPlayerListener(), this);
        filterEngine.registerClient(new PwnFilterServerCommandListener(), this);
        filterEngine.registerClient(new PwnFilterSignListener(), this);
        filterEngine.registerClient(new PwnFilterBookListener(), this);


        // The Entity Death handler, for custom death messages.
        Sponge.getGame().getEventManager().registerListeners(this, new PwnFilterEntityListener());
        // The DataCache handler, for async-safe player info (name/world/permissions)
        Sponge.getGame().getEventManager().registerListeners(this, new PlayerCacheListener());

        // Enable the listeners
        filterEngine.enableClients();

        // Set up Command Handlers
        //TODO sponge
        // getCommand("pfreload").setExecutor(new pfreload());
        //getCommand("pfcls").setExecutor(new pfcls());
        //getCommand("pfmute").setExecutor(new pfmute());
        //getCommand("pfdumpcache").setExecutor(new pfdumpcache());

    }

    /**
     * <p>activateMetrics.</p>
     */
    public void activateMetrics() {
        // Activate Plugin Metrics
        try {
            if (metrics == null) {
                metrics = new Metrics(Sponge.getGame(), Sponge.getPluginManager().getPlugin("pwnfilter").get());//TODO might explode

                eventGraph = metrics.createGraph("Rules by Event");
                updateMetrics();

                Metrics.Graph matchGraph = metrics.createGraph("Matches");
                matchTracker = new Tracker("Matches");

                matchGraph.addPlotter(matchTracker);
            }
            metrics.start();


        } catch (IOException e) {
            LogManager.error(e.getMessage());
        }
    }

    /**
     * <p>updateMetrics.</p>
     */
    public void updateMetrics() {
        ArrayList<String> activeListenerNames = new ArrayList<String>();
        for (FilterClient f : FilterEngine.getInstance().getActiveClients()) {
            activeListenerNames.add(f.getShortName());
        }

        // Remove old plotters
        for (Metrics.Plotter p : eventGraph.getPlotters()) {
            if (!activeListenerNames.contains(p.getColumnName())) {
                eventGraph.removePlotter(p);
            }
        }

        // Add new plotters
        for (final FilterClient f : FilterEngine.getInstance().getActiveClients()) {
            final String eventName = f.getShortName();
            eventGraph.addPlotter(new Metrics.Plotter(eventName) {
                @Override
                public int getValue() {
                    RuleChain r = f.getRuleChain();
                    if (r != null) {
                        return r.ruleCount(); // Number of rules for this event type
                    } else
                        return 0;
                }
            });
        }
    }

    /**
     * <p>configurePlugin.</p>
     */
    public void configurePlugin() {

        try {
            SpongeConfig.loadConfiguration(configManager, dataFolder);
        } catch (RuntimeException ex) {
            LogManager.error("Fatal configuration failure: " + ex.getMessage());
            LogManager.error("PwnFilter disabled.");
            //TODO sponge
            // getPluginLoader().disablePlugin(this);
        }

    }

    private void setupEconomy() {//TODO economy api required
/*
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                LogManager.logger.info("Vault found. Enabling actions requiring Vault");
                return;
            }
        }
        LogManager.logger.info("Vault dependency not found.  Disabling actions requiring Vault");
        */

    }



}

