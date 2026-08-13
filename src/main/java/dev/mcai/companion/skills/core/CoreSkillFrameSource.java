package dev.mcai.companion.skills.core;

import dev.mcai.companion.perception.VisibleEntity;
import dev.mcai.companion.waypoint.DimensionRef;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface CoreSkillFrameSource {
    Optional<CoreSkillFrame> current();

    /**
     * Resolves a recently published fair observation by its public semantic
     * sequence. Server implementations retain a small bounded history so a
     * model response can still bind the exact thing it saw while the 2-5 Hz
     * perception loop continues to publish newer samples.
     */
    default Optional<CoreSkillFrame> atObservation(
            final long observationRevision
    ) {
        return current().filter(frame ->
                frame.observationRevision() == observationRevision
        );
    }

    /**
     * Resolves one public visible-entity index from the exact fair semantic
     * sample that authored a model response.
     *
     * <p>The default implementation uses the full frame history. Production
     * sources may retain a longer compact binding history without retaining
     * hundreds of complete navigation maps.</p>
     */
    default Optional<VisibleEntityBinding> visibleEntityAtObservation(
            final long observationRevision,
            final int observationIndex
    ) {
        if (observationIndex < 0) {
            return Optional.empty();
        }
        return atObservation(observationRevision).flatMap(frame -> {
            if (observationIndex >= frame.visibleEntities().size()) {
                return Optional.empty();
            }
            return Optional.of(new VisibleEntityBinding(
                    frame.dimension(),
                    frame.visibleEntities().get(observationIndex)
            ));
        });
    }

    record VisibleEntityBinding(
            DimensionRef dimension,
            VisibleEntity entity
    ) {
        public VisibleEntityBinding {
            Objects.requireNonNull(dimension, "dimension");
            Objects.requireNonNull(entity, "entity");
        }
    }
}
