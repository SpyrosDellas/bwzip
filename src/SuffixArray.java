import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Calculate the suffix array of a given {@code String} represented
 * as a sequence of bytes using the SA-IS (Suffix Array Induced Sorting) algorithm.
 * <p>
 * The running time is guaranteed O(n).
 *
 * @author Spyros Dellas
 */
public class SuffixArray {

    private static final int RADIX = 256;
    private static final int SENTINEL = -1;
    private static final int L = -1;
    private static final int S = 0;
    private static final int LMS = 1;

    private final byte[] s;   // the input byte[] array
    private final int length; // the length of the input string
    private int[] sa;         // the suffix array

    /**
     * Construct the suffix array of s
     *
     * @param s The input {@code byte[]} array
     * @throws IllegalArgumentException if {@code s} is {@code null}
     */
    public SuffixArray(byte[] s) {

        if (s == null)
            throw new IllegalArgumentException("Input string cannot be null");

        this.s = s;
        length = s.length;

        // instantiate the suffix array; includes space for an additional sentinel character at the end
        sa = new int[length + 1];

        // if the string is empty, no further action is required
        if (length == 0) return;

        // calculate the suffix array
        sais0();
    }

    private void sais0() {
        // the boundaries of the buckets for each letter in the alphabet of s,
        // including one extra bucket for the sentinel character at the end
        int[] buckets = new int[SuffixArray.RADIX + 2];

        // stores the classification of the suffixes
        byte[] types = new byte[length + 1];

        classifySuffixes(types, buckets);
        induceSort(types, buckets);
        reduce(types, buckets);
    }

    // Classify all suffixes as L-type, S-type or LMS-type
    private void classifySuffixes(byte[] types, int[] buckets) {

        // initialize buckets[1] for sentinel (end of string character)
        buckets[1]++;

        // previous character is the sentinel
        int previous = SENTINEL;

        for (int i = types.length - 2; i >= 0; i--) {
            int current = s[i] & 0xff;
            buckets[current + 2]++;
            if (current > previous) {
                types[i] = L;   // L-type
            } else if (current == previous && types[i + 1] == L) {
                types[i] = L;   // L-type
            }
            if (types[i] == L && types[i + 1] == S) {
                types[i + 1] = LMS;  // LMS-type
            }
            previous = current;
        }

        // Calculate the boundaries of the buckets for each character
        for (int i = 1; i < buckets.length; i++) {
            buckets[i] += buckets[i - 1];
        }
    }

