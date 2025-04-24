package model;

import util.Direction;
import util.ScrabbleConstants;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates moves in a Scrabble game according to the rules.
 * Enhanced with support for bidirectional word reading.
 */
public class MoveValidator {
    private final Board board;
    private final Gaddag dictionary;
    private final boolean allowBidirectionalWords;

    /**
     * Creates a new MoveValidator.
     *
     * @param board the game board
     * @param dictionary the dictionary for word validation
     * @param allowBidirectionalWords whether to allow words to be valid in either direction
     */
    public MoveValidator(Board board, Gaddag dictionary, boolean allowBidirectionalWords) {
        this.board = board;
        this.dictionary = dictionary;
        this.allowBidirectionalWords = allowBidirectionalWords;
    }

    /**
     * Creates a new MoveValidator with default settings (standard Scrabble rules).
     *
     * @param board the game board
     * @param dictionary the dictionary for word validation
     */
    public MoveValidator(Board board, Gaddag dictionary) {
        this(board, dictionary, false);
    }

    /**
     * Checks if a placement move is valid.
     * This validates the move when the player finalizes it,
     * allowing placement of tiles anywhere during setup.
     *
     * @param move the move to check
     * @return true if the move is valid
     */
    public boolean isValidPlaceMove(Move move) {
        int startRow = move.getStartRow();
        int startCol = move.getStartCol();
        Direction direction = move.getDirection();
        List<Tile> tiles = move.getTiles();

        if (tiles.isEmpty()) {
            System.out.println("Invalid move: No tiles to place");
            return false;
        }

        if (startRow < 0 || startRow >= Board.SIZE ||
                startCol < 0 || startCol >= Board.SIZE) {
            System.out.println("Invalid move: Starting position out of bounds");
            return false;
        }

        // First move must cover the center square
        if (board.isEmpty()) {
            return isValidFirstMove(move, startRow, startCol, direction, tiles);
        }

        // Create a temporary board to test the move
        Board tempBoard = createTempBoardWithMove(move);
        if (tempBoard == null) {
            return false;
        }

        // Validate word formation
        WordValidationResult validationResult = validateWords(tempBoard, move);
        if (!validationResult.isValid() || validationResult.getFormedWords().isEmpty()) {
            System.out.println("Invalid move: No valid words formed");
            return false;
        }

        // Update the move with the formed words
        move.setFormedWords(validationResult.getFormedWords());

        // Update the direction if it was reversed
        if (validationResult.isDirectionReversed()) {
            // Reverse the move direction if needed - this will affect scoring
            move.setDirection(move.getDirection().getOpposite());
        }

        // Validate connectivity only after words are formed
        if (!connectsToExistingWords(tempBoard, move, validationResult.getFormedWords())) {
            System.out.println("Invalid move: Does not connect to existing tiles");
            return false;
        }

        return true;
    }

    /**
     * Result class for word validation that includes whether direction was reversed.
     */
    public static class WordValidationResult {
        private final List<String> formedWords;
        private final boolean directionReversed;

        public WordValidationResult(List<String> formedWords, boolean directionReversed) {
            this.formedWords = formedWords;
            this.directionReversed = directionReversed;
        }

        public boolean isValid() {
            return !formedWords.isEmpty();
        }

        public List<String> getFormedWords() {
            return formedWords;
        }

        public boolean isDirectionReversed() {
            return directionReversed;
        }
    }

