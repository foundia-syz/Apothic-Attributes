package dev.shadowsoffire.apothic_attributes.commands;

import java.util.Arrays;
import java.util.Locale;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class BonusModifierCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_OP = (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Operation.values()).map(Operation::name), builder);

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_SLOT = (ctx, builder) -> SharedSuggestionProvider
        .suggest(Arrays.stream(EquipmentSlotGroup.values()).map(EquipmentSlotGroup::name).map(s -> s.toLowerCase(Locale.ROOT)), builder);

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_ATTRIB = (ctx, builder) -> SharedSuggestionProvider.suggest(BuiltInRegistries.ATTRIBUTE.keySet().stream().map(ResourceLocation::toString), builder);

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("add_bonus_modifier").requires(c -> c.hasPermission(2))
            .then(Commands.argument("attribute", ResourceLocationArgument.id()).suggests(SUGGEST_ATTRIB)
                .then(Commands.argument("op", StringArgumentType.word()).suggests(SUGGEST_OP).then(Commands.argument("value", FloatArgumentType.floatArg())
                    .then(Commands.argument("slot", StringArgumentType.word()).suggests(SUGGEST_SLOT).executes(c -> {
                        Player p = c.getSource().getPlayerOrException();
                        Holder<Attribute> attrib = BuiltInRegistries.ATTRIBUTE.getHolder(c.getArgument("attribute", ResourceLocation.class)).orElseThrow();
                        Operation op = Operation.valueOf(c.getArgument("op", String.class).toUpperCase(Locale.ROOT));
                        EquipmentSlotGroup slot = EquipmentSlotGroup.valueOf(c.getArgument("slot", String.class).toUpperCase(Locale.ROOT));
                        float value = c.getArgument("value", Float.class);
                        ItemStack stack = p.getMainHandItem();

                        ItemAttributeModifiers bonus = stack.getOrDefault(ALObjects.Components.BONUS_ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
                        AttributeModifier modif = new AttributeModifier(ApothicAttributes.loc("command_generated_" + p.level().random.nextInt()), value, op);

                        stack.set(ALObjects.Components.BONUS_ATTRIBUTE_MODIFIERS, bonus.withModifierAdded(attrib, modif, slot));
                        return 0;
                    }))))));
    }

}
