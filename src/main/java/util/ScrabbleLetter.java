package util;

public enum ScrabbleLetter {
    A(9, 1), B(2, 3), C(2, 3), D(4, 2), E(12, 1),
    F(2, 4), G(3, 2), H(2, 4), I(9, 1), J(1, 8),
    K(1, 5), L(4, 1), M(2, 3), N(6, 1), O(8, 1),
    P(2, 3), Q(1, 10), R(6, 1), S(4, 1), T(6, 1),
    U(4, 1), V(2, 4), W(2, 4), X(1, 8), Y(2, 4),
    Z(1, 10), BLANK(2, 0, '*');

    private final int count;
    private final int value;
    private final char character;

    ScrabbleLetter(int count, int value) {
        this.count = count;
        this.value = value;
        this.character = name().charAt(0);
    }

    ScrabbleLetter(int count, int value, char character) {
        this.count = count;
        this.value = value;
        this.character = character;
    }

    public int getCount() { return count; }
    public int getValue() { return value; }
    public char getCharacter() { return character; }

    public static ScrabbleLetter fromChar(char c) {
        if (c == '*') return BLANK;
        try {
            return valueOf(String.valueOf(Character.toUpperCase(c)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}