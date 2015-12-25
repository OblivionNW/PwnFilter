package com.pwn9.PwnFilter.minecraft.command;

import com.pwn9.PwnFilter.minecraft.api.MinecraftConsole;
import com.pwn9.PwnFilter.util.LogManager;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;

public class ClearChatCommandExecutor implements CommandExecutor {

	@Override
	public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
		src.sendMessage(Texts.of(TextColors.RED, "Clearing chat screen"));
		LogManager.info("chat screen cleared by " + src.getName());

		for(int i = 0; i < 120; i++) {
			MinecraftConsole.getInstance().sendBroadcast("");
		}

		return CommandResult.success();
	}

}
