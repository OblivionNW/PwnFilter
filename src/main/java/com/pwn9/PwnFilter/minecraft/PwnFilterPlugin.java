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

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.pwn9.PwnFilter.FilterEngine;
import com.pwn9.PwnFilter.api.FilterClient;
import com.pwn9.PwnFilter.config.SpongeConfig;
import com.pwn9.PwnFilter.minecraft.api.SpongeAPI;
import com.pwn9.PwnFilter.minecraft.api.MinecraftAPI;
import com.pwn9.PwnFilter.minecraft.api.MinecraftServer;
import com.pwn9.PwnFilter.minecraft.command.ClearChatCommandExecutor;
import com.pwn9.PwnFilter.minecraft.command.GlobalMuteCommandExecutor;
import com.pwn9.PwnFilter.minecraft.command.ReloadCommandExecutor;
import com.pwn9.PwnFilter.minecraft.listener.PlayerCacheListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterBookListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterCommandListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterEntityListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterInvListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterPlayerListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterServerCommandListener;
import com.pwn9.PwnFilter.minecraft.listener.PwnFilterSignListener;
import com.pwn9.PwnFilter.minecraft.util.FileUtil;
import com.pwn9.PwnFilter.minecraft.util.Tracker;
import com.pwn9.PwnFilter.rules.RuleChain;
import com.pwn9.PwnFilter.util.LogManager;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.apache.commons.io.FileUtils;
import org.mcstats.Metrics;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.economy.EconomyService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * A Regular Expression (REGEX) Chat Filter For Sponge with many great features
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
    public static EconomyService economy = null;

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
        saveDefaultConfig();

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
        CommandSpec pfreloadCommandSpec = CommandSpec.builder().executor(new ReloadCommandExecutor()).permission("pwnfilter.reload").build();
        Sponge.getGame().getCommandManager().register(this, pfreloadCommandSpec, "pfreload");
        CommandSpec pfmuteCommandSpec = CommandSpec.builder().executor(new GlobalMuteCommandExecutor()).permission("pwnfilter.mute").build();
        Sponge.getGame().getCommandManager().register(this, pfmuteCommandSpec, "pfmute");
        CommandSpec pfclsCommandSpec = CommandSpec.builder().executor(new ClearChatCommandExecutor()).permission("pwnfilter.cls").build();
        Sponge.getGame().getCommandManager().register(this, pfclsCommandSpec, "pfcls");

    }

    @Listener
    public void postInit(GamePostInitializationEvent event) {
        // Set up a economy for actions like "fine" (optional)
        setupEconomy();
    }

    private void saveDefaultConfig() {
        HashMap<String, String> configFiles = Maps.newHashMap();
        configFiles.put("pwnfilter.conf", "pwnfilter.conf");
        configFiles.put("book.txt", "rules/book.txt");
        configFiles.put("chat.txt", "rules/chat.txt");
        configFiles.put("command.txt", "rules/command.txt");
        configFiles.put("console.txt", "rules/console.txt");
        configFiles.put("item.txt", "rules/item.txt");
        configFiles.put("book.txt", "rules/sign.txt");
        if(!dataFolder.exists()) {
            new File(dataFolder, "rules").mkdirs();
            for(HashMap.Entry<String, String> entry : configFiles.entrySet()) {
                URL url = PwnFilterPlugin.class.getResource(entry.getKey());
                try {
                    FileUtils.copyURLToFile(url, new File(dataFolder, entry.getValue()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
        ArrayList<String> activeListenerNames = FilterEngine.getInstance().getActiveClients().stream().map(FilterClient::getShortName).collect(Collectors.toCollection(ArrayList::new));

        // Remove old plotters
        eventGraph.getPlotters().stream().filter(p -> !activeListenerNames.contains(p.getColumnName())).forEach(p -> eventGraph.removePlotter(p));

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

    private void setupEconomy() {
        Optional<EconomyService> economyService = Sponge.getGame().getServiceManager().provide(EconomyService.class);
        if(economyService.isPresent()) {
            economy = economyService.get();
            LogManager.info("EconomyService found. Enabling actions requiring EconomyService");
            return;
        }
        LogManager.info("EconomyService dependency not found. Disabling actions requiring EconomyService");
    }



}

