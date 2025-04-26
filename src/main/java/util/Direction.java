package util;

/**
 * Represents the direction of a word placement on the Scrabble board.
 */
public enum Direction {
    HORIZONTAL(true),      // Left to right
    VERTICAL(false);       // Top to bottom

    private final boolean horizontal;

    Direction(boolean horizontal) {
        this.horizontal = horizontal;
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
}