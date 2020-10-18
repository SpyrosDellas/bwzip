import java.io.*;

/**
 * The {@code BinaryOut} class provides methods for writing in bits
 * to a binary output stream as follows:
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
public class BinaryOut implements Closeable {

    private static final int FULL = 0;
    private static final int EMPTY = 8;

    private int buffer;       // a byte buffer; used for buffering individual bits
    private int shift;        // equals to (8 - bits in the buffer)
    private boolean closed;   // is this object closed?
    private BufferedOutputStream out;

    public BinaryOut(File outputFile) throws IOException {
        out = new BufferedOutputStream(new FileOutputStream(outputFile));
        buffer = 0;
        shift = EMPTY;
        closed = false;
    }

    /**
     * Write a single bit.
     */
    public void writeBitArray(boolean bit) throws IOException {
        if (closed)
            throw new IllegalStateException("Object already closed.");
        // make space for the new bit
        buffer <<= 1;
        // write the bit if 1; no action required if 0
        if (bit)
            buffer |= 1;
        shift--;
        // if buffer is full, write it out as a single byte
        if (shift == FULL) {
            shift = EMPTY;
            out.write(buffer);
        }
    }

    /**
     * Write the 8 right-most bits of the given {@code int}.
     */
    public void write(int value) throws IOException {
        if (closed)
            throw new IllegalStateException("Object already closed.");
        if (shift == EMPTY) {
            out.write(value);
        } else {
            out.write((buffer << shift) | (value >> (8 - shift)));
            buffer = value & (0xff >> shift);
        }
    }

    /**
     * Write all 32 bits of the given {@code int}.
     */
    public void writeInt(int value) throws IOException {
        write(value >> 24 & 0xff);
        write(value >> 16 & 0xff);
        write(value >> 8 & 0xff);
        write(value & 0xff);
    }

    /**
     * Write an array of bits.
     */
    public void writeBitArray(boolean[] bits) throws IOException {
        if (closed)
            throw new IllegalStateException("Object already closed.");
        for (boolean bit : bits) {
            // make space for the new bit
            buffer <<= 1;
            // write the bit if 1; no action required if 0
            if (bit)
                buffer |= 1;
            shift--;
            // if buffer is full, write it out as a single byte
            if (shift == FULL) {
                shift = EMPTY;
                out.write(buffer);
            }
        }
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
        if (shift != EMPTY) {
            buffer <<= shift;  // pad the buffer with zeros
            out.write(buffer);
        }
        out.flush();
        out.close();
    }

    /**
     * Test client
     */
    public static void main(String[] args) {

        // create binary output stream to write to file
        File filename = new File("./data/BinaryOutTest.txt");
        try (BinaryOut out = new BinaryOut(filename)) {
            // read from standard input and write to file
            out.write(65);
            out.write(66);
            out.write(67);
            out.writeBitArray(false);
            out.writeBitArray(true);
            out.writeBitArray(false);
            out.writeBitArray(false);
            out.writeBitArray(false);
            out.writeBitArray(false);
            out.writeBitArray(false);
            out.writeBitArray(true);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}
