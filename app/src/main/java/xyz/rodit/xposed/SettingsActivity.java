package xyz.rodit.xposed;

import static xyz.rodit.xposed.Constants.CONFIGURATION_UPDATE_BROADCAST;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import xyz.rodit.xposed.updates.UpdateManager;
import xyz.rodit.xposed.updates.model.UpdatePackage;
import xyz.rodit.xposed.utils.Consumer;

public class SettingsActivity extends AppCompatActivity {

    private static int preferenceResource;
    private static Consumer<SettingsFragment> createCallback;

    protected UpdateManager updates;

    public SettingsActivity(int preferenceResource) {
        SettingsActivity.preferenceResource = preferenceResource;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        updates = new UpdateManager(this, update -> runOnUiThread(() -> onUpdateFound(update)));

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

    protected void onUpdateFound(UpdatePackage updatePackage) {
        if (updatePackage.mappingsUrl != null || updatePackage.apkUrl != null) {
            String message = "Updates:\n" +
                    (updatePackage.mappingsUrl != null ? '✓' : '×') +
                    " Mappings\n" +
                    (updatePackage.apkUrl != null ? '✓' : '×') +
                    " Module APK\n\n" +
                    updatePackage.release.body;

            new AlertDialog.Builder(this)
                    .setTitle("Update Available")
                    .setMessage(message)
                    .setPositiveButton("Download", (dialogInterface, i) -> {
                        if (updatePackage.mappingsUrl != null) {
                            updates.installMappings(updatePackage,
                                    success -> runOnUiThread(() -> onMappingsInstallationStatus(success)));
                        }

                        if (updatePackage.apkUrl != null) {
                            Toast.makeText(this, "You must install the new APK from your downloads folder.", Toast.LENGTH_SHORT).show();
                            updates.downloadApk(updatePackage);
                        }
                    })
                    .show();
        }
    }

    protected void onMappingsInstallationStatus(boolean success) {
        Toast.makeText(this,
                success ? "New mappings installed successfully." : "Error installing new mappings.",
                Toast.LENGTH_SHORT).show();
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
