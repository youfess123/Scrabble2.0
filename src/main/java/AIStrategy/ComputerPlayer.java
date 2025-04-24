package AIStrategy;

import model.Game;
import model.Move;
import model.Player;
import util.PlayerStrategy;

/**
 * Represents a computer-controlled player in Scrabble.
 * Uses different strategies based on difficulty level.
 */
public class ComputerPlayer {
    private final Player player;
    private PlayerStrategy strategy;

    /**
     * Creates a new computer player with the specified player and difficulty level.
     *
     * @param player the player to control
     * @param difficultyLevel the difficulty level (1=Easy, 2=Medium, 3=Hard)
     */
    public ComputerPlayer(Player player, int difficultyLevel) {
        this.player = player;
        player.setComputer(true);
        setDifficultyLevel(difficultyLevel);
    }

    /**
     * Sets the difficulty level for this computer player.
     *
     * @param difficultyLevel the difficulty level (1=Easy, 2=Medium, 3=Hard)
     */
    public void setDifficultyLevel(int difficultyLevel) {
        // Ensure difficulty is within valid range
        int level = Math.max(1, Math.min(3, difficultyLevel));

        // Create the appropriate strategy based on difficulty level
        switch (level) {
            case 1:
                this.strategy = new EasyStrategy();
                break;
            case 2:
                this.strategy = new MediumStrategy();
                break;
            case 3:
                this.strategy = new HardStrategy();
                break;
            default:
                this.strategy = new MediumStrategy();
        }

        System.out.println("Computer player using strategy: " + strategy.getName());
    }

    /**
     * Gets the player controlled by this computer player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the strategy used by this computer player.
     *
     * @return the strategy
     */
    public PlayerStrategy getStrategy() {
        return strategy;
    }

    /**
     * Generates a move for the computer player based on the current game state.
     *
     * @param game the current game
     * @return the generated move
     */
    public Move generateMove(Game game) {
        try {
            System.out.println("Computer player generating move using " + strategy.getName());

            if (player.getRack().size() == 0) {
                System.out.println("Computer has no tiles, passing");
                return Move.createPassMove(player);
            }

            // Delegate to the strategy
            return strategy.generateMove(game, player);

        } catch (Exception e) {
            System.err.println("Error generating computer move: " + e.getMessage());
            e.printStackTrace();
            return Move.createPassMove(player);
        }
    }
}