package dev.mcai.companion.model;

/**
 * A capability-probed request hint for latency-sensitive game control.
 *
 * <p>{@link #DISABLED} is used only after the provider accepted the
 * protocol-specific field during capability negotiation. {@link #DEFAULT}
 * omits the field completely so providers which do not implement it retain
 * their native behavior.</p>
 */
public enum ReasoningControl {
    DISABLED,
    DEFAULT
}
