package model;


public class Square {

    public enum SquareType {
        NONE(1, 1, ""),
        DOUBLE_LETTER(2, 1, "DL"),
        TRIPLE_LETTER(3, 1, "TL"),
        DOUBLE_WORD(1, 2, "DW"),
        TRIPLE_WORD(1, 3, "TW"),
        CENTER(1, 2, "★"); // Center is also a double word square

        private final int letterMultiplier;
        private final int wordMultiplier;
        private final String label;

        SquareType(int letterMultiplier, int wordMultiplier, String label) {
            this.letterMultiplier = letterMultiplier;
            this.wordMultiplier = wordMultiplier;
            this.label = label;
        }

        public int getLetterMultiplier() {
            return letterMultiplier;
        }

        public int getWordMultiplier() {
            return wordMultiplier;
        }

        public String getLabel() {
            return label;
        }
    }

    private final int row;
    private final int col;
    private final SquareType squareType;
    private Tile tile;
    private boolean squareTypeUsed;

    public Square(int row, int col, SquareType squareType) {
        this.row = row;
        this.col = col;
        this.squareType = squareType;
        this.tile = null;
        this.squareTypeUsed = false;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public SquareType getSquareType() {
        return squareType;
    }

    public boolean hasTile() {
        return tile != null;
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public void useSquareType() {
        this.squareTypeUsed = true;
    }

    public boolean isSquareTypeUsed() {
        return squareTypeUsed;
    }

    public String getPosition() {
        return "(" + row + "," + col + ")";
    }

    @Override
    public String toString() {
        if (hasTile()) {
            return tile.toString();
        } else {
            return squareType.getLabel();
        }
    }
}