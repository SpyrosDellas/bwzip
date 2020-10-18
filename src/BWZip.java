import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@code BWZip} class provides static methods for compressing
 * and expanding a file using the Burrows-Wheeler transform combined
 * with Move-To-Front and Huffman encodings.
 * <p>
 * This bare-bones implementation achieves considerably better compression than
 * conventional LZ77/LZ78-based compressors such as the zip utility;
 * it is within 2% of the compression ratios achieved by bzip2 on typical
 * inputs.
 * Not bad for a weekends work!
 * <p>
 * The suffix array construction, which is the first step of the
 * Burrows-Wheeler transform, is done in a single block for the whole
 * file in guaranteed O(n) time using the 'Suffix Array Induced Sorting'
 * algorithm.
 *
 * @author Spyros Dellas
 */
public class BWZip {

    /**
     * Do not instantiate
     */
    private BWZip() {
    }

    /**
     * Compress the given file named <i>fileName</i> into a file
     * named <i>fileName.burrows</i>
     *
     * @param fileName The file to compress
     */
    public static void compress(String fileName) {
        try {
            byte[] text = Files.readAllBytes(Path.of(fileName));
            byte[] bwt = BurrowsWheeler.transform(text);
            byte[] mtf = MoveToFront.encode(bwt);
            Huffman.compress(mtf, new File(fileName + ".burrows"));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * Expand the given file named <i>fileName.burrows</i> into <i>fileName</i>
     *
     * @param fileName The file to expand
     */
    public static void expand(String fileName) {
        try {
            byte[] mtf = Huffman.expand(new File(fileName));
            byte[] bwt = MoveToFront.decode(mtf);
            byte[] text = BurrowsWheeler.inverseTransform(bwt);
            String expandedFileName = fileName.substring(0, fileName.lastIndexOf('.'));
            Files.write(Path.of(expandedFileName), text);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void test(String fileName) {
        compress(fileName);
        expandTest(fileName + ".burrows");
        verify(fileName, fileName + ".burrows.expanded");
    }

    public static void expandTest(String fileName) {
        try {
            byte[] mtf = Huffman.expand(new File(fileName));
            byte[] bwt = MoveToFront.decode(mtf);
            byte[] text = BurrowsWheeler.inverseTransform(bwt);
            Files.write(Path.of(fileName + ".expanded"), text);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void verify(String originalFileName, String expandedFileName) {
        try (BufferedInputStream original = new BufferedInputStream(new FileInputStream(originalFileName));
             BufferedInputStream expanded = new BufferedInputStream(new FileInputStream(expandedFileName))) {
            long originalSize = Files.size(Path.of(originalFileName));
            long expandedSize = Files.size(Path.of(expandedFileName));
            if (originalSize != expandedSize)
                System.err.println("Tests failed. Original and expanded file sizes don't match.");
            int buffer = original.read();
            while (buffer != -1) {
                if (buffer != expanded.read()) {
                    System.err.println("Tests failed for " + originalFileName);
                    return;
                }
                buffer = original.read();
            }
            System.out.println("Tests passed for " + originalFileName);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /*
     *  Unit testing
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
