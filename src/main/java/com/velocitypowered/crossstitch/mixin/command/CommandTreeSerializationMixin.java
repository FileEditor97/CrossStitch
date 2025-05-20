package com.velocitypowered.crossstitch.mixin.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.velocitypowered.crossstitch.command.ModIdArgument;
import io.netty.buffer.Unpooled;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(CommandTreeS2CPacket.class)
public class CommandTreeSerializationMixin {
	@Unique
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandTreeSerializationMixin.class);

	@Unique
	private static final Identifier MOD_ARGUMENT_INDICATOR = new Identifier("crossstitch:mod_argument");

	@Redirect(method = "writeNode", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/ArgumentTypes;toPacket(Lnet/minecraft/network/PacketByteBuf;Lcom/mojang/brigadier/arguments/ArgumentType;)V"))
	private static void writeNode$wrapInVelocityModArgument(PacketByteBuf packetByteBuf, ArgumentType<?> type) {
		try {
			ArgumentTypes.Entry entry = ArgumentTypes.byClass(type);
			if (entry == null) {
				if (type.getClass().getName().equals("net.minecraftforge.server.command.ModIdArgument")) {
					serializeWrappedArgumentType(packetByteBuf, type,
						new ArgumentTypes.Entry(
							new ConstantArgumentSerializer<>(ModIdArgument::modIdArgument),
							new Identifier("forge:modid")
						)
					);
					LOGGER.info("Wrapped {}", type.getClass().getName());
					return;
				}
				LOGGER.error("Could not serialize {} ({}) - will not be sent to client!", type, type.getClass().getName());
				packetByteBuf.writeIdentifier(new Identifier("crossstitch:unknown"));
				return;
			}

			if (entry.id.getNamespace().equals("minecraft") || entry.id.getNamespace().equals("brigadier")) {
				packetByteBuf.writeIdentifier(entry.id);
				entry.serializer.toPacket(type, packetByteBuf);
				return;
			}

			// Not a standard Minecraft argument type - so we need to wrap it
			serializeWrappedArgumentType(packetByteBuf, type, entry);

		} catch (Exception e) {
			LOGGER.error("Failed to serialize argument: {}", e.getMessage(), e);
		}
	}

	@Unique
	private static void serializeWrappedArgumentType(PacketByteBuf packetByteBuf, ArgumentType argumentType, ArgumentTypes.Entry entry) {
		packetByteBuf.writeIdentifier(MOD_ARGUMENT_INDICATOR);
		packetByteBuf.writeIdentifier(entry.id);

		PacketByteBuf extraData = new PacketByteBuf(Unpooled.buffer());
		entry.serializer.toPacket(argumentType, extraData);

		packetByteBuf.writeVarInt(extraData.readableBytes());
		packetByteBuf.writeBytes(extraData).slice();
	}
}