package xyz.rodit.xposed.client.http.streams;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import xyz.rodit.xposed.client.http.StreamProvider;
import xyz.rodit.xposed.utils.StreamUtils;
import xyz.rodit.xposed.utils.Supplier;

public class FileProxyStreamProvider implements StreamProvider {

    private final Context context;
    private final Supplier<InputStream> supplier;
    private File tmpFile;
    private long size;

    public FileProxyStreamProvider(Context context, Supplier<InputStream> supplier) {
        this.context = context;
        this.supplier = supplier;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public InputStream provide() throws IOException {
        InputStream stream = supplier.get();
        tmpFile = File.createTempFile("snapmod", "tmp", context.getCacheDir());
        try (FileOutputStream out = new FileOutputStream(tmpFile)) {
            StreamUtils.copyTo(stream, out);
            size = tmpFile.length();
            tmpFile.deleteOnExit();
            return new FileInputStream(tmpFile);
        }
    }

    @Override
    public void dispose() {
        tmpFile.delete();
    }
}
