package xyz.rodit.xposed.updates;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import xyz.rodit.xposed.updates.model.Release;
import xyz.rodit.xposed.updates.model.UpdatePackage;
import xyz.rodit.xposed.updates.model.VersionInfo;
import xyz.rodit.xposed.utils.Consumer;
import xyz.rodit.xposed.utils.PathUtils;
import xyz.rodit.xposed.utils.Predicate;
import xyz.rodit.xposed.utils.StreamUtils;

public class UpdateManager {

    private static final String TAG = "UpdateClient";

    private static final String GITHUB_API_BASE_URL = "https://api.github.com/";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new GsonBuilder()
            .setDateFormat("YYYY-MM-DD'T'HH:MM:SSZ")
            .create();

    private final Context context;
    private final Consumer<UpdatePackage> updateFoundHandler;

    public UpdateManager(Context context, Consumer<UpdatePackage> updateFoundHandler) {
        this.context = context;
        this.updateFoundHandler = updateFoundHandler;
    }

    public void checkForUpdates(String user, String repo) {
        get(GITHUB_API_BASE_URL + "repos/" + user + "/" + repo + "/releases/latest", Release.class, this::handleReleaseResponse);
    }

    public void installMappings(UpdatePackage updatePackage, Consumer<Boolean> callback) {
        client.newCall(new Request.Builder().url(updatePackage.mappingsUrl).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.consume(false);
                Log.e(TAG, "Error downloading latest mappings.", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body == null) {
                    callback.consume(false);
                    Log.e(TAG, "Mappings response body was null.");
                    return;
                }

                try (FileOutputStream out = new FileOutputStream(updatePackage.mappingsFile)) {
                    StreamUtils.copyTo(body.byteStream(), out);
                }

                callback.consume(true);
            }
        });
    }

    public void downloadApk(UpdatePackage updatePackage) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(new DownloadManager.Request(Uri.parse(updatePackage.apkUrl))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, context.getApplicationInfo().name + "-" + updatePackage.release.tag_name + ".apk")
                .setTitle(context.getApplicationInfo().name + " " + updatePackage.release.tag_name));
    }

    private void handleReleaseResponse(Release release) {
        Release.ReleaseAsset versionAsset = findAsset(release, name -> name.equals("version.json"));
        if (versionAsset == null) {
            Log.e(TAG, "No version information for latest release (" + release.tag_name + ").");
            return;
        }

        get(versionAsset.browser_download_url, VersionInfo.class, v -> handleVersionInfo(release, v));
    }

    private void handleVersionInfo(Release release, VersionInfo version) {
        int currentVersionCode = getVersionCode();
        if (currentVersionCode == 0) {
            return;
        }

        long releaseTime = release.created_at.getTime();
        long mappingsTime = 0;
        List<File> possible = PathUtils.getPossibleMappingFiles(context, version.build + ".json");
        File mappingsFile = possible.get(0);
        for (File file : possible) {
            if (file.exists()) {
                mappingsFile = file;
                mappingsTime = file.lastModified();
                break;
            }
        }

        String mappingsUrl = null;
        Release.ReleaseAsset mappingsAsset = findAsset(release, name -> name.endsWith(".json") && !name.equals("version.json"));
        if (mappingsAsset != null && releaseTime > mappingsTime) {
            mappingsUrl = mappingsAsset.browser_download_url;
            Log.d(TAG, "Release mappings are newer than local mappings.");
        }

        String apkUrl = null;
        Release.ReleaseAsset apkAsset = findAsset(release, name -> name.endsWith(".apk"));
        if (apkAsset != null && version.versionCode > currentVersionCode) {
            apkUrl = apkAsset.browser_download_url;
            Log.d(TAG, "Release APK newer than installed.");
        }

        if (updateFoundHandler != null) {
            updateFoundHandler.consume(new UpdatePackage(release, mappingsUrl, apkUrl, mappingsFile));
        }
    }

    private Release.ReleaseAsset findAsset(Release release, Predicate<String> search) {
        for (Release.ReleaseAsset asset : release.assets) {
            if (search.test(asset.name)) {
                return asset;
            }
        }

        return null;
    }

    private <T> void get(String url, Class<T> type, Consumer<T> callback) {
        client.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error getting " + url + ".", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "Null response body for " + url + ".");
                    return;
                }

                callback.consume(gson.fromJson(body.charStream(), type));
            }
        });
    }

    private int getVersionCode() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting version code.", e);
            return 0;
        }
    }
}
