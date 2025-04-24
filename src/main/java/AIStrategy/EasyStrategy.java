package AIStrategy;

import model.Move;

import java.util.List;

/**
 * Easy difficulty level AI strategy.
 * Characteristics:
 * - Makes completely random moves from all possible moves
 * - Exchanges up to 4 tiles when stuck
 * - Focuses on immediate points only
 */
public class EasyStrategy extends BaseStrategy {
    @Override
    public String getName() {
        return "Easy AI";
    }

    @Override
    public int getDifficultyLevel() {
        return 1;
    }

    /**
     * Selects a completely random move from all possible moves.
     * Does not favor higher scoring moves.
     */
    @Override
    protected Move selectMove(List<Move> possibleMoves) {
        if (possibleMoves.isEmpty()) {
            throw new IllegalArgumentException("No possible moves to select from");
        }

        // Pick a completely random move regardless of score
        return possibleMoves.get(random.nextInt(possibleMoves.size()));
    }

    /**
     * Determines how many tiles to exchange for Easy level.
     * Easy level is more liberal with exchanges, up to 4 tiles.
     */
    @Override
    protected int determineExchangeCount(int availableTileCount) {
        // Easy difficulty is more likely to exchange more tiles
        return Math.min(4, availableTileCount);
    }

    /**
     * Overridden to limit the number of potential moves checked.
     * This makes the AI "miss" good opportunities, simulating a novice player.
     */
    @Override
    protected List<Move> findPossibleMoves(model.Game game, model.Player player) {
        List<Move> allMoves = super.findPossibleMoves(game, player);

        // Easy AI only considers a maximum of 10 moves to simulate limited thinking
        if (allMoves.size() > 10) {
            return allMoves.subList(0, 10);
        }

        return allMoves;
    }
}