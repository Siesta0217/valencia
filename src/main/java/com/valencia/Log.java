package com.valencia;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-shot error logging. Each key logs at most once per session, so a
 * failure that fires every tick (packet send, registry lookup) can't flood
 * the log — but breakage after an MC update is VISIBLE instead of silently
 * swallowed by a catch-and-ignore. Generalizes AutoFishMod's loggedError
 * flag pattern (the B4 audit item from the porting-prep roadmap).
 */
public final class Log {

    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    private Log() {}

    /** Log {@code key: throwable} once; subsequent calls with the same key are no-ops. */
    public static void once(String key, Throwable t) {
        if (SEEN.add(key))
            System.err.println("[Valencia] " + key + ": " + t + " (further occurrences suppressed)");
    }
}
