package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

/**
 * InputStream that emits lines joined by a provided separator without creating
 * a single large String/byte[] in memory.
 */
public class LinesInputStream extends InputStream {
    private final Iterator<String> iterator;
    private final byte[] sepBytes;
    private final Charset charset;
    private byte[] current;
    private int pos;

    public LinesInputStream(List<String> lines, String separator, Charset charset) {
        this.iterator = lines.iterator();
        this.sepBytes = separator.getBytes(charset);
        this.charset = charset;
        this.current = null;
        this.pos = 0;
    }

    @Override
    public int read() throws IOException {
        if (current == null || pos >= current.length) {
            if (!loadNext()) return -1;
        }
        return current[pos++] & 0xFF;
    }

    private boolean loadNext() {
        if (!iterator.hasNext()) return false;
        String next = iterator.next();
        byte[] nextBytes = next.getBytes(charset);
        if (iterator.hasNext()) {
            // append separator
            current = new byte[nextBytes.length + sepBytes.length];
            System.arraycopy(nextBytes, 0, current, 0, nextBytes.length);
            System.arraycopy(sepBytes, 0, current, nextBytes.length, sepBytes.length);
        } else {
            current = nextBytes;
        }
        pos = 0;
        return true;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
        if (len == 0) return 0;
        int read = 0;
        while (read < len) {
            if (current == null || pos >= current.length) {
                if (!loadNext()) return read == 0 ? -1 : read;
            }
            int toCopy = Math.min(len - read, current.length - pos);
            System.arraycopy(current, pos, b, off + read, toCopy);
            pos += toCopy;
            read += toCopy;
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        // nothing to close
        super.close();
    }
}
