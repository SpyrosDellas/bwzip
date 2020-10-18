import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PriorityQueue;

/**
 * The {@code Huffman} class provides static methods for compressing to
 * and expanding from a file using Huffman compression.
 *
 * @author Spyros Dellas
 */
public class Huffman {

    private static final int RADIX = 256;

    /*
     * Inner private class defining the Huffman code trie
     *
     * Note: This class has a natural ordering that is inconsistent with equals
     */
    private static class Node implements Comparable<Node> {

        private final int letter;     // unused for internal nodes
        private final int frequency;  // used for the trie construction during the compression phase
        private Node left;
        private Node right;

        public Node(int letter, int frequency, Node left, Node right) {
            this.letter = letter;
            this.frequency = frequency;
            this.left = left;
            this.right = right;
        }

        public boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(Node that) {
            return this.frequency - that.frequency;
        }
    }

    /**
     * Do not instantiate
     */
    private Huffman() {

    }

    /**
     * Compress the given byte-encoded input using Huffman encoding;
     *
     * @param text       A {@code byte[]} array to be compressed
     * @param outputFile The compressed {@code File}
     * @throws IOException If an I/O error occurs writing to the file
     */
    public static void compress(byte[] text, File outputFile) throws IOException {

        try (BinaryOut out = new BinaryOut(outputFile)) {
            int length = text.length;
            if (length == 0) {
                return;
            }
            // Build the trie
            Node root = buildTrie(text);
            // Build the lookup table to speed-up compression
            boolean[][] codes = buildCode(root);
            // Write the trie
            writeTrie(root, out);
            // Write the number of characters
            out.writeInt(length);
            // Write the compressed data
            for (byte c : text) {
                out.writeBitArray(codes[c & 0xff]);
            }
        }
    }

    /*
     * Build the Huffman code trie
     */
    private static Node buildTrie(byte[] text) {
        // Compute the frequency counts for each byte
        int[] frequencies = new int[RADIX];
        for (byte c : text) {
            frequencies[c & 0xff]++;
        }
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>();
        for (int index = 0; index < RADIX; index++) {
            if (frequencies[index] > 0) {
                priorityQueue.add(new Node(index, frequencies[index], null, null));
            }
        }
        while (priorityQueue.size() > 1) {
            Node left = priorityQueue.poll();
            Node right = priorityQueue.poll();
            Node parent = new Node(0, left.frequency + right.frequency, left, right);
            priorityQueue.add(parent);
        }
        return priorityQueue.poll();
    }

    /*
     * Make a lookup table from the trie for efficiency during compression
     *
     * The lookup table is implemented as a character-indexed array that
     * associates a boolean[] array representing the Huffman code with each character.
     * */
    private static boolean[][] buildCode(Node root) {
        boolean[][] code = new boolean[RADIX][];
        buildCode(root, code, "");
        return code;
    }

    private static void buildCode(Node x, boolean[][] code, String prefix) {
        if (x.isLeaf()) {
            int index = x.letter;
            code[index] = new boolean[prefix.length()];
            for (int i = 0; i < prefix.length(); i++) {
                code[index][i] = (prefix.charAt(i) == '1');
            }
        } else {
            buildCode(x.left, code, prefix + "0");
            buildCode(x.right, code, prefix + "1");
        }
    }

    /*
     * Traverse the trie in preorder:
     * - When we visit an internal node, we write a single 0 bit;
     * - When we visit a leaf, we write a single 1 bit followed by the 8-bit character in the leaf
     */
    private static void writeTrie(Node x, BinaryOut out) throws IOException {
        if (x.isLeaf()) {
            out.writeBitArray(true);
            out.write(x.letter);
            return;
        } else {
            out.writeBitArray(false);
        }
        writeTrie(x.left, out);
        writeTrie(x.right, out);
    }

    /**
     * Expand the given Huffman-encoded compressed file.
     *
     * @param inputFile The compressed {@code File} to be expanded
     * @throws IOException If an I/O error occurs reading from the file
     */
    public static byte[] expand(File inputFile) throws IOException {
        try (BinaryIn in = new BinaryIn(inputFile)) {
            // Read the trie stored in the input file
            Node root = readTrie(in);
            // Read the number of byte-characters encoded
            int length = in.readInt();
            // Expand the file
            byte[] text = new byte[length];
            for (int i = 0; i < length; i++) {
                Node current = root;
                while (!current.isLeaf()) {
                    boolean bit = in.readBoolean();
                    if (bit)
                        current = current.right;
                    else
                        current = current.left;
                }
                text[i] = (byte) current.letter;
            }
            return text;
        }
    }

    private static Node readTrie(BinaryIn in) throws IOException {
        boolean bit = in.readBoolean();
        if (bit) {
            return new Node(in.readByte(), 0, null, null);
        } else {
            Node parent = new Node(0, 0, null, null);
            parent.left = readTrie(in);
            parent.right = readTrie(in);
            return parent;
        }
    }

    private static void test(String fileName) {
        try {
            byte[] text = Files.readAllBytes(Path.of(fileName));
            File outputFile = new File(fileName + ".huf");
            compress(text, outputFile);
            byte[] decoded = expand(outputFile);

            for (int i = 0; i < text.length; i++) {
                if (text[i] != decoded[i]) {
                    System.err.println("Tests failed..");
                    return;
                }
            }
            System.out.println("Tests passed for " + fileName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /*
     *  Unit testing
     */
    public static void main(String[] args) {
        String fileName = "./data/abra.txt";
        test(fileName);
        fileName = "./data/aesop.txt";
        test(fileName);
        fileName = "./data/amendments.txt";
        test(fileName);
        fileName = "./data/rand10K.bin";
        test(fileName);
        fileName = "./data/purple.gif";
        test(fileName);
        fileName = "./data/etext99.txt";
        test(fileName);
    }
}
