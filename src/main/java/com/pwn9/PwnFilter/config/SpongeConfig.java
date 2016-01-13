/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2015 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.config;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.pwn9.PwnFilter.rules.action.Action;
import com.pwn9.PwnFilter.rules.action.ActionFactory;
import com.pwn9.PwnFilter.util.LogManager;
import com.pwn9.PwnFilter.util.PointManager;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.event.Order;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A largely static object, which serves as an interface to the PwnFilter Sponge
 * configuration.
 * <p>
 * Created by ptoal on 15-09-10.
 */
public class SpongeConfig {

	private static CommentedConfigurationNode rootNode;

	// Global Plugin switches
	private static boolean globalMute = false;

	private static File dataFolder;

	public static void loadConfiguration(ConfigurationLoader<CommentedConfigurationNode> configManagerIn, File folder) {
		try {
			dataFolder = folder;
			rootNode = configManagerIn.load(ConfigurationOptions.defaults().setShouldCopyDefaults(true));

			CommentedConfigurationNode loggingNode = rootNode.getNode("Logging");
			if (loggingNode.getNode("logfile").getBoolean(true)) {
				LogManager.getInstance().start();
			} else { // Needed during configuration reload to turn off logging if the option changes
				LogManager.getInstance().stop();
			}

			CommentedConfigurationNode folderNode = rootNode.getNode("Folders");
			// Set the directory containing rules files.
			File ruleDir = setupDirectory(folderNode.getNode("ruledirectory").getString("rules"));
			if (ruleDir != null) {
				FilterConfig.getInstance().setRulesDir(ruleDir);
			} else {
				throw new RuntimeException(
						"Unable to create or access rule directory.");
			}

			// Set the directory containing Text Files
			FilterConfig.getInstance().setTextDir(
					setupDirectory(folderNode.getNode("textdir").getString("textfiles"))
			);

			// Setup logging
			LogManager.setRuleLogLevel(loggingNode.getNode("loglevel").getString("info"));
			LogManager.setDebugMode(loggingNode.getNode("debug").getString("off"));

			setupPoints();

			configManagerIn.save(rootNode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private static void setupPoints() {
		CommentedConfigurationNode pointsSection = rootNode.getNode("Points");
		if (!pointsSection.getNode("enabled").getBoolean(false)) {
			if (PointManager.isEnabled()) {
				PointManager.getInstance().shutdown();
			}
		} else {
			if (!PointManager.isEnabled()) {
				PointManager.setup(
						pointsSection.getNode("leak", "points").getDouble(1),
						pointsSection.getNode("leak", "interval").getInt(30)
				);

				parseThresholds(pointsSection);
			}
		}
	}

	private static void parseThresholds(CommentedConfigurationNode pointsSection) {
		CommentedConfigurationNode thresholdSection = pointsSection.getNode("thresholds");
		for(CommentedConfigurationNode threshold : thresholdSection.getChildrenMap().values()) {
			List<String> ascendingLines = getStringList(threshold.getNode("actions", "ascending"));
			List<Action> ascendingActions = Lists.newArrayListWithCapacity(ascendingLines.size());
			List<String> descendingLines = getStringList(threshold.getNode("actions", "descending"));
			List<Action> descendingActions = Lists.newArrayListWithCapacity(ascendingLines.size());
			ascendingActions.addAll(ascendingLines.stream().map(ActionFactory::getActionFromString).collect(Collectors
					.toList()));
			descendingActions.addAll(descendingLines.stream().map(ActionFactory::getActionFromString).collect(Collectors
					.toList()));
			PointManager.getInstance().addThreshold(
					threshold.getNode("name").getString(),
					threshold.getNode("points").getDouble(),
					ascendingActions,
					descendingActions
			);
		}
	}


	/**
	 * Ensure that the named directory exists and is accessible.  If the
	 * directory begins with a / (slash), it is assumed to be an absolute
	 * path.  Otherwise, the directory is assumed to be relative to the root
	 * data folder.
	 * <p>
	 * If the directory doesn't exist, an attempt is made to create it.
	 *
	 * @param directoryName relative or absolute path to the directory
	 * @return {@link File} referencing the directory.
	 */
	private static File setupDirectory(@Nonnull String directoryName) {
		File dir;
		if (directoryName.startsWith("/")) {
			dir = new File(directoryName);
		} else {
			dir = new File(dataFolder, directoryName);//TODO Lazy way for now
		}
		try {
			if (!dir.exists()) {
				if (dir.mkdirs())
					LogManager.info("Created directory: " + dir.getAbsolutePath());
			}
			return dir;
		} catch (Exception ex) {
			LogManager.warn("Unable to access/create directory: " + dir.getAbsolutePath());
			return null;
		}

	}

	/* Accessors */
	public File getDataFolder() {
		return dataFolder;
	}

	public static boolean decolor() {
		return rootNode.getNode("FilterOptions").getNode("decolor").getBoolean(false);
	}

	public static void setDecolor(boolean decolor) {
		rootNode.getNode("FilterOptions").getNode("decolor").setValue(decolor);
	}

	public static boolean isGlobalMute() {
		return globalMute;
	}

	public static void setGlobalMute(boolean globalMute) {
		SpongeConfig.globalMute = globalMute;
	}

	public static List<String> getStringList(CommentedConfigurationNode node) {
		try {
			return node.getList(new TypeToken<String>() {});
		} catch (ObjectMappingException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	public static List<String> getCmdlist() {
		return getStringList(rootNode.getNode("FilterOptions", "cmdlist"));
	}

	public static List<String> getCmdblist() {
		return getStringList(rootNode.getNode("FilterOptions", "cmdblist"));
	}

	public static List<String> getCmdchat() {
		return getStringList(rootNode.getNode("FilterOptions", "cmdchat"));
	}

	public static Order getCmdpriority() {
		return Order.valueOf(rootNode.getNode("cmdpriority").getString("LAST").toUpperCase());
	}

	public static Order getChatpriority() {
		return Order.valueOf(rootNode.getNode("chatpriority").getString("LAST").toUpperCase());
	}

	public static boolean cmdfilterEnabled() {
		return rootNode.getNode("FilterOptions").getNode("commandfilter").getBoolean(true);
	}

	public static boolean commandspamfilterEnabled() {
		return rootNode.getNode("FilterOptions").getNode("commandspamfilter").getBoolean(true);
	}

	public static boolean spamfilterEnabled() {
		return rootNode.getNode("FilterOptions").getNode("spamfilter").getBoolean(true);
	}

	public static Order getBookpriority() {
		return Order.valueOf(rootNode.getNode("bookpriority").getString("LAST").toUpperCase());
	}

	public static boolean bookfilterEnabled() {
		return rootNode.getNode("FilterOptions").getNode("bookfilter").getBoolean(true);
	}

	public static boolean itemFilterEnabled() {
		return rootNode.getNode("FilterOptions").getNode("itemfilter").getBoolean(true);
	}

	public static Order getItempriority() {
		return Order.valueOf(rootNode.getNode("FilterOptions").getNode("itempriority").getString("LAST").toUpperCase());
	}

	public static boolean consolefilterEnabled() {
		return rootNode.getNode("FilterOptions").getNode("consolefilter").getBoolean(false);
	}

	public static Order getSignpriority() {
		return Order.valueOf(rootNode.getNode("signpriority").getString("LAST").toUpperCase());
	}

	public static boolean signfilterEnabled() {
		return rootNode.getNode("FilterOptions").getNode("signfilter").getBoolean(true);
	}


	public static String getString(String stringName) {
		return rootNode.getNode("Strings").getNode(stringName).getString("UNKNOWN STRING ;-; "+stringName);
	}
}
