package ui;

import AIStrategy.ComputerPlayer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import model.*;
import util.Direction;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JavaFX implementation for the Scrabble game.
 */
public class ScrabbleFX extends Application {
    // Game components
    private Game game;
    private ComputerPlayer computerPlayer;

    // Game state
    private final Map<Point, Tile> temporaryPlacements = new HashMap<>();
    private final List<Integer> selectedRackIndices = new ArrayList<>();
    private int selectedRackIndex = -1;
    private Direction currentDirection = Direction.HORIZONTAL;

    // UI components
    private BorderPane mainLayout;
    private GridPane boardGrid;
    private HBox rackPane;
    private TextArea gameLog;
    private Label statusLabel;
    private Label scoreLabel;
    private Button directionButton;
    private Button playButton;
    private Button exchangeButton;
    private Button passButton;

    // Board and rack UI elements
    private BoardButton[][] boardButtons;
    private RackButton[] rackButtons;

    // Background task handling
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void start(Stage primaryStage) {
        // Initialize the game
        initializeGame();

        // Create UI components
        createUI();

        // Create scene and set it on the stage
        Scene scene = new Scene(mainLayout, 900, 700);
        scene.getStylesheets().add(getClass().getResource("/ui/scrabble.css").toExternalForm());

        primaryStage.setTitle("Scrabble FX");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Log initial state
        logMessage("Game started. Human plays first.");
        logMessage("Place your tiles and click 'Play Word'");

        // Update UI to show initial state
        updateUI();
    }

    /**
     * Initializes the game logic.
     */
    private void initializeGame() {
        try {
            // Create game with bidirectional words enabled
            game = new Game();

            // Add a human player
            Player humanPlayer = new Player("Human");
            game.addPlayer(humanPlayer);

            // Add a computer player
            Player aiPlayer = new Player("Computer", true);
            game.addPlayer(aiPlayer);

            // Create the computer player controller
            computerPlayer = new ComputerPlayer(aiPlayer);

            // Start the game
            game.start();

        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Error initializing game: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
            Platform.exit();
        }
    }

    /**
     * Creates the UI components.
     */
    private void createUI() {
        // Main layout
        mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(10));

        // Board area (center)
        boardGrid = createBoardGrid();
        mainLayout.setCenter(boardGrid);

        // Controls area (right)
        VBox controlsBox = createControlsPanel();
        mainLayout.setRight(controlsBox);

