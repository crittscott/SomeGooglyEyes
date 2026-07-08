package com.github.crittscott.somegoogly.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * A Brigadier argument that parses either a number or the literal {@code ~} — the "leave this axis/angle
 * unchanged" no-op. {@code ~} yields an empty {@link Optional}; a number yields {@code Optional.of(v)}.
 * Used by {@code /sg move} and {@code /sg rot}. The picker stores placement as {@code double}, so this
 * yields doubles all the way through.
 */
public class MaybeDoubleArgumentType implements ArgumentType<Optional<Double>> {

    private static final Collection<String> EXAMPLES = Arrays.asList("0", "0.5", "~", "-1.25");

    /** Read the parsed argument; empty means {@code ~} (unchanged). */
    @SuppressWarnings("unchecked")
    public static Optional<Double> get(CommandContext<?> ctx, String name) {
        return (Optional<Double>) ctx.getArgument(name, Optional.class);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if ("~".startsWith(builder.getRemaining())) {
            builder.suggest("~");
        }
        return builder.buildFuture();
    }

    public static MaybeDoubleArgumentType maybeDouble() {
        return new MaybeDoubleArgumentType();
    }

    @Override
    public Optional<Double> parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '~') {
            reader.skip();
            return Optional.empty();
        }
        return Optional.of(reader.readDouble());
    }
}
