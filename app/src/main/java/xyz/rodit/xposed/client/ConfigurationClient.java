package xyz.rodit.xposed.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XposedBridge;
import xyz.rodit.xposed.services.MappingCallback;
import xyz.rodit.xposed.services.Messages;
import xyz.rodit.xposed.utils.BuildUtils;

public class ConfigurationClient {

    private final Context context;
    private final String actionPackage;
    private final String actionName;
    private final Bundle bundle = new Bundle();
    private final ServiceConnection connection = new ConfigurationServiceConnection();
    private final Messenger incoming;

    private Messenger outgoing;

    public ConfigurationClient(@NonNull Context context, @NonNull String actionPackage, @NonNull String actionName, @NonNull MappingCallback mappingCallback) {
        this.context = context;
        this.actionPackage = actionPackage;
        this.actionName = actionName;
        this.incoming = new Messenger(new ConfigurationHandler(bundle, mappingCallback));
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getString(String key, String... defaultValue) {
        return bundle.getString(key, first(defaultValue));
    }

    public boolean getBoolean(String key, Boolean... defaultValue) {
        String val = getString(key);
        return val == null ? first(defaultValue) : val.equalsIgnoreCase("true");
    }

    public int getInt(String key, Integer... defaultValue) {
        String val = getString(key);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return first(defaultValue);
        }
    }

    public long getLong(String key, Long... defaultValue) {
        String val = getString(key);
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return first(defaultValue);
        }
    }

    public float getFloat(String key, Float... defaultValue) {
        String val = getString(key);
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            return first(defaultValue);
        }
    }

    public double getDouble(String key, Double... defaultValue) {
        String val = getString(key);
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return first(defaultValue);
        }
    }

    private static <T> T first(T[] array) {
        return array == null || array.length == 0 ? null : array[0];
    }

    public boolean bind() {
        Intent configIntent = new Intent(actionName);
        configIntent.setPackage(actionPackage);
        return context.bindService(configIntent, connection, Context.BIND_AUTO_CREATE);
    }

    private class ConfigurationServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            outgoing = new Messenger(binder);

            try {
                // Request config
                Message request = Message.obtain(null, Messages.GET_CONFIG);
                request.replyTo = incoming;
                outgoing.send(request);

                // Request mappings
                request = Message.obtain(null, Messages.GET_MAPPINGS);
                request.replyTo = incoming;
                Bundle requestBundle = new Bundle();
                requestBundle.putInt(Messages.MAPPINGS_BUILD_NAME, BuildUtils.getBuildVersion(context));
                request.setData(requestBundle);
                outgoing.send(request);
            } catch (RemoteException e) {
                XposedBridge.log("Error requesting config.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            outgoing = null;
        }
    }

    private static class ConfigurationHandler extends Handler {

        private final Bundle configBundle;
        private final MappingCallback mappingCallback;

        public ConfigurationHandler(Bundle configBundle, MappingCallback mappingCallback) {
            super(Looper.getMainLooper());
            this.configBundle = configBundle;
            this.mappingCallback = mappingCallback;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Messages.CONFIG_VALUE:
                    configBundle.clear();
                    configBundle.putAll(msg.getData());
                    break;
                case Messages.MAPPINGS_VALUE:
                    String content = msg.getData().getString(Messages.MAPPINGS_VALUE_NAME);
                    mappingCallback.onMappingsReceived(content);
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