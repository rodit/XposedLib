package xyz.rodit.xposed.client.http.streams;

import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import xyz.rodit.xposed.client.http.StreamProvider;

public class URLStreamProvider implements StreamProvider {

    private final URL url;
    private InputStream stream;
    private long contentLength;

    public URLStreamProvider(String url) {
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid url.");
        }
    }

    @Override
    public long getSize() {
        return contentLength;
    }

    @Override
    public InputStream provide() throws IOException {
        URLConnection connection = url.openConnection();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contentLength = connection.getContentLengthLong();
        } else {
            contentLength = connection.getContentLength();
        }

        return stream = modifyStream(connection.getInputStream());
    }

    public InputStream modifyStream(InputStream stream) {
        return stream;
    }

    @Override
    public void dispose() {

    }
}
