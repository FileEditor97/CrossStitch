package com.velocitypowered.crossstitch.util;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ArgumentTypeRegistryAccessor {
	public record Entry<T extends ArgumentType<?>>(ArgumentSerializer<T> serializer, Identifier id) {}

	private static final Map<Class<?>, Entry<?>> CLASS_MAP_COPY = new HashMap<>();

	static {
		try {
			Field field = ArgumentTypes.class.getDeclaredField("CLASS_MAP");
			field.setAccessible(true);
			Map<?, ?> internal = (Map<?, ?>) field.get(null);

			for (Map.Entry<?, ?> e : internal.entrySet()) {
				Class<?> key = (Class<?>) e.getKey();

				Object entry = e.getValue();
				Field idField = entry.getClass().getDeclaredField("id");
				Field serializerField = entry.getClass().getDeclaredField("serializer");

				idField.setAccessible(true);
				serializerField.setAccessible(true);

				Identifier id = (Identifier) idField.get(entry);
				ArgumentSerializer<?> serializer = (ArgumentSerializer<?>) serializerField.get(entry);

				CLASS_MAP_COPY.put(key, new Entry<>(serializer, id));
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to access ArgumentTypes.CLASS_MAP", e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends ArgumentType<?>> Entry<T> getEntry(T type) {
		return (Entry<T>) CLASS_MAP_COPY.get(type.getClass());
	}

	public static boolean isKnown(ArgumentType<?> type) {
		return CLASS_MAP_COPY.containsKey(type.getClass());
	}
}