    /**
     * Validates words formed by a move, considering both directions if enabled.
     * This checks that all words formed by the move are valid dictionary words.
     *
     * @param tempBoard the temporary board with the move applied
     * @param move the move to validate
     * @return the validation result with formed words and direction information
     */
    public WordValidationResult validateWords(Board tempBoard, Move move) {
        List<String> formedWords = new ArrayList<>();
        List<Point> newTilePositions = findNewTilePositions(tempBoard, move);
        boolean directionReversed = false;

        // If no new tiles were placed, this isn't a valid move
        if (newTilePositions.isEmpty()) {
            System.out.println("Invalid move: No tiles placed");
            return new WordValidationResult(formedWords, directionReversed);
        }

        int row = move.getStartRow();
        int col = move.getStartCol();
        Direction direction = move.getDirection();

        // Find the main word in forward direction
        String mainWord = getWordInDirection(tempBoard, row, col, direction);

        // Check if the main word is valid in the forward direction
        boolean mainWordValid = mainWord.length() >= 2 && dictionary.isValidWord(mainWord);

        // If bidirectional words are allowed and the word isn't valid forward, try backward
        String reversedMainWord = null;
        boolean reversedWordValid = false;

        if (allowBidirectionalWords && !mainWordValid && mainWord.length() >= 2) {
            reversedMainWord = new StringBuilder(mainWord).reverse().toString();
            reversedWordValid = dictionary.isValidWord(reversedMainWord);
        }

        if (!mainWordValid && !reversedWordValid) {
            if (allowBidirectionalWords) {
                System.out.println("Invalid move: Main word '" + mainWord +
                        "' is not valid in either direction");
            } else {
                System.out.println("Invalid move: Main word '" + mainWord +
                        "' is not in dictionary");
            }
            return new WordValidationResult(formedWords, directionReversed);
        }

        // If the reversed word is valid and the forward word isn't, we'll use the reversed word
        if (!mainWordValid && reversedWordValid) {
            directionReversed = true;
            mainWord = reversedMainWord;
            System.out.println("Word validated in reverse direction: " + mainWord);
        }

        // Add the valid main word to formed words
        formedWords.add(mainWord);

        // Check for any crossing words
        for (Point p : newTilePositions) {
            // Skip checking crossing words in the same direction as the main word
            boolean isPositionInMainWordLine = (direction.isHorizontal() && p.x == row) ||
                    (direction.isVertical() && p.y == col);

            if (isPositionInMainWordLine) {
                continue;
            }

            // Check for crossing words - these are always read in standard directions
            // because they're perpendicular to the main word
            Direction crossDirection = direction.isHorizontal() ? Direction.VERTICAL : Direction.HORIZONTAL;
            String crossWord = getWordInDirection(tempBoard, p.x, p.y, crossDirection);

            if (crossWord.length() >= 2) {
                // For crossing words, we apply the same bidirectional logic if enabled
                boolean crossWordValid = dictionary.isValidWord(crossWord);

                if (!crossWordValid && allowBidirectionalWords) {
                    String reversedCrossWord = new StringBuilder(crossWord).reverse().toString();
                    boolean reversedCrossValid = dictionary.isValidWord(reversedCrossWord);

                    if (reversedCrossValid) {
                        crossWord = reversedCrossWord;
                        crossWordValid = true;
                        System.out.println("Crossing word validated in reverse: " + crossWord);
                    }
                }

                if (!crossWordValid) {
                    System.out.println("Invalid move: Crossing word '" + crossWord +
                            "' is not valid");
                    return new WordValidationResult(new ArrayList<>(), false);
                }

                formedWords.add(crossWord);
            }
        }

        return new WordValidationResult(formedWords, directionReversed);
    }

