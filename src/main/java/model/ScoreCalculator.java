package model;

import util.Direction;
import util.ScrabbleConstants;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Calculates scores for moves in a Scrabble game.
 * This class is responsible for computing the score of a move
 * based on the board state, premium squares, and game rules.
 */
public class ScoreCalculator {
    private final Board board;
    private final MoveValidator moveValidator;

    /**
     * Creates a new ScoreCalculator.
     *
     * @param board the game board
     * @param moveValidator validator for finding word positions
     */
    public ScoreCalculator(Board board, MoveValidator moveValidator) {
        this.board = board;
        this.moveValidator = moveValidator;
    }

    /**
     * Calculates the score for a move.
     *
     * @param tempBoard the board with the move applied
     * @param move the move
     * @return the score for the move
     */
    public int calculateMoveScore(Board tempBoard, Move move) {
        int totalScore = 0;
        List<String> formedWords = move.getFormedWords();
        List<Point> newTilePositions = moveValidator.findNewTilePositions(tempBoard, move);

        // Track which premium squares have been used
        Set<Point> usedPremiumSquares = new HashSet<>();

        for (String word : formedWords) {
            int wordScore = calculateWordScore(tempBoard, word, move.getDirection(), newTilePositions, usedPremiumSquares);
            totalScore += wordScore;
            System.out.println("Word: " + word + ", Score: " + wordScore);
        }

        // Add bonus points for using all 7 tiles (bingo)
        if (move.getTiles().size() == 7) {
            totalScore += ScrabbleConstants.BINGO_BONUS;
            System.out.println("Bingo bonus: " + ScrabbleConstants.BINGO_BONUS);
        }

        System.out.println("Total move score: " + totalScore);
        return totalScore;
    }

    /**
     * Calculates the score for a single word.
     *
     * @param tempBoard the board with the move applied
     * @param word the word
     * @param direction the direction the word is read in
     * @param newTilePositions positions of newly placed tiles
     * @param usedPremiumSquares set of premium squares already used in this move
     * @return the score for the word
     */
    private int calculateWordScore(Board tempBoard, String word, Direction direction,
                                   List<Point> newTilePositions, Set<Point> usedPremiumSquares) {
        int wordScore = 0;
        int wordMultiplier = 1;

        // Find all instances of this word on the board
        List<Point> wordPositions = findAllPositionsOfWord(tempBoard, word, direction);
        if (wordPositions.isEmpty()) {
            System.out.println("Warning: Could not find word '" + word + "' on the board");
            return 0;
        }

        // Use the first position found (should be the one we just placed)
        Point wordPos = wordPositions.get(0);
        int startRow = wordPos.x;
        int startCol = wordPos.y;

        int currentRow = startRow;
        int currentCol = startCol;

        boolean isHorizontal = direction.isHorizontal();

        // Process each letter in the word
        for (int i = 0; i < word.length(); i++) {
            if (currentRow < 0 || currentRow >= Board.SIZE ||
                    currentCol < 0 || currentCol >= Board.SIZE) {
                break;
            }

            Square square = tempBoard.getSquare(currentRow, currentCol);
            Point currentPoint = new Point(currentRow, currentCol);
            boolean isNewTile = newTilePositions.contains(currentPoint);

            Tile tile = square.getTile();
            int letterValue = tile.getValue();

            // Apply letter and word multipliers for new tiles on premium squares
            if (isNewTile && !board.getSquare(currentRow, currentCol).isSquareTypeUsed()) {
                // Apply letter multipliers
                if (square.getSquareType() == Square.SquareType.DOUBLE_LETTER) {
                    letterValue *= 2;
                } else if (square.getSquareType() == Square.SquareType.TRIPLE_LETTER) {
                    letterValue *= 3;
                }

                // Track word multipliers (to be applied later)
                if (!usedPremiumSquares.contains(currentPoint)) {
                    if (square.getSquareType() == Square.SquareType.DOUBLE_WORD ||
                            square.getSquareType() == Square.SquareType.CENTER) {
                        wordMultiplier *= 2;
                        usedPremiumSquares.add(currentPoint);
                    } else if (square.getSquareType() == Square.SquareType.TRIPLE_WORD) {
                        wordMultiplier *= 3;
                        usedPremiumSquares.add(currentPoint);
                    }
                }
            }

            wordScore += letterValue;

            // Move to the next position based on the direction
            if (isHorizontal) {
                currentCol++;
            } else {
                currentRow++;
            }
        }

        // Apply word multiplier
        return wordScore * wordMultiplier;
    }

    /**
     * Finds all positions of a word on the board, considering direction.
     *
     * @param board the board
     * @param word the word to find
     * @param direction the direction to search in
     * @return list of starting positions for the word
     */
    private List<Point> findAllPositionsOfWord(Board board, String word, Direction direction) {
        // Find positions of the word on the board in the specified direction
        return findWordPosition(board, word, direction);
    }

    /**
     * Finds positions of a word on the board in the specified direction.
     *
     * @param board the board
     * @param word the word to find
     * @param direction the direction to search in
     * @return list of starting positions for the word
     */
    private List<Point> findWordPosition(Board board, String word, Direction direction) {
        List<Point> positions = new ArrayList<>();
        boolean isHorizontal = direction.isHorizontal();

        // Search the entire board
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                // Check if this position could be the start of our word
                if (!board.getSquare(row, col).hasTile()) {
                    continue;
                }

                boolean found = true;
                int r = row;
                int c = col;

                // Try to match each letter of the word
                for (int i = 0; i < word.length(); i++) {
                    if (r < 0 || r >= Board.SIZE || c < 0 || c >= Board.SIZE ||
                            !board.getSquare(r, c).hasTile() ||
                            board.getSquare(r, c).getTile().getLetter() != word.charAt(i)) {
                        found = false;
                        break;
                    }

                    // Move in the appropriate direction for the next letter
                    if (isHorizontal) {
                        c++; // Move right
                    } else {
                        r++; // Move down
                    }
                }

                if (found) {
                    positions.add(new Point(row, col));
                }
            }
        }

        return positions;
    }
}