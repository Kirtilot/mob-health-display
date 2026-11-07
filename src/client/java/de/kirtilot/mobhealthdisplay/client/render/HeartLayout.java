package de.kirtilot.mobhealthdisplay.client.render;

/**
 * Result of the bar calculation. One heart is always 2 HP, a row holds 10 hearts.
 * A "bar" is as many rows as configured; only the top (partially filled) bar is shown.
 *
 * Absorption hearts come after the health slots, like in the vanilla HUD:
 * a player with 20 HP and 4 absorption looks like a player with 24 HP, the last 2 hearts golden.
 * Absorption gets as many rows as it needs (like the vanilla HUD), up to MAX_ABSORPTION_ROWS.
 */
public record HeartLayout(int barCount, int healthSlots, int fullHearts, boolean halfHeart,
                          int absorptionHearts, boolean absorptionHalf) {

    public static final int HEARTS_PER_ROW = 10;
    private static final float EPS = 1.0E-4f;
    private static final int MAX_ABSORPTION_ROWS = 10; // safety limit for huge values from commands

    public static HeartLayout of(float health, float maxHealth, float absorption, int rows) {
        int slotsPerBar = Math.max(1, rows) * HEARTS_PER_ROW;
        float hpPerBar = slotsPerBar * 2f;
        int slots = maxHealth <= hpPerBar
                ? Math.max(1, (int) Math.ceil(maxHealth / 2f - EPS))
                : slotsPerBar;

        int absorptionSteps = absorption > 0f
                ? Math.min((int) Math.ceil(absorption - EPS), MAX_ABSORPTION_ROWS * HEARTS_PER_ROW * 2)
                : 0;
        int absorptionHearts = absorptionSteps / 2;
        boolean absorptionHalf = absorptionSteps % 2 == 1;

        if (health <= 0f) {
            return new HeartLayout(1, slots, 0, false, absorptionHearts, absorptionHalf);
        }

        int barCount = Math.max(1, (int) Math.ceil(health / hpPerBar - EPS));
        float currentHp = health - (barCount - 1) * hpPerBar;
        int halfSteps = Math.min((int) Math.ceil(currentHp - EPS), slots * 2);

        return new HeartLayout(barCount, slots, halfSteps / 2, halfSteps % 2 == 1, absorptionHearts, absorptionHalf);
    }

    /** Slots taken by absorption hearts (a half heart takes a whole slot). */
    public int absorptionSlots() {
        return absorptionHearts + (absorptionHalf ? 1 : 0);
    }

    /** Health slots plus absorption slots. */
    public int totalSlots() {
        return healthSlots + absorptionSlots();
    }

    /** Number of heart rows needed for this layout. */
    public int rowCount() {
        return (totalSlots() + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;
    }
}