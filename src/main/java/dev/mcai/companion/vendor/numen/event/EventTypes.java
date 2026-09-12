// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.event;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Bounded persistent event queue; adapted from the pinned Numen source. */
public final class EventTypes {

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static final String QUERY = "query";
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static final String EVENT = "event";
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static final String COMPACT = "compact";
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static final String CLEAR = "clear";
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static final String GOAL = "goal";

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public record Type(String id,
                       Function<String, String> toModel,
                       Function<String, String> chatPreview,
                       boolean clearedByInterrupt,
                       boolean fromOwner) {}

    private static final Map<String, Type> TYPES = new HashMap<>();

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    static final Type UNKNOWN = new Type("?", s -> s, s -> null, false, false);

    static {

        register(new Type(QUERY, s -> s, s -> s, true, true));
        register(new Type(EVENT, s -> s, s -> null, false, false));

        register(new Type(COMPACT, s -> null, s -> s, true, true));
        register(new Type(CLEAR, s -> null, s -> s, true, true));
        register(new Type(GOAL, s -> s, s -> null, true, true));
    }

    private EventTypes() {}

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static synchronized void register(Type type) {
        if (type != null && type.id() != null && !type.id().isBlank()) {
            TYPES.put(type.id(), type);
        }
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static synchronized Type get(String id) {
        Type t = TYPES.get(id);
        return t == null ? UNKNOWN : t;
    }
}
