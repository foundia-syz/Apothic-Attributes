package dev.shadowsoffire.apothic_attributes.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import dev.shadowsoffire.apothic_attributes.client.ModifierSource.EffectModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSource.ItemModifierSource;
import dev.shadowsoffire.apothic_attributes.util.Comparators;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.AttributeUtil;

/**
 * A Modifier Source Type is a the registration component of a ModifierSource.
 *
 * @param <T>
 */
public abstract class ModifierSourceType<T> {

    private static final List<ModifierSourceType<?>> SOURCE_TYPES = new ArrayList<>();

    public static final ModifierSourceType<ItemStack> EQUIPMENT = register(new ModifierSourceType<>(){

        @Override
        public void extract(LivingEntity entity, BiConsumer<AttributeModifier, ModifierSource<?>> map) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack item = entity.getItemBySlot(slot);
                item.forEachModifier(slot, (attr, modif) -> {
                    map.accept(modif, new ItemModifierSource(item));
                });
            }
        }

        @Override
        public int getPriority() {
            return 0;
        }

    });

    public static final ModifierSourceType<MobEffectInstance> MOB_EFFECT = register(new ModifierSourceType<>(){

        @Override
        public void extract(LivingEntity entity, BiConsumer<AttributeModifier, ModifierSource<?>> map) {
            for (MobEffectInstance effectInst : entity.getActiveEffects()) {
                effectInst.getEffect().value().attributeModifiers.values().forEach(template -> {
                    map.accept(template.create(effectInst.getAmplifier()), new EffectModifierSource(effectInst));
                });
            }
        }

        @Override
        public int getPriority() {
            return 100;
        }

    });

    public static Collection<ModifierSourceType<?>> getTypes() {
        return Collections.unmodifiableCollection(SOURCE_TYPES);
    }

    public static <T extends ModifierSourceType<?>> T register(T type) {
        SOURCE_TYPES.add(type);
        return type;
    }

    public static Comparator<AttributeModifier> compareBySource(Map<ResourceLocation, ModifierSource<?>> sources) {

        Comparator<AttributeModifier> comp = Comparators.chained(
            Comparator.comparingInt(a -> sources.get(a.id()).getType().getPriority()),
            Comparator.comparing(a -> sources.get(a.id())),
            AttributeUtil.ATTRIBUTE_MODIFIER_COMPARATOR);

        return (a1, a2) -> {
            var src1 = sources.get(a1.id());
            var src2 = sources.get(a2.id());

            if (src1 != null && src2 != null) return comp.compare(a1, a2);

            return src1 != null ? -1 : src2 != null ? 1 : 0;
        };
    }

    /**
     * Extracts all ModifierSource(s) of this type from the source entity.
     *
     * @param entity
     * @param map
     */
    public abstract void extract(LivingEntity entity, BiConsumer<AttributeModifier, ModifierSource<?>> map);

    /**
     * Integer priority for display sorting.<br>
     * Lower priority values will be displayed at the top of the list.
     */
    public abstract int getPriority();

}
