// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.task;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;


public final class RecoveryLadder<T,F> {

    
    public record Rung<T,F>(Supplier<T> strategy, Set<F> handles, int maxAttempts) {}

    private final List<Rung<T,F>> rungs;

    
    private int index;
    
    private int attempts = 1;
    
    private T cached;

    public RecoveryLadder(List<Rung<T,F>> rungs) {
        this.rungs = List.copyOf(rungs);
    }

    
    public T current() {
        if (index >= rungs.size()) return null;
        if (cached == null) cached = rungs.get(index).strategy().get();
        return cached;
    }

    
    public boolean advance(F lastFail) {
        if (index < rungs.size()) {
            Rung<T,F> r = rungs.get(index);
            if (r.handles().contains(lastFail) && attempts < r.maxAttempts()) {
                attempts++;
                cached = null;         // rebuild the strategy for the retry
                return true;
            }
        }
        for (int i = index + 1; i < rungs.size(); i++) {
            if (rungs.get(i).handles().contains(lastFail)) {
                index = i;
                attempts = 1;
                cached = null;
                return true;
            }
        }
        index = rungs.size();          // exhausted
        cached = null;
        return false;
    }

    
    public int currentRung() {
        return index;
    }

    
    public int currentAttempt() {
        return attempts;
    }

    
    public boolean exhausted() {
        return index >= rungs.size();
    }
}
