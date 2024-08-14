package dev.shadowsoffire.apothic_attributes.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import dev.shadowsoffire.apothic_attributes.api.IFormattableAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.BooleanAttribute;

/**
 * Handles formatting rules for boolean attributes since Neo is not aware of the formatting API.
 */
@Mixin(value = BooleanAttribute.class, remap = false)
public class BooleanAttributeMixin implements IFormattableAttribute {

    @Override
    public MutableComponent toValueComponent(@Nullable Operation op, double value, TooltipFlag flag) {
        if (op == null) {
            return Component.translatable("apothic_attributes.value.boolean." + (value > 0 ? "enabled" : "disabled"));
        }
        else if (op == Operation.ADD_VALUE && value > 0) {
            return Component.translatable("apothic_attributes.value.boolean.enable");
        }
        else if (op == Operation.ADD_MULTIPLIED_TOTAL && (int) value == -1) {
            return Component.translatable("apothic_attributes.value.boolean.force_disable");
        }
        else {
            return Component.translatable("apothic_attributes.value.boolean.invalid");
        }
    }

    @Override
    public MutableComponent toComponent(AttributeModifier modif, TooltipFlag flag) {
        Attribute attr = this.ths();
        double value = modif.amount();

        MutableComponent comp;

        if (value > 0.0D) {
            comp = Component.translatable("apothic_attributes.modifier.bool", this.toValueComponent(modif.operation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.BLUE);
        }
        else {
            value *= -1.0D;
            comp = Component.translatable("apothic_attributes.modifier.bool", this.toValueComponent(modif.operation(), value, flag), Component.translatable(attr.getDescriptionId())).withStyle(ChatFormatting.RED);
        }

        return comp.append(this.getDebugInfo(modif, flag));
    }

}
