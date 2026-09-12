// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from Dwinovo/minecraft-numen 34ef004dac3095fbbd928a897927e277c69d02fa.
package dev.mcai.companion.vendor.numen.event;

import java.util.ArrayList;
import java.util.List;

/** Bounded persistent event queue; adapted from the pinned Numen source. */
public final class EventQueue {

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static final int DEFAULT_CAP = 200;

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public record Entry(String type, String text, long ts, boolean urgent) {}

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public interface Journal {
        List<Entry> load();

        void save(List<Entry> entries);

        /** Bounded persistent event queue; adapted from the pinned Numen source. */
        Journal NONE = new Journal() {
            @Override public List<Entry> load() {
                return List.of();
            }

            @Override public void save(List<Entry> entries) {
            }
        };
    }

    private final List<Entry> entries = new ArrayList<>();
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    private final List<Runnable> urgentListeners = new ArrayList<>();
    private final Journal journal;
    private final int cap;
    private int dropped;

    public EventQueue(Journal journal) {
        this(journal, DEFAULT_CAP);
    }

    public EventQueue(Journal journal, int cap) {
        this.journal = journal == null ? Journal.NONE : journal;
        this.cap = Math.max(1, cap);
        entries.addAll(this.journal.load());
        while(entries.size()>this.cap){entries.remove(0);dropped++;}
    }



    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public void push(String type, String text, long now, boolean urgent) {
        if (text == null || text.isBlank()) {
            return;
        }
        entries.add(new Entry(type, text, now, urgent));
        while (entries.size() > cap) {
            entries.remove(0);
            dropped++;
        }
        journal.save(entries);
        if (urgent) {


            for (Runnable listener : List.copyOf(urgentListeners)) {
                listener.run();
            }
        }
    }



    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public void addUrgentListener(Runnable listener) {
        if (listener != null) {
            urgentListeners.add(listener);
        }
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public void removeUrgentListener(Runnable listener) {
        urgentListeners.remove(listener);
    }



    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public boolean shouldDrain(long now, int level) {
        if (entries.isEmpty()) {
            return false;
        }
        for (Entry e : entries) {
            if (e.urgent()) {
                return true;
            }
        }
        return entries.size() >= thresholdOf(level) || oldestAgeMs(now) >= maxWaitMsOf(level);
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public List<Entry> takeEntries(long now) {
        List<Entry> out = new ArrayList<>(entries);


        out.sort(java.util.Comparator.comparingLong(Entry::ts));
        flushDropped(out, now);
        entries.clear();
        journal.save(entries);
        return out;
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static List<String> render(List<Entry> taken, long now) {
        List<Entry> ordered = new ArrayList<>(taken);
        ordered.sort(java.util.Comparator.comparingLong(Entry::ts));
        List<String> events = new ArrayList<>();
        List<String> owner = new ArrayList<>();
        for (Entry e : ordered) {
            EventTypes.Type t = EventTypes.get(e.type());
            String rendered = t.toModel().apply(e.text());
            if (rendered == null || rendered.isBlank()) {
                continue;
            }
            (t.fromOwner() ? owner : events).add(annotateAge(rendered, e.ts(), now));
        }
        List<String> out = new ArrayList<>();
        if (!events.isEmpty()) {
            out.add("<events>\n" + String.join("\n", events) + "\n</events>");
        }
        out.addAll(owner);
        return out;
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static String droppedNote(int n) {
        return "<event kind=\"body_log\">About " + n + " events could not be retained</event>";
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public List<Entry> takeWhile(java.util.function.Predicate<Entry> keep, long now) {
        if (keep == null) {
            return List.of();
        }
        int n = 0;
        while (n < entries.size() && keep.test(entries.get(n))) {
            n++;
        }
        if (n == 0) {
            return List.of();
        }
        List<Entry> taken = new ArrayList<>(entries.subList(0, n));
        entries.subList(0, n).clear();
        flushDropped(taken, now);
        journal.save(entries);
        return taken;
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    private void flushDropped(List<Entry> out, long now) {
        if (dropped > 0) {
            out.add(new Entry(EventTypes.EVENT, droppedNote(dropped), now, false));
            dropped = 0;
        }
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public int clearInterrupted() {
        int before = entries.size();
        entries.removeIf(e -> EventTypes.get(e.type()).clearedByInterrupt());
        int n = before - entries.size();
        if (n > 0) {
            journal.save(entries);
        }
        return n;
    }



    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public int count(String type) {
        int n = 0;
        for (Entry e : entries) {
            if (e.type().equals(type)) n++;
        }
        return n;
    }

    public boolean hasUrgent() {
        for (Entry e : entries) {
            if (e.urgent()) return true;
        }
        return false;
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public long oldestAgeMs(long now) {
        long oldest = Long.MAX_VALUE;
        for (Entry e : entries) {
            if (e.ts() > 0 && e.ts() < oldest) oldest = e.ts();
        }
        return oldest == Long.MAX_VALUE ? 0L : Math.max(0L, now - oldest);
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public int droppedCount() {
        return dropped;
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public List<String> chatPreview() {
        List<String> out = new ArrayList<>();
        for (Entry e : entries) {
            String s = EventTypes.get(e.type()).chatPreview().apply(e.text());
            if (s != null) out.add(s);
        }
        return List.copyOf(out);
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }



    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 10;
    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static final int DEFAULT_LEVEL = 3;

    public static int clampLevel(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static int thresholdOf(int level) {
        return clampLevel(level);
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    public static long maxWaitMsOf(int level) {
        int lv = clampLevel(level);
        return (60_000L * lv * lv) / 3L;
    }

    /** Bounded persistent event queue; adapted from the pinned Numen source. */
    private static String annotateAge(String text, long ts, long now) {
        long ageMs = ts > 0 ? Math.max(0L, now - ts) : 0L;
        if (ageMs < 10 * 60_000L) {
            return text;
        }
        String age = ageMs < 3_600_000L ? (ageMs / 60_000L) + " minutes"
                : ageMs < 86_400_000L ? (ageMs / 3_600_000L) + " hours"
                : (ageMs / 86_400_000L) + " days";
        return "[Occurred about " + age + " ago] " + text;
    }
}
