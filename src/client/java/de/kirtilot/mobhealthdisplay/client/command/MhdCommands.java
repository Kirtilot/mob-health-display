package de.kirtilot.mobhealthdisplay.client.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;
import de.kirtilot.mobhealthdisplay.client.config.MhdSettings;
import de.kirtilot.mobhealthdisplay.client.config.MhdSettings.BoolSetting;
import de.kirtilot.mobhealthdisplay.client.config.MhdSettings.FloatSetting;
import de.kirtilot.mobhealthdisplay.client.filter.EntityCategory;
import de.kirtilot.mobhealthdisplay.client.filter.EntityFilter;
import de.kirtilot.mobhealthdisplay.client.input.MhdKeybinds;
import de.kirtilot.mobhealthdisplay.client.texture.DesignOverrides;
import de.kirtilot.mobhealthdisplay.client.texture.HeartDesignManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import de.kirtilot.mobhealthdisplay.client.filter.EffectDetector;
import de.kirtilot.mobhealthdisplay.mixin.client.LivingEntityAccessor;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client-side /mhd command for changing settings in game. */
public final class MhdCommands {
    private MhdCommands() {}

    private static final List<BoolSetting> BOOL_SETTINGS = MhdSettings.BOOLEANS;
    private static final List<FloatSetting> FLOAT_SETTINGS = MhdSettings.FLOATS;

    private static final List<String> CATEGORY_NAMES = Arrays.stream(EntityCategory.values())
            .map(c -> c.name().toLowerCase(Locale.ROOT))
            .toList();

    /** Only entity types that have health (no boats, arrows, items, ...). Full ids like "minecraft:zombie". */
    private static final SuggestionProvider<FabricClientCommandSource> ENTITY_SUGGESTIONS = (ctx, builder) ->
            suggest(livingEntityIds(), builder);

    /** Same, but in the short form used by /mhd design: "zombie", or "\"othermod:mob\"" in quotes. */
    private static final SuggestionProvider<FabricClientCommandSource> ENTITY_WORD_SUGGESTIONS = (ctx, builder) ->
            suggest(livingEntityIds().stream().map(MhdCommands::toEntityWord).toList(), builder);

    private static final SuggestionProvider<FabricClientCommandSource> DESIGN_SUGGESTIONS = (ctx, builder) ->
            suggest(HeartDesignManager.availableDesigns(), builder);

    private static final SuggestionProvider<FabricClientCommandSource> CATEGORY_SUGGESTIONS = (ctx, builder) ->
            suggest(CATEGORY_NAMES, builder);

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> root = literal("mhd").executes(MhdCommands::listAll);

            for (BoolSetting s : BOOL_SETTINGS) {
                LiteralArgumentBuilder<FabricClientCommandSource> node = literal(s.name())
                        .executes(ctx -> showValue(ctx, s.name(), s.get().get()))
                        .then(argument("value", BoolArgumentType.bool()).executes(ctx -> {
                            boolean value = BoolArgumentType.getBool(ctx, "value");
                            s.set().accept(value);
                            MhdConfig.save();
                            return showValue(ctx, s.name(), value);
                        }));
                if (s.name().equals("blackList")) addListCommands(node, c -> c.blackListEntries);
                if (s.name().equals("whiteList")) addListCommands(node, c -> c.whiteListEntries);
                root.then(node);
            }

            for (FloatSetting s : FLOAT_SETTINGS) {
                root.then(literal(s.name())
                        .executes(ctx -> showValue(ctx, s.name(), s.get().get()))
                        .then(argument("value", FloatArgumentType.floatArg(s.min(), s.max())).executes(ctx -> {
                            float value = FloatArgumentType.getFloat(ctx, "value");
                            s.set().accept(value);
                            MhdConfig.save();
                            return showValue(ctx, s.name(), value);
                        })));
            }

            root.then(literal("config").executes(ctx -> {
                MhdKeybinds.requestConfigScreen();
                return 1;
            }));

