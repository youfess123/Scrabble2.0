package AIStrategy;

import model.Game;
import model.Move;
import model.Player;
import util.PlayerStrategy;

/**
 * Represents a computer-controlled player in Scrabble.
 * Uses the optimized GADDAG-based strategy.
 */
public class ComputerPlayer {
    private final Player player;
    private PlayerStrategy strategy;

    /**
     * Creates a new computer player with the specified player.
     *
     * @param player the player to control
     */
    public ComputerPlayer(Player player) {
        this.player = player;
        player.setComputer(true);
        this.strategy = new OptimizedStrategy();
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