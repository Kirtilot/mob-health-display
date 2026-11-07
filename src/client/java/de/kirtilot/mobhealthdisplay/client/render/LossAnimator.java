package de.kirtilot.mobhealthdisplay.client.render;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import de.kirtilot.mobhealthdisplay.client.config.MhdConfig;

/**
 * Remembers which half hearts an entity just lost, so they can shrink away instead of vanishing.
 * Works in "half steps": 1 half step = 1 HP = half a heart, counted from the bottom (step 0 = left half of heart 1).
 * Weak keys: entries disappear automatically once the entity is gone.
 */
public final class LossAnimator {
    public static final long DURATION_MILLIS = 350; // how long a lost heart needs to disappear
    private static final float EPS = 1.0E-4f;

    /** One lost half heart in the visible bar. progress: 0 = just lost, 1 = gone */
    public record LostHeart(int slot, boolean rightHalf, float progress) {}

    private static final class State {
        int steps;                                        // half steps the entity had last frame
        final Map<Integer, Long> lost = new HashMap<>();  // lost half step -> time it was lost

        State(int steps) {
            this.steps = steps;
        }
    }

    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    private LossAnimator() {}

    /** Called every frame for every visible entity; returns the lost hearts to draw in the current bar. */
    public static List<LostHeart> update(LivingEntity entity, float health, int rows) {
        long now = System.currentTimeMillis();
        long duration = (long) (DURATION_MILLIS / Math.max(0.1f, MhdConfig.get().damageAnimationSpeed));
        int steps = Math.max(0, (int) Math.ceil(health - EPS));

        State state = STATES.get(entity);
        if (state == null) { // first time we see this entity: nothing to animate
            STATES.put(entity, new State(steps));
            return List.of();
        }

        if (steps < state.steps) {
            for (int k = steps; k < state.steps; k++) state.lost.put(k, now); // newly lost
        } else if (steps > state.steps) {
            state.lost.keySet().removeIf(k -> k < steps);                     // healed again
        }
        state.steps = steps;
        state.lost.values().removeIf(time -> now - time >= duration);  // finishedished
        if (state.lost.isEmpty()) return List.of();

        // only the bar that is shown right now (same calculation as HeartLayout)
        int stepsPerBar = Math.max(1, rows) * HeartLayout.HEARTS_PER_ROW * 2;
        int barStart = (Math.max(1, (steps + stepsPerBar - 1) / stepsPerBar) - 1) * stepsPerBar;

        List<LostHeart> result = new ArrayList<>();
        for (Map.Entry<Integer, Long> e : state.lost.entrySet()) {
            int local = e.getKey() - barStart;
            if (local < 0 || local >= stepsPerBar) continue;
            float progress = (now - e.getValue()) / (float) duration;
            result.add(new LostHeart(local / 2, local % 2 == 1, progress));
        }
        return result;
    }
}