    /**
     * Gets a word starting from the given position in the specified direction.
     * This method handles all four possible directions.
     */
    private String getWordInDirection(Board board, int row, int col, Direction direction) {
        boolean isHorizontal = direction.isHorizontal();
        boolean isReverse = direction.isReverse();

        // Find the start of the word first
        int startRow = row;
        int startCol = col;

        // If reading in standard direction, find the true start
        if (!isReverse) {
            if (isHorizontal) {
                startCol = findWordStart(board, row, col, true);
            } else {
                startRow = findWordStart(board, row, col, false);
            }
        } else {
            // If reading in reverse, find the end (which is the start for reading backward)
            if (isHorizontal) {
                while (startCol < Board.SIZE - 1 && board.getSquare(row, startCol + 1).hasTile()) {
                    startCol++;
                }
            } else {
                while (startRow < Board.SIZE - 1 && board.getSquare(startRow + 1, col).hasTile()) {
                    startRow++;
                }
            }
        }

        // Read the word in the appropriate direction
        StringBuilder word = new StringBuilder();
        int currentRow = startRow;
        int currentCol = startCol;

        // Collect letters until we reach an empty square or the board edge
        while (currentRow >= 0 && currentRow < Board.SIZE &&
                currentCol >= 0 && currentCol < Board.SIZE) {

            Square square = board.getSquare(currentRow, currentCol);
            if (!square.hasTile()) {
                break;
            }

            word.append(square.getTile().getLetter());

            // Move in the appropriate direction
            if (isHorizontal) {
                if (isReverse) {
                    currentCol--; // Move left
                } else {
                    currentCol++; // Move right
                }
            } else {
                if (isReverse) {
                    currentRow--; // Move up
                } else {
                    currentRow++; // Move down
                }
            }
        }

        return word.toString();
    }

    /**
     * Checks if a move connects to existing words on the board.
     * This is only called after words have been validated.
     */
    private boolean connectsToExistingWords(Board tempBoard, Move move, List<String> formedWords) {
        // First move doesn't need to connect to anything
        if (board.isEmpty()) {
            return true;
        }

        List<Point> newTilePositions = findNewTilePositions(tempBoard, move);

        // Check if any words formed include existing tiles
        for (String word : formedWords) {
            // Find all instances of this word on the board
            List<Point> wordPositions = findAllWordPositions(tempBoard, word);

            for (Point wordPos : wordPositions) {
                boolean isHorizontal = isWordHorizontal(tempBoard, wordPos.x, wordPos.y, word);
                int row = wordPos.x;
                int col = wordPos.y;

                for (int i = 0; i < word.length(); i++) {
                    Point p = new Point(row, col);

                    // If this position has an existing tile (not one of our new tiles)
                    // then we're connecting to an existing word
                    if (board.getSquare(row, col).hasTile() && !newTilePositions.contains(p)) {
                        return true;
                    }

                    if (isHorizontal) {
                        col++;
                    } else {
                        row++;
                    }
                }
            }
        }

        // Also check if any new tiles are adjacent to existing tiles
        // This handles the case of parallel plays
        for (Point p : newTilePositions) {
            if (hasAdjacentTile(p.x, p.y)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a first move is valid (must cover center square).
     */
    private boolean isValidFirstMove(Move move, int startRow, int startCol, Direction direction, List<Tile> tiles) {
        boolean touchesCenter = false;

        if (direction.isHorizontal()) {
            if (startRow == ScrabbleConstants.CENTER_SQUARE &&
                    startCol <= ScrabbleConstants.CENTER_SQUARE &&
                    startCol + tiles.size() > ScrabbleConstants.CENTER_SQUARE) {
                touchesCenter = true;
            }
        } else {
            if (startCol == ScrabbleConstants.CENTER_SQUARE &&
                    startRow <= ScrabbleConstants.CENTER_SQUARE &&
                    startRow + tiles.size() > ScrabbleConstants.CENTER_SQUARE) {
                touchesCenter = true;
            }
        }

        if (!touchesCenter) {
            System.out.println("Invalid first move: Must cover center square");
            return false;
        }

        return true;
    }

    /**
     * Creates a temporary board with the move applied.
     */
    private Board createTempBoardWithMove(Move move) {
        Board tempBoard = new Board();

        // Copy the current board state
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (board.getSquare(r, c).hasTile()) {
                    tempBoard.placeTile(r, c, board.getSquare(r, c).getTile());
                }
            }
        }

        // Apply the move to the temporary board
        int startRow = move.getStartRow();
        int startCol = move.getStartCol();
        Direction direction = move.getDirection();
        List<Tile> tiles = move.getTiles();

        int currentRow = startRow;
        int currentCol = startCol;

        for (Tile tile : tiles) {
            // Skip over existing tiles
            while (currentRow < Board.SIZE && currentCol < Board.SIZE &&
                    tempBoard.getSquare(currentRow, currentCol).hasTile()) {
                if (direction.isHorizontal()) {
                    currentCol++;
                } else {
                    currentRow++;
                }
            }

            if (currentRow >= Board.SIZE || currentCol >= Board.SIZE) {
                System.out.println("Invalid move: Placement extends beyond board");
                return null;
            }

            tempBoard.placeTile(currentRow, currentCol, tile);

            if (direction.isHorizontal()) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        return tempBoard;
    }

    /**
     * Finds the positions of new tiles placed by a move.
     */
    public List<Point> findNewTilePositions(Board tempBoard, Move move) {
        List<Point> newTilePositions = new ArrayList<>();

        // Find tiles that exist on the temp board but not on the real board
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (tempBoard.getSquare(row, col).hasTile() &&
                        !board.getSquare(row, col).hasTile()) {
                    newTilePositions.add(new Point(row, col));
                }
            }
        }

        return newTilePositions;
    }

