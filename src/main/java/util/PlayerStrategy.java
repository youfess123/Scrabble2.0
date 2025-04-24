package util;

import model.Board;
import model.Game;
import model.Gaddag;
import model.Move;
import model.Player;

/**
 * Interface for different AI player strategies.
 * This allows for implementing various difficulty levels with different algorithms.
 */
public interface PlayerStrategy {
    /**
     * Generates a move based on the current game state.
     *
     * @param game the current game
     * @param player the player to generate a move for
     * @return the generated move
     */
    Move generateMove(Game game, Player player);

    /**
     * Gets the name of this strategy.
     *
     * @return a descriptive name for the strategy
     */
    String getName();

    /**
     * Gets the difficulty level of this strategy.
     *
     * @return the difficulty level (1-3, where 3 is hardest)
     */
    int getDifficultyLevel();
}