            // TEMP: Daten-Test für 0.4 – später wieder entfernen
            root.then(literal("debug").executes(ctx -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return 0;
                ctx.getSource().sendFeedback(Component.literal(
                        "poison=" + Integer.toHexString(MobEffects.POISON.value().getColor())
                                + " wither=" + Integer.toHexString(MobEffects.WITHER.value().getColor())));
                for (LivingEntity e : mc.player.level().getEntitiesOfClass(LivingEntity.class,
                        mc.player.getBoundingBox().inflate(10), e -> true)) {
                    StringBuilder particles = new StringBuilder();
                    for (ParticleOptions p : e.getEntityData().get(LivingEntityAccessor.mhd$effectParticles())) {
                        particles.append(BuiltInRegistries.PARTICLE_TYPE.getKey(p.getType()));
                        if (p instanceof ColorParticleOption c) {
                            particles.append(String.format("(%02x%02x%02x)",
                                    Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255), Math.round(c.getBlue() * 255)));
                        }
                        particles.append(" ");
                    }
                    ctx.getSource().sendFeedback(Component.literal(
                            e.getType().toShortString()
                                    + " | hp=" + e.getHealth()
                                    + " | type=" + EffectDetector.of(e)
                                    + " | particles=" + (particles.isEmpty() ? "-" : particles.toString().trim())));
                }
                return 1;
            }));

            root.then(designCommands());

            dispatcher.register(root);
        });
    }

    // ---------- /mhd design ----------

    private static LiteralArgumentBuilder<FabricClientCommandSource> designCommands() {
        return literal("design")
                .executes(MhdCommands::listDesigns)
                .then(literal("category")
                        .then(argument("category", StringArgumentType.word()).suggests(CATEGORY_SUGGESTIONS)
                                .then(argument("design", StringArgumentType.word()).suggests(DESIGN_SUGGESTIONS)
                                        .executes(MhdCommands::setCategoryDesign))))
                .then(literal("entity")
                        .then(argument("entity", StringArgumentType.string()).suggests(ENTITY_WORD_SUGGESTIONS)
                                .then(argument("design", StringArgumentType.word()).suggests(DESIGN_SUGGESTIONS)
                                        .executes(MhdCommands::setEntityDesign))))
                .then(literal("reset")
                        .then(literal("all").executes(ctx -> {
                            DesignOverrides.categories().clear();
                            DesignOverrides.entities().clear();
                            DesignOverrides.save();
                            ctx.getSource().sendFeedback(Component.literal("All design overrides removed"));
                            return 1;
                        }))
                        .then(literal("category")
                                .then(argument("category", StringArgumentType.word())
                                        .suggests((ctx, b) -> suggest(DesignOverrides.categories().keySet(), b))
                                        .executes(ctx -> {
                                            String category = StringArgumentType.getString(ctx, "category").toLowerCase(Locale.ROOT);
                                            return resetOverride(ctx, DesignOverrides.categories(), category);
                                        })))
                        .then(literal("entity")
                                .then(argument("entity", StringArgumentType.string())
                                        .suggests((ctx, b) -> suggest(DesignOverrides.entities().keySet().stream()
                                                .map(MhdCommands::toEntityWord).toList(), b))
                                        .executes(ctx -> {
                                            String id = normalize(StringArgumentType.getString(ctx, "entity"));
                                            return resetOverride(ctx, DesignOverrides.entities(), id);
                                        }))));
    }

    private static int setCategoryDesign(CommandContext<FabricClientCommandSource> ctx) {
        String category = StringArgumentType.getString(ctx, "category").toLowerCase(Locale.ROOT);
        String design = StringArgumentType.getString(ctx, "design").toLowerCase(Locale.ROOT);
        if (!CATEGORY_NAMES.contains(category)) {
            ctx.getSource().sendError(Component.literal("Unknown category: " + category + " (" + String.join(", ", CATEGORY_NAMES) + ")"));
            return 0;
        }
        if (!checkDesign(ctx, design)) return 0;

        DesignOverrides.categories().put(category, design);
        DesignOverrides.save();
        ctx.getSource().sendFeedback(Component.literal("Category " + category + " now uses design " + design));
        return 1;
    }

    private static int setEntityDesign(CommandContext<FabricClientCommandSource> ctx) {
        String id = normalize(StringArgumentType.getString(ctx, "entity"));
        String design = StringArgumentType.getString(ctx, "design").toLowerCase(Locale.ROOT);
        if (!isKnownEntity(id)) {
            ctx.getSource().sendError(Component.literal("Unknown entity: " + id));
            return 0;
        }
        if (!checkDesign(ctx, design)) return 0;

        DesignOverrides.entities().put(id, design);
        DesignOverrides.save();
        ctx.getSource().sendFeedback(Component.literal(id + " now uses design " + design));
        return 1;
    }

    private static boolean checkDesign(CommandContext<FabricClientCommandSource> ctx, String design) {
        if (HeartDesignManager.isDesign(design)) return true;
        ctx.getSource().sendError(Component.literal("Unknown design: " + design
                + " (available: " + String.join(", ", HeartDesignManager.availableDesigns()) + ")"));
        return false;
    }

    private static int resetOverride(CommandContext<FabricClientCommandSource> ctx, Map<String, String> map, String key) {
        if (map.remove(key) == null) {
            ctx.getSource().sendError(Component.literal(key + " has no design override"));
            return 0;
        }
        DesignOverrides.save();
        ctx.getSource().sendFeedback(Component.literal("Design override for " + key + " removed"));
        return 1;
    }

    private static int listDesigns(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        source.sendFeedback(Component.literal("Available designs: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.join(", ", HeartDesignManager.availableDesigns())).withStyle(ChatFormatting.GOLD)));
        source.sendFeedback(Component.literal("Your category overrides: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.valueOf(DesignOverrides.categories())).withStyle(ChatFormatting.GOLD)));
        source.sendFeedback(Component.literal("Your entity overrides: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(String.valueOf(DesignOverrides.entities())).withStyle(ChatFormatting.GOLD)));
        return 1;
    }

    // ---------- /mhd blackList | whiteList ----------

    /** Adds "add", "remove" and "list" below /mhd blackList or /mhd whiteList. */
    private static void addListCommands(LiteralArgumentBuilder<FabricClientCommandSource> node,
                                        Function<MhdConfig, List<String>> list) {
        node.then(literal("add").then(argument("entity", StringArgumentType.greedyString())
                .suggests(ENTITY_SUGGESTIONS)
                .executes(ctx -> {
                    String id = normalize(StringArgumentType.getString(ctx, "entity"));
                    if (!isKnownEntity(id)) {
                        ctx.getSource().sendError(Component.literal("Unknown entity: " + id));
                        return 0;
                    }
                    List<String> entries = list.apply(MhdConfig.get());
                    if (!entries.contains(id)) entries.add(id);
                    MhdConfig.save();
                    ctx.getSource().sendFeedback(Component.literal("Added " + id));
                    return 1;
                })));

        node.then(literal("remove").then(argument("entity", StringArgumentType.greedyString())
                .suggests((ctx, builder) -> suggest(list.apply(MhdConfig.get()), builder))
                .executes(ctx -> {
                    String id = normalize(StringArgumentType.getString(ctx, "entity"));
                    if (!list.apply(MhdConfig.get()).remove(id)) {
                        ctx.getSource().sendError(Component.literal(id + " is not in the list"));
                        return 0;
                    }
                    MhdConfig.save();
                    ctx.getSource().sendFeedback(Component.literal("Removed " + id));
                    return 1;
                })));

        node.then(literal("list").executes(ctx -> showValue(ctx, "entries", list.apply(MhdConfig.get()))));
    }

    // ---------- helpers ----------

    private static int listAll(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal("Mob Health Display settings:").withStyle(ChatFormatting.YELLOW));
        for (BoolSetting s : BOOL_SETTINGS) showValue(ctx, s.name(), s.get().get());
        for (FloatSetting s : FLOAT_SETTINGS) showValue(ctx, s.name(), s.get().get());
        showValue(ctx, "blackList entries", MhdConfig.get().blackListEntries);
        showValue(ctx, "whiteList entries", MhdConfig.get().whiteListEntries);
        return 1;
    }

    private static int showValue(CommandContext<FabricClientCommandSource> ctx, String name, Object value) {
        ctx.getSource().sendFeedback(Component.literal(name + " = ")
                .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.GOLD)));
        return 1;
    }

    private static List<String> livingEntityIds() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(EntityFilter::hasHealth)
                .map(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString())
                .sorted()
                .toList();
    }

    /** "minecraft:zombie" -> "zombie"; ids of other mods need quotes because of the colon. */
    private static String toEntityWord(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : "\"" + id + "\"";
    }

    private static CompletableFuture<Suggestions> suggest(Iterable<String> values, SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String value : values) {
            String path = value.substring(value.indexOf(':') + 1);
            if (value.startsWith(input) || path.startsWith(input)) builder.suggest(value);
        }
        return builder.buildFuture();
    }

    /** "zombie" becomes "minecraft:zombie". */
    private static String normalize(String input) {
        String id = input.trim().toLowerCase(Locale.ROOT);
        return id.contains(":") ? id : "minecraft:" + id;
    }

    private static boolean isKnownEntity(String id) {
        return BuiltInRegistries.ENTITY_TYPE.keySet().stream().anyMatch(key -> key.toString().equals(id));
    }
}