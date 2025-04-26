package model;

import util.ScrabbleConstants;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * GADDAG data structure for Scrabble word validation and word finding.
 * Enhanced with improved word-finding capabilities for AI strategies.
 */
public class Gaddag {
    private static final char DELIMITER = '+';
    private final Node root;
    private final Set<String> wordSet; // For simple lookups
    private final Map<Character, ArcLetter> alphabetMap;

    /**
     * Default constructor.
     */
    public Gaddag() throws IOException {
        this.root = new Node();
        this.wordSet = new HashSet<>();
        this.alphabetMap = new HashMap<>();

        try {
            loadFromFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loaded dictionary with " + wordSet.size() + " words");
    }

    /**
     * Loads words from a file.
     *
     * @throws IOException if file cannot be read
     */
    private void loadFromFile() throws IOException {
        FileReader dictionary = new FileReader("src/main/resources/Dictionary.txt");
        try (BufferedReader reader = new BufferedReader(dictionary)) {
            String line;
            while ((line = reader.readLine()) != null) {
                addWord(line);
            }
        }
    }


    /**
     * Adds a word to both the wordSet and GADDAG structure.
     *
     * @param word Word to add
     */
    public void addWord(String word) {
        word = word.trim().toUpperCase();
        if (word.isEmpty() || !word.matches("[A-Z]+")) {
            return;
        }
        wordSet.add(word);
        insert(word);
    }

    /**
     * Inserts a word into the GADDAG structure.
     *
     * @param word Word to insert
     */
    public void insert(String word) {
        word = word.toUpperCase();

        if (word.length() < 2) {
            return;
        }

        // For each letter position in the word
        for (int i = 0; i < word.length(); i++) {
            StringBuilder sequence = new StringBuilder();

            // Reverse the letters before this position
            for (int j = i; j > 0; j--) {
                sequence.append(word.charAt(j - 1));
            }

            // Add delimiter
            sequence.append(DELIMITER);

            // Add the remaining letters
            sequence.append(word.substring(i));

            // Insert this sequence
            insertSequence(sequence.toString());
        }

        // Insert the sequence with just the delimiter at the start
        insertSequence(DELIMITER + word);
    }

    /**
     * Inserts a sequence into the GADDAG.
     *
     * @param sequence Sequence to insert
     */
    private void insertSequence(String sequence) {
        Node current = root;

        for (int i = 0; i < sequence.length(); i++) {
            char c = sequence.charAt(i);
            current = current.getOrCreateChild(getArcLetter(c));
        }

        current.setWord(true);
    }

    /**
     * Gets or creates an ArcLetter for the given character.
     *
     * @param c Character to get ArcLetter for
     * @return ArcLetter for the character
     */
    private ArcLetter getArcLetter(char c) {
        if (c == DELIMITER) {
            return new Delimiter();
        }

        ArcLetter arcLetter = alphabetMap.get(c);
        if (arcLetter == null) {
            arcLetter = new ArcLetter(c);
            alphabetMap.put(c, arcLetter);
        }
        return arcLetter;
    }

    /**
     * Simple API for word validation.
     *
     * @param word Word to validate
     * @return true if word is valid
     */
    public boolean isValidWord(String word) {
        if (word == null || word.isEmpty()) {
            return false;
        }
        word = word.trim().toUpperCase();
        return wordSet.contains(word);
    }

    /**
     * Gets words that can be formed from a rack given an anchor and direction constraints.
     * This is used by the AI to find potential moves.
     *
     * @param rack Letters available in the rack
     * @param anchor Anchor character
     * @param allowLeft Whether words can extend left of the anchor
     * @param allowRight Whether words can extend right of the anchor
     * @return Set of possible words
     */
    public Set<String> getWordsFrom(String rack, char anchor, boolean allowLeft, boolean allowRight) {
        Set<String> words = new HashSet<>();
        StringBuilder currentWord = new StringBuilder();
        currentWord.append(anchor);

        Map<Character, Integer> rackMap = new HashMap<>();
        for (char c : rack.toUpperCase().toCharArray()) {
            rackMap.put(c, rackMap.getOrDefault(c, 0) + 1);
        }

        Node startNode = null;
        for (Map.Entry<ArcLetter, Node> entry : root.getChildren().entrySet()) {
            if (entry.getKey().getChar() == anchor) {
                startNode = entry.getValue();
                break;
            }
        }

        if (startNode == null) {
            return words;
        }

        dfs(startNode, currentWord, rackMap, words, allowLeft, allowRight, false);

        return words;
    }

    /**
     * Find all possible words that can be formed from the given rack.
     * This is an enhanced method specifically for the AI to efficiently
     * find all possible words without needing to try every letter combination.
     *
     * @param rack Letters available in the rack
     * @return Set of possible words that can be formed
     */
    public Set<String> getAllPossibleWords(String rack) {
        Set<String> result = new HashSet<>();

        // Try each letter as an anchor
        for (char letter : rack.toCharArray()) {
            // Try to find words with this letter in any position
            Set<String> wordsWithLetter = getWordsFrom(rack, letter, true, true);
            result.addAll(wordsWithLetter);
        }

        // Also try with blank tiles (if any)
        if (rack.contains("*")) {
            for (char letter = 'A'; letter <= 'Z'; letter++) {
                // Replace one blank with this letter and try
                String modifiedRack = rack.replaceFirst("\\*", String.valueOf(letter));
                Set<String> wordsWithBlank = getWordsFrom(modifiedRack, letter, true, true);
                result.addAll(wordsWithBlank);
            }
        }

        return result;
    }

    /**
     * Depth-first search to find words.
     */
    private void dfs(Node node, StringBuilder currentWord, Map<Character, Integer> rack,
                     Set<String> words, boolean allowLeft, boolean allowRight, boolean passedDelimiter) {

        if (node.isWord() && passedDelimiter) {
            words.add(currentWord.toString());
        }

        for (Map.Entry<ArcLetter, Node> entry : node.getChildren().entrySet()) {
            ArcLetter arcLetter = entry.getKey();
            Node child = entry.getValue();
            char c = arcLetter.getChar();

            if (c == DELIMITER) {
                if (allowLeft) {
                    dfs(child, currentWord, rack, words, allowLeft, allowRight, true);
                }
            } else if (!passedDelimiter && allowLeft) {
                if (rack.getOrDefault(c, 0) > 0) {
                    rack.put(c, rack.get(c) - 1);
                    currentWord.insert(0, c);
                    dfs(child, currentWord, rack, words, allowLeft, allowRight, passedDelimiter);
                    currentWord.deleteCharAt(0);
                    rack.put(c, rack.get(c) + 1);
                }
            } else if (passedDelimiter && allowRight) {
                if (rack.getOrDefault(c, 0) > 0) {
                    rack.put(c, rack.get(c) - 1);
                    currentWord.append(c);
                    dfs(child, currentWord, rack, words, allowLeft, allowRight, passedDelimiter);
                    currentWord.deleteCharAt(currentWord.length() - 1);
                    rack.put(c, rack.get(c) + 1);
                }
            }
        }
    }

    /**
     * Finds words that can start with or contain the given prefix.
     * Useful for finding words that can be played at specific board positions.
     *
     * @param prefix Letters already on the board
     * @param rack Available letters in the rack
     * @param allowExtendLeft Whether to allow extending to the left of prefix
     * @param allowExtendRight Whether to allow extending to the right of prefix
     * @return Set of valid words that can be formed
     */
    public Set<String> getWordsWithPrefix(String prefix, String rack,
                                          boolean allowExtendLeft, boolean allowExtendRight) {
        Set<String> words = new HashSet<>();
        prefix = prefix.toUpperCase();
        rack = rack.toUpperCase();

        // Convert rack to a letter count map
        Map<Character, Integer> rackMap = new HashMap<>();
        for (char c : rack.toCharArray()) {
            rackMap.put(c, rackMap.getOrDefault(c, 0) + 1);
        }

        // If prefix is empty, just find all possible words
        if (prefix.isEmpty()) {
            for (char letter : rack.toCharArray()) {
                StringBuilder currentWord = new StringBuilder();
                currentWord.append(letter);

                // Remove the letter from available tiles
                rackMap.put(letter, rackMap.get(letter) - 1);

                // Find all nodes with this letter
                for (Map.Entry<ArcLetter, Node> entry : root.getChildren().entrySet()) {
                    if (entry.getKey().getChar() == letter) {
                        dfs(entry.getValue(), currentWord, rackMap, words, allowExtendLeft, allowExtendRight, false);
                        break;
                    }
                }

                // Put the letter back
                rackMap.put(letter, rackMap.getOrDefault(letter, 0) + 1);
            }
            return words;
        }

        // Otherwise, find words containing or starting with prefix
        Node current = root;

        // Handle left extension first (if allowed)
        if (allowExtendLeft) {
            // Start with delimiter + prefix
            String path = DELIMITER + prefix;

            // Navigate to the node representing this path
            boolean pathFound = true;
            for (char c : path.toCharArray()) {
                ArcLetter arcLetter = c == DELIMITER ? new Delimiter() : getArcLetter(c);
                Node next = null;
                for (Map.Entry<ArcLetter, Node> entry : current.getChildren().entrySet()) {
                    if (entry.getKey().getChar() == c) {
                        next = entry.getValue();
                        break;
                    }
                }

                if (next == null) {
                    pathFound = false;
                    break;
                }
                current = next;
            }

            // If found, do DFS from this node
            if (pathFound) {
                StringBuilder currentWord = new StringBuilder(prefix);
                dfs(current, currentWord, rackMap, words, false, allowExtendRight, true);
            }
        }

        // Handle right extension
        if (allowExtendRight) {
            // For right extension, we need to find all paths that could form our prefix
            for (int i = 0; i <= prefix.length(); i++) {
                String reversed = new StringBuilder(prefix.substring(0, i)).reverse().toString();
                String forward = prefix.substring(i);
                String path = reversed + DELIMITER + forward;

                current = root;
                boolean pathFound = true;

                // Navigate to the path
                for (char c : path.toCharArray()) {
                    ArcLetter arcLetter = c == DELIMITER ? new Delimiter() : getArcLetter(c);
                    Node next = null;
                    for (Map.Entry<ArcLetter, Node> entry : current.getChildren().entrySet()) {
                        if (entry.getKey().getChar() == c) {
                            next = entry.getValue();
                            break;
                        }
                    }

                    if (next == null) {
                        pathFound = false;
                        break;
                    }
                    current = next;
                }

                // If found, do DFS
                if (pathFound) {
                    StringBuilder currentWord = new StringBuilder(prefix);
                    dfs(current, currentWord, rackMap, words, false, true, forward.isEmpty());
                }
            }
        }

        return words;
    }

    /**
     * Node class for the GADDAG trie structure.
     */
    private static class Node {
        private final Map<ArcLetter, Node> children;
        private boolean isWord;

        public Node() {
            this.children = new HashMap<>();
            this.isWord = false;
        }

        public Map<ArcLetter, Node> getChildren() {
            return children;
        }

        public Node getChild(ArcLetter letter) {
            return children.get(letter);
        }

        public Node getOrCreateChild(ArcLetter letter) {
            return children.computeIfAbsent(letter, k -> new Node());
        }

        public boolean isWord() {
            return isWord;
        }

        public void setWord(boolean isWord) {
            this.isWord = isWord;
        }
    }

    /**
     * ArcLetter class for the GADDAG.
     */
    private static class ArcLetter implements Comparable<ArcLetter> {
        private final char c;

        public ArcLetter(char c) {
            this.c = c;
        }

        public char getChar() {
            return c;
        }

        @Override
        public int compareTo(ArcLetter other) {
            if (other instanceof Delimiter) {
                return -1;
            }
            return Character.compare(this.c, other.c);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ArcLetter arcLetter = (ArcLetter) obj;
            return c == arcLetter.c;
        }

        @Override
        public int hashCode() {
            return Character.hashCode(c);
        }
    }

    /**
     * Delimiter class for the GADDAG.
     */
    private static class Delimiter extends ArcLetter {
        public Delimiter() {
            super(DELIMITER);
        }

        @Override
        public int compareTo(ArcLetter other) {
            if (other instanceof Delimiter) {
                return 0;
            }
            return 1;
        }
    }
}