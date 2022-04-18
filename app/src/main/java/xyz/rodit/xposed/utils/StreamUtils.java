package xyz.rodit.xposed.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

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
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static void writeFile(File file, String contents) throws IOException {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(contents);
        }
    }

    public static ByteArrayInputStream toMemoryStream(InputStream src) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            copyTo(src, out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
