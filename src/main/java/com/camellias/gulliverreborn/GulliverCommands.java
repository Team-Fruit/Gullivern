package com.camellias.gulliverreborn;

import com.artemis.artemislib.attributes.Attributes;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GulliverCommands {
    private static UUID uuidHeight = UUID.fromString("5440b01a-974f-4495-bb9a-c7c87424bca4");
    private static UUID uuidWidth = UUID.fromString("3949d2ed-b6cc-4330-9c13-98777f48ea51");
    private static UUID uuidReach1 = UUID.fromString("854e0004-c218-406c-a9e2-590f1846d80b");
    private static UUID uuidReach2 = UUID.fromString("216080dc-22d3-4eff-a730-190ec0210d5c");
    private static UUID uuidHealth = UUID.fromString("3b901d47-2d30-495c-be45-f0091c0f6fb2");
    private static UUID uuidStrength = UUID.fromString("558f55be-b277-4091-ae9b-056c7bc96e84");
    private static UUID uuidSpeed = UUID.fromString("f2fb5cda-3fbe-4509-a0af-4fc994e6aeca");

    public void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralCommandNode<CommandSource> node = dispatcher.register(
                LiteralArgumentBuilder.<CommandSource>literal(GulliverReborn.MODID)
                        .requires(cs -> cs.hasPermissionLevel(0))
                        .then(
                                RequiredArgumentBuilder.<CommandSource, Float>argument("size", FloatArgumentType.floatArg(0.125F))
                                        .executes(ctx -> {
                                            PlayerEntity sender = ctx.getSource().asPlayer();
                                            float size = FloatArgumentType.getFloat(ctx, "size");
                                            changeSize(sender, size);
                                            ctx.getSource().sendFeedback(
                                                    sender.getDisplayName()
                                                            .appendSibling(new StringTextComponent(" set their size to " + size)),
                                                    false
                                            );
                                            return 1;
                                        })
                                        .then(
                                                RequiredArgumentBuilder.<CommandSource, EntitySelector>argument("entities", EntityArgument.entities())
                                                        .requires(cs -> cs.hasPermissionLevel(4))
                                                        .executes(ctx -> {
                                                            Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "entities");
                                                            float size = FloatArgumentType.getFloat(ctx, "size");
                                                            Map<String, List<LivingEntity>> listEntity = entities.stream()
                                                                    .filter(LivingEntity.class::isInstance)
                                                                    .map(LivingEntity.class::cast)
                                                                    .peek(entity -> changeSize(entity, size))
                                                                    .collect(Collectors.groupingBy(entity -> entity.getDisplayName().getFormattedText()));
                                                            ITextComponent text = TextComponentUtils.makeList(listEntity.entrySet(), entry -> {
                                                                int length = entry.getValue().size();
                                                                if (length <= 0)
                                                                    return new StringTextComponent("");
                                                                ITextComponent name = entry.getValue().get(0).getDisplayName();
                                                                if (length > 1)
                                                                    return new StringTextComponent("")
                                                                            .appendSibling(new StringTextComponent(length + "×")
                                                                                    .setStyle(new Style().setColor(TextFormatting.GRAY)))
                                                                            .appendSibling(name);
                                                                return name;
                                                            });
                                                            ctx.getSource().sendFeedback(
                                                                    new StringTextComponent("")
                                                                            .appendSibling(new StringTextComponent("[").setStyle(new Style().setColor(TextFormatting.GRAY)))
                                                                            .appendSibling(text)
                                                                            .appendSibling(new StringTextComponent("]").setStyle(new Style().setColor(TextFormatting.GRAY)))
                                                                            .appendSibling(new StringTextComponent(" set their size to " + size)),
                                                                    true
                                                            );
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }

    public void changeSize(LivingEntity sender, float size) {
        Multimap<String, AttributeModifier> attributes = HashMultimap.create();
        Multimap<String, AttributeModifier> removeableAttributes = HashMultimap.create();
        Multimap<String, AttributeModifier> removeableAttributes2 = HashMultimap.create();

        attributes.put(Attributes.ENTITY_HEIGHT.getName(), new AttributeModifier(uuidHeight, "Player Height", size - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        attributes.put(Attributes.ENTITY_WIDTH.getName(), new AttributeModifier(uuidWidth, "Player Width", MathHelper.clamp(size - 1, 0.4 - 1, Config.GENERAL.MAX_SIZE.get()), AttributeModifier.Operation.MULTIPLY_TOTAL));

        if (Config.MODIFIER.SPEED_MODIFIER.get())
            attributes.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(uuidSpeed, "Player Speed", (size - 1) / 2, AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (Config.MODIFIER.REACH_MODIFIER.get())
            removeableAttributes.put(PlayerEntity.REACH_DISTANCE.getName(), new AttributeModifier(uuidReach1, "Player Reach 1", size - 1, AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (Config.MODIFIER.REACH_MODIFIER.get())
            removeableAttributes2.put(PlayerEntity.REACH_DISTANCE.getName(), new AttributeModifier(uuidReach2, "Player Reach 2", -MathHelper.clamp(size - 1, 0.33, Double.MAX_VALUE), AttributeModifier.Operation.MULTIPLY_TOTAL));
        if (Config.MODIFIER.STRENGTH_MODIFIER.get())
            attributes.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(uuidStrength, "Player Strength", size - 1, AttributeModifier.Operation.ADDITION));
        if (Config.MODIFIER.HEALTH_MODIFIER.get())
            attributes.put(SharedMonsterAttributes.MAX_HEALTH.getName(), new AttributeModifier(uuidHealth, "Player Health", (size - 1) * Config.GENERAL.HEALTH_MULTIPLIER.get(), AttributeModifier.Operation.MULTIPLY_TOTAL));

        if (size > 1) {
            sender.getAttributes().applyAttributeModifiers(removeableAttributes);
        } else {
            sender.getAttributes().removeAttributeModifiers(removeableAttributes);
        }

        if (size < 1) {
            sender.getAttributes().applyAttributeModifiers(removeableAttributes2);
        } else {
            sender.getAttributes().removeAttributeModifiers(removeableAttributes2);
        }

        sender.getAttributes().applyAttributeModifiers(attributes);
        sender.setHealth(sender.getMaxHealth());
    }
}