        // Rack area (bottom)
        rackPane = createRackPane();
        mainLayout.setBottom(rackPane);
    }

    /**
     * Creates the board grid.
     */
    private GridPane createBoardGrid() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(10));

        // Initialize board buttons array
        boardButtons = new BoardButton[Board.SIZE][Board.SIZE];

        // Create all board squares
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                // Get square type
                Square.SquareType squareType = game.getBoard().getSquare(row, col).getSquareType();

                // Create button for this square
                BoardButton button = new BoardButton(row, col, squareType);

                // Set action on click
                int finalRow = row;
                int finalCol = col;
                button.setOnAction(e -> handleBoardButtonClick(finalRow, finalCol));

                // Add to grid and store reference
                grid.add(button, col, row);
                boardButtons[row][col] = button;
            }
        }

        return grid;
    }

    /**
     * Creates the controls panel.
     */
    private VBox createControlsPanel() {
        VBox controls = new VBox(10);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.TOP_CENTER);
        controls.setPrefWidth(200);

        // Status label
        statusLabel = new Label("Your Turn");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        // Score label
        scoreLabel = new Label("Score: 0 - 0");

        // Direction toggle button
        directionButton = new Button("Direction: →");
        directionButton.setOnAction(e -> toggleDirection());

        // Play button
        playButton = new Button("Play Word");
        playButton.setOnAction(e -> playCurrentWord());

        // Exchange button
        exchangeButton = new Button("Exchange Tiles");
        exchangeButton.setOnAction(e -> exchangeTiles());

        // Pass button
        passButton = new Button("Pass Turn");
        passButton.setOnAction(e -> passTurn());

        // Clear button
        Button clearButton = new Button("Clear Selection");
        clearButton.setOnAction(e -> clearTemporaryPlacements());

        // New Game button
        Button newGameButton = new Button("New Game");
        newGameButton.setOnAction(e -> resetGame());

        // Debug button
        Button debugButton = new Button("Debug AI Move");
        debugButton.setOnAction(e -> forceComputerMove());

        // Game log
        gameLog = new TextArea();
        gameLog.setEditable(false);
        gameLog.setPrefHeight(200);
        gameLog.setWrapText(true);

        // Add all controls to the panel
        controls.getChildren().addAll(
                statusLabel, scoreLabel, new Separator(),
                directionButton, playButton, exchangeButton, passButton,
                clearButton, newGameButton, debugButton, new Separator(),
                new Label("Game Log:"), gameLog
        );

        return controls;
    }

    /**
     * Creates the rack pane.
     */
    private HBox createRackPane() {
        HBox rack = new HBox(5);
        rack.setPadding(new Insets(10));
        rack.setAlignment(Pos.CENTER);

        // Create a titled border for the rack
        TitledPane titledPane = new TitledPane();
        titledPane.setText("Your Rack");
        titledPane.setCollapsible(false);
        titledPane.setContent(rack);

        // Initialize rack buttons array
        rackButtons = new RackButton[7];

        // Create buttons for each rack position
        for (int i = 0; i < 7; i++) {
            RackButton button = new RackButton(i);

            // Set action on click
            int index = i;
            button.setOnAction(e -> handleRackButtonClick(index));

            // Add to rack and store reference
            rack.getChildren().add(button);
            rackButtons[i] = button;
        }

        return rack;
    }

    /**
     * Updates the UI to reflect the current game state.
     */
    private void updateUI() {
        // Update board squares
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                BoardButton button = boardButtons[row][col];

                // Update with permanent tiles
                if (game.getBoard().getSquare(row, col).hasTile()) {
                    button.setTile(game.getBoard().getSquare(row, col).getTile(), false);
                }
                // Update with temporary placements
                else if (temporaryPlacements.containsKey(new Point(row, col))) {
                    button.setTile(temporaryPlacements.get(new Point(row, col)), true);
                }
                // Clear any tiles
                else {
                    button.clearTile();
                }
            }
        }

        // Update rack tiles
        Player humanPlayer = game.getPlayers().get(0);
        Rack rack = humanPlayer.getRack();

        for (int i = 0; i < 7; i++) {
            if (i < rack.size()) {
                rackButtons[i].setTile(rack.getTile(i));
                rackButtons[i].setSelected(selectedRackIndices.contains(i));
            } else {
                rackButtons[i].clearTile();
            }
        }

        // Update score label
        scoreLabel.setText(String.format("Human: %d | Computer: %d\nTiles left: %d",
                humanPlayer.getScore(),
                game.getPlayers().get(1).getScore(),
                game.getTileBag().getTileCount()));

        // Update status label
        if (game.getCurrentPlayer() == humanPlayer) {
            statusLabel.setText("Your Turn");
            enablePlayerControls(true);
        } else {
            statusLabel.setText("Computer's Turn");
            enablePlayerControls(false);
        }

        // Update play button state
        playButton.setDisable(game.getCurrentPlayer() != humanPlayer || temporaryPlacements.isEmpty());
    }

    /**
     * Enables or disables player controls based on whose turn it is.
     */
    private void enablePlayerControls(boolean enabled) {
        // Enable/disable rack buttons
        for (RackButton button : rackButtons) {
            if (button.getTile() != null) {
                button.setDisable(!enabled || selectedRackIndices.contains(button.getIndex()));
            }
        }

        // Enable/disable control buttons
        directionButton.setDisable(!enabled);
        exchangeButton.setDisable(!enabled || selectedRackIndices.isEmpty());
        passButton.setDisable(!enabled);
    }

    /**
     * Handles a click on a rack button.
     */
    private void handleRackButtonClick(int index) {
        // Toggle selection
        if (selectedRackIndex == index) {
            selectedRackIndex = -1;
        } else {
            selectedRackIndex = index;
        }

        // Update UI
        updateUI();
    }

    /**
     * Handles a click on a board button.
     */
    private void handleBoardButtonClick(int row, int col) {
        // If no rack tile is selected, can't place on board
        if (selectedRackIndex == -1) {
            return;
        }

        // Can't place on a square that already has a permanent tile
        if (game.getBoard().getSquare(row, col).hasTile()) {
            return;
        }

        // Can't place on a square that already has a temporary tile
        if (temporaryPlacements.containsKey(new Point(row, col))) {
            return;
        }

        // Place the selected tile on the board temporarily
        Player player = game.getPlayers().get(0);
        Tile tile = player.getRack().getTile(selectedRackIndex);

        // Store the temporary placement
        temporaryPlacements.put(new Point(row, col), tile);
        selectedRackIndices.add(selectedRackIndex);

        // Reset selection
        selectedRackIndex = -1;

        // Update UI
        updateUI();
    }

    /**
     * Toggles the direction between horizontal and vertical.
     */
    private void toggleDirection() {
        if (currentDirection == Direction.HORIZONTAL) {
            currentDirection = Direction.VERTICAL;
            directionButton.setText("Direction: ↓");
        } else {
            currentDirection = Direction.HORIZONTAL;
            directionButton.setText("Direction: →");
        }
    }

    /**
     * Plays the current temporary word on the board.
     */
    private void playCurrentWord() {
        if (temporaryPlacements.isEmpty()) {
            showAlert("No tiles placed on the board.");
            return;
        }

        // Find the starting position and verify placement is in a line
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;

        for (Point p : temporaryPlacements.keySet()) {
            minRow = Math.min(minRow, p.x);
            maxRow = Math.max(maxRow, p.x);
            minCol = Math.min(minCol, p.y);
            maxCol = Math.max(maxCol, p.y);
        }

        // Check if tiles are in a line
        boolean inLine = (minRow == maxRow) || (minCol == maxCol);
        if (!inLine && temporaryPlacements.size() > 1) {
            showAlert("Tiles must be placed in a straight line.");
            return;
        }

        // Determine direction from placement
        Direction direction;
        if (temporaryPlacements.size() <= 1) {
            // Single tile - use the selected direction
            direction = currentDirection;
        } else {
            // Multiple tiles - infer direction from placement
            direction = (minRow == maxRow) ? Direction.HORIZONTAL : Direction.VERTICAL;
        }

        // Create the move
        Player player = game.getPlayers().get(0);
        Move move = Move.createPlaceMove(player, minRow, minCol, direction);

        // Add tiles to the move (this needs to be in order)
        List<Tile> tilesToPlace = new ArrayList<>();
        if (direction == Direction.HORIZONTAL) {
            // For horizontal words, add tiles in left-to-right order
            for (int col = minCol; col <= maxCol; col++) {
                Point p = new Point(minRow, col);
                if (temporaryPlacements.containsKey(p)) {
                    tilesToPlace.add(temporaryPlacements.get(p));
                }
            }
        } else {
            // For vertical words, add tiles in top-to-bottom order
            for (int row = minRow; row <= maxRow; row++) {
                Point p = new Point(row, minCol);
                if (temporaryPlacements.containsKey(p)) {
                    tilesToPlace.add(temporaryPlacements.get(p));
                }
            }
        }

        move.addTiles(tilesToPlace);

        // Execute the move
        if (game.executeMove(move)) {
            // Move successful
            logMessage("You played: " + move);

            // Clear temporary placements
            temporaryPlacements.clear();
            selectedRackIndices.clear();

            // Update UI
            updateUI();

            // Check if game is over
            if (game.isGameOver()) {
                handleGameOver();
                return;
            }

            // Computer's turn
            handleComputerTurn();
        } else {
            // Move failed
            showAlert("Invalid move. Please try again.");
        }
    }

    /**
     * Exchanges selected tiles.
     */
    private void exchangeTiles() {
        if (selectedRackIndices.isEmpty()) {
            showAlert("Select tiles to exchange first.");
            return;
        }

        // Create list of tiles to exchange
        List<Tile> tilesToExchange = new ArrayList<>();
        for (int index : selectedRackIndices) {
            tilesToExchange.add(game.getPlayers().get(0).getRack().getTile(index));
        }

        // Create exchange move
        Move move = Move.createExchangeMove(game.getPlayers().get(0), tilesToExchange);

        // Execute the move
        if (game.executeMove(move)) {
            // Move successful
            logMessage("You exchanged " + tilesToExchange.size() + " tiles");

            // Clear temporary placements
            temporaryPlacements.clear();
            selectedRackIndices.clear();

            // Update UI
            updateUI();

            // Computer's turn
            handleComputerTurn();
        } else {
            // Move failed
            showAlert("Failed to exchange tiles. Try again.");
        }
    }

    /**
     * Passes the current turn.
     */
    private void passTurn() {
        // Create pass move
        Move move = Move.createPassMove(game.getPlayers().get(0));

        // Execute the move
        if (game.executeMove(move)) {
            // Move successful
            logMessage("You passed your turn");

            // Clear temporary placements
            temporaryPlacements.clear();
            selectedRackIndices.clear();

            // Update UI
            updateUI();

            // Check if game is over
            if (game.isGameOver()) {
                handleGameOver();
                return;
            }

            // Computer's turn
            handleComputerTurn();
        }
    }

    /**
     * Clears all temporary tile placements.
     */
    private void clearTemporaryPlacements() {
        temporaryPlacements.clear();
        selectedRackIndices.clear();
        selectedRackIndex = -1;
        updateUI();
    }

    /**
     * Resets the game.
     */
    private void resetGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to start a new game?",
                ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // Clear temporary state
                temporaryPlacements.clear();
                selectedRackIndices.clear();
                selectedRackIndex = -1;

                // Initialize new game
                initializeGame();

                // Clear log
                gameLog.clear();
                logMessage("Game started. Human plays first.");

                // Update UI
                updateUI();
            }
        });
    }

    /**
     * Force a computer move for debugging.
     */
    private void forceComputerMove() {
        if (game.getCurrentPlayer() != game.getPlayers().get(1)) {
            logMessage("DEBUG: Not computer's turn. Switching turn to computer.");
            game.nextPlayer();  // Force turn to computer
        }

        logMessage("DEBUG: Forcing computer move");
        handleComputerTurn();
    }

    /**
     * Handles the computer's turn.
     */
    private void handleComputerTurn() {
        // Update status
        statusLabel.setText("Computer is thinking...");
        logMessage("Computer is thinking...");

        // Disable all controls during computer turn
        enablePlayerControls(false);

        // Create a task for the computer's move
        Task<Move> task = new Task<>() {
            @Override
            protected Move call() {
                try {
                    // Ensure we're using the correct player reference
                    Player aiPlayer = game.getPlayers().get(1);

                    // Re-create the computer player to ensure proper references
                    ComputerPlayer cp = new ComputerPlayer(aiPlayer);

                    // Generate and return computer's move
                    return cp.generateMove(game);
                } catch (Exception e) {
                    logMessage("Error generating computer move: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
        };

        // Handle task completion
        task.setOnSucceeded(event -> {
            Move move = task.getValue();

            if (move == null) {
                logMessage("Computer couldn't generate a move. Passing.");
                // Create a pass move
                move = Move.createPassMove(game.getPlayers().get(1));
            }

            // Execute the move
            final Move finalMove = move; // Need a final reference for the Platform.runLater
            Platform.runLater(() -> {
                if (game.executeMove(finalMove)) {
                    // Log the move
                    logMessage("Computer played: " + finalMove);

                    // Check if game is over
                    if (game.isGameOver()) {
                        handleGameOver();
                    }
                } else {
                    // Log failure and create pass move instead
                    logMessage("Computer move failed. Passing turn.");
                    Move passMove = Move.createPassMove(game.getPlayers().get(1));
                    if (game.executeMove(passMove)) {
                        logMessage("Computer passed.");
                    }
                }

                // Update UI
                updateUI();
            });
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            logMessage("Error in computer turn: " + (e != null ? e.getMessage() : "Unknown error"));
            if (e != null) e.printStackTrace();

            // Try to recover by passing
            Platform.runLater(() -> {
                try {
                    Move passMove = Move.createPassMove(game.getPlayers().get(1));
                    if (game.executeMove(passMove)) {
                        logMessage("Computer passed (error recovery).");
                    }
                } catch (Exception ex) {
                    logMessage("Fatal error in computer turn. Try restarting the game.");
                }

                updateUI();
            });
        });

        // Start the task
        executor.submit(task);
    }

    /**
     * Handles the end of the game.
     */
    private void handleGameOver() {
        // Find the winner
        Player winner = null;
        int highScore = -1;

        for (Player player : game.getPlayers()) {
            if (player.getScore() > highScore) {
                highScore = player.getScore();
                winner = player;
            }
        }

        // Display results
        StringBuilder resultMessage = new StringBuilder("Game Over!\n\nFinal Scores:\n");
        for (Player player : game.getPlayers()) {
            resultMessage.append(player.getName()).append(": ").append(player.getScore()).append("\n");
        }

        if (winner != null) {
            resultMessage.append("\nWinner: ").append(winner.getName());
        } else {
            resultMessage.append("\nTie game!");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("Game Finished");
        alert.setContentText(resultMessage.toString());
        alert.showAndWait();
    }

    /**
     * Shows an alert dialog.
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }

    /**
     * Adds a message to the game log.
     */
    private void logMessage(String message) {
        Platform.runLater(() -> {
            gameLog.appendText(message + "\n");
            gameLog.positionCaret(gameLog.getText().length());
        });
    }

    @Override
    public void stop() {
        // Shutdown the executor service
        executor.shutdownNow();
    }

    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Custom button class for board squares.
     */
    private static class BoardButton extends Button {
        private final int row;
        private final int col;
        private final Square.SquareType squareType;
        private Tile tile;
        private boolean isTemporary;

        public BoardButton(int row, int col, Square.SquareType squareType) {
            this.row = row;
            this.col = col;
            this.squareType = squareType;
            this.isTemporary = false;

            // Set button properties
            setPrefSize(40, 40);
            setMinSize(40, 40);
            setMaxSize(40, 40);

            // Set initial appearance
            updateAppearance();

            // Add CSS class
            getStyleClass().add("board-button");
        }

        public void setTile(Tile tile, boolean isTemporary) {
            this.tile = tile;
            this.isTemporary = isTemporary;
            updateAppearance();
        }

        public void clearTile() {
            this.tile = null;
            this.isTemporary = false;
            updateAppearance();
        }

        private void updateAppearance() {
            if (tile != null) {
                // Display tile
                setText(createTileText());

                // Set tile style
                getStyleClass().removeAll("tw-square", "dw-square", "tl-square", "dl-square", "center-square", "normal-square");
                getStyleClass().add(isTemporary ? "temp-tile" : "tile");
            } else {
                // Display square type
                setText(getSquareLabel());

                // Set square style
                getStyleClass().removeAll("tile", "temp-tile");

                switch (squareType) {
                    case TRIPLE_WORD:
                        getStyleClass().add("tw-square");
                        break;
                    case DOUBLE_WORD:
                        getStyleClass().add("dw-square");
                        break;
                    case TRIPLE_LETTER:
                        getStyleClass().add("tl-square");
                        break;
                    case DOUBLE_LETTER:
                        getStyleClass().add("dl-square");
                        break;
                    case CENTER:
                        getStyleClass().add("center-square");
                        break;
                    default:
                        getStyleClass().add("normal-square");
                        break;
                }
            }
        }

        private String createTileText() {
            if (tile != null) {
                return tile.getLetter() + (tile.getValue() > 0 ? "\n" + tile.getValue() : "");
            }
            return "";
        }

        private String getSquareLabel() {
            switch (squareType) {
                case TRIPLE_WORD:
                    return "TW";
                case DOUBLE_WORD:
                    return "DW";
                case TRIPLE_LETTER:
                    return "TL";
                case DOUBLE_LETTER:
                    return "DL";
                case CENTER:
                    return "★";
                default:
                    return "";
            }
        }
    }

    /**
     * Custom button class for rack tiles.
     */
    private static class RackButton extends Button {
        private final int index;
        private Tile tile;
        private boolean isSelected;

        public RackButton(int index) {
            this.index = index;
            this.isSelected = false;

            // Set button properties
            setPrefSize(45, 45);
            setMinSize(45, 45);
            setMaxSize(45, 45);

            // Set initial appearance
            updateAppearance();

            // Add CSS class
            getStyleClass().add("rack-button");
        }

        public int getIndex() {
            return index;
        }

        public Tile getTile() {
            return tile;
        }

        public void setTile(Tile tile) {
            this.tile = tile;
            setDisable(tile == null);
            updateAppearance();
        }

        public void clearTile() {
            this.tile = null;
            setDisable(true);
            updateAppearance();
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
            updateAppearance();
        }

        private void updateAppearance() {
            if (tile != null) {
                // Display tile
                setText(createTileText());

                // Set tile style
                getStyleClass().removeAll("empty-rack");
                if (isSelected) {
                    getStyleClass().add("selected-tile");
                } else {
                    getStyleClass().removeAll("selected-tile");
                    getStyleClass().add("rack-tile");
                }
            } else {
                // Empty rack slot
                setText("");
                getStyleClass().removeAll("rack-tile", "selected-tile");
                getStyleClass().add("empty-rack");
            }
        }

        private String createTileText() {
            if (tile != null) {
                return tile.getLetter() + (tile.getValue() > 0 ? "\n" + tile.getValue() : "");
            }
            return "";
        }
    }

    /**
     * Represents a point on the board (row, column).
     */
    private static class Point {
        public final int x;
        public final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}