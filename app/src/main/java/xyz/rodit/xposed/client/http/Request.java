package xyz.rodit.xposed.client.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

    private final String method;
    private final String resource;
    private final Map<String, String> headers;

    public Request(String method, String resource, Map<String, String> headers) {
        this.method = method;
        this.resource = resource;
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }

    public String getResource() {
        return resource;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static Request parse(List<String> lines) throws IOException {
        if (lines.isEmpty()) {
            throw new IOException("Request is empty.");
        }

        String line = lines.get(0);
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            throw new IOException("Resource not given in first line: " + line);
        }

        String method = parts[0];
        String resource = parts[1];
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] headerParts = lines.get(i).split(":", 2);
            if (headerParts.length != 2) {
                throw new IOException("Malformed header: " + lines.get(i));
            }

            headers.put(headerParts[0], headerParts[1]);
        }

        return new Request(method, resource, headers);
    }
}
