package AIStrategy;

import model.*;
import util.Direction;
import util.PlayerStrategy;
import util.ScrabbleConstants;

import java.awt.Point;
import java.util.*;
import java.util.List;

/**
 * Optimized AI strategy that implements GADDAG for efficient word finding.
 * This strategy focuses on maximizing score while maintaining good rack balance.
 */
public class OptimizedStrategy implements PlayerStrategy {
    private final Random random = new Random();

    // Strategic weight factors
    private static final double IMMEDIATE_POINTS_WEIGHT = 0.7;
    private static final double PREMIUM_SQUARE_WEIGHT = 0.15;
    private static final double RACK_LEAVE_WEIGHT = 0.15;

    // Bonus points for specific strategic situations
    private static final int BONUS_USING_ALL_TILES = 10;
    private static final int BONUS_FORMING_TWO_WORDS = 5;

    @Override
    public String getName() {
        return "Optimized AI";
    }

    @Override
    public int getDifficultyLevel() {
        return 3; // Single high-quality implementation
    }

    @Override
    public Move generateMove(Game game, Player player) {
        try {
            // Find all possible moves
            List<Move> possibleMoves = findPossibleMoves(game, player);
            System.out.println("Found " + possibleMoves.size() + " possible moves");

            // If moves found, select one based on strategy
            if (!possibleMoves.isEmpty()) {
                Move selectedMove = selectBestMove(possibleMoves);
                System.out.println("Selected move with score: " + selectedMove.getScore());
                if (!selectedMove.getFormedWords().isEmpty()) {
                    System.out.println("Words formed: " + String.join(", ", selectedMove.getFormedWords()));
                }
                return selectedMove;
            }

            // No moves found, generate fallback
            System.out.println("No possible word placements found, trying fallback");
            return generateFallbackMove(game, player);

        } catch (Exception e) {
            System.err.println("Error in strategy: " + e.getMessage());
            e.printStackTrace();
            return Move.createPassMove(player);
        }
    }

    /**
     * Finds all possible placement moves for the player using GADDAG.
     *
     * @param game the current game
     * @param player the player to find moves for
     * @return list of possible moves sorted by score (highest first)
     */
    protected List<Move> findPossibleMoves(Game game, Player player) {
        List<Move> possibleMoves = new ArrayList<>();
        Board board = game.getBoard();
        Gaddag dictionary = game.getDictionary();
        Rack rack = player.getRack();
        String rackLetters = getTilesAsString(rack.getTiles());

        // For empty board, place word through center
        if (board.isEmpty()) {
            findMovesForEmptyBoard(game, player, possibleMoves);
        } else {
            // Find anchor points (empty squares adjacent to existing tiles)
            List<Point> anchorPoints = findAnchorPoints(board);
            System.out.println("Found " + anchorPoints.size() + " anchor points");

            // For each anchor point, try placements
            for (Point anchor : anchorPoints) {
                int row = anchor.x;
                int col = anchor.y;

                // Try horizontal and vertical placements only
                tryPlacementsAtAnchor(game, player, row, col, Direction.HORIZONTAL, possibleMoves);
                tryPlacementsAtAnchor(game, player, row, col, Direction.VERTICAL, possibleMoves);
            }
        }

        // Apply strategic evaluation
        possibleMoves = evaluateMovesWithAdvancedHeuristics(possibleMoves, game, player);

        return possibleMoves;
    }

