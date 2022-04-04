package xyz.rodit.xposed.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

public class StreamUtils {

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    public static void copyTo(InputStream in, OutputStream out) throws IOException {
        copyTo(in, out, DEFAULT_BUFFER_SIZE);
    }

    public static void copyTo(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int read;
        while ((read = in.read(buffer, 0, bufferSize)) > -1) {
            out.write(buffer, 0, read);
        }
    }

    public static String readFile(File file) throws IOException {
        try (Scanner scanner = new Scanner(file)) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    public static ByteArrayInputStream toMemoryStream(InputStream src) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            copyTo(src, out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
