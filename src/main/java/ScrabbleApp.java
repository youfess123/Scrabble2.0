import AIStrategy.ComputerPlayer;
import model.*;
import util.Direction;

import java.io.IOException;
import java.util.*;


/**
 * Main application class to run the Scrabble game in console mode.
 * This is a simple test harness for the game logic.
 */
public class ScrabbleApp {
    private static Game game;
    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);

        try {
            // Ask if bidirectional words should be allowed
            System.out.println("Welcome to Scrabble 2.0!");
            System.out.println("Would you like to allow bidirectional words? (y/n)");
            boolean allowBidirectional = scanner.nextLine().trim().toLowerCase().startsWith("y");

            // Create the game
            game = new Game(allowBidirectional);

            // Set up players
            setupPlayers();

            // Start the game
            game.start();

            // Main game loop
            while (!game.isGameOver()) {
                // Print game state
                printGameState();

                // Handle player's turn
                Player currentPlayer = game.getCurrentPlayer();

                if (currentPlayer.isComputer()) {
                    handleComputerTurn(currentPlayer);
                } else {
                    handleHumanTurn(currentPlayer);
                }
            }

            // Game over
            printGameResults();

        } catch (IOException e) {
            System.err.println("Error starting game: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    /**
     * Set up players for the game.
     */
    private static void setupPlayers() {
        System.out.println("How many human players? (1-3)");
        int humanPlayers = readInt(1, 3);

        for (int i = 1; i <= humanPlayers; i++) {
            System.out.println("Enter name for player " + i + ":");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) name = "Player " + i;
            game.addPlayer(new Player(name));
        }

        // Add a computer player
        System.out.println("Adding a computer player (uses optimized GADDAG-based strategy)");
        Player computerPlayer = new Player("Computer", true);
        game.addPlayer(computerPlayer);

        // Create the AI controller
        new ComputerPlayer(computerPlayer);
    }

    /**
     * Print the current game state.
     */
    private static void printGameState() {
        System.out.println("\n===================================");
        System.out.println("CURRENT PLAYER: " + game.getCurrentPlayer().getName());
        System.out.println("Tiles in bag: " + game.getTileBag().getTileCount());

        // Print scores
        System.out.println("\nSCORES:");
        for (Player player : game.getPlayers()) {
            System.out.println(player.getName() + ": " + player.getScore());
        }

        // Print board
        System.out.println("\nBOARD:");
        System.out.println(game.getBoard());

        // Print rack for human player
        if (!game.getCurrentPlayer().isComputer()) {
            System.out.println("\nYOUR RACK:");
            System.out.println(game.getCurrentPlayer().getRack());
        }

        System.out.println("===================================\n");
    }

    /**
     * Handle a human player's turn.
     */
    private static void handleHumanTurn(Player player) {
        System.out.println("Your turn, " + player.getName() + "!");
        System.out.println("Choose an action:");
        System.out.println("1 - Place tiles");
        System.out.println("2 - Exchange tiles");
        System.out.println("3 - Pass");

        int choice = readInt(1, 3);

        switch (choice) {
            case 1:
                handlePlaceTiles(player);
                break;
            case 2:
                handleExchangeTiles(player);
                break;
            case 3:
                handlePass(player);
                break;
        }
    }

    /**
     * Handle placing tiles on the board.
     */
    private static void handlePlaceTiles(Player player) {
        System.out.println("Enter row, column, direction (H/V/HR/VR) and word to place");
        System.out.println("Example: 7 7 H WORD");
        System.out.println("Direction can be H (horizontal), V (vertical), HR (horizontal reverse), VR (vertical reverse)");

        String input = scanner.nextLine().trim();
        String[] parts = input.split("\\s+", 4);

        if (parts.length < 4) {
            System.out.println("Invalid input. Please try again.");
            handlePlaceTiles(player);
            return;
        }

        try {
            int row = Integer.parseInt(parts[0]) - 1; // Adjust for 0-based indexing
            int col = Integer.parseInt(parts[1]) - 1; // Adjust for 0-based indexing

            Direction direction;
            switch (parts[2].toUpperCase()) {
                case "H":
                    direction = Direction.HORIZONTAL;
                    break;
                case "V":
                    direction = Direction.VERTICAL;
                    break;
                case "HR":
                    direction = Direction.HORIZONTAL_REVERSE;
                    break;
                case "VR":
                    direction = Direction.VERTICAL_REVERSE;
                    break;
                default:
                    System.out.println("Invalid direction. Please use H, V, HR, or VR.");
                    handlePlaceTiles(player);
                    return;
            }

            String word = parts[3].toUpperCase();

            // Create move
            Move move = Move.createPlaceMove(player, row, col, direction);

            // Get tiles from rack
            List<Tile> tilesForWord = getTilesForWord(word, player.getRack().getTiles());
            if (tilesForWord == null) {
                System.out.println("You don't have the tiles to form this word.");
                handleHumanTurn(player);
                return;
            }

            move.addTiles(tilesForWord);

            // Execute move
            boolean success = game.executeMove(move);
            if (!success) {
                System.out.println("Invalid move. Please try again.");
                handleHumanTurn(player);
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid row or column. Please enter numbers.");
            handlePlaceTiles(player);
        }
    }

    /**
     * Get tiles needed to form a word.
     */
    private static List<Tile> getTilesForWord(String word, List<Tile> rackTiles) {
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
     * Handle exchanging tiles.
     */
    private static void handleExchangeTiles(Player player) {
        System.out.println("Enter the letters you want to exchange (e.g., ABC):");
        String lettersToExchange = scanner.nextLine().trim().toUpperCase();

        if (lettersToExchange.isEmpty()) {
            System.out.println("No tiles selected. Try again.");
            handleExchangeTiles(player);
            return;
        }

        List<Tile> tilesToExchange = new ArrayList<>();

        for (char c : lettersToExchange.toCharArray()) {
            boolean found = false;

            for (Tile tile : player.getRack().getTiles()) {
                if (tile.getLetter() == c && !tilesToExchange.contains(tile)) {
                    tilesToExchange.add(tile);
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("You don't have the tile: " + c);
                handleExchangeTiles(player);
                return;
            }
        }

        Move move = Move.createExchangeMove(player, tilesToExchange);
        boolean success = game.executeMove(move);

        if (!success) {
            System.out.println("Exchange failed. Try again.");
            handleHumanTurn(player);
        }
    }

    /**
     * Handle passing a turn.
     */
    private static void handlePass(Player player) {
        Move move = Move.createPassMove(player);
        game.executeMove(move);
        System.out.println(player.getName() + " passed their turn.");
    }

    /**
     * Handle a computer player's turn.
     */
    private static void handleComputerTurn(Player player) {
        System.out.println("Computer is thinking...");

        // Find the computer player's controller
        for (Player p : game.getPlayers()) {
            if (p.isComputer()) {
                ComputerPlayer computerPlayer = new ComputerPlayer(p);
                Move move = computerPlayer.generateMove(game);
                boolean success = game.executeMove(move);

                if (success) {
                    System.out.println("Computer played: " + move);
                } else {
                    System.out.println("Computer failed to make a move. Passing.");
                    handlePass(player);
                }

                // Pause to let the player see the move
                System.out.println("Press Enter to continue...");
                scanner.nextLine();
                break;
            }
        }
    }

    /**
     * Print game results at the end.
     */
    private static void printGameResults() {
        System.out.println("\n===== GAME OVER =====");
        System.out.println("Final scores:");

        // Find the winner(s)
        int highScore = -1;
        List<Player> winners = new ArrayList<>();

        for (Player player : game.getPlayers()) {
            int score = player.getScore();
            System.out.println(player.getName() + ": " + score);

            if (score > highScore) {
                highScore = score;
                winners.clear();
                winners.add(player);
            } else if (score == highScore) {
                winners.add(player);
            }
        }

        // Print winner(s)
        System.out.println("\nWinner" + (winners.size() > 1 ? "s" : "") + ":");
        for (Player winner : winners) {
            System.out.println("🏆 " + winner.getName() + " (" + winner.getScore() + " points)");
        }
    }

    /**
     * Read an integer within a range from the console.
     */
    private static int readInt(int min, int max) {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                int value = Integer.parseInt(input);

                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println("Please enter a number between " + min + " and " + max);
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number");
            }
        }
    }
}