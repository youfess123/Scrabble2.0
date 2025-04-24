package AIStrategy;

import model.*;
import util.Direction;
import util.PlayerStrategy;
import util.ScrabbleConstants;

import java.awt.Point;
import java.util.*;
import java.util.List;

/**
 * Base implementation of PlayerStrategy with common functionality for all difficulty levels.
 */
public abstract class BaseStrategy implements PlayerStrategy {
    protected final Random random = new Random();

    /**
     * Generates a move for the specified player based on the current game state.
     * Template method that follows these steps:
     * 1. Find possible placement moves
     * 2. If valid moves exist, select one based on strategy
     * 3. If no valid moves, generate a fallback move (exchange or pass)
     *
     * @param game the current game
     * @param player the player to generate a move for
     * @return the generated move
     */
    @Override
    public Move generateMove(Game game, Player player) {
        try {
            // Find all possible moves
            List<Move> possibleMoves = findPossibleMoves(game, player);
            System.out.println("Found " + possibleMoves.size() + " possible moves");

            // If moves found, select one based on strategy
            if (!possibleMoves.isEmpty()) {
                Move selectedMove = selectMove(possibleMoves);
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
     * Finds all possible placement moves for the player.
     * This implementation is shared by all strategies.
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

                // Try horizontal and vertical placements
                tryPlacementsAtAnchor(game, player, row, col, Direction.HORIZONTAL, possibleMoves);
                tryPlacementsAtAnchor(game, player, row, col, Direction.VERTICAL, possibleMoves);

                // If bidirectional words are allowed
                if (game.allowsBidirectionalWords()) {
                    tryPlacementsAtAnchor(game, player, row, col, Direction.HORIZONTAL_REVERSE, possibleMoves);
                    tryPlacementsAtAnchor(game, player, row, col, Direction.VERTICAL_REVERSE, possibleMoves);
                }
            }
        }

        // Sort by score (highest first)
        possibleMoves.sort(Comparator.comparing(Move::getScore).reversed());
        return possibleMoves;
    }

    /**
     * Tries placing tiles at an anchor point in a specific direction.
     *
     * @param game the current game
     * @param player the player
     * @param row the anchor row
     * @param col the anchor column
     * @param direction the direction to place tiles
     * @param possibleMoves list to add valid moves to
     */
    protected void tryPlacementsAtAnchor(Game game, Player player, int row, int col,
                                         Direction direction, List<Move> possibleMoves) {
        Rack rack = player.getRack();
        Board board = game.getBoard();
        MoveValidator validator = new MoveValidator(board, game.getDictionary(),
                game.allowsBidirectionalWords());

        // Try each tile in the rack at this position
        for (int i = 0; i < rack.size(); i++) {
            Tile tile = rack.getTile(i);

            // Create a move with just this tile
            Move move = Move.createPlaceMove(player, row, col, direction);
            List<Tile> tiles = new ArrayList<>();
            tiles.add(tile);
            move.addTiles(tiles);

            // Check if this forms valid words
            if (validator.isValidPlaceMove(move)) {
                // Calculate score
                Board tempBoard = createTempBoardWithMove(board, move);
                ScoreCalculator calculator = new ScoreCalculator(board, validator);
                int score = calculator.calculateMoveScore(tempBoard, move);
                move.setScore(score);

                possibleMoves.add(move);

                // Try extending this placement
                tryExtendingPlacement(game, player, move, tempBoard, row, col,
                        direction, possibleMoves);
            }
        }
    }

