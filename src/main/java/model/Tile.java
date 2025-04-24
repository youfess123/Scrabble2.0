package model;


public class Tile {
    private char letter;
    private final int value;
    private final boolean isBlank;


    public Tile(char letter, int value) {
        this.letter = Character.toUpperCase(letter);
        this.value = value;
        this.isBlank = (letter == '*');
    }

    public static Tile createBlankTile(char letter) {
        Tile tile = new Tile('*', 0);
        tile.setLetter(Character.toUpperCase(letter));
        return tile;
    }

    public int getValue() {
        return value;
    }

    public char getLetter() {
        return letter;
    }

    public void setLetter(char letter) {
        this.letter = Character.toUpperCase(letter);
    }

    public boolean isBlank() {
        return isBlank;
    }

    @Override
    public String toString() {
        return isBlank ? (letter + "(Blank)") : String.valueOf(letter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tile other = (Tile) obj;
        return letter == other.letter && value == other.value && isBlank == other.isBlank;
    }

    @Override
    public int hashCode() {
        int result = letter;
        result = 31 * result + value;
        result = 31 * result + (isBlank ? 1 : 0);
        return result;
    }
}