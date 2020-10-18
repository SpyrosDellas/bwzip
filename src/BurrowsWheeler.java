import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Implementation of the Burrows-Wheeler transform and its inverse.
 *
 * @author Spyros Dellas
 */
public class BurrowsWheeler {

    private static final int RADIX = 256;

    /**
     * Do not instantiate
     */
    private BurrowsWheeler() {

    }

    /**
     * Apply the Burrows-Wheeler transform to given input.
     * <p>
     * - The running time is proportional to n + R (or better) in the worst case,
     * excluding the time to construct the circular suffix array.
     * <p>
     * - The amount of memory used is proportional to n + R (or better) in the worst case.
     * <p>
     * The goal of the Burrows–Wheeler transform is not to compress a message, but
     * rather to transform it into a form that is more amenable for compression.
     * It rearranges the characters in the input so that there are lots of clusters
     * with repeated characters, but in such a way that it is still possible to recover
     * the original input.
     * It relies on the following intuition: if you see the letters 'hen' in English
     * text, then, most of the time, the letter preceding it is either 't' or 'w'.
     * If you could somehow group all such preceding letters together (mostly t’s and
     * some w’s), then you would have a propitious opportunity for data compression.
     * <p>
     * The Burrows–Wheeler transform of a {@code String} s of length n is defined
     * as follows:
     * Consider the suffix array {@code sa[]} of s, when s is augmented with a sentinel
     * character at the end which is lexicographically smaller than any other
     * character in the string.
     * The Burrows–Wheeler transform {@code bwt[]} then is:
     * - s.charAt(sa[i] - 1), if sa[i] != 0, and
     * - the sentinel, if sa[i] == 0
     * preceded by the index <i>first</i> in the suffix array for which sa[first] == 0.
     */
    public static byte[] transform(byte[] text) {

        SuffixArray suffixArray = new SuffixArray(text);

        // include 4 bytes in the beginning to store first, plus one more byte for the sentinel
        // that augments the text
        byte[] bwt = new byte[text.length + 5];

        // Find and write the index of the original string in the suffix array
        int first = findFirst(suffixArray);
        writeFirst(first, bwt);

        // Write the bwt transform
        for (int i = 0; i < first; i++) {
            bwt[i + 4] = text[suffixArray.index(i) - 1];
        }
        bwt[first + 4] = -1; // this is the sentinel
        for (int i = first + 1; i <= text.length; i++) {
            bwt[i + 4] = text[suffixArray.index(i) - 1];
        }
        return bwt;
    }

    private static int findFirst(SuffixArray suffixArray) {
        int first = 0;
        while (suffixArray.index(first) != 0) {
            first++;
        }
        return first;
    }

    private static void writeFirst(int first, byte[] transformed) {
        transformed[0] = (byte) (first >> 24 & 0xff);
        transformed[1] = (byte) (first >> 16 & 0xff);
        transformed[2] = (byte) (first >> 8 & 0xff);
        transformed[3] = (byte) (first & 0xff);
    }

    /**
     * Apply the Burrows-Wheeler inverse transform.
     * <p>
     * The running time is proportional to n + R (or better) in the worst case.
     * <p>
     * The amount of memory used is proportional to n + R (or better) in the
     * worst case.
     *
     * @param bwt The Burrows-Wheeler transform to inverse
     */
    public static byte[] inverseTransform(byte[] bwt) {
        /*
         * We can invert the Burrows–Wheeler transform and recover the original
         * input string as follows.
         * Definition:
         * Suppose the original suffix starting at index k appears in the ith index of the
         * sorted sequence of characters contained in bwt[]. Then we define next[i] to be
         * the index where the original suffix starting at index (k + 1) appears.
         * For example, if first is the index in which the original input string appears,
         * then next[first] is the index in the sorted sequence where the original suffix
         * starting at position 1 appears; next[next[first]] is the index in the sorted sequence
         * where the original suffix starting at position 2 appears; and so forth.
         * Algorithm:
         * The goal is to recover the original string given the Burrows-Wheeler
         * transform btw[], which includes the index 'start' of the original string
         * in the suffix array.
         * From bwt[], we could easily construct the sorted sequence of characters, because
         * it consists of precisely the same characters contained in bwt[], but in sorted order.
         * However, this is not necessary since we can construct next[] directly:
         * We use counting sort (which is a stable sort) to sort the characters in bwt[]
         * and then we populate next[] not with the characters themselves, but with their
         * corresponding indices in bwt[].
         * In essence, next[] is the ordered sequence of characters, where each character
         * is represented by its index in bwt[].
         */

        // Read the index of the original string in the suffix array
        int first = readFirst(bwt);

        // Instantiate the array that will store the original text
        int textLength = bwt.length - 5;
        byte[] text = new byte[textLength];

        // Calculate the next[] array
        int[] next = next(bwt, first);

        // Recover the original text from the bwt[] and next[] arrays
        int index = first + 4;
        for (int counter = 0; counter < textLength; counter++) {
            text[counter] = bwt[next[index]];
            index = next[index];
        }
        return text;
    }