    /**
     * Finds the starting position of a word.
     */
    private int findWordStart(Board board, int row, int col, boolean isHorizontal) {
        int position = isHorizontal ? col : row;

        while (position > 0) {
            int prevPos = position - 1;
            Square square = isHorizontal ? board.getSquare(row, prevPos) : board.getSquare(prevPos, col);

            if (!square.hasTile()) {
                break;
            }
            position = prevPos;
        }

        return position;
    }

    /**
     * Finds all positions of a word on the board.
     */
    private List<Point> findAllWordPositions(Board board, String word) {
        List<Point> positions = new ArrayList<>();

        // Check horizontal words
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                String horizontalWord = getWordInDirection(board, row, col, Direction.HORIZONTAL);
                if (horizontalWord.equals(word)) {
                    positions.add(new Point(row, col));
                }

                // Also check reversed horizontal
                String reversedHorizontal = getWordInDirection(board, row, col, Direction.HORIZONTAL_REVERSE);
                if (reversedHorizontal.equals(word)) {
                    // Find the position that would be the start when read forward
                    int endCol = col;
                    while (endCol > 0 && board.getSquare(row, endCol-1).hasTile()) {
                        endCol--;
                    }
                    positions.add(new Point(row, endCol));
                }
            }
        }

        // Check vertical words
        for (int col = 0; col < Board.SIZE; col++) {
            for (int row = 0; row < Board.SIZE; row++) {
                String verticalWord = getWordInDirection(board, row, col, Direction.VERTICAL);
                if (verticalWord.equals(word)) {
                    positions.add(new Point(row, col));
                }

                // Also check reversed vertical
                String reversedVertical = getWordInDirection(board, row, col, Direction.VERTICAL_REVERSE);
                if (reversedVertical.equals(word)) {
                    // Find the position that would be the start when read forward
                    int endRow = row;
                    while (endRow > 0 && board.getSquare(endRow-1, col).hasTile()) {
                        endRow--;
                    }
                    positions.add(new Point(endRow, col));
                }
            }
        }

        return positions;
    }

    /**
     * Determines if a word at a given position is horizontal.
     */
    private boolean isWordHorizontal(Board board, int row, int col, String word) {
        String horizontalWord = getWordInDirection(board, row, col, Direction.HORIZONTAL);
        return horizontalWord.equals(word);
    }

    /**
     * Checks if a position has an adjacent tile.
     */
    private boolean hasAdjacentTile(int row, int col) {
        if (row > 0 && board.getSquare(row - 1, col).hasTile()) return true;
        if (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile()) return true;
        if (col > 0 && board.getSquare(row, col - 1).hasTile()) return true;
        if (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile()) return true;

        return false;
    }
}