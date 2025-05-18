package com.velocitypowered.crossstitch.mixin.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.velocitypowered.crossstitch.util.ArgumentTypeRegistryAccessor;
import io.netty.buffer.Unpooled;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CommandTreeS2CPacket.class)
public class CommandTreeSerializationMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("CrossStitch");
    @Unique
    private static final Identifier MOD_ARGUMENT_INDICATOR = new Identifier("crossstitch:mod_argument");

    @Redirect(method = "writeNode",at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/command/argument/ArgumentTypes;toPacket(Lnet/minecraft/network/PacketByteBuf;Lcom/mojang/brigadier/arguments/ArgumentType;)V"
    ))
    private static void writeNode$wrapInVelocityModArgument(PacketByteBuf packetByteBuf, ArgumentType<?> type) {
		try {
			ArgumentTypeRegistryAccessor.Entry<?> entry = ArgumentTypeRegistryAccessor.getEntry(type);
            if (entry == null) {
                LOGGER.warn("Unknown ArgumentType class: {}", type);
                packetByteBuf.writeIdentifier(new Identifier(""));
                return;
            }

			if (entry.id().getNamespace().equals("minecraft") || entry.id().getNamespace().equals("brigadier")) {
				packetByteBuf.writeIdentifier(new Identifier(""));
				return;
			}

			// Not a standard Minecraft argument type - so we need to wrap it
			serializeWrappedArgumentType(packetByteBuf, type, entry);
		} catch (Exception e) {
			LOGGER.error("Failed to serialize ArgumentType {}: {}", type.getClass(), e.getMessage(), e);
            packetByteBuf.writeIdentifier(new Identifier(""));
		}
	}

    @Unique
    private static void serializeWrappedArgumentType(PacketByteBuf packetByteBuf, ArgumentType<?> argumentType, ArgumentTypeRegistryAccessor.Entry<?> entry) {
        packetByteBuf.writeIdentifier(MOD_ARGUMENT_INDICATOR);
        packetByteBuf.writeIdentifier(entry.id());

        PacketByteBuf extraData = new PacketByteBuf(Unpooled.buffer());
		//noinspection unchecked
		ArgumentSerializer<ArgumentType<?>> serializer = (ArgumentSerializer<ArgumentType<?>>) entry.serializer();
        serializer.toPacket(argumentType, extraData);

        packetByteBuf.writeVarInt(extraData.readableBytes());
        packetByteBuf.writeBytes(extraData);
    }
}