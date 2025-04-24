package AIStrategy;

import AIStrategy.BaseStrategy;
import model.*;
import util.Direction;
import util.ScrabbleConstants;

import java.awt.Point;
import java.util.*;
import java.util.List;

/**
 * Hard difficulty level AI strategy.
 * Characteristics:
 * - Uses advanced heuristics to evaluate moves
 * - Considers board control and defensive play
 * - Maintains excellent rack balance and planning
 * - Uses GADDAG for efficient word finding
 * - Considers both immediate and future scoring potential
 * - Exchanges only 1-2 tiles when necessary
 */
public class HardStrategy extends BaseStrategy {
    // Constants for strategic weight factors
    private static final double IMMEDIATE_POINTS_WEIGHT = 0.5;
    private static final double PREMIUM_SQUARE_WEIGHT = 0.2;
    private static final double DEFENSIVE_PLAY_WEIGHT = 0.15;
    private static final double RACK_LEAVE_WEIGHT = 0.15;

    // Bonus points for specific strategic situations
    private static final int BONUS_USING_ALL_TILES = 10;
    private static final int BONUS_FORMING_TWO_WORDS = 5;
    private static final int BONUS_DEFENSIVE_PLAY = 8;

    @Override
    public String getName() {
        return "Hard AI";
    }

    @Override
    public int getDifficultyLevel() {
        return 3;
    }

    /**
     * Selects a move from the top 3 possible moves by score.
     * Adds limited randomness but strongly favors highest scoring moves.
     */
    @Override
    protected Move selectMove(List<Move> possibleMoves) {
        if (possibleMoves.isEmpty()) {
            throw new IllegalArgumentException("No possible moves to select from");
        }

        // Choose from the top 3 moves
        int hardCutoff = Math.min(3, possibleMoves.size());
        return possibleMoves.get(random.nextInt(hardCutoff));
    }

    /**
     * Determines how many tiles to exchange for Hard level.
     * Hard level is very conservative with exchanges, 1-2 tiles.
     */
    @Override
    protected int determineExchangeCount(int availableTileCount) {
        return Math.min(2, availableTileCount);
    }

    /**
     * Overridden to apply advanced strategic considerations.
     * Implements a sophisticated strategy considering multiple factors.
     */
    @Override
    protected List<Move> findPossibleMoves(Game game, Player player) {
        List<Move> allMoves = super.findPossibleMoves(game, player);

        // For hard difficulty, no move count limitations
        return evaluateMovesWithAdvancedHeuristics(allMoves, game, player);
    }

    /**
     * Evaluates moves using advanced heuristics that consider:
     * - Immediate points
     * - Board control (blocking opponent's high-value opportunities)
     * - Rack management (leaving a good set of tiles)
     * - Using premium squares
     */
    private List<Move> evaluateMovesWithAdvancedHeuristics(List<Move> moves, Game game, Player player) {
        Board board = game.getBoard();
        List<Move> evaluatedMoves = new ArrayList<>(moves);

        // Identify high-value positions for defensive play
        Set<Point> highValuePositions = identifyHighValuePositions(board);

        // Evaluate each move with multiple factors
        for (Move move : evaluatedMoves) {
            int baseScore = move.getScore();
            double strategicValue = calculateStrategicValue(move, game, highValuePositions);

            // Calculate combined score (base + strategic)
            double combinedScore = baseScore + strategicValue;

            // Store for sorting
            move.setMetadata("combinedScore", combinedScore);
        }

        // Sort by combined strategic score
        evaluatedMoves.sort((m1, m2) -> {
            Double score1 = (Double) m1.getMetadata("combinedScore");
            Double score2 = (Double) m2.getMetadata("combinedScore");
            return score2.compareTo(score1); // Descending order
        });

        return evaluatedMoves;
    }

    /**
     * Calculates the strategic value of a move beyond just immediate points.
     */
    private double calculateStrategicValue(Move move, Game game, Set<Point> highValuePositions) {
        double strategicValue = 0;

        // 1. Value for using all tiles (bingo potential)
        if (move.getTiles().size() == 7) {
            strategicValue += BONUS_USING_ALL_TILES;
        }

        // 2. Value for forming multiple words
        if (move.getFormedWords().size() > 1) {
            strategicValue += BONUS_FORMING_TWO_WORDS;
        }

        // 3. Value for defensive play (blocking high-value positions)
        if (playsDefensively(move, highValuePositions)) {
            strategicValue += BONUS_DEFENSIVE_PLAY;
        }

        // 4. Value for rack leave quality
        double rackLeaveValue = evaluateRackLeave(move);
        strategicValue += rackLeaveValue * RACK_LEAVE_WEIGHT;

        // 5. Value for using premium squares
        double premiumSquareValue = evaluatePremiumSquareUsage(move, game.getBoard());
        strategicValue += premiumSquareValue * PREMIUM_SQUARE_WEIGHT;

        return strategicValue;
    }

