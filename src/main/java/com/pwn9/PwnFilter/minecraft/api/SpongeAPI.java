/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.minecraft.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pwn9.PwnFilter.api.MessageAuthor;
import com.pwn9.PwnFilter.minecraft.DeathMessages;
import com.pwn9.PwnFilter.minecraft.PwnFilterPlugin;
import com.pwn9.PwnFilter.util.LogManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Texts;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles keeping a cache of data that we need during Async event handling.
 * We can't get this data in our Async method, as Bukkit API calls are not threadsafe.
 * Also, we can't always schedule a task, because we might be running in the
 * main thread.
 * <p/>
 * This will cache data about players for 10s.
 */

@SuppressWarnings("UnusedDeclaration")
public class SpongeAPI implements MinecraftAPI {

    private final LoadingCache<UUID, PlayerData> playerDataMap = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(
                    new CacheLoader<UUID, PlayerData>() {
                        @Override
                        public PlayerData load(@Nonnull final UUID uuid) {
                            Callable<PlayerData> cacheTask = () -> dataFetch(uuid);
                            return safeSpongeAPICall(cacheTask);
                        }
                    }
            );

    private final PwnFilterPlugin plugin;

    // Permissions we are interested in caching
    protected final Set<String> permSet = new HashSet<>();

    private SpongeAPI(PwnFilterPlugin p) {
        plugin = p;
    }

    public static SpongeAPI getInstance(PwnFilterPlugin plugin) {
        return new SpongeAPI(plugin);
    }

    @Override
    public synchronized void reset() {
        permSet.clear();
        playerDataMap.invalidateAll();
    }


//    /**
//     * <p>Getter for the field <code>onlinePlayers</code>.</p>
//     *
//     * @return an array of {@link org.bukkit.entity.Player} objects.
//     */
//    public Player[] getOnlinePlayers() {
//        return onlinePlayers.toArray(new Player[onlinePlayers.size()]);
//    }


    /**
     * <p>addCachedPermission.</p>
     *
     * @param permission a {@link java.lang.String} object.
     */
    @Override
    public synchronized void addCachedPermission(String permission) {
        permSet.add(permission);
    }


    /**
     * <p>addCachedPermissions.</p>
     *
     * @param permissions a {@link java.util.Set} object.
     */
    @Override
    public synchronized void addCachedPermissions(Set<String> permissions) {
        permSet.addAll(permissions);
    }

    // NOTE: This is not synchronized, but it is private, so that only the
    // synchronized methods can call it.

    private Set<String> cachePlayerPermissions(Player p) {
        return permSet.stream().filter(p::hasPermission).collect(Collectors.toSet());
    }

