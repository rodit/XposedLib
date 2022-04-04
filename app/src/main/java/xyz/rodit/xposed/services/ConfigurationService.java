package xyz.rodit.xposed.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.rodit.xposed.utils.StreamUtils;

public class ConfigurationService extends Service {

    private static final String TAG = "ConfigurationService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(new ConfigurationRequestHandler(getApplicationContext())).getBinder();
    }

    static class ConfigurationRequestHandler extends Handler {

        private final Context context;

        public ConfigurationRequestHandler(Context context) {
            super(Looper.getMainLooper());
            this.context = context;
        }

        private Map<String, String> loadConfig() {
            SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            Map<String, ?> all = prefs.getAll();
            Map<String, String> config = new HashMap<>();
            for (String key : all.keySet()) {
                Object val = all.get(key);
                if (val != null) {
                    config.put(key, val.toString());
                }
            }

            return config;
        }

        private List<File> getPossibleMappingFiles(String name) {
            List<File> files = new ArrayList<>();
            files.add(new File(context.getFilesDir(), name));
            files.add(new File(context.getExternalFilesDir(null), name));
            return files;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Messages.GET_CONFIG:
                    handleConfigRequest(msg);
                    break;
                case Messages.GET_MAPPINGS:
                    handleMappingsRequest(msg);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

        private void handleConfigRequest(Message msg) {
            Log.d(TAG, "Handling config request.");
            Message response = Message.obtain(null, Messages.CONFIG_VALUE);
            Map<String, String> config = loadConfig();
            Bundle configBundle = new Bundle();
            for (String key : config.keySet()) {
                configBundle.putString(key, config.get(key));
            }
            response.setData(configBundle);
            try {
                msg.replyTo.send(response);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending configuration bundle.", e);
            }
        }

        private void handleMappingsRequest(Message msg) {
            Log.d(TAG, "Handling mappings request.");
            try {
                int build = msg.getData().getInt(Messages.MAPPINGS_BUILD_NAME);
                String mappingsFileName = (build == 0 ? "latest" : String.valueOf(build)) + ".json";
                List<File> possibleFiles = getPossibleMappingFiles(mappingsFileName);
                boolean success = false;
                for (File file : possibleFiles) {
                    Log.d(TAG, "Looking for mappings at " + file + ".");
                    if (file.exists()) {
                        try {
                            String content = StreamUtils.readFile(file);
                            Message mappingsMessage = Message.obtain(null, Messages.MAPPINGS_VALUE);
                            Bundle mappingsBundle = new Bundle();
                            mappingsBundle.putString(Messages.MAPPINGS_VALUE_NAME, content);
                            mappingsMessage.setData(mappingsBundle);
                            msg.replyTo.send(mappingsMessage);
                            success = true;
                            break;
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading mappings file.", e);
                        }
                    }
                }

                if (!success) {
                    Message failureMessage = Message.obtain(null, Messages.MAPPINGS_FAILED);
                    msg.replyTo.send(failureMessage);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending mappings.", e);
            }
        }
    }
}
