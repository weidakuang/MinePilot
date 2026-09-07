package dev.mcai.companion.agent.model;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/** Runtime-only model settings. Secrets are never persisted by this record. */
public record ModelConfig(
        URI baseUri,
        String apiKey,
        String model,
        double temperature
) {
    public ModelConfig {
        Objects.requireNonNull(baseUri, "baseUri");
        Objects.requireNonNull(apiKey, "apiKey");
        Objects.requireNonNull(model, "model");
        String scheme = baseUri.getScheme() == null
                ? ""
                : baseUri.getScheme().toLowerCase(Locale.ROOT);
        boolean loopback = "localhost".equalsIgnoreCase(baseUri.getHost())
                || "127.0.0.1".equals(baseUri.getHost())
                || "::1".equals(baseUri.getHost());
        if (!("https".equals(scheme) || "http".equals(scheme) && loopback)) {
            throw new IllegalArgumentException(
                    "Model Base URL must use HTTPS, except for loopback development endpoints");
        }
        if (apiKey.isBlank() || model.isBlank()) {
            throw new IllegalArgumentException("API key and model are required");
        }
        if (!Double.isFinite(temperature) || temperature < 0.0 || temperature > 1.0) {
            throw new IllegalArgumentException("temperature must be in [0.0, 1.0]");
        }
    }

    public static ModelConfig fromEnvironment() {
        String base = requiredEnvironment("MINEPILOT_BASE_URL");
        String key = requiredEnvironment("MINEPILOT_API_KEY");
        String model = requiredEnvironment("MINEPILOT_MODEL");
        String rawTemperature = System.getenv().getOrDefault(
                "MINEPILOT_TEMPERATURE", "0.2");
        return new ModelConfig(
                URI.create(base.trim()),
                key.trim(),
                model.trim(),
                Double.parseDouble(rawTemperature)
        );
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new MissingModelConfigurationException(
                    "Missing runtime environment variable " + name);
        }
        return value;
    }

    public static final class MissingModelConfigurationException extends RuntimeException {
        public MissingModelConfigurationException(String message) {
            super(message);
        }
    }
}
