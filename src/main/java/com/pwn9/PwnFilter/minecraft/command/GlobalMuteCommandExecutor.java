package com.pwn9.PwnFilter.minecraft.command;

import com.pwn9.PwnFilter.config.SpongeConfig;
import com.pwn9.PwnFilter.minecraft.api.MinecraftConsole;
import com.pwn9.PwnFilter.util.LogManager;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.format.TextColors;

public class GlobalMuteCommandExecutor implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		if (SpongeConfig.isGlobalMute()) {
			MinecraftConsole.getInstance().sendBroadcast(TextColors.RED + "Global mute cancelled by " + src.getName());
			LogManager.info("global mute cancelled by " + src.getName());
			SpongeConfig.setGlobalMute(false);
		}
		else {
			MinecraftConsole.getInstance().sendBroadcast(TextColors.RED + "Global mute initiated by " + src.getName());
			LogManager.info("global mute initiated by " + src.getName());
			SpongeConfig.setGlobalMute(true);
		}
		return CommandResult.success();
	}

}
