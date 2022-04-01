package xyz.rodit.xposed.services;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import xyz.rodit.xposed.utils.StreamUtils;

public class FileService extends Service {

    private static final String TAG = "FileService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(new FileRequestHandler(getApplicationContext())).getBinder();
    }

    static class FileRequestHandler extends Handler {

        private final Context context;

        public FileRequestHandler(Context context) {
            super(Looper.getMainLooper());
            this.context = context;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Messages.DOWNLOAD_FILE:
                    try {
                        Bundle request = msg.getData();
                        boolean useDownloadManager = request.getByte(Messages.DownloadRequest.USE_DOWNLOAD_MANAGER) == 1;
                        String source = request.getString(Messages.DownloadRequest.SOURCE);
                        String destination = request.getString(Messages.DownloadRequest.DESTINATION);
                        String title = request.getString(Messages.DownloadRequest.TITLE);
                        String description = request.getString(Messages.DownloadRequest.DESCRIPTION);

                        if (useDownloadManager) {
                            DownloadManager downloads = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                            downloads.enqueue(new DownloadManager.Request(Uri.parse(source))
                                    .setTitle(title)
                                    .setDescription(description)
                                    .setDestinationUri(Uri.parse(destination)));
                        } else {
                            new Thread(() -> {
                                try {
                                    URL url = new URL(source);
                                    try (InputStream in = url.openStream();
                                         OutputStream out = new FileOutputStream(destination)) {
                                        StreamUtils.copyTo(in, out);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error downloading file.", e);
                                }
                            }).start();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error downloading file.", e);
                    }
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
}
