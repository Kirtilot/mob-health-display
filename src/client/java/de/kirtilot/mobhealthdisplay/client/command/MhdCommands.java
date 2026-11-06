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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client-side /mhd command for changing settings in game. */
public final class MhdCommands {
    private MhdCommands() {}

    private record BoolSetting(String name, Supplier<Boolean> get, Consumer<Boolean> set) {}
    private record FloatSetting(String name, Supplier<Float> get, Consumer<Float> set, float min, float max) {}

    private static final List<BoolSetting> BOOL_SETTINGS = List.of(
            new BoolSetting("showHearts", () -> MhdConfig.get().showHearts, v -> MhdConfig.get().showHearts = v),
            new BoolSetting("showHostile", () -> MhdConfig.get().showHostile, v -> MhdConfig.get().showHostile = v),
            new BoolSetting("showPassive", () -> MhdConfig.get().showPassive, v -> MhdConfig.get().showPassive = v),
            new BoolSetting("showSelf", () -> MhdConfig.get().showSelf, v -> MhdConfig.get().showSelf = v),
            new BoolSetting("onlyShowDamaged", () -> MhdConfig.get().onlyShowDamaged, v -> MhdConfig.get().onlyShowDamaged = v),
            new BoolSetting("showBarCount", () -> MhdConfig.get().showBarCount, v -> MhdConfig.get().showBarCount = v),
            new BoolSetting("blackList", () -> MhdConfig.get().blackList, v -> MhdConfig.get().blackList = v),
            new BoolSetting("whiteList", () -> MhdConfig.get().whiteList, v -> MhdConfig.get().whiteList = v)
    );

    private static final List<FloatSetting> FLOAT_SETTINGS = List.of(
            new FloatSetting("height", () -> MhdConfig.get().height, v -> MhdConfig.get().height = v, -5f, 5f),
            new FloatSetting("hpPerBar", () -> MhdConfig.get().hpPerBar, v -> MhdConfig.get().hpPerBar = v, 1f, 1000f)
    );

    private static final SuggestionProvider<FabricClientCommandSource> ENTITY_SUGGESTIONS = (ctx, builder) ->
            suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(Object::toString).sorted().toList(), builder);

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

            dispatcher.register(root);
        });
    }

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