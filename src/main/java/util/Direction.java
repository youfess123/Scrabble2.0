package util;

/**
 * Represents the direction of a word placement on the Scrabble board.
 * Supports standard and reverse directions for both horizontal and vertical placements.
 */
public enum Direction {
    HORIZONTAL(true, false),      // Left to right
    VERTICAL(false, false),       // Top to bottom
    HORIZONTAL_REVERSE(true, true),  // Right to left
    VERTICAL_REVERSE(false, true);   // Bottom to top

    private final boolean horizontal;
    private final boolean reverse;

    Direction(boolean horizontal, boolean reverse) {
        this.horizontal = horizontal;
        this.reverse = reverse;
    }

    /**
     * Checks if this direction is horizontal.
     * @return true if horizontal, false if vertical
     */
    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * Checks if this direction is vertical.
     * @return true if vertical, false if horizontal
     */
    public boolean isVertical() {
        return !horizontal;
    }

    /**
     * Checks if this direction is reversed.
     * @return true if reversed (right-to-left or bottom-to-top)
     */
    public boolean isReverse() {
        return reverse;
    }

    /**
     * Gets the opposite direction.
     * @return the direction in the opposite orientation
     */
    public Direction getOpposite() {
        if (this == HORIZONTAL) return HORIZONTAL_REVERSE;
        if (this == HORIZONTAL_REVERSE) return HORIZONTAL;
        if (this == VERTICAL) return VERTICAL_REVERSE;
        return VERTICAL;
    }
}