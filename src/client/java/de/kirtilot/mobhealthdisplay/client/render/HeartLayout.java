package de.kirtilot.mobhealthdisplay.client.render;

/** Result of the bar calculation: only the top (partially filled) bar is shown. */
public record HeartLayout(int barCount, int totalSlots, int fullHearts, boolean halfHeart) {

    private static final float EPS = 1.0E-4f;

    public static HeartLayout of(float health, float maxHealth, float hpPerBar) {
        float hpPerHalf = hpPerBar / 20f; // with the default 20 HP per bar, 1 HP = half a heart
        int slots = maxHealth <= hpPerBar
                ? Math.max(1, (int) Math.ceil(maxHealth / (2f * hpPerHalf) - EPS))
                : 10;

        if (health <= 0f) {
            return new HeartLayout(1, slots, 0, false);
        }

        int barCount = Math.max(1, (int) Math.ceil(health / hpPerBar - EPS));
        float currentHp = health - (barCount - 1) * hpPerBar;
        int halfSteps = (int) Math.ceil(currentHp / hpPerHalf - EPS);
        halfSteps = Math.min(halfSteps, slots * 2);

        return new HeartLayout(barCount, slots, halfSteps / 2, halfSteps % 2 == 1);
    }
}