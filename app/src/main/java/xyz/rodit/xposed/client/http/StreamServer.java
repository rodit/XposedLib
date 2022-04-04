package xyz.rodit.xposed.client.http;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamServer {

    private static final String TAG = "StreamServer";

    private final int port;
    private final ExecutorService executorService;
    private final Map<String, StreamProvider> streams = new HashMap<>();

    private boolean running;
    private ServerSocket socket;

    public StreamServer(int port, int threadPoolSize) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
    }

    public String getRoot() {
        return "http://127.0.0.1:" + socket.getLocalPort();
    }

    public void mapStream(String key, StreamProvider provider) {
        streams.put("/" + key, provider);
    }

    public void unmapStream(String key) {
        streams.remove(key);
    }

    public void start() throws IOException {
        if (running) {
            throw new IllegalStateException("Cannot start server that's already running.");
        }

        socket = new ServerSocket(port);
        new Thread(this::listen).start();
        running = true;
    }

    public void stop() {
        if (!running) {
            throw new IllegalStateException("Cannot stop server that's not running.");
        }

        try {
            socket.close();
        } catch (IOException e) {
            // ignored
        }
    }

    private void listen() {
        Log.d(TAG, "Started stream server at " + getRoot());
        while (running) {
            try {
                Socket client = socket.accept();
                executorService.submit(() -> this.handleRequest(client));
            } catch (IOException e) {
                Log.e(TAG, "Error handling stream server socket request." ,e);
            }
        }
    }

    private void handleRequest(Socket client) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null && !line.equals("")) {
                lines.add(line);
            }

            Request request = Request.parse(lines);
            String resource = request.getResource();
            Log.d(TAG, "Handling stream server request for " + request.getMethod() + " " + request.getResource() + ", headerCount=" + request.getHeaders().size());
            StreamProvider provider = streams.get(resource);
            OutputStream out = client.getOutputStream();
            if (provider != null) {
                try (InputStream in = provider.provide()) {
                    if (in != null) {
                        new Response.Builder()
                                .withBody(in)
                                .withContentLength(provider.getSize())
                                .build()
                                .writeTo(out);
                    } else {
                        Log.w(TAG, "Stream provider gave null stream.");
                    }
                }

                provider.dispose();
                unmapStream(resource);
            } else {
                Log.w(TAG, "No stream found @ " + request.getResource());
                new Response.Builder(404, "Not Found")
                        .build()
                        .writeTo(out);
            }

            client.close();
        } catch (Exception e) {
            Log.e(TAG, "Error while processing client request.", e);
        }
    }
}
