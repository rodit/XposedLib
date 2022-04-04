package xyz.rodit.xposed.client.http;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;
import xyz.rodit.xposed.utils.StreamUtils;

public class Response {

    private static final String TAG = "Response";

    private static final String HTTP_VERSION = "HTTP/1.1";

    private final int statusCode;
    private final String statusMessage;
    private final Map<String, String> headers;
    private final InputStream body;
    private final long contentLength;

    public Response(int statusCode, @NonNull String statusMessage, @NonNull Map<String, String> headers, @Nullable InputStream body, long contentLength) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers;
        this.body = body;
        this.contentLength = contentLength;
    }

    public void writeTo(OutputStream stream) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream))) {
            writer.write(HTTP_VERSION + " " + statusCode + " " + statusMessage + "\r\n");
            for (String header : headers.keySet()) {
                writer.write(header + ": " + headers.get(header) + "\r\n");
            }

            if (body != null) {
                writer.write("Content-Length: " + contentLength + "\r\n");
                Log.d(TAG, "Response content length: " + contentLength);
            }

            writer.write("\r\n");
            writer.flush();
            if (body != null) {
                StreamUtils.copyTo(body, stream);
            }
        }
    }

    public static class Builder {

        private final int statusCode;
        private final String statusMessage;
        private final Map<String, String> headers = new HashMap<>();
        private InputStream body;
        private long contentLength;

        public Builder() {
            this(200, "OK");
        }

        public Builder(int statusCode, @NonNull String statusMessage) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
        }

        public Builder withHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder withBody(InputStream body) {
            this.body = body;
            return this;
        }

        public Builder withContentLength(long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Response build() {
            return new Response(statusCode, statusMessage, headers, body, contentLength);
        }
    }
}
