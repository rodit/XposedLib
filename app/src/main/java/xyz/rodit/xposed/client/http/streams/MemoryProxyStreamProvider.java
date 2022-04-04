package xyz.rodit.xposed.client.http.streams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import xyz.rodit.xposed.client.http.StreamProvider;
import xyz.rodit.xposed.utils.StreamUtils;
import xyz.rodit.xposed.utils.Supplier;

public class MemoryProxyStreamProvider implements StreamProvider {

    private final Supplier<InputStream> supplier;
    private int size;

    public MemoryProxyStreamProvider(Supplier<InputStream> supplier) {
        this.supplier = supplier;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public InputStream provide() throws IOException {
        ByteArrayInputStream stream = StreamUtils.toMemoryStream(supplier.get());
        size = stream.available();
        return stream;
    }

    @Override
    public void dispose() {

    }
}
