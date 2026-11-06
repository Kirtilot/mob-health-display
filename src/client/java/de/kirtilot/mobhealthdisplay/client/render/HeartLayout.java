package de.kirtilot.mobhealthdisplay.client.render;

/**
 * Result of the bar calculation. One heart is always 2 HP, a row holds 10 hearts.
 * A "bar" is as many rows as configured; only the top (partially filled) bar is shown.
 */
public record HeartLayout(int barCount, int totalSlots, int fullHearts, boolean halfHeart) {

    public static final int HEARTS_PER_ROW = 10;
    private static final float EPS = 1.0E-4f;

    public static HeartLayout of(float health, float maxHealth, int rows) {
        int slotsPerBar = Math.max(1, rows) * HEARTS_PER_ROW;
        float hpPerBar = slotsPerBar * 2f;
        int slots = maxHealth <= hpPerBar
                ? Math.max(1, (int) Math.ceil(maxHealth / 2f - EPS))
                : slotsPerBar;

        if (health <= 0f) {
            return new HeartLayout(1, slots, 0, false);
        }

        int barCount = Math.max(1, (int) Math.ceil(health / hpPerBar - EPS));
        float currentHp = health - (barCount - 1) * hpPerBar;
        int halfSteps = Math.min((int) Math.ceil(currentHp - EPS), slots * 2);

        return new HeartLayout(barCount, slots, halfSteps / 2, halfSteps % 2 == 1);
    }

    /** Number of heart rows needed for this layout. */
    public int rowCount() {
        return (totalSlots + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;
    }
}