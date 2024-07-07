package dev.shadowsoffire.apothic_attributes.event;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The {@link ApotheosisCommandEvent} is fired during the {@link RegisterCommandsEvent} to allow registration of children of the /apoth command.
 */
public class ApotheosisCommandEvent extends Event {

    private final LiteralArgumentBuilder<CommandSourceStack> root;

    public ApotheosisCommandEvent(LiteralArgumentBuilder<CommandSourceStack> root) {
        this.root = root;
    }

    public LiteralArgumentBuilder<CommandSourceStack> getRoot() {
        return this.root;
    }
}
