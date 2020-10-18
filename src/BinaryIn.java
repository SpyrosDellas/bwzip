import java.io.*;
import java.util.NoSuchElementException;

/**
 * The {@code BinaryIn} class provides methods for reading in bits
 * from a binary input stream as follows:
 * <p>
 * one bit at a time as a {@code boolean},
 * <p>
 * 8 bits at a time as an {@code int}, or
 * <p>
 * 32 bits at a time as an {@code int}
 * <p>
 *
 * @author Spyros Dellas
 */
public class BinaryIn implements Closeable {

    private static final byte EOF = -1;  // end of file

    private int buffer;      // a byte buffer; enables reading of individual bits
    private int shift;        // equals to (8 - bits already read)
    private boolean closed;   // is this object closed?
    private boolean isEmpty;
    private final BufferedInputStream in;

    public BinaryIn(File inputFile) throws IOException {
        in = new BufferedInputStream(new FileInputStream(inputFile));
        isEmpty = false;
        closed = false;
        fillBuffer();
    }

    private void fillBuffer() throws IOException {
        buffer = in.read();
        shift = 8;   // no bits have been read from the buffer
        if (buffer == EOF)
            isEmpty = true;
    }

    /**
     * Is this binary input stream empty?
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * Read a single bit.
     *
     * @return True if the bit is 1 and false if the bit is 0
     * @throws IOException If an I/O error occurs reading from the file
     */
    public boolean readBoolean() throws IOException {
        if (closed)
            throw new IllegalStateException("Object already closed.");
        if (isEmpty)
            throw new NoSuchElementException("Reading from empty input stream.");
        shift--;
        boolean bit = (buffer >> shift & 1) == 1;
        // if all bits from the buffer have been read, read the next byte
        if (shift == 0) {
            fillBuffer();
        }
        return bit;
    }

    /**
     * Read the next 8 bits.
     *
     * @return An {@code int} value with the 8 bits stored in the rightmost end.
     * @throws IOException If an I/O error occurs reading from the file
     */
    public int readByte() throws IOException {
        if (closed)
            throw new IllegalStateException("Object already closed.");
        if (isEmpty)
            throw new NoSuchElementException("Reading from empty input stream.");
        // if no bits from the buffer have been read, refill and return the buffer itself
        int cache = buffer;
        if (shift == 8) {
            fillBuffer();
            return cache;
        }
        // refill and return next 8-bits
        int cachedShift = shift;
        fillBuffer();
        shift = cachedShift;
        if (isEmpty)
            throw new NoSuchElementException("Reading from empty input stream.");
        return (cache << (8 - shift)) | (buffer >> shift);
    }

    /**
     * Read the next 32 bits.
     *
     * @return An {@code int} value containing the 32 bits.
     * @throws IOException If an I/O error occurs reading from the file
     */
    public int readInt() throws IOException {
        int result = readByte();
        result = result << 8 | readByte();
        result = result << 8 | readByte();
        return result << 8 | readByte();
    }

    /**
     * Close this binary input stream.
     *
     * @throws IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        in.close();
    }

    /**
     * Test client
     */
    public static void main(String[] args) {

        // create binary output stream to write to file
        File filename = new File("./data/BinaryOutTest.txt");
        try (BinaryIn in = new BinaryIn(filename)) {
            while (!in.isEmpty()) {
                System.out.print((char) in.readByte());
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        System.out.println();
        try (BinaryIn in = new BinaryIn(filename)) {
            while (!in.isEmpty()) {
                System.out.print(in.readInt());
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
