package xyz.rodit.xposed.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.robv.android.xposed.XposedBridge;
import xyz.rodit.xposed.services.Messages;

public class FileClient {

    private final Context context;
    private final String actionPackage;
    private final String actionName;
    private final ServiceConnection connection = new FileServiceConnection();

    private Messenger outgoing;

    public FileClient(@NonNull Context context, @NonNull String actionPackage, @NonNull String actionName) {
        this.context = context;
        this.actionPackage = actionPackage;
        this.actionName = actionName;
    }

    public boolean bind() {
        Intent configIntent = new Intent(actionName);
        configIntent.setPackage(actionPackage);
        return context.bindService(configIntent, connection, Context.BIND_AUTO_CREATE);
    }

    public void download(boolean useDownloadManager, @NonNull String source, @NonNull String destination, @Nullable String title, @Nullable String description) {
        Message message = Message.obtain(null, Messages.DOWNLOAD_FILE);
        Bundle request = new Bundle();
        request.putByte(Messages.DownloadRequest.USE_DOWNLOAD_MANAGER, (byte) (useDownloadManager ? 1 : 0));
        request.putString(Messages.DownloadRequest.SOURCE, source);
        request.putString(Messages.DownloadRequest.DESTINATION, destination);
        request.putString(Messages.DownloadRequest.TITLE, title);
        request.putString(Messages.DownloadRequest.DESCRIPTION, description);
        message.setData(request);
        try {
            outgoing.send(message);
        } catch (RemoteException e) {
            XposedBridge.log("Error sending download request.");
            XposedBridge.log(e);
        }
    }

    private class FileServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            outgoing = new Messenger(binder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            outgoing = null;
        }
    }
}