    private static int readFirst(byte[] bwt) {
        int first = bwt[0] & 0xff;
        first = first << 8 | bwt[1] & 0xff;
        first = first << 8 | bwt[2] & 0xff;
        first = first << 8 | bwt[3] & 0xff;
        return first;
    }

    /*
     * Counting sort bwt[] and populate next[] not with the characters
     * themselves, but with their indices in bwt[] instead.
     */
    private static int[] next(byte[] bwt, int first) {
        int[] next = new int[bwt.length];
        int[] count = count(bwt, first);
        // Update for sentinel
        next[4] = first + 4;
        // Update for the rest of the characters
        for (int index = 4; index < first + 4; index++) {
            byte c = bwt[index];
            next[count[(c & 0xff) + 1]++] = index;
        }
        for (int index = first + 5; index < bwt.length; index++) {
            byte c = bwt[index];
            next[count[(c & 0xff) + 1]++] = index;
        }
        return next;
    }

    /*
     * Count the frequencies of appearance of each character in the given bwt[] array
     * and calculate the corresponding starting indices in the sorted array.
     * The first 4 bytes are excluded as they contain the index of the original string
     * in the suffix array.
     */
    private static int[] count(byte[] bwt, int first) {
        // the count array includes one extra space for the sentinel
        int[] count = new int[RADIX + 1];
        // update for the sentinel
        count[0] = 1;
        // count all characters in the bwt excluding the sentinel
        for (int i = 4; i < first + 4; i++)
            count[(bwt[i] & 0xff) + 1]++;
        for (int i = first + 5; i < bwt.length; i++)
            count[(bwt[i] & 0xff) + 1]++;
        // calculate the starting indices
        count[RADIX] = bwt.length - count[RADIX];
        for (int i = RADIX - 1; i >= 0; i--) {
            count[i] = count[i + 1] - count[i];
        }
        return count;
    }

    private static void test(String fileName) {
        try {
            byte[] text = Files.readAllBytes(Path.of(fileName));
            byte[] bwt = transform(text);
            byte[] decoded = inverseTransform(bwt);
            for (int i = 0; i < text.length; i++) {
                if (text[i] != decoded[i]) {
                    System.err.println("Tests failed at index i = " + i + " for " + fileName);
                    return;
                }
            }
            System.out.println("Tests passed for " + fileName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * Test client
     */
    public static void main(String[] args) {
        String fileName = "./data/test.txt";
        test(fileName);
        fileName = "./data/aesop.txt";
        test(fileName);
        fileName = "./data/amendments.txt";
        test(fileName);
        fileName = "./data/rand10K.bin";
        test(fileName);
        fileName = "./data/purple.gif";
        test(fileName);
        fileName = "./data/chromosome11.txt";
        test(fileName);
        fileName = "./data/pi.txt";
        test(fileName);
        fileName = "./data/pipi.txt";
        test(fileName);
        fileName = "./data/chromosome22.txt";
        test(fileName);
        fileName = "./data/etext99.txt";
        test(fileName);
    }

}
