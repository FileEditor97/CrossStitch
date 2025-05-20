package com.velocitypowered.crossstitch.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModIdArgument implements ArgumentType<String> {
	private static final List<String> EXAMPLES = Arrays.asList("fabric", "modmenu");

	public static ModIdArgument modIdArgument() {
		return new ModIdArgument();
	}

	@Override
	public String parse(StringReader reader) {
		return reader.readUnquotedString();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(FabricLoader.getInstance().getAllMods().stream().map(container -> container.getMetadata().getId()), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}