    @Nullable
    public <T> T safeSpongeAPICall(Callable<T> callable) {
        /*
        if (Bukkit.isPrimaryThread()) {
            // We are in the main thread, just execute API calls directly.
            try {
                return callable.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Red Alert, Shields Up.  We are an Async task.  Ask the main
            // thread to execute these calls, and return the Player data to
            // cache.
            Future<T> task = Bukkit.getScheduler().callSyncMethod(plugin, callable);
            try {
                // This will block the current thread for up to 3s
                return task.get(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                LogManager.getInstance().debugLow("Bukkit API call timed out (>3s).");
                return null;
            }

        }
        */
        try {
            return callable.call();// will prob explode at some point
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        //return null;
    }

    public boolean isPrimaryThread() {
        return Thread.currentThread().getName().equals("Server thread");
    }

    public void safeSpongeDispatch(Runnable runnable) {
        if (isPrimaryThread()) {
            // We are in the main thread, just execute API calls directly.
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Red Alert, Shields Up.  We are an Async task.  Ask the main
            // thread to execute these calls, and return the Player data to
            // cache.
            Sponge.getGame().getScheduler().createTaskBuilder().execute(runnable).submit(plugin);
        }
    }


    private PlayerData dataFetch(UUID uuid) {
        PlayerData cache = new PlayerData();
        Optional<Player> p = Sponge.getGame().getServer().getPlayer(uuid);
        if (!p.isPresent()) return null; // Couldn't find the player!
        cache.setPermissionSet(cachePlayerPermissions(p.get()));
        //TODO displaynamedata sponge
//        cache.setName(p.get().getDisplayNameData().toString());
        cache.setName(p.get().getName());
        cache.setWorld(p.get().getLocation().getExtent());
        return cache;
    }

    @Override
    public MessageAuthor getAuthor(UUID uuid) {
        return MinecraftPlayer.getInstance(uuid);
    }

    @Override
    public PlayerData getData(UUID uuid) throws ExecutionException {
        return playerDataMap.get(uuid);
    }


    /*
      **********
      Player API
      **********
    */

    @Override
    public boolean burn(final UUID uuid, final int duration, final String messageString) {
        safeSpongeDispatch(
                () -> {
                    Optional<Player> player = Sponge.getGame().getServer().getPlayer(uuid);
                    if (player.isPresent()) {
                        player.get().offer(Keys.FIRE_TICKS, duration);
                        player.get().sendMessage(Texts.of(messageString));
                    }
                });

        return true;
    }

    @Override
    public void sendMessage(final UUID uuid, final String message) {
        safeSpongeDispatch(
                () -> {
                    Optional<Player> player = Sponge.getGame().getServer().getPlayer(uuid);
                    if (player.isPresent()) {
                        player.get().sendMessage(Texts.of(message));
                    }
                }
        );

    }

    @Override
    public void sendMessages(final UUID uuid, final List<String> messages) {
        safeSpongeDispatch(
                () -> {
                    for(String message : messages) {
                        sendMessage(uuid, message);
                    }
                }
        );
    }

    @Override
    public void executePlayerCommand(final UUID uuid, final String command) {
        safeSpongeDispatch(
                () -> {
                    Optional<Player> player = Sponge.getGame().getServer().getPlayer(uuid);
                    if (player.isPresent()) {
                        Sponge.getGame().getCommandManager().process(player.get(), command);
                    }
                });
    }

    @Override
    public boolean withdrawMoney(final UUID uuid, final Double amount, final String messageString) {
        /*
        if (PwnFilterPlugin.economy != null) {
            Boolean result = safeSpongeAPICall(
                    () -> {
                        Player bukkitPlayer = Bukkit.getPlayer(uuid);
                        if (bukkitPlayer != null) {
                            EconomyResponse resp = PwnFilterPlugin.economy.withdrawPlayer(
                                    Bukkit.getOfflinePlayer(uuid), amount);
                            bukkitPlayer.sendMessage(messageString);
                            return resp.transactionSuccess();
                        }
                        return false;
                    });
            if (result != null) return result;
        }*/
        return false;
    }

    @Override
    public void kick(final UUID uuid, final String messageString) {
        safeSpongeDispatch(
                () -> {
                    Optional<Player> player = Sponge.getGame().getServer().getPlayer(uuid);
                    if (player.isPresent())
                        player.get().kick(Texts.of(messageString));
                });

    }

    @Override
    public void kill(final UUID uuid, final String messageString) {
        safeSpongeDispatch(
                () -> {
                    Optional<Player> player = Sponge.getGame().getServer().getPlayer(uuid);
                    if (player.isPresent()) {
                        player.get().offer(Keys.HEALTH, 0d);
                        DeathMessages.addKilledPlayer(uuid, player.get() + " " + messageString);
                    }
                });
    }

    @Override
    public String getPlayerWorldName(UUID uuid) {
        try {
            return getData(uuid).getWorld().getName();
        } catch (ExecutionException e) {
            return null;
        }

    }

    /*
      ***********
      Console API
      ***********
     */

    @Override
    public void sendConsoleMessage(final String message) {
        safeSpongeDispatch(() -> Sponge.getGame().getServer().getConsole().sendMessage(Texts.of(message)));
    }

    @Override
    public void sendConsoleMessages(final List<String> messageList) {
        safeSpongeDispatch(() -> messageList.forEach(this::sendConsoleMessage));

    }

    @Override
    public void sendBroadcast(final String message) {
        safeSpongeDispatch(() -> Sponge.getGame().getServer().getBroadcastSink().sendMessage(Texts.of(message)));
    }

    @Override
    public void sendBroadcast(final List<String> messageList) {
        safeSpongeDispatch(() -> messageList.forEach(this::sendBroadcast));
    }

    @Override
    public void executeCommand(final String command) {
        safeSpongeDispatch(() -> Sponge.getCommandDispatcher().process(Sponge.getGame().getServer().getConsole(), command));
    }

    @Override
    public boolean notifyWithPerm(final String permissionString, final String sendString) {
        safeSpongeDispatch(
                () -> {
                    if (permissionString.equalsIgnoreCase("console")) {
                        sendConsoleMessage(sendString);
                    } else {
                        Sponge.getGame().getServer().getOnlinePlayers().stream().filter(p -> p.hasPermission(permissionString)).forEach(p -> p.sendMessage(Texts.of(sendString)));
                    }
                });

        return true;

    }
}
