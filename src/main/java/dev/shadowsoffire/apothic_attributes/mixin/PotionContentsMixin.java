package dev.shadowsoffire.apothic_attributes.mixin;

import java.util.List;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.IFormattableAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

@Mixin(value = PotionContents.class, remap = false)
public class PotionContentsMixin {

    /**
     * Redirects the second {@link List#isEmpty()} call that is checked before adding tooltips to potions to replace vanilla tooltip handling.<br>
     * Target Line: <code>if (!list.isEmpty()) {</code>.
     *
     * @param list           The potion's attribute modifiers.
     * @param stack          The potion stack.
     * @param tooltips       The tooltip list.
     * @param durationFactor The duration factor of the potion.
     * @return True, unconditionally, so that the vanilla tooltip logic is ignored.
     * @see PotionUtils#addPotionTooltip(ItemStack, List, float)
     * @see PotionUtils#addPotionTooltip(List, List, float)
     */
    @Redirect(method = "addPotionTooltip(Ljava/lang/Iterable;Ljava/util/function/Consumer;FF)V", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"), require = 1)
    private static boolean apothic_attributes_potionTooltips(List<Pair<Holder<Attribute>, AttributeModifier>> list, Iterable<MobEffectInstance> effects, Consumer<Component> tooltips, float durationFactor) {
        if (!list.isEmpty()) {
            tooltips.accept(CommonComponents.EMPTY);
            tooltips.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));

            for (Pair<Holder<Attribute>, AttributeModifier> pair : list) {
                tooltips.accept(IFormattableAttribute.toComponent(pair.getFirst(), pair.getSecond(), ApothicAttributes.getTooltipFlag()));
            }
        }
        return true;
    }

}
