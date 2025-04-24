package model;

import util.Direction;
import util.ScrabbleConstants;

import java.awt.Point;
import java.util.*;

/**
 * Computer player implementation for Scrabble.
 * Supports different difficulty levels and can generate moves.
 */
public class ComputerPlayer {
    private final Player player;
    private final Random random;
    private int difficultyLevel;

    /**
     * Difficulty levels
     * 1 = Easy (random moves)
     * 2 = Medium (mid-score moves)
     * 3 = Hard (high-score moves)
     */
    public ComputerPlayer(Player player, int difficultyLevel) {
        this.player = player;
        this.random = new Random();
        this.difficultyLevel = Math.max(1, Math.min(3, difficultyLevel));
        player.setComputer(true);
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Generate a move for the computer player.
     *
     * @param game the current game
     * @return the move to make
     */
    public Move generateMove(Game game) {
        try {
            System.out.println("Computer player generating move at difficulty " + difficultyLevel);

            if (player.getRack().size() == 0) {
                System.out.println("Computer has no tiles, passing");
                return Move.createPassMove(player);
            }

            // Find possible moves
            List<Move> possibleMoves = new ArrayList<>();
            try {
                possibleMoves = findPossibleMoves(game);
                System.out.println("Found " + possibleMoves.size() + " possible moves");
            } catch (Exception e) {
                System.err.println("Error finding possible moves: " + e.getMessage());
                e.printStackTrace();
                return generateFallbackMove(game);
            }

            if (possibleMoves.isEmpty()) {
                System.out.println("Computer: No possible word placements found, trying fallback");
                return generateFallbackMove(game);
            }

            // Sort moves by score (highest first)
            possibleMoves.sort(Comparator.comparing(Move::getScore).reversed());

            // Select a move based on difficulty
            Move selectedMove;
            switch (difficultyLevel) {
                case 1: // Easy - random move
                    selectedMove = possibleMoves.get(random.nextInt(possibleMoves.size()));
                    break;
                case 2: // Medium - choose from top half
                    int mediumCutoff = Math.max(1, possibleMoves.size() / 2);
                    selectedMove = possibleMoves.get(random.nextInt(mediumCutoff));
                    break;
                case 3: // Hard - choose from top 3
                    int hardCutoff = Math.min(3, possibleMoves.size());
                    selectedMove = possibleMoves.get(random.nextInt(hardCutoff));
                    break;
                default:
                    int defaultCutoff = Math.max(1, possibleMoves.size() / 2);
                    selectedMove = possibleMoves.get(random.nextInt(defaultCutoff));
            }

            System.out.println("Computer selected move with score: " + selectedMove.getScore());
            System.out.println("Words formed: " + String.join(", ", selectedMove.getFormedWords()));
            return selectedMove;

        } catch (Exception e) {
            System.err.println("Error generating computer move: " + e.getMessage());
            e.printStackTrace();
            return Move.createPassMove(player);
        }
    }

    /**
     * Generate a fallback move (exchange or pass).
     * Used when no word placements are found.
     *
     * @param game the current game
     * @return the fallback move
     */
    private Move generateFallbackMove(Game game) {
        try {
            // If enough tiles in the bag, exchange some tiles
            if (game.getTileBag().getTileCount() >= 7) {
                System.out.println("Computer: Generating exchange move");
                List<Tile> tilesToExchange = selectTilesToExchange();
                if (!tilesToExchange.isEmpty()) {
                    System.out.println("Computer exchanging " + tilesToExchange.size() + " tiles");
                    return Move.createExchangeMove(player, tilesToExchange);
                }
            }

            // Otherwise pass
            System.out.println("Computer: Generating pass move");
            return Move.createPassMove(player);
        } catch (Exception e) {
            System.err.println("Error generating fallback move: " + e.getMessage());
            e.printStackTrace();
            return Move.createPassMove(player);
        }
    }

    /**
     * Select tiles to exchange when no good word placements are found.
     * Strategy: Exchange tiles with low probability of forming words.
     *
     * @return list of tiles to exchange
     */
    private List<Tile> selectTilesToExchange() {
        Rack rack = player.getRack();
        List<Tile> availableTiles = new ArrayList<>(rack.getTiles());
        List<Tile> tilesToExchange = new ArrayList<>();

        // Count vowels and rare letters
        int vowelCount = 0;
        int rareLetterCount = 0;
        Set<Character> rareLetters = Set.of('J', 'Q', 'X', 'Z');

        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();
            if (isVowel(letter)) {
                vowelCount++;
            }
            if (rareLetters.contains(letter)) {
                rareLetterCount++;
            }
        }

        // Prioritize tiles to exchange
        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();

            // Exchange rare letters if we already have one
            if (rareLetters.contains(letter) && rareLetterCount > 1) {
                tilesToExchange.add(tile);
                rareLetterCount--;
                continue;
            }

            // Exchange high-value tiles (but keep one rare letter)
            if (tile.getValue() >= 8 && !rareLetters.contains(letter)) {
                tilesToExchange.add(tile);
                continue;
            }

            // Balance vowels and consonants
            if (isVowel(letter) && vowelCount > 3) {
                tilesToExchange.add(tile);
                vowelCount--;
                continue;
            }

            // Don't exchange more than 3 tiles
            if (tilesToExchange.size() >= 3) {
                break;
            }
        }

        return tilesToExchange;
    }

    /**
     * Find possible moves for the computer player.
     * This implementation is a simplified version that checks
     * only basic word placements. A full implementation would
     * use more advanced algorithms.
     *
     * @param game the current game
     * @return list of possible moves
     */
    private List<Move> findPossibleMoves(Game game) {
        List<Move> possibleMoves = new ArrayList<>();
        Board board = game.getBoard();
        Gaddag dictionary = game.getDictionary();
        Rack rack = player.getRack();

        // Get all possible anchor points (empty squares adjacent to existing tiles)
        List<Point> anchorPoints = findAnchorPoints(board);

        // For empty board, just place a word through the center
        if (board.isEmpty()) {
            // Try to form words from rack tiles
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

                            // Validate words and score
                            MoveValidator validator = new MoveValidator(board, dictionary,
                                    game.allowsBidirectionalWords());
                            if (validator.isValidPlaceMove(move)) {
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

                            // Validate words and score
                            MoveValidator validator = new MoveValidator(board, dictionary,
                                    game.allowsBidirectionalWords());
                            if (validator.isValidPlaceMove(move)) {
                                possibleMoves.add(move);
                            }
                        }
                    }
                }
            }
        }
        // For non-empty board, try extending existing words
        else {
            for (Point anchor : anchorPoints) {
                int row = anchor.x;
                int col = anchor.y;

                // Try horizontal placement
                tryPlacementAtAnchor(game, row, col, Direction.HORIZONTAL, possibleMoves);

                // Try vertical placement
                tryPlacementAtAnchor(game, row, col, Direction.VERTICAL, possibleMoves);

                // If bidirectional words are allowed, try reversed directions
                if (game.allowsBidirectionalWords()) {
                    tryPlacementAtAnchor(game, row, col, Direction.HORIZONTAL_REVERSE, possibleMoves);
                    tryPlacementAtAnchor(game, row, col, Direction.VERTICAL_REVERSE, possibleMoves);
                }
            }
        }

        return possibleMoves;
    }

    /**
     * Try placing a tile at an anchor point and see if valid words can be formed.
     */
    private void tryPlacementAtAnchor(Game game, int row, int col, Direction direction,
                                      List<Move> possibleMoves) {
        Rack rack = player.getRack();
        Board board = game.getBoard();
        Gaddag dictionary = game.getDictionary();

        // Try each tile in the rack at this position
        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.getTile(i);

            // Create a move with just this tile
            Move move = Move.createPlaceMove(player, row, col, direction);
            List<Tile> tiles = new ArrayList<>();
            tiles.add(tile);
            move.addTiles(tiles);

            // Check if this forms valid words
            MoveValidator validator = new MoveValidator(board, dictionary,
                    game.allowsBidirectionalWords());
            if (validator.isValidPlaceMove(move)) {
                // Calculate score
                Board tempBoard = createTempBoardWithMove(board, move);
                ScoreCalculator calculator = new ScoreCalculator(board, validator);
                int score = calculator.calculateMoveScore(tempBoard, move);
                move.setScore(score);

                possibleMoves.add(move);

                // Try extending this placement
                tryExtendingPlacement(game, move, tempBoard, row, col, direction, possibleMoves);
            }
        }
    }

    /**
     * Try extending a placement with additional tiles.
     */
    private void tryExtendingPlacement(Game game, Move baseMove, Board tempBoard,
                                       int row, int col, Direction direction,
                                       List<Move> possibleMoves) {
        // Get remaining tiles
        List<Tile> remainingTiles = new ArrayList<>(player.getRack().getTiles());
        for (Tile t : baseMove.getTiles()) {
            remainingTiles.remove(t);
        }

        if (remainingTiles.isEmpty()) {
            return;
        }

        int nextRow = row;
        int nextCol = col;

        // Move to next position based on direction
        if (direction.isHorizontal()) {
            if (direction.isReverse()) {
                nextCol--;
            } else {
                nextCol++;
            }
        } else {
            if (direction.isReverse()) {
                nextRow--;
            } else {
                nextRow++;
            }
        }

        // Check if next position is valid
        if (nextRow < 0 || nextRow >= Board.SIZE || nextCol < 0 || nextCol >= Board.SIZE ||
                tempBoard.getSquare(nextRow, nextCol).hasTile()) {
            return;
        }

        // Try each remaining tile
        for (Tile tile : remainingTiles) {
            // Create extended move
            Move extendedMove = Move.createPlaceMove(player,
                    baseMove.getStartRow(),
                    baseMove.getStartCol(),
                    direction);
            List<Tile> allTiles = new ArrayList<>(baseMove.getTiles());
            allTiles.add(tile);
            extendedMove.addTiles(allTiles);

            // Check if this forms valid words
            MoveValidator validator = new MoveValidator(game.getBoard(), game.getDictionary(),
                    game.allowsBidirectionalWords());
            if (validator.isValidPlaceMove(extendedMove)) {
                // Calculate score
                Board extendedBoard = createTempBoardWithMove(game.getBoard(), extendedMove);
                ScoreCalculator calculator = new ScoreCalculator(game.getBoard(), validator);
                int score = calculator.calculateMoveScore(extendedBoard, extendedMove);
                extendedMove.setScore(score);

                possibleMoves.add(extendedMove);

                // Recursively try extending further
                tryExtendingPlacement(game, extendedMove, extendedBoard,
                        nextRow, nextCol, direction, possibleMoves);
            }
        }
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
                    col += direction.isReverse() ? -1 : 1;
                } else {
                    row += direction.isReverse() ? -1 : 1;
                }
            }

            // Place the tile
            if (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) {
                tempBoard.placeTile(row, col, tile);

                // Move to next position
                if (direction.isHorizontal()) {
                    col += direction.isReverse() ? -1 : 1;
                } else {
                    row += direction.isReverse() ? -1 : 1;
                }
            }
        }

        return tempBoard;
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
     * Generate all valid words that can be formed with the given rack letters.
     */
    private Set<String> generateWordsFromRack(String rackLetters, Gaddag dictionary) {
        Set<String> validWords = new HashSet<>();
        generateWords("", rackLetters, dictionary, validWords);
        return validWords;
    }

    /**
     * Recursive word generation.
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
}
