package model;

import util.ScrabbleLetter;


import java.util.*;

public class TileBag {
    private final List<Tile> tiles;
    private final Random random;

    public TileBag() {
        this.tiles = new ArrayList<>();
        this.random = new Random();
        initialiseTiles();
        shuffle();
    }

    private void initialiseTiles() {
        for (ScrabbleLetter letter : ScrabbleLetter.values()) {
            for (int i = 0; i < letter.getCount(); i++) {
                tiles.add(new Tile(letter.getCharacter(), letter.getValue()));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(tiles, random);
    }

    public void returnTiles(List<Tile> tilesToReturn) {
        if (tilesToReturn == null || tilesToReturn.isEmpty()) {
            System.out.println("WARNING: No tiles to return to bag");
            return;
        }

        int beforeCount = tiles.size();
        this.tiles.addAll(tilesToReturn);
        int afterCount = tiles.size();
        System.out.println("TileBag: Added " + (afterCount - beforeCount) + " tiles to bag");
        shuffle();
    }

    public List<Tile> drawTiles(int count) {
        List<Tile> drawnTiles = new ArrayList<>();

        if (count <= 0) {
            System.out.println("WARNING: Tried to draw " + count + " tiles");
            return drawnTiles;
        }

        System.out.println("TileBag: Attempting to draw " + count + " tiles. Available: " + tiles.size());

        int tilesToDraw = Math.min(count, tiles.size());
        for (int i = 0; i < tilesToDraw; i++) {
            Tile tile = drawTile();
            if (tile != null) {
                drawnTiles.add(tile);
            }
        }

        System.out.println("TileBag: Drew " + drawnTiles.size() + " tiles. Remaining: " + tiles.size());
        return drawnTiles;
    }

    public Tile drawTile() {
        if (tiles.isEmpty()) {
            System.out.println("WARNING: Tile bag is empty");
            return null;
        }
        return tiles.remove(tiles.size() - 1);
    }

    public int getTileCount() {
        return tiles.size();
    }

    public boolean isEmpty() {
        return tiles.isEmpty();
    }

    public static int getPointValue(char letter) {
        ScrabbleLetter scrabbleLetter = ScrabbleLetter.fromChar(letter);
        return scrabbleLetter != null ? scrabbleLetter.getValue() : 0;
    }
}