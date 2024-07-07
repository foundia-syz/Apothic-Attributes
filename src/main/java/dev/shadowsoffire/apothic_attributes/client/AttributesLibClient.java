package dev.shadowsoffire.apothic_attributes.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Pair;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.AttributeHelper;
import dev.shadowsoffire.apothic_attributes.api.IFormattableAttribute;
import dev.shadowsoffire.apothic_attributes.api.client.AddAttributeTooltipsEvent;
import dev.shadowsoffire.apothic_attributes.api.client.GatherEffectScreenTooltipsEvent;
import dev.shadowsoffire.apothic_attributes.api.client.GatherSkippedAttributeTooltipsEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffect.AttributeTemplate;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class AttributesLibClient {

    private static final ResourceLocation FAKE_MERGED_ID = ApothicAttributes.loc("fake_merged_modifier");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tooltips(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();
        List<Component> list = e.getToolTip();
        int markIdx1 = -1, markIdx2 = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getContents() instanceof LiteralContents tc) {
                if ("APOTH_REMOVE_MARKER".equals(tc.text())) {
                    markIdx1 = i;
                }
                if ("APOTH_REMOVE_MARKER_2".equals(tc.text())) {
                    markIdx2 = i;
                    break;
                }
            }
        }
        if (markIdx1 == -1 || markIdx2 == -1) return;
        var it = list.listIterator(markIdx1);
        for (int i = markIdx1; i < markIdx2 + 1; i++) {
            it.next();
            it.remove();
        }
        boolean shouldShow = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).showInTooltip();
        if (shouldShow) {
            applyModifierTooltips(e.getEntity(), stack, it::add, e.getFlags());
        }
        NeoForge.EVENT_BUS.post(new AddAttributeTooltipsEvent(stack, e.getEntity(), list, it, e.getFlags()));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void addAttribComponent(ScreenEvent.Init.Post e) {
        if (ALConfig.enableAttributesGui && e.getScreen() instanceof InventoryScreen scn) {
            var atrComp = new AttributesGui(scn);
            e.addListener(atrComp);
            e.addListener(atrComp.toggleBtn);
            e.addListener(atrComp.hideUnchangedBtn);
            if (AttributesGui.wasOpen || AttributesGui.swappedFromCurios) atrComp.toggleVisibility();
            AttributesGui.swappedFromCurios = false;
        }
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void effectGuiTooltips(GatherEffectScreenTooltipsEvent e) {
        List<Component> tooltips = e.getTooltip();
        MobEffectInstance effectInst = e.getEffectInstance();
        Holder<MobEffect> effect = effectInst.getEffect();

        MutableComponent name = (MutableComponent) tooltips.get(0);
        Component duration = tooltips.remove(1);
        duration = Component.translatable("(%s)", duration).withStyle(ChatFormatting.WHITE);

        name.append(" ").append(duration);

        if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
            name.append(" ").append(Component.translatable("[%s]", effect.unwrapKey().get().location().toString()).withStyle(ChatFormatting.GRAY));
        }

        String key = effect.value().getDescriptionId() + ".desc";
        if (I18n.exists(key)) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        }
        else if (ApothicAttributes.getTooltipFlag().isAdvanced() && effect.value().attributeModifiers.isEmpty()) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        List<Pair<Holder<Attribute>, AttributeModifier>> list = Lists.newArrayList();
        Map<Holder<Attribute>, AttributeTemplate> map = effect.value().attributeModifiers;
        if (!map.isEmpty()) {
            for (Map.Entry<Holder<Attribute>, AttributeTemplate> entry : map.entrySet()) {
                AttributeTemplate template = entry.getValue();
                list.add(new Pair<>(entry.getKey(), template.create(effectInst.getAmplifier())));
            }
        }

        if (!list.isEmpty()) {
            for (Pair<Holder<Attribute>, AttributeModifier> pair : list) {
                tooltips.add(IFormattableAttribute.toComponent(pair.getFirst(), pair.getSecond(), ApothicAttributes.getTooltipFlag()));
            }
        }
    }

    @SubscribeEvent
    public void potionTooltips(ItemTooltipEvent e) {
        if (!ALConfig.enablePotionTooltips) return;

        ItemStack stack = e.getItemStack();
        List<Component> tooltips = e.getToolTip();

        if (stack.getItem() instanceof PotionItem) {
            List<MobEffectInstance> effects = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).customEffects();
            if (effects.size() == 1 && tooltips.size() >= 2) {
                MobEffect effect = effects.get(0).getEffect().value();
                String key = effect.getDescriptionId() + ".desc";
                if (I18n.exists(key)) {
                    tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
                }
                else if (e.getFlags().isAdvanced() && effect.attributeModifiers.isEmpty()) {
                    tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
        }
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> getSortedModifiers(ItemStack stack, EquipmentSlot slot) {
        var unsorted = stack.getAttributeModifiers();
        Multimap<Holder<Attribute>, AttributeModifier> map = AttributeHelper.sortedMap();
        unsorted.forEach(slot, (attr, modif) -> {
            if (attr != null && modif != null) {
                map.put(attr, modif);
            }
            else {
                ApothicAttributes.LOGGER.debug("Detected broken attribute modifier entry on item {}.  Attr={}, Modif={}", stack, attr, modif);
            }
        });
        return map;
    }

    public static void apothCrit(int entityId) {
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, ALObjects.Particles.APOTH_CRIT.get());
        }
    }

    private static void applyModifierTooltips(@Nullable Player player, ItemStack stack, Consumer<Component> tooltip, TooltipFlag flag) {
        Multimap<Holder<Attribute>, AttributeModifier> mainhand = getSortedModifiers(stack, EquipmentSlot.MAINHAND);
        Multimap<Holder<Attribute>, AttributeModifier> offhand = getSortedModifiers(stack, EquipmentSlot.OFFHAND);
        Multimap<Holder<Attribute>, AttributeModifier> dualHand = AttributeHelper.sortedMap();
        for (Holder<Attribute> atr : mainhand.keys()) {
            Collection<AttributeModifier> modifMh = mainhand.get(atr);
            Collection<AttributeModifier> modifOh = offhand.get(atr);
            modifMh.stream().filter(a1 -> modifOh.stream().anyMatch(a2 -> a1.id().equals(a2.id()))).forEach(modif -> dualHand.put(atr, modif));
        }

        dualHand.values().forEach(m -> {
            mainhand.values().remove(m);
            offhand.values().removeIf(m1 -> m1.id().equals(m.id()));
        });

        Set<ResourceLocation> skips = new HashSet<>();
        NeoForge.EVENT_BUS.post(new GatherSkippedAttributeTooltipsEvent(stack, player, skips, flag));

        applyTextFor(player, stack, tooltip, dualHand, "both_hands", skips, flag);
        applyTextFor(player, stack, tooltip, mainhand, EquipmentSlot.MAINHAND.getName(), skips, flag);
        applyTextFor(player, stack, tooltip, offhand, EquipmentSlot.OFFHAND.getName(), skips, flag);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.ordinal() < 2) continue;
            Multimap<Holder<Attribute>, AttributeModifier> modifiers = getSortedModifiers(stack, slot);
            applyTextFor(player, stack, tooltip, modifiers, slot.getName(), skips, flag);
        }
    }

    private static MutableComponent padded(String padding, Component comp) {
        return Component.literal(padding).append(comp);
    }

    private static MutableComponent list() {
        return AttributeHelper.list();
    }

    public static class ModBusSub {
        @SubscribeEvent
        public static void clientReload(RegisterClientReloadListenersEvent e) {
            e.registerReloadListener(ALConfig.makeReloader());
        }

        // @SubscribeEvent
        // public static void clientSetup(FMLClientSetupEvent e) {
        // if (ModList.get().isLoaded("curios")) {
        // NeoForge.EVENT_BUS.register(new CuriosClientCompat());
        // }
        // }

        @SubscribeEvent
        public static void particleFactories(RegisterParticleProvidersEvent e) {
            e.registerSprite(ALObjects.Particles.APOTH_CRIT.get(), ApothCritParticle::new);
        }
    }

    private static record BaseModifier(AttributeModifier base, List<AttributeModifier> children) {}

    private static void applyTextFor(@Nullable Player player, ItemStack stack, Consumer<Component> tooltip, Multimap<Holder<Attribute>, AttributeModifier> modifierMap, String group, Set<ResourceLocation> skips, TooltipFlag flag) {
        if (!modifierMap.isEmpty()) {
            modifierMap.values().removeIf(m -> skips.contains(m.id()));

            tooltip.accept(Component.empty());
            tooltip.accept(Component.translatable("item.modifiers." + group).withStyle(ChatFormatting.GRAY));

            if (modifierMap.isEmpty()) return;

            Map<Holder<Attribute>, BaseModifier> baseModifs = new IdentityHashMap<>();

            modifierMap.forEach((attr, modif) -> {
                if (modif.id().equals(IFormattableAttribute.cast(attr).getBaseID())) {
                    baseModifs.put(attr, new BaseModifier(modif, new ArrayList<>()));
                }
            });

            modifierMap.forEach((attr, modif) -> {
                BaseModifier base = baseModifs.get(attr);
                if (base != null && base.base != modif) {
                    base.children.add(modif);
                }
            });

            for (Map.Entry<Holder<Attribute>, BaseModifier> entry : baseModifs.entrySet()) {
                Holder<Attribute> attr = entry.getKey();
                BaseModifier baseModif = entry.getValue();
                double entityBase = player == null ? 0 : player.getAttributeBaseValue(attr);
                double base = baseModif.base.amount() + entityBase;
                final double rawBase = base;
                double amt = base;
                double baseBonus = IFormattableAttribute.cast(attr).getBonusBaseValue(stack);
                for (AttributeModifier modif : baseModif.children) {
                    if (modif.operation() == Operation.ADD_VALUE) base = amt = amt + modif.amount();
                    else if (modif.operation() == Operation.ADD_MULTIPLIED_BASE) amt += modif.amount() * base;
                    else amt *= 1 + modif.amount();
                }
                amt += baseBonus;
                boolean isMerged = !baseModif.children.isEmpty() || baseBonus != 0;
                MutableComponent text = IFormattableAttribute.toBaseComponent(attr, amt, entityBase, isMerged, flag);
                tooltip.accept(padded(" ", text).withStyle(isMerged ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN));
                if (Screen.hasShiftDown() && isMerged) {
                    // Display the raw base value, and then all children modifiers.
                    text = IFormattableAttribute.toBaseComponent(attr, rawBase, entityBase, false, flag);
                    tooltip.accept(list().append(text.withStyle(ChatFormatting.DARK_GREEN)));
                    for (AttributeModifier modifier : baseModif.children) {
                        tooltip.accept(list().append(IFormattableAttribute.toComponent(attr, modifier, flag)));
                    }
                    if (baseBonus > 0) {
                        IFormattableAttribute.cast(attr).addBonusTooltips(stack, tooltip, flag);
                    }
                }
            }

            for (Holder<Attribute> attr : modifierMap.keySet()) {
                if (baseModifs.containsKey(attr)) continue;
                Collection<AttributeModifier> modifs = modifierMap.get(attr);
                // Initiate merged-tooltip logic if we have more than one modifier for a given attribute.
                if (modifs.size() > 1) {
                    double[] sums = new double[3];
                    boolean[] merged = new boolean[3];
                    Map<Operation, List<AttributeModifier>> shiftExpands = new HashMap<>();
                    for (AttributeModifier modifier : modifs) {
                        if (modifier.amount() == 0) continue;
                        if (sums[modifier.operation().ordinal()] != 0) merged[modifier.operation().ordinal()] = true;
                        sums[modifier.operation().ordinal()] += modifier.amount();
                        shiftExpands.computeIfAbsent(modifier.operation(), k -> new LinkedList<>()).add(modifier);
                    }
                    for (Operation op : Operation.values()) {
                        int i = op.ordinal();
                        if (sums[i] == 0) continue;
                        if (merged[i]) {
                            TextColor color = sums[i] < 0 ? TextColor.fromRgb(0xF93131) : TextColor.fromRgb(0x7A7AF9);
                            if (sums[i] < 0) sums[i] *= -1;
                            var fakeModif = new AttributeModifier(FAKE_MERGED_ID, sums[i], op);
                            MutableComponent comp = IFormattableAttribute.toComponent(attr, fakeModif, flag);
                            tooltip.accept(comp.withStyle(comp.getStyle().withColor(color)));
                            if (merged[i] && Screen.hasShiftDown()) {
                                shiftExpands.get(Operation.BY_ID.apply(i)).forEach(modif -> tooltip.accept(list().append(IFormattableAttribute.toComponent(attr, modif, flag))));
                            }
                        }
                        else {
                            var fakeModif = new AttributeModifier(FAKE_MERGED_ID, sums[i], op);
                            tooltip.accept(IFormattableAttribute.toComponent(attr, fakeModif, flag));
                        }
                    }
                }
                else modifs.forEach(m -> {
                    if (m.amount() != 0) tooltip.accept(IFormattableAttribute.toComponent(attr, m, flag));
                });
            }
        }
    }

    public static class ApothCritParticle extends CritParticle {

        public ApothCritParticle(SimpleParticleType type, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
            super(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
            this.bCol = 1F;
            this.rCol = 0.3F;
            this.gCol = 0.8F;
        }

    }

}
