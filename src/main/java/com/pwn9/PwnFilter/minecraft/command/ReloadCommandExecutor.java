package com.pwn9.PwnFilter.minecraft.command;

import com.pwn9.PwnFilter.FilterEngine;
import com.pwn9.PwnFilter.minecraft.PwnFilterPlugin;
import com.pwn9.PwnFilter.rules.RuleManager;
import com.pwn9.PwnFilter.util.LogManager;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class ReloadCommandExecutor implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		src.sendMessage(Text.of(TextColors.RED, "Reloading pwnfilter.conf and rules/*.txt files."));

		LogManager.info("Disabling all listeners");
		FilterEngine.getInstance().disableClients();

		PwnFilterPlugin.getInstance().configurePlugin();

		LogManager.info("Reloaded config.yml as requested by " + src.getName());

		RuleManager.getInstance().reloadAllConfigs();
		LogManager.info("All rules reloaded by " + src.getName());

		// Re-register our listeners
		FilterEngine.getInstance().enableClients();
		LogManager.info("All listeners re-enabled");

		return CommandResult.success();
	}

}