    /**
     * Identifies high-value positions on the board (premium squares that could
     * be used by opponents for high scores).
     */
    private Set<Point> identifyHighValuePositions(Board board) {
        Set<Point> highValuePositions = new HashSet<>();

        // Look for premium squares adjacent to existing tiles
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Square square = board.getSquare(row, col);

                // Skip used squares
                if (square.hasTile() || square.isSquareTypeUsed()) {
                    continue;
                }

                // Check if this is a premium square
                Square.SquareType type = square.getSquareType();
                boolean isPremium = type == Square.SquareType.DOUBLE_WORD ||
                        type == Square.SquareType.TRIPLE_WORD;

                // If it's a premium square adjacent to an existing tile, it's high value
                if (isPremium && hasAdjacentTile(board, row, col)) {
                    highValuePositions.add(new Point(row, col));
                }
            }
        }

        return highValuePositions;
    }

    /**
     * Checks if a move plays defensively by blocking high-value positions.
     */
    private boolean playsDefensively(Move move, Set<Point> highValuePositions) {
        int row = move.getStartRow();
        int col = move.getStartCol();

        for (int i = 0; i < move.getTiles().size(); i++) {
            if (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) {
                Point position = new Point(row, col);
                if (highValuePositions.contains(position)) {
                    return true;
                }
            }

            // Move to next position
            if (move.getDirection().isHorizontal()) {
                col += move.getDirection().isReverse() ? -1 : 1;
            } else {
                row += move.getDirection().isReverse() ? -1 : 1;
            }
        }

        return false;
    }

    /**
     * Evaluates the quality of tiles left in the rack after a move.
     */
    private double evaluateRackLeave(Move move) {
        List<Tile> rack = move.getPlayer().getRack().getTiles();
        List<Tile> remainingTiles = new ArrayList<>(rack);
        remainingTiles.removeAll(move.getTiles());

        double value = 0;

        // Count vowels and consonants
        int vowels = 0;
        int consonants = 0;
        double totalValue = 0;

        for (Tile tile : remainingTiles) {
            if (isVowel(tile.getLetter())) {
                vowels++;
            } else if (tile.getLetter() != '*') { // Skip blank tiles
                consonants++;
            }
            totalValue += tile.getValue();
        }

        // Vowel-consonant balance (optimal is about 40-60% vowels)
        double vowelRatio = (double) vowels / Math.max(1, remainingTiles.size());
        if (vowelRatio >= 0.3 && vowelRatio <= 0.6) {
            value += 5;
        } else {
            // Penalize extreme imbalances
            value -= Math.abs(vowelRatio - 0.4) * 10;
        }

        // Prefer having some blank tiles
        for (Tile tile : remainingTiles) {
            if (tile.isBlank()) {
                value += 8;
            }
        }

        // Avoid high-value tiles that are hard to play
        Set<Character> difficultLetters = Set.of('Q', 'Z', 'X', 'J');
        int difficultCount = 0;

        for (Tile tile : remainingTiles) {
            if (difficultLetters.contains(tile.getLetter())) {
                difficultCount++;
            }
        }

        if (difficultCount > 1) {
            value -= 5 * difficultCount;
        }

        // Prefer S tiles (versatile for plurals)
        for (Tile tile : remainingTiles) {
            if (tile.getLetter() == 'S') {
                value += 3;
            }
        }

        return value;
    }

    /**
     * Evaluates how well a move uses premium squares.
     */
    private double evaluatePremiumSquareUsage(Move move, Board board) {
        int row = move.getStartRow();
        int col = move.getStartCol();
        double value = 0;

        for (int i = 0; i < move.getTiles().size(); i++) {
            if (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) {
                Square square = board.getSquare(row, col);
                Tile tile = move.getTiles().get(i);

                if (!square.hasTile() && !square.isSquareTypeUsed()) {
                    switch (square.getSquareType()) {
                        case TRIPLE_WORD:
                            value += 15;
                            break;
                        case DOUBLE_WORD:
                        case CENTER:
                            value += 8;
                            break;
                        case TRIPLE_LETTER:
                            // Higher value for high-point letters on letter premium
                            value += 3 * Math.min(8, tile.getValue());
                            break;
                        case DOUBLE_LETTER:
                            value += 1.5 * Math.min(8, tile.getValue());
                            break;
                    }
                }
            }

            // Move to next position
            if (move.getDirection().isHorizontal()) {
                col += move.getDirection().isReverse() ? -1 : 1;
            } else {
                row += move.getDirection().isReverse() ? -1 : 1;
            }
        }

        return value;
    }

    /**
     * Enhanced version that uses GADDAG for more efficient word finding.
     * For Hard AI, we implement a more sophisticated approach.
     */
    @Override
    protected Set<String> generateWordsFromRack(String rackLetters, Gaddag dictionary) {
        // For hard AI, use the full dictionary and all possible words
        Set<String> validWords = new HashSet<>();

        // For each letter as a potential anchor
        for (char letter : rackLetters.toCharArray()) {
            // Try to find all words that can contain this letter
            Set<String> wordsWithLetter = dictionary.getWordsFrom(
                    rackLetters, letter, true, true);
            validWords.addAll(wordsWithLetter);
        }

        // Also still use recursive generation for completeness
        super.generateWords("", rackLetters, dictionary, validWords);

        return validWords;
    }

    /**
     * Hard AI is smarter about selecting tiles to exchange.
     */
    @Override
    protected List<Tile> selectTilesToExchange(Player player) {
        Rack rack = player.getRack();
        List<Tile> availableTiles = new ArrayList<>(rack.getTiles());
        List<Tile> tilesToExchange = new ArrayList<>();
        Map<Tile, Double> tileValues = evaluateTileValues(availableTiles);

        // Sort tiles by their strategic value (lowest first)
        availableTiles.sort(Comparator.comparing(tileValues::get));

        // Exchange 1-2 of the worst tiles
        int numToExchange = determineExchangeCount(availableTiles.size());
        for (int i = 0; i < numToExchange && i < availableTiles.size(); i++) {
            tilesToExchange.add(availableTiles.get(i));
        }

        return tilesToExchange;
    }

    /**
     * Evaluates strategic value of each tile for rack management.
     */
    private Map<Tile, Double> evaluateTileValues(List<Tile> tiles) {
        Map<Tile, Double> values = new HashMap<>();
        Map<Character, Integer> letterCounts = new HashMap<>();

        // Count occurrences of each letter
        for (Tile tile : tiles) {
            char letter = tile.getLetter();
            letterCounts.put(letter, letterCounts.getOrDefault(letter, 0) + 1);
        }

        // Evaluate each tile
        for (Tile tile : tiles) {
            double value = 0;
            char letter = tile.getLetter();

            // Base value from tile point value (high value tiles are less flexible)
            value += 10 - Math.min(8, tile.getValue());

            // Blank tiles are extremely valuable
            if (tile.isBlank()) {
                value += 20;
                values.put(tile, value);
                continue;
            }

            // Vowels are valuable but not in excess
            if (isVowel(letter)) {
                int vowelCount = countVowels(tiles);
                if (vowelCount <= 2) {
                    value += 5;
                } else if (vowelCount > 4) {
                    value -= 3;
                }
            }

            // S is valuable for plurals
            if (letter == 'S') {
                value += 8;
            }

            // Common letters are valuable
            if ("ETAOINSHRD".indexOf(letter) >= 0) {
                value += 3;
            }

            // Penalize duplicate letters beyond 2
            if (letterCounts.getOrDefault(letter, 0) > 2) {
                value -= 5;
            }

            // Q without U is problematic
            if (letter == 'Q' && !letterCounts.containsKey('U')) {
                value -= 10;
            }

            // Hard-to-use letters
            if ("JQXZ".indexOf(letter) >= 0) {
                value -= 3;
            }

            values.put(tile, value);
        }

        return values;
    }

    /**
     * Counts the number of vowels in a list of tiles.
     */
    private int countVowels(List<Tile> tiles) {
        int count = 0;
        for (Tile tile : tiles) {
            if (isVowel(tile.getLetter())) {
                count++;
            }
        }
        return count;
    }
}