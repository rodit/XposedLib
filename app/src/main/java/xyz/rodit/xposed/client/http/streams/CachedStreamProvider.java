package xyz.rodit.xposed.client.http.streams;

import java.io.IOException;
import java.io.InputStream;

import xyz.rodit.xposed.client.http.StreamProvider;

public class CachedStreamProvider implements StreamProvider {

    private final StreamProvider original;

    private boolean tried;
    private InputStream cached;

    public CachedStreamProvider(StreamProvider original) {
        this.original = original;
    }

    @Override
    public long getSize() {
        return original.getSize();
    }

    @Override
    public InputStream provide() throws IOException {
        if (!tried) {
            tried = true;
            cached = original.provide();
        }

        return cached;
    }

    @Override
    public void dispose() {
        original.dispose();
    }
}
