package xyz.rodit.xposed.client;

import static xyz.rodit.xposed.Constants.CONFIGURATION_UPDATE_BROADCAST;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.robv.android.xposed.XposedBridge;
import xyz.rodit.xposed.services.Messages;
import xyz.rodit.xposed.utils.BuildUtils;
import xyz.rodit.xposed.utils.Consumer;

public class ConfigurationClient {

    private static final String TAG = "ConfigurationClient";

    private final Context context;
    private final String actionPackage;
    private final String actionName;
    private final Bundle bundle = new Bundle();
    private final ConfigurationServiceConnection connection = new ConfigurationServiceConnection();
    private final Messenger incoming;

    private Messenger outgoing;
    private boolean loaded;

    public ConfigurationClient(@NonNull Context context, @NonNull String actionPackage, @NonNull String actionName, @Nullable Runnable configCallback, @Nullable Consumer<String> mappingCallback) {
        this.context = context;
        this.actionPackage = actionPackage;
        this.actionName = actionName;
        this.incoming = new Messenger(new ConfigurationHandler(bundle, () -> {
            this.loaded = true;
            if (configCallback != null) {
                configCallback.run();
            }
        }, mappingCallback));
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getString(String key) {
        return getString(key, null);
    }

    public String getString(String key, String defaultValue) {
        return bundle.getString(key, defaultValue);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = getString(key);
        return val == null ? defaultValue : val.equalsIgnoreCase("true");
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        String val = getString(key);
        try {
            return Integer.parseInt(val);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        String val = getString(key);
        try {
            return Long.parseLong(val);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public float getFloat(String key) {
        return getFloat(key, 0f);
    }

    public float getFloat(String key, float defaultValue) {
        String val = getString(key);
        try {
            return Float.parseFloat(val);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public double getDouble(String key) {
        return getDouble(key, 0d);
    }

    public double getDouble(String key, double defaultValue) {
        String val = getString(key);
        try {
            return Double.parseDouble(val);
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    public boolean bind() {
        Intent configIntent = new Intent(actionName);
        configIntent.setPackage(actionPackage);
        if (context.bindService(configIntent, connection, Context.BIND_AUTO_CREATE)) {
            registerUpdateReceiver();
            return true;
        }

        return false;
    }

    public boolean isLoaded() {
        return loaded;
    }

    private void registerUpdateReceiver() {
        context.registerReceiver(new ConfigurationBroadcastReceiver(), new IntentFilter(CONFIGURATION_UPDATE_BROADCAST));
    }

    private class ConfigurationServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            outgoing = new Messenger(binder);

            try {
                // Request config
                requestConfig();
                // Request mappings
                requestMappings();
            } catch (RemoteException e) {
                XposedBridge.log("Error requesting config.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            outgoing = null;
        }

        public void requestConfig() throws RemoteException {
            Message request = Message.obtain(null, Messages.GET_CONFIG);
            request.replyTo = incoming;
            outgoing.send(request);
        }

        public void requestMappings() throws RemoteException {
            Message request = Message.obtain(null, Messages.GET_MAPPINGS);
            request.replyTo = incoming;
            Bundle requestBundle = new Bundle();
            requestBundle.putInt(Messages.MAPPINGS_BUILD_NAME, BuildUtils.getBuildVersion(context));
            request.setData(requestBundle);
            outgoing.send(request);
        }
    }

    private class ConfigurationBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (outgoing != null) {
                    connection.requestConfig();
                }
            } catch (RemoteException e) {
                XposedBridge.log("Error requesting updated config.");
            }
        }
    }

    private static class ConfigurationHandler extends Handler {

        private final Bundle configBundle;
        private final Runnable configCallback;
        private final Consumer<String> mappingCallback;

        public ConfigurationHandler(Bundle configBundle, @Nullable Runnable configCallback, @Nullable Consumer<String> mappingCallback) {
            super(Looper.getMainLooper());
            this.configBundle = configBundle;
            this.configCallback = configCallback;
            this.mappingCallback = mappingCallback;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Messages.CONFIG_VALUE:
                    configBundle.clear();
                    configBundle.putAll(msg.getData());
                    if (this.configCallback != null) {
                        this.configCallback.run();
                    }
                    Log.d(TAG, "Received configuration from server.");
                    break;
                case Messages.MAPPINGS_VALUE:
                    String content = msg.getData().getString(Messages.MAPPINGS_VALUE_NAME);
                    if (this.mappingCallback != null) {
                        mappingCallback.consume(content);
                    }
                    Log.d(TAG, "Received mappings from server.");
                    break;
                case Messages.MAPPINGS_FAILED:
                    XposedBridge.log("Configuration server failed to find valid mappings.");
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }
}