    /**
     * Tries placing tiles at an anchor point in a specific direction.
     */
    private void tryPlacementsAtAnchor(Game game, Player player, int row, int col,
                                       Direction direction, List<Move> possibleMoves) {
        try {
            Rack rack = player.getRack();
            Board board = game.getBoard();
            MoveValidator validator = new MoveValidator(board, game.getDictionary());
            String rackLetters = getTilesAsString(rack.getTiles());

            // Use GADDAG to find all valid words that could start/pass through this anchor point
            Set<String> possibleWords = new HashSet<>();

            // Try each letter in the rack as an anchor
            for (char letter : rackLetters.toCharArray()) {
                // Find all words that can be formed with this letter at the anchor position
                Set<String> wordsWithAnchor = game.getDictionary().getWordsFrom(
                        rackLetters, letter, true, true);
                possibleWords.addAll(wordsWithAnchor);
            }

            // For each possible word, try placing it at the anchor
            for (String word : possibleWords) {
                if (word.length() < 2) continue;

                // Try different alignments of the word with the anchor
                for (int i = 0; i < word.length(); i++) {
                    // Calculate starting position
                    int startRow = direction.isHorizontal() ? row : row - i;
                    int startCol = direction.isHorizontal() ? col - i : col;

                    // Check if this placement would fit on the board
                    if (startRow < 0 || startRow >= Board.SIZE ||
                            startCol < 0 || startCol >= Board.SIZE) {
                        continue;
                    }

                    // Check if the ending position would be off the board
                    int endRow = startRow;
                    int endCol = startCol;
                    if (direction.isHorizontal()) {
                        endCol = startCol + word.length() - 1;
                    } else {
                        endRow = startRow + word.length() - 1;
                    }

                    // Skip if end position is off the board
                    if (endRow < 0 || endRow >= Board.SIZE ||
                            endCol < 0 || endCol >= Board.SIZE) {
                        continue;
                    }

                    // Create a move for this word placement
                    Move move = Move.createPlaceMove(player, startRow, startCol, direction);

                    // Get the tiles needed for this word
                    List<Tile> tilesForWord = getTilesForWord(word, rack.getTiles());
                    if (tilesForWord == null) {
                        continue; // Player doesn't have the tiles for this word
                    }

                    move.addTiles(tilesForWord);

                    // Validate the move
                    if (validator.isValidPlaceMove(move)) {
                        // Calculate score
                        Board tempBoard = createTempBoardWithMove(board, move);
                        ScoreCalculator calculator = new ScoreCalculator(board, validator);
                        int score = calculator.calculateMoveScore(tempBoard, move);
                        move.setScore(score);

                        possibleMoves.add(move);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error evaluating anchor at " + row + "," + col + " in direction " + direction + ": " + e.getMessage());
            // Continue to other anchors rather than failing completely
        }
    }

    /**
     * Finds moves for an empty board (first move of the game).
     */
    private void findMovesForEmptyBoard(Game game, Player player, List<Move> possibleMoves) {
        Gaddag dictionary = game.getDictionary();
        Rack rack = player.getRack();
        String rackLetters = getTilesAsString(rack.getTiles());

        // Find all valid words that can be formed with rack tiles
        Set<String> validWords = generateWordsFromRack(rackLetters, dictionary);

        for (String word : validWords) {
            if (word.length() < 2) continue;

            // Try placing horizontally through center
            for (int offset = 0; offset < word.length(); offset++) {
                int startCol = ScrabbleConstants.CENTER_SQUARE - offset;
                if (startCol >= 0 && startCol + word.length() <= Board.SIZE) {
                    List<Tile> tilesForWord = getTilesForWord(word, rack.getTiles());
                    if (tilesForWord != null) {
                        Move move = Move.createPlaceMove(player,
                                ScrabbleConstants.CENTER_SQUARE,
                                startCol,
                                Direction.HORIZONTAL);
                        move.addTiles(tilesForWord);

                        // Validate move
                        MoveValidator validator = new MoveValidator(game.getBoard(), dictionary);
                        if (validator.isValidPlaceMove(move)) {
                            // Create a temporary board to score the move
                            Board tempBoard = createTempBoardWithMove(game.getBoard(), move);
                            ScoreCalculator calculator = new ScoreCalculator(game.getBoard(), validator);
                            int score = calculator.calculateMoveScore(tempBoard, move);
                            move.setScore(score);

                            // Make sure the word is added to formed words
                            List<String> formedWords = new ArrayList<>();
                            formedWords.add(word);
                            move.setFormedWords(formedWords);

                            possibleMoves.add(move);
                        }
                    }
                }
            }

            // Try placing vertically through center
            for (int offset = 0; offset < word.length(); offset++) {
                int startRow = ScrabbleConstants.CENTER_SQUARE - offset;
                if (startRow >= 0 && startRow + word.length() <= Board.SIZE) {
                    List<Tile> tilesForWord = getTilesForWord(word, rack.getTiles());
                    if (tilesForWord != null) {
                        Move move = Move.createPlaceMove(player,
                                startRow,
                                ScrabbleConstants.CENTER_SQUARE,
                                Direction.VERTICAL);
                        move.addTiles(tilesForWord);

                        // Validate move
                        MoveValidator validator = new MoveValidator(game.getBoard(), dictionary);
                        if (validator.isValidPlaceMove(move)) {
                            // Create a temporary board to score the move
                            Board tempBoard = createTempBoardWithMove(game.getBoard(), move);
                            ScoreCalculator calculator = new ScoreCalculator(game.getBoard(), validator);
                            int score = calculator.calculateMoveScore(tempBoard, move);
                            move.setScore(score);

                            // Make sure the word is added to formed words
                            List<String> formedWords = new ArrayList<>();
                            formedWords.add(word);
                            move.setFormedWords(formedWords);

                            possibleMoves.add(move);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate all valid words that can be formed with the given rack letters using GADDAG.
     */
    private Set<String> generateWordsFromRack(String rackLetters, Gaddag dictionary) {
        Set<String> validWords = new HashSet<>();

        // For each letter as a potential anchor
        for (char letter : rackLetters.toCharArray()) {
            // Try to find all words that can contain this letter
            Set<String> wordsWithLetter = dictionary.getWordsFrom(
                    rackLetters, letter, true, true);
            validWords.addAll(wordsWithLetter);
        }

        // Also use recursive generation for completeness
        generateWords("", rackLetters, dictionary, validWords);

        return validWords;
    }

    /**
     * Recursive word generation (complementary to GADDAG).
     */
    private void generateWords(String prefix, String remaining, Gaddag dictionary, Set<String> validWords) {
        if (prefix.length() >= 2 && dictionary.isValidWord(prefix)) {
            validWords.add(prefix);
        }

        if (remaining.isEmpty()) {
            return;
        }

        for (int i = 0; i < remaining.length(); i++) {
            char c = remaining.charAt(i);
            String newRemaining = remaining.substring(0, i) + remaining.substring(i + 1);
            generateWords(prefix + c, newRemaining, dictionary, validWords);
        }
    }

    /**
     * Selects the best move from the list of possible moves.
     */
    private Move selectBestMove(List<Move> possibleMoves) {
        if (possibleMoves.isEmpty()) {
            throw new IllegalArgumentException("No possible moves to select from");
        }

        // Filter out moves with zero score (likely invalid)
        List<Move> scoringMoves = new ArrayList<>();
        for (Move move : possibleMoves) {
            if (move.getScore() > 0) {
                scoringMoves.add(move);
            }
        }

        // If we have moves with actual scores, use those
        if (!scoringMoves.isEmpty()) {
            possibleMoves = scoringMoves;
        }

        // Sort the moves by score (highest first)
        possibleMoves.sort(Comparator.comparing(Move::getScore).reversed());

        // Choose from the top 3 moves with a slight randomization
        int cutoff = Math.min(3, possibleMoves.size());

        // 70% chance to pick the best move, 20% for second best, 10% for third best
        double rand = random.nextDouble();
        if (rand < 0.7 || cutoff == 1) {
            return possibleMoves.get(0);
        } else if (rand < 0.9 || cutoff == 2) {
            return possibleMoves.get(1);
        } else {
            return possibleMoves.get(2);
        }
    }

    /**
     * Generates a fallback move when no valid placements are found.
     */
    private Move generateFallbackMove(Game game, Player player) {
        // If enough tiles in the bag, exchange some tiles
        if (game.getTileBag().getTileCount() >= 7) {
            System.out.println("Computer: Generating exchange move");
            List<Tile> tilesToExchange = selectTilesToExchange(player);
            if (!tilesToExchange.isEmpty()) {
                System.out.println("Computer exchanging " + tilesToExchange.size() + " tiles");
                return Move.createExchangeMove(player, tilesToExchange);
            }
        }

        // Otherwise pass
        System.out.println("Computer: Generating pass move");
        return Move.createPassMove(player);
    }

    /**
     * Selects which tiles to exchange from the player's rack.
     */
    private List<Tile> selectTilesToExchange(Player player) {
        Rack rack = player.getRack();
        List<Tile> availableTiles = new ArrayList<>(rack.getTiles());
        List<Tile> tilesToExchange = new ArrayList<>();
        Map<Tile, Double> tileValues = evaluateTileValues(availableTiles);

        // Sort tiles by their strategic value (lowest first)
        availableTiles.sort(Comparator.comparing(tileValues::get));

        // Exchange 1-2 of the worst tiles
        int numToExchange = Math.min(2, availableTiles.size());
        for (int i = 0; i < numToExchange; i++) {
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
     * Evaluates moves using advanced heuristics to balance scoring and rack management.
     */
    private List<Move> evaluateMovesWithAdvancedHeuristics(List<Move> moves, Game game, Player player) {
        Board board = game.getBoard();
        List<Move> evaluatedMoves = new ArrayList<>(moves);

        // Evaluate each move with multiple factors
        for (Move move : evaluatedMoves) {
            int baseScore = move.getScore();
            double strategicValue = calculateStrategicValue(move, game);

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
    private double calculateStrategicValue(Move move, Game game) {
        double strategicValue = 0;

        // Value for using all tiles (bingo potential)
        if (move.getTiles().size() == 7) {
            strategicValue += BONUS_USING_ALL_TILES;
        }

        // Value for forming multiple words
        if (move.getFormedWords().size() > 1) {
            strategicValue += BONUS_FORMING_TWO_WORDS;
        }

        // Value for rack leave quality
        double rackLeaveValue = evaluateRackLeave(move);
        strategicValue += rackLeaveValue * RACK_LEAVE_WEIGHT;

        // Value for using premium squares
        double premiumSquareValue = evaluatePremiumSquareUsage(move, game.getBoard());
        strategicValue += premiumSquareValue * PREMIUM_SQUARE_WEIGHT;

        return strategicValue;
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
                col++;
            } else {
                row++;
            }
        }

        return value;
    }

    /**
     * Find all anchor points (empty squares adjacent to existing tiles).
     */
    private List<Point> findAnchorPoints(Board board) {
        List<Point> anchorPoints = new ArrayList<>();

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (!board.getSquare(row, col).hasTile() && hasAdjacentTile(board, row, col)) {
                    anchorPoints.add(new Point(row, col));
                }
            }
        }

        return anchorPoints;
    }

    /**
     * Check if a position has an adjacent tile.
     */
    private boolean hasAdjacentTile(Board board, int row, int col) {
        if (row > 0 && board.getSquare(row - 1, col).hasTile()) return true;
        if (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile()) return true;
        if (col > 0 && board.getSquare(row, col - 1).hasTile()) return true;
        if (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile()) return true;
        return false;
    }

    /**
     * Create a temporary board with a move applied.
     */
    private Board createTempBoardWithMove(Board originalBoard, Move move) {
        Board tempBoard = new Board();

        // Copy existing tiles
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (originalBoard.getSquare(r, c).hasTile()) {
                    tempBoard.placeTile(r, c, originalBoard.getSquare(r, c).getTile());
                }
            }
        }

        // Apply the move
        int row = move.getStartRow();
        int col = move.getStartCol();
        Direction direction = move.getDirection();

        for (Tile tile : move.getTiles()) {
            // Skip occupied squares
            while (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE &&
                    tempBoard.getSquare(row, col).hasTile()) {
                if (direction.isHorizontal()) {
                    col++;
                } else {
                    row++;
                }
            }

            // Check if we're still on the board
            if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {
                break;
            }

            // Place the tile
            tempBoard.placeTile(row, col, tile);

            // Move to next position
            if (direction.isHorizontal()) {
                col++;
            } else {
                row++;
            }
        }

        return tempBoard;
    }

    /**
     * Get tiles needed to form a word.
     */
    private List<Tile> getTilesForWord(String word, List<Tile> rackTiles) {
        Map<Character, List<Tile>> tilesByLetter = new HashMap<>();

        // Group tiles by letter
        for (Tile tile : rackTiles) {
            char letter = tile.getLetter();
            tilesByLetter.computeIfAbsent(letter, k -> new ArrayList<>()).add(tile);
        }

        List<Tile> result = new ArrayList<>();

        // For each letter in the word, find a matching tile
        for (char c : word.toCharArray()) {
            List<Tile> availableTiles = tilesByLetter.getOrDefault(c, new ArrayList<>());

            if (!availableTiles.isEmpty()) {
                Tile tile = availableTiles.remove(0);
                result.add(tile);
            } else {
                // Try using a blank tile
                List<Tile> blankTiles = tilesByLetter.getOrDefault('*', new ArrayList<>());
                if (!blankTiles.isEmpty()) {
                    Tile blankTile = blankTiles.remove(0);
                    // Create a blank tile with the needed letter
                    Tile letterTile = Tile.createBlankTile(c);
                    result.add(letterTile);
                } else {
                    // Can't form the word
                    return null;
                }
            }
        }

        return result;
    }

    /**
     * Convert a list of tiles to a string of letters.
     */
    private String getTilesAsString(List<Tile> tiles) {
        StringBuilder sb = new StringBuilder();
        for (Tile tile : tiles) {
            sb.append(tile.getLetter());
        }
        return sb.toString();
    }

    /**
     * Check if a letter is a vowel.
     */
    private boolean isVowel(char letter) {
        char c = Character.toUpperCase(letter);
        return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
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