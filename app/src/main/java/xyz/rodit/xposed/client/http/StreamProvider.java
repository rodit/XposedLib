package xyz.rodit.xposed.client.http;

import java.io.IOException;
import java.io.InputStream;

public interface StreamProvider {

    long getSize();
    InputStream provide() throws IOException;
    void dispose();
}
