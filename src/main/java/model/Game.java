package model;

import util.Direction;
import util.ScrabbleConstants;

import java.awt.Point;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Main game class that manages the Scrabble game state and flow.
 */
public class Game {
    private final Board board;
    private final TileBag tileBag;
    private final List<Player> players;
    private final Gaddag dictionary;
    private int currentPlayerIndex;
    private boolean gameOver;
    private int consecutivePasses;
    private final List<Move> moveHistory;
    private static final int EMPTY_RACK_BONUS = 50;

    // Game settings
    private final boolean allowBidirectionalWords;

    // Components for move validation and scoring
    private final MoveValidator moveValidator;
    private final ScoreCalculator scoreCalculator;

    // Temporary placement tracking
    private final Map<Point, Tile> temporaryPlacements = new HashMap<>();
    private final List<Integer> temporaryIndices = new ArrayList<>();

    /**
     * Constructs a new Game with the specified dictionary.
     *
     * @param allowBidirectionalWords whether to allow bidirectional word reading
     * @throws IOException if the dictionary cannot be loaded
     */
    public Game(boolean allowBidirectionalWords) throws IOException {
        this.board = new Board();
        this.tileBag = new TileBag();
        this.players = new ArrayList<>();
        this.dictionary = new Gaddag();
        this.currentPlayerIndex = 0;
        this.gameOver = false;
        this.consecutivePasses = 0;
        this.moveHistory = new ArrayList<>();
        this.allowBidirectionalWords = allowBidirectionalWords;

        // Initialize validator and score calculator
        this.moveValidator = new MoveValidator(board, dictionary, allowBidirectionalWords);
        this.scoreCalculator = new ScoreCalculator(board, moveValidator);
    }

    /**
     * Constructs a new Game with default settings.
     *
     * @throws IOException if the dictionary cannot be loaded
     */
    public Game() throws IOException {
        this(false); // Default to standard Scrabble rules (no bidirectional words)
    }

    /**
     * Adds a player to the game.
     *
     * @param player the player to add
     */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * Starts the game by dealing tiles to all players.
     * Randomly selects the first player.
     */
    public void start() {
        if (players.isEmpty()) {
            throw new IllegalStateException("Cannot start game with no players");
        }

        tileBag.shuffle();

        for (Player player : players) {
            fillRack(player);
        }

        currentPlayerIndex = (int) (Math.random() * players.size());
        gameOver = false;
        consecutivePasses = 0;
        moveHistory.clear();
    }

    /**
     * Fills a player's rack with tiles from the bag.
     *
     * @param player the player whose rack to fill
     */
    public void fillRack(Player player) {
        Rack rack = player.getRack();
        int tilesToDraw = rack.getEmptySlots();

        if (tilesToDraw == 0) {
            return;
        }

        List<Tile> drawnTiles = tileBag.drawTiles(tilesToDraw);
        rack.addTiles(drawnTiles);
    }

