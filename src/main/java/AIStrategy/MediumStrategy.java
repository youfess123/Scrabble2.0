package AIStrategy;

import model.Move;
import model.Square;
import model.Board;
import model.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Medium difficulty level AI strategy.
 * Characteristics:
 * - Picks from the top 50% of moves by score
 * - Has basic awareness of premium squares
 * - Balances between immediate points and rack management
 * - Exchanges up to 3 tiles when stuck
 */
public class MediumStrategy extends BaseStrategy {
    @Override
    public String getName() {
        return "Medium AI";
    }

    @Override
    public int getDifficultyLevel() {
        return 2;
    }

    /**
     * Selects a move from the top half of possible moves by score.
     * Adds some randomness but favors higher scoring moves.
     */
    @Override
    protected Move selectMove(List<Move> possibleMoves) {
        if (possibleMoves.isEmpty()) {
            throw new IllegalArgumentException("No possible moves to select from");
        }

        // Choose from the top half of moves by score
        int mediumCutoff = Math.max(1, possibleMoves.size() / 2);
        return possibleMoves.get(random.nextInt(mediumCutoff));
    }

    /**
     * Determines how many tiles to exchange for Medium level.
     * Medium level exchanges up to 3 tiles.
     */
    @Override
    protected int determineExchangeCount(int availableTileCount) {
        return Math.min(3, availableTileCount);
    }

    /**
     * Overridden to apply medium-level strategic considerations.
     * Reorders moves to prioritize those using premium squares.
     */
    @Override
    protected List<Move> findPossibleMoves(model.Game game, model.Player player) {
        List<Move> allMoves = super.findPossibleMoves(game, player);

        // Apply medium-level strategic considerations
        return prioritizeMovesByStrategy(allMoves, game.getBoard());
    }

    /**
     * Prioritizes moves based on medium-level strategic considerations:
     * - Using premium squares
     * - Rack balance after move
     */
    private List<Move> prioritizeMovesByStrategy(List<Move> moves, Board board) {
        // Medium level prioritizes a balance of scoring and rack management
        List<Move> prioritizedMoves = new ArrayList<>(moves);

        // Adjust scores based on strategic factors
        for (Move move : prioritizedMoves) {
            int baseScore = move.getScore();
            int adjustedScore = baseScore;

            // Bonus for moves that use premium squares
            if (usesPremiumSquares(move, board)) {
                adjustedScore += 5; // Small bonus to influence decisions
            }

            // Bonus for maintaining a good vowel/consonant balance in rack
            List<Tile> remainingTiles = new ArrayList<>(move.getPlayer().getRack().getTiles());
            remainingTiles.removeAll(move.getTiles());
            if (hasGoodLetterBalance(remainingTiles)) {
                adjustedScore += 3;
            }

            // Apply the adjusted score (just for sorting purposes)
            move.setMetadata("adjustedScore", adjustedScore);
        }

        // Sort by adjusted score
        prioritizedMoves.sort((m1, m2) -> {
            Integer score1 = (Integer) m1.getMetadata("adjustedScore");
            Integer score2 = (Integer) m2.getMetadata("adjustedScore");
            return score2.compareTo(score1); // Descending order
        });

        // Medium AI only considers a maximum of 30 moves
        if (prioritizedMoves.size() > 30) {
            return prioritizedMoves.subList(0, 30);
        }

        return prioritizedMoves;
    }

    /**
     * Checks if a move uses premium squares on the board.
     */
    private boolean usesPremiumSquares(Move move, Board board) {
        // Check if the move uses premium squares that haven't been used yet
        int row = move.getStartRow();
        int col = move.getStartCol();

        for (int i = 0; i < move.getTiles().size(); i++) {
            if (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) {
                Square square = board.getSquare(row, col);
                if (!square.isSquareTypeUsed()) {
                    Square.SquareType type = square.getSquareType();
                    if (type == Square.SquareType.DOUBLE_WORD ||
                            type == Square.SquareType.TRIPLE_WORD ||
                            type == Square.SquareType.DOUBLE_LETTER ||
                            type == Square.SquareType.TRIPLE_LETTER) {
                        return true;
                    }
                }
            }

            // Move to next position based on direction
            if (move.getDirection().isHorizontal()) {
                col += move.getDirection().isReverse() ? -1 : 1;
            } else {
                row += move.getDirection().isReverse() ? -1 : 1;
            }
        }

        return false;
    }

    /**
     * Checks if a set of tiles has a good balance of vowels and consonants.
     */
    private boolean hasGoodLetterBalance(List<Tile> tiles) {
        int vowels = 0;
        int consonants = 0;

        for (Tile tile : tiles) {
            if (isVowel(tile.getLetter())) {
                vowels++;
            } else if (tile.getLetter() != '*') { // Skip blank tiles
                consonants++;
            }
        }

        // Good balance is roughly 40-60% vowels
        double vowelRatio = (double) vowels / Math.max(1, tiles.size());
        return vowelRatio >= 0.3 && vowelRatio <= 0.6;
    }