    /**
     * Tries extending a placement with additional tiles.
     */
    protected void tryExtendingPlacement(Game game, Player player, Move baseMove,
                                         Board tempBoard, int row, int col,
                                         Direction direction, List<Move> possibleMoves) {
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
                tryExtendingPlacement(game, player, extendedMove, extendedBoard,
                        nextRow, nextCol, direction, possibleMoves);
            }
        }
    }

    /**
     * Finds moves for an empty board (first move of the game).
     */
    protected void findMovesForEmptyBoard(Game game, Player player, List<Move> possibleMoves) {
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

                        // Validate words and score
                        MoveValidator validator = new MoveValidator(game.getBoard(), dictionary,
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
                        MoveValidator validator = new MoveValidator(game.getBoard(), dictionary,
                                game.allowsBidirectionalWords());
                        if (validator.isValidPlaceMove(move)) {
                            possibleMoves.add(move);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate all valid words that can be formed with the given rack letters.
     */
    protected Set<String> generateWordsFromRack(String rackLetters, Gaddag dictionary) {
        Set<String> validWords = new HashSet<>();
        generateWords("", rackLetters, dictionary, validWords);
        return validWords;
    }

    /**
     * Recursive word generation.
     */
    protected void generateWords(String prefix, String remaining, Gaddag dictionary, Set<String> validWords) {
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
     * Generates a fallback move when no valid placements are found.
     * This could be exchanging tiles or passing.
     */
    protected Move generateFallbackMove(Game game, Player player) {
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
     * This is a basic implementation - subclasses may override.
     */
    protected List<Tile> selectTilesToExchange(Player player) {
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

        // Determine how many tiles to exchange based on difficulty
        int numToExchange = determineExchangeCount(availableTiles.size());

        // Prioritize tiles to exchange
        for (Tile tile : availableTiles) {
            char letter = tile.getLetter();

            // Exchange rare letters if we already have one
            if (rareLetters.contains(letter) && rareLetterCount > 1) {
                tilesToExchange.add(tile);
                rareLetterCount--;
                if (tilesToExchange.size() >= numToExchange) break;
                continue;
            }

            // Exchange high-value tiles (but keep one rare letter)
            if (tile.getValue() >= 8 && !rareLetters.contains(letter)) {
                tilesToExchange.add(tile);
                if (tilesToExchange.size() >= numToExchange) break;
                continue;
            }

            // Balance vowels and consonants
            if (isVowel(letter) && vowelCount > 3) {
                tilesToExchange.add(tile);
                vowelCount--;
                if (tilesToExchange.size() >= numToExchange) break;
                continue;
            }
        }

        return tilesToExchange;
    }

    /**
     * Determines how many tiles to exchange.
     * Abstract method to be implemented by each difficulty level.
     *
     * @param availableTileCount number of tiles in the rack
     * @return number of tiles to exchange
     */
    protected abstract int determineExchangeCount(int availableTileCount);

    /**
     * Selects a move from the list of possible moves.
     * Abstract method to be implemented by each difficulty level.
     *
     * @param possibleMoves list of possible moves sorted by score
     * @return the selected move
     */
    protected abstract Move selectMove(List<Move> possibleMoves);

    /**
     * Find all anchor points (empty squares adjacent to existing tiles).
     */
    protected List<Point> findAnchorPoints(Board board) {
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
    protected boolean hasAdjacentTile(Board board, int row, int col) {
        if (row > 0 && board.getSquare(row - 1, col).hasTile()) return true;
        if (row < Board.SIZE - 1 && board.getSquare(row + 1, col).hasTile()) return true;
        if (col > 0 && board.getSquare(row, col - 1).hasTile()) return true;
        if (col < Board.SIZE - 1 && board.getSquare(row, col + 1).hasTile()) return true;
        return false;
    }

    /**
     * Create a temporary board with a move applied.
     */
    protected Board createTempBoardWithMove(Board originalBoard, Move move) {
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
     * Get tiles needed to form a word.
     */
    protected List<Tile> getTilesForWord(String word, List<Tile> rackTiles) {
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
    protected String getTilesAsString(List<Tile> tiles) {
        StringBuilder sb = new StringBuilder();
        for (Tile tile : tiles) {
            sb.append(tile.getLetter());
        }
        return sb.toString();
    }

    /**
     * Check if a letter is a vowel.
     */
    protected boolean isVowel(char letter) {
        char c = Character.toUpperCase(letter);
        return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
    }
}