    /**
     * Gets the current player.
     *
     * @return the current player
     */
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }

    /**
     * Advances the game to the next player.
     */
    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        System.out.println("Current player is now: " + getCurrentPlayer().getName());
    }

    /**
     * Gets the game board.
     *
     * @return the game board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Gets the tile bag.
     *
     * @return the tile bag
     */
    public TileBag getTileBag() {
        return tileBag;
    }

    /**
     * Gets the dictionary.
     *
     * @return the dictionary
     */
    public Gaddag getDictionary() {
        return dictionary;
    }

    /**
     * Gets an unmodifiable view of the players.
     *
     * @return the players
     */
    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /**
     * Checks if the game is over.
     *
     * @return true if the game is over
     */
    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Gets an unmodifiable view of the move history.
     *
     * @return the move history
     */
    public List<Move> getMoveHistory() {
        return Collections.unmodifiableList(moveHistory);
    }

    /**
     * Returns whether bidirectional words are allowed in this game.
     *
     * @return true if bidirectional words are allowed
     */
    public boolean allowsBidirectionalWords() {
        return allowBidirectionalWords;
    }

    /**
     * Executes a move.
     *
     * @param move the move to execute
     * @return true if the move was successfully executed
     */
    public boolean executeMove(Move move) {
        if (gameOver) {
            System.out.println("Game is already over");
            return false;
        }

        if (move.getPlayer() != getCurrentPlayer()) {
            System.out.println("Not this player's turn");
            return false;
        }

        boolean success = false;
        System.out.println("Executing move of type: " + move.getType());

        try {
            switch (move.getType()) {
                case PLACE:
                    success = executePlaceMove(move);
                    break;
                case EXCHANGE:
                    success = executeExchangeMove(move);
                    break;
                case PASS:
                    success = executePassMove(move);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error executing move: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        if (success) {
            System.out.println("Move executed successfully");
            moveHistory.add(move);

            if (checkGameOver()) {
                finalizeGameScore();
                return true;
            }

            nextPlayer();
            System.out.println("Turn passed to: " + getCurrentPlayer().getName());
        } else {
            System.out.println("Move execution failed");
        }

        return success;
    }

    /**
     * Executes a placement move.
     * The enhanced approach allows players to place tiles anywhere during setup,
     * and supports bidirectional word reading.
     *
     * @param move the placement move to execute
     * @return true if the move was successfully executed
     */
    private boolean executePlaceMove(Move move) {
        // Create a temporary board to validate words and calculate score
        Board tempBoard = createTempBoardWithMove(move);
        if (tempBoard == null) {
            return false;
        }

        // Validate the move
        if (!moveValidator.isValidPlaceMove(move)) {
            return false;
        }

        // Calculate the score
        int score = scoreCalculator.calculateMoveScore(tempBoard, move);
        move.setScore(score);

        // Find the positions of the new tiles
        List<Point> newTilePositions = moveValidator.findNewTilePositions(tempBoard, move);

        // Apply the move to the actual board
        Player player = move.getPlayer();
        List<Tile> tilesToPlace = move.getTiles();

        applyMoveToBoard(move, newTilePositions, tilesToPlace);

        // Update the player's score
        player.addScore(score);

        // Reset consecutive passes counter
        consecutivePasses = 0;

        // Refill the player's rack
        fillRack(player);

        // Check if the player used all tiles and the bag is empty (end game condition)
        if (player.getRack().isEmpty() && tileBag.isEmpty()) {
            player.addScore(EMPTY_RACK_BONUS);
        }

        System.out.println("Move executed successfully - formed words: " + String.join(", ", move.getFormedWords()) +
                " for " + score + " points");

        return true;
    }

    /**
     * Applies a move to the game board by placing tiles and removing them from the player's rack.
     */
    private void applyMoveToBoard(Move move, List<Point> newTilePositions, List<Tile> tilesToPlace) {
        Player player = move.getPlayer();

        for (Point position : newTilePositions) {
            for (Tile tile : tilesToPlace) {
                if (!player.getRack().getTiles().contains(tile)) {
                    continue; // Skip tiles that have already been placed
                }

                // Place the tile on the board
                board.placeTile(position.x, position.y, tile);
                // Mark the square as used (premium squares are only used once)
                board.getSquare(position.x, position.y).useSquareType();
                // Remove the tile from the player's rack
                player.getRack().removeTile(tile);

                break;
            }
        }
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
     * Executes an exchange move.
     *
     * @param move the exchange move to execute
     * @return true if the move was successfully executed
     */
    private boolean executeExchangeMove(Move move) {
        try {
            Player player = move.getPlayer();
            List<Tile> tilesToExchange = move.getTiles();

            if (tileBag.getTileCount() < 1) {
                System.out.println("Not enough tiles in bag: " + tileBag.getTileCount());
                return false;
            }

            System.out.println("Before removal - Rack size: " + player.getRack().size());
            System.out.println("Tiles to exchange: " + tilesToExchange.size());

            StringBuilder exchangeLog = new StringBuilder("Exchanging tiles: ");
            for (Tile t : tilesToExchange) {
                exchangeLog.append(t.getLetter()).append(" ");
            }
            System.out.println(exchangeLog);

            if (!player.getRack().removeTiles(tilesToExchange)) {
                System.out.println("Failed to remove tiles from rack");
                return false;
            }

            System.out.println("After removal - Rack size: " + player.getRack().size());

            int numTilesToDraw = tilesToExchange.size();
            List<Tile> newTiles = tileBag.drawTiles(numTilesToDraw);

            System.out.println("Drew " + newTiles.size() + " new tiles");

            int tilesAdded = player.getRack().addTiles(newTiles);

            System.out.println("Added " + tilesAdded + " tiles to rack");

            tileBag.returnTiles(tilesToExchange);

            System.out.println("Returned " + tilesToExchange.size() + " tiles to bag");
            System.out.println("After exchange - Rack size: " + player.getRack().size());

            consecutivePasses = 0;

            return true;
        } catch (Exception e) {
            System.err.println("Error in executeExchangeMove: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Executes a pass move.
     *
     * @param move the pass move to execute
     * @return true if the move was successfully executed
     */
    private boolean executePassMove(Move move) {
        consecutivePasses++;
        return true;
    }

    /**
     * Checks if the game is over.
     *
     * @return true if the game is over
     */
    public boolean checkGameOver() {
        // Game ends if a player is out of tiles and the bag is empty
        for (Player player : players) {
            if (player.isOutOfTiles() && tileBag.isEmpty()) {
                gameOver = true;
                return true;
            }
        }

        // Game also ends if there have been too many consecutive passes
        if (consecutivePasses >= players.size() * 2) {
            gameOver = true;
            return true;
        }

        return false;
    }

    /**
     * Places a tile temporarily on the board during a player's turn.
     * This allows the player to experiment with placements before committing.
     *
     * @param rackIndex the index of the tile in the player's rack
     * @param row the row to place the tile
     * @param col the column to place the tile
     * @return true if the tile was placed successfully
     */
    public boolean placeTileTemporarily(int rackIndex, int row, int col) {
        try {
            if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {
                return false;
            }

            Player currentPlayer = getCurrentPlayer();
            Rack rack = currentPlayer.getRack();

            if (rackIndex < 0 || rackIndex >= rack.size()) {
                return false;
            }

            // Can't place on an occupied square
            if (board.getSquare(row, col).hasTile() || hasTemporaryTileAt(row, col)) {
                return false;
            }

            Tile tile = rack.getTile(rackIndex);

            // Can't use the same tile twice
            if (temporaryIndices.contains(rackIndex)) {
                return false;
            }

            temporaryPlacements.put(new Point(row, col), tile);
            temporaryIndices.add(rackIndex);

            return true;
        } catch (Exception e) {
            System.err.println("Error in placeTileTemporarily: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if there is a temporary tile placement at the specified position.
     *
     * @param row the row
     * @param col the column
     * @return true if there is a temporary tile at the position
     */
    public boolean hasTemporaryTileAt(int row, int col) {
        return temporaryPlacements.containsKey(new Point(row, col));
    }

    /**
     * Gets the temporary tile at the specified position.
     *
     * @param row the row
     * @param col the column
     * @return the tile at the position, or null if none
     */
    public Tile getTemporaryTileAt(int row, int col) {
        return temporaryPlacements.get(new Point(row, col));
    }

    /**
     * Cancels all temporary placements, returning tiles to the rack.
     */
    public void cancelTemporaryPlacements() {
        temporaryPlacements.clear();
        temporaryIndices.clear();
    }

    /**
     * Gets all temporary placements.
     *
     * @return a map of positions to tiles
     */
    public Map<Point, Tile> getTemporaryPlacements() {
        return new HashMap<>(temporaryPlacements);
    }

    /**
     * Commits the temporary placements as an actual move.
     *
     * @return true if the move was valid and executed successfully
     */
    public boolean commitTemporaryPlacements() {
        if (temporaryPlacements.isEmpty()) {
            return false;
        }

        // Determine move direction
        Direction direction = determineDirection();
        if (direction == null && temporaryPlacements.size() > 1) {
            System.out.println("Invalid move: Tiles must be placed in a straight line");
            return false;
        }

        // Find the starting position of the word
        int startRow = Integer.MAX_VALUE;
        int startCol = Integer.MAX_VALUE;
        for (Point p : temporaryPlacements.keySet()) {
            startRow = Math.min(startRow, p.x);
            startCol = Math.min(startCol, p.y);
        }

        // Create a move from the temporary placements
        Move move = Move.createPlaceMove(getCurrentPlayer(), startRow, startCol,
                direction != null ? direction : Direction.HORIZONTAL);

        // Add tiles to the move in the correct order
        List<Tile> tilesToPlace = new ArrayList<>();

        if (direction == Direction.HORIZONTAL || direction == Direction.HORIZONTAL_REVERSE) {
            // Sort by column for horizontal words
            List<Point> points = new ArrayList<>(temporaryPlacements.keySet());
            points.sort(Comparator.comparingInt(p -> p.y));

            for (Point p : points) {
                tilesToPlace.add(temporaryPlacements.get(p));
            }
        } else {
            // Sort by row for vertical words
            List<Point> points = new ArrayList<>(temporaryPlacements.keySet());
            points.sort(Comparator.comparingInt(p -> p.x));

            for (Point p : points) {
                tilesToPlace.add(temporaryPlacements.get(p));
            }
        }

        move.addTiles(tilesToPlace);

        // Execute the move
        boolean success = executeMove(move);

        if (success) {
            temporaryPlacements.clear();
            temporaryIndices.clear();
        }

        return success;
    }

    /**
     * Determines the direction of tile placement based on temporary placements.
     *
     * @return the direction of the word being formed, or null if undetermined
     */
    public Direction determineDirection() {
        if (temporaryPlacements.size() <= 1) {
            return null; // Can't determine direction with 0 or 1 tile
        }

        List<Point> points = new ArrayList<>(temporaryPlacements.keySet());

        boolean sameRow = true;
        int firstRow = points.get(0).x;

        boolean sameColumn = true;
        int firstCol = points.get(0).y;

        for (Point p : points) {
            if (p.x != firstRow) {
                sameRow = false;
            }
            if (p.y != firstCol) {
                sameColumn = false;
            }
        }

        if (sameRow && !sameColumn) {
            return Direction.HORIZONTAL;
        }
        if (!sameRow && sameColumn) {
            return Direction.VERTICAL;
        }

        return null; // Tiles are not in a straight line
    }

    /**
     * Finalizes the game score.
     * - If a player is out of tiles, they get bonus points from other players' racks
     * - Otherwise, each player loses points for tiles remaining in their rack
     */
    private void finalizeGameScore() {
        Player outPlayer = null;
        for (Player player : players) {
            if (player.isOutOfTiles()) {
                outPlayer = player;
                break;
            }
        }

        if (outPlayer != null) {
            int bonusPoints = 0;
            for (Player player : players) {
                if (player != outPlayer) {
                    int rackValue = player.getRackValue();
                    player.addScore(-rackValue);
                    bonusPoints += rackValue;
                }
            }
            outPlayer.addScore(bonusPoints);
        } else {
            for (Player player : players) {
                player.addScore(-player.getRackValue());
            }
        }
    }
}