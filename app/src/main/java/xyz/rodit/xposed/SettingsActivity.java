package xyz.rodit.xposed;

import static xyz.rodit.xposed.Constants.CONFIGURATION_UPDATE_BROADCAST;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import xyz.rodit.xposed.utils.Consumer;

public class SettingsActivity extends AppCompatActivity {

    private static int preferenceResource;
    private static Consumer<SettingsFragment> createCallback;

    public SettingsActivity(int preferenceResource) {
        SettingsActivity.preferenceResource = preferenceResource;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        createCallback = this::onCreatePreferences;

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    public void onCreatePreferences(SettingsFragment fragment) {

    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(preferenceResource, rootKey);

            if (createCallback != null) {
                createCallback.consume(this);
            }
        }

        @Override
        public void onResume() {
            super.onResume();

            SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
            if (preferences != null) {
                preferences.registerOnSharedPreferenceChangeListener(this);
            }
        }

        @Override
        public void onPause() {
            SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
            if (preferences != null) {
                preferences.unregisterOnSharedPreferenceChangeListener(this);
            }

            super.onPause();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            Context context = getContext();
            if (context != null) {
                context.sendBroadcast(new Intent(CONFIGURATION_UPDATE_BROADCAST));
            }
        }
    }
}