    private void induceSort(byte[] types, int[] buckets) {
        // create a copy of buckets[]
        int[] boundaries = buckets.clone();

        // update for sentinel
        sa[0] = length;

        // place the LMS suffixes at the ends of their buckets
        for (int i = types.length - 2; i >= 1; i--) {
            if (types[i] == LMS)
                sa[--boundaries[(s[i] & 0xff) + 2]] = i;
        }

        // induce sort the L-prefixes
        boundaries = buckets.clone();
        for (int i = 0; i < length; i++) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] == L) {
                sa[boundaries[(s[previous] & 0xff) + 1]++] = previous;
            }
        }

        // induce sort the S-prefixes
        boundaries = buckets.clone();
        for (int i = length; i >= 1; i--) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] != L) {
                sa[--boundaries[(s[previous] & 0xff) + 2]] = previous;
            }
        }
    }

    private void reduce(byte[] types, int[] buckets) {
        // compact the sorted lms substrings into sa[0...lmsCounter - 1]
        int lmsCounter = 0;
        for (int i = 0; i < length + 1; i++) {
            if (types[sa[i]] == LMS)
                sa[lmsCounter++] = sa[i];
        }

        // clean the rest of the suffix array to be used as a name buffer for the reduced string
        for (int i = lmsCounter; i <= length; i++)
            sa[i] = -1;

        // find the lexicographic names of the sorted lms substrings and store them (in order of appearance in the
        // original string) into sa[lmsCounter..lmsCounter + length/2]

        sa[lmsCounter + length / 2] = 0;    // update for sentinel Id = 0
        int previousId = 0;                 // identification of the previous lms substring
        int previousPosition = length;      // starting position of the previous lms substring

        for (int i = 1; i < lmsCounter; i++) {
            int currentPosition = sa[i];
            if (isEqual(previousPosition, currentPosition, types)) {
                sa[lmsCounter + currentPosition / 2] = previousId;
            } else {
                sa[lmsCounter + currentPosition / 2] = ++previousId;
            }
            previousPosition = currentPosition;
        }

        // compact the reduced string into sa[lmsCounter...lmsCounter + lmsCounter - 1]
        int pointer = lmsCounter;
        for (int i = lmsCounter; i <= length; i++) {
            if (sa[i] >= 0)
                sa[pointer++] = sa[i];
        }

        // sort the suffixes of the reduced string into sa[0...lmsCounter - 1]
        if (previousId + 1 == lmsCounter) {
            // All lms substrings have unique ids
            suffixSortFromUniqueChars(lmsCounter);
        } else {
            // Two or more lms substrings have the same id; sort the reduced string recursively
            sais1(previousId, lmsCounter);
        }
        induceSortReduced(types, buckets, lmsCounter);
    }

    private boolean isEqual(int p1, int p2, byte[] types) {
        // compare the first characters; do not check if LMS
        if (p1 == length || p2 == length || s[p1] != s[p2])
            return false;
        p1++;
        p2++;

        // compare the remaining characters
        for (int i = 0; i <= length; i++) {
            if (p1 == length || p2 == length || s[p1] != s[p2])
                return false;
            if (types[p1] == LMS || types[p2] == LMS)
                break;
            p1++;
            p2++;
        }
        return (types[p1] == LMS && types[p2] == LMS);
    }

    private void suffixSortFromUniqueChars(int lmsCounter) {
        for (int i = 0; i < lmsCounter; i++) {
            sa[sa[lmsCounter + i]] = i;
        }
    }

    private void induceSortReduced(byte[] types, int[] buckets, int lmsCounter) {
        // store the positions of the lms suffixes into sa[lmsCounter]...sa[lmsCounter + lmsCounter - 1]
        int pointer = lmsCounter;
        for (int i = 0; i <= length; i++) {
            if (types[i] == LMS)
                sa[pointer++] = i;
        }

        // recover the indices of the sorted lms suffixes
        for (int i = 0; i < lmsCounter; i++) {
            sa[i] = sa[lmsCounter + sa[i]];
        }

        // clean the remaining part of sa[]
        for (int i = lmsCounter; i <= length; i++)
            sa[i] = -1;

        // create a copy of buckets[]
        int[] boundaries = buckets.clone();

        // update for sentinel
        sa[0] = length;

        // place the sorted LMS suffixes at the ends of their buckets
        for (int i = lmsCounter - 1; i >= 1; i--) {
            int index = sa[i];
            sa[i] = -1;
            sa[--boundaries[(s[index] & 0xff) + 2]] = index;
        }

        // induce sort the L-prefixes
        boundaries = buckets.clone();
        for (int i = 0; i < length; i++) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] == L) {
                sa[boundaries[(s[previous] & 0xff) + 1]++] = previous;
            }
        }

        // induce sort the S and LMS prefixes
        boundaries = buckets.clone();
        for (int i = length; i >= 1; i--) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] != L) {
                sa[--boundaries[(s[previous] & 0xff) + 2]] = previous;
            }
        }
    }

    private void sais1(int reducedRadix, int reducedStringLength) {

        // the boundaries of the buckets for each letter in the (reduced) alphabet
        int[] buckets = new int[reducedRadix + 2];

        // stores the classification of the suffixes
        byte[] types = new byte[reducedStringLength];

        classifySuffixesInt(types, buckets);
        induceSortInt(types, buckets);
        reduceInt(types, buckets);
    }

    // Classify all suffixes as L-type, S-type or LMS-type
    private void classifySuffixesInt(byte[] types, int[] buckets) {

        int previous = 0;   // previous character is the sentinel
        buckets[1]++;       // update buckets[1] for sentinel

        int length = types.length;
        int index = length - 2;
        for (int indexAtSA = 2 * length - 2; indexAtSA >= length; indexAtSA--) {
            int current = sa[indexAtSA];
            buckets[current + 1]++;
            if (current > previous) {
                types[index] = L;
            } else if (current == previous && types[index + 1] == L) {
                types[index] = L;
            }
            if (types[index] == L && types[index + 1] == S) {
                types[index + 1] = LMS;     // LMS-type
            }
            index--;
            previous = current;
        }

        // Calculate the boundaries of the buckets for each character
        for (int i = 1; i < buckets.length; i++) {
            buckets[i] += buckets[i - 1];
        }
    }

    private void induceSortInt(byte[] types, int[] buckets) {

        // create a copy of buckets[]
        int[] boundaries = buckets.clone();

        int length = types.length;

        // clean the part of sa[] that will be storing the reduced suffix array
        for (int i = 0; i < length; i++)
            sa[i] = -1;

        // place the LMS suffixes at the ends of their buckets
        int pointer = length - 1;
        for (int i = 2 * length - 1; i >= length; i--) {
            if (types[pointer] == LMS)
                sa[--boundaries[sa[i] + 1]] = pointer;
            pointer--;
        }

        // induce sort the L-prefixes
        boundaries = buckets.clone();
        for (int i = 0; i < length; i++) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] == L) {
                sa[boundaries[sa[length + previous]]++] = previous;
            }
        }

        // induce sort the S-prefixes
        boundaries = buckets.clone();
        for (int i = length - 1; i >= 0; i--) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] != L) {
                sa[--boundaries[sa[length + previous] + 1]] = previous;
            }
        }
    }

    private void reduceInt(byte[] types, int[] buckets) {

        int length = types.length;

        // compact the sorted lms substrings
        int lmsCounter = 0;
        for (int i = 0; i < length; i++) {
            if (types[sa[i]] == LMS)
                sa[lmsCounter++] = sa[i];
        }

        // clean the rest of the suffix array to be used as a name buffer for the reduced string
        for (int i = lmsCounter; i < length; i++)
            sa[i] = -1;

        // find the lexicographic names of the sorted lms substrings and store them (in order of appearance in the
        // original string) into sa[lmsCounter..lmsCounter + length/2]

        sa[lmsCounter + (length - 1) / 2] = 0;    // update for sentinel
        int previousId = 0;                       // identification of the previous lms substring
        int previousPosition = length - 1;        // starting position of the previous lms substring

        for (int i = 1; i < lmsCounter; i++) {
            int currentPosition = sa[i];
            if (isEqualInt(previousPosition, currentPosition, types)) {
                sa[lmsCounter + currentPosition / 2] = previousId;
            } else {
                sa[lmsCounter + currentPosition / 2] = ++previousId;
            }
            previousPosition = currentPosition;
        }

        // compact the reduced string from sa[lmsCounter]...sa[lmsCounter + lmsCounter - 1]
        int index = lmsCounter;
        for (int i = lmsCounter; i < length; i++) {
            if (sa[i] >= 0)
                sa[index++] = sa[i];
        }

        // sort the suffixes of the reduced string into sa[0...lmsCounter - 1]
        if (previousId + 1 == lmsCounter) {
            // All lms substrings have unique ids
            suffixSortFromUniqueChars(lmsCounter);
        } else {
            // Two or more lms substrings have the same id; sort the reduced string recursively
            sais1(previousId, lmsCounter);
        }
        induceSortReducedInt(types, buckets, lmsCounter);
    }

    private boolean isEqualInt(int p1, int p2, byte[] types) {
        int length = types.length;
        int endOfStringIndex = length - 1;
        int index1 = length + p1;
        int index2 = length + p2;

        // compare the first characters; do not check if LMS
        if (p1 == endOfStringIndex || p2 == endOfStringIndex || sa[index1] != sa[index2])
            return false;
        index1++;
        index2++;
        p1++;
        p2++;

        // compare the remaining characters
        for (int i = 0; i < length; i++) {
            if (p1 == endOfStringIndex || p2 == endOfStringIndex || sa[index1] != sa[index2])
                return false;
            if (types[p1] == LMS || types[p2] == LMS)
                break;
            index1++;
            index2++;
            p1++;
            p2++;
        }
        return (types[p1] == LMS && types[p2] == LMS);
    }

    private void induceSortReducedInt(byte[] types, int[] buckets, int lmsCounter) {

        int length = types.length;

        // store the positions of the lms suffixes into sa[lmsCounter]...sa[lmsCounter + lmsCounter - 1]
        int pointer = lmsCounter;
        for (int i = 0; i < length; i++) {
            if (types[i] == LMS)
                sa[pointer++] = i;
        }

        // recover the indices of the sorted lms suffixes
        for (int i = 0; i < lmsCounter; i++) {
            sa[i] = sa[lmsCounter + sa[i]];
        }

        // clean the remaining part of sa[]
        for (int i = lmsCounter; i < length; i++)
            sa[i] = -1;

        // create a copy of buckets[]
        int[] boundaries = buckets.clone();

        // place the sorted LMS suffixes at the ends of their buckets
        for (int i = lmsCounter - 1; i >= 0; i--) {
            int index = sa[i];
            sa[i] = -1;
            sa[--boundaries[sa[length + index] + 1]] = index;
        }

        // induce sort the L-prefixes
        boundaries = buckets.clone();
        for (int i = 0; i < length - 1; i++) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] == L) {
                sa[boundaries[sa[length + previous]]++] = previous;
            }
        }

        // induce sort the S-prefixes
        boundaries = buckets.clone();
        for (int i = length - 1; i >= 1; i--) {
            int current = sa[i];
            int previous = current - 1;
            if (current > 0 && types[previous] != L) {
                sa[--boundaries[sa[length + previous] + 1]] = previous;
            }
        }
    }

    /**
     * Returns the ith suffix in the suffix array, represented as it's starting
     * index in the original string.
     * <p>
     * Important note:
     * The original string <i>s</i> is always augmented with a unique sentinel character at the end.
     * The sentinel is lexicographically smaller than any other character in the string.
     * Thus index(0) will always point to the index of the sentinel, i.e. index(0) == s.length()
     *
     * @param i An index into the suffix array
     * @return The ith suffix in the suffix array
     */
    public int index(int i) {
        return sa[i];
    }

    /**
     * Unit testing.
     */
    public static void main(String[] args) {
        /*
        String s = "ABABAABAAA";
        byte[] text = s.getBytes();
        SuffixArray suffixArray = new SuffixArray(text);
        System.out.println("The length of the input string is: " + text.length);

        System.out.println("The suffix array is:");
        System.out.print("[ ");
        for (int i = 0; i < text.length + 1; i++) {
            System.out.print(suffixArray.index(i) + " ");
        }
        System.out.println("]");

        System.out.println("Stored internally as: \n" + Arrays.toString(suffixArray.sa));

        for (int i = 1; i < text.length + 1; i++) {
            System.out.println(s.substring(suffixArray.index(i)));
        }

         */
        String f = "./data/etext99.txt";
        System.out.println("Importing file: " + f);
        try {
            byte[] s = Files.readAllBytes(Path.of(f));
            System.out.println("Number of characters: " + s.length);

            System.out.println("Creating the suffix array...");
            double start = System.currentTimeMillis();
            SuffixArray sa = new SuffixArray(s);
            double end = System.currentTimeMillis();
            System.out.println("Total time = " + (end - start) / 1000 + " sec");

        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
