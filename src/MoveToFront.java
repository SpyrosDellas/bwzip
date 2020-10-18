import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The main idea of move-to-front encoding is to maintain an ordered sequence of
 * the characters in the alphabet by repeatedly reading a character from the
 * input message; saving the position in the sequence in which that character
 * appears; and moving that character to the front of the sequence.
 * <p>
 * If equal characters occur near one another other many times in the input,
 * then many of the output values will be small integers (such as 0, 1, and 2).
 * The resulting high frequency of certain characters (0s, 1s, and 2s) provides
 * exactly the kind of input for which Huffman coding achieves favorable
 * compression ratios.
 * <p>
 * Performance:
 * The running time of both move-to-front encoding and decoding is proportional
 * to n*R (or better) in the worst case and proportional to n+R (or better) on
 * inputs that arise when compressing typical English text, where n is the
 * number of characters in the input and R is the alphabet size.
 * The amount of memory used by both move-to-front encoding and decoding is
 * proportional to n + R (or better) in the worst case.
 *
 * @author Spyros Dellas
 */
public class MoveToFront {

    private static final int RADIX = 256;

    /**
     * Do not instantiate
     */
    private MoveToFront() {
    }

    /**
     * Apply move-to-front encoding.
     * <p>
     */
    public static byte[] encode(byte[] text) {
        /*
         * We maintain an ordered sequence of the 256 values a byte can take.
         * - First we initialize the sequence by making the ith character in the sequence equal
         *   to i.
         * - Now, we read each 8-bit character from the input one at a time and output the 8-bit index
         *   in the sequence were the character appears.
         * - Finally we move the character to the front.
         */
        byte[] moveToFrontOrder = initializeMoveToFrontOrder();
        byte[] encoded = new byte[text.length];
        int counter = 0;
        for (byte character : text) {
            int position = position(character, moveToFrontOrder);
            encoded[counter++] = (byte) position;
            System.arraycopy(moveToFrontOrder, 0, moveToFrontOrder, 1, position);
            moveToFrontOrder[0] = character;
        }
        return encoded;
    }

    private static byte[] initializeMoveToFrontOrder() {
        byte[] moveToFrontOrder = new byte[RADIX];
        for (int index = 0; index < RADIX; index++) {
            moveToFrontOrder[index] = (byte) index;
        }
        return moveToFrontOrder;
    }

    private static int position(byte symbol, byte[] symbols) {
        int position = 0;
        while (symbols[position] != symbol) {
            position++;
        }
        return position;
    }

    /**
     * Apply move-to-front decoding.
     */
    public static byte[] decode(byte[] encoded) {
        byte[] encodings = initializeMoveToFrontOrder();
        byte[] text = new byte[encoded.length];
        int counter = 0;
        for (byte b : encoded) {
            int position = b & 0xff;
            byte character = encodings[position];
            text[counter++] = character;
            System.arraycopy(encodings, 0, encodings, 1, position);
            encodings[0] = character;
        }
        return text;
    }

    private static void test(String fileName) {
        try {
            byte[] text = Files.readAllBytes(Path.of(fileName));
            byte[] encoded = encode(text);
            byte[] decoded = decode(encoded);
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

    /**
     * Unit tests
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
