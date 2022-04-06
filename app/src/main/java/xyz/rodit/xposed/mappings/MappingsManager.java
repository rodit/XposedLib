package xyz.rodit.xposed.mappings;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import xyz.rodit.xposed.utils.StreamUtils;

public class MappingsManager {

    private final File dir;

    public MappingsManager(Context context) {
        this(new File(context.getFilesDir(), "mappings"));
    }

    public MappingsManager(File dir) {
        this.dir = dir;

        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public List<Integer> getAvailable() {
        List<Integer> available = new ArrayList<>();
        File[] files = dir.listFiles(f -> f.getName().endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    available.add(Integer.valueOf(file.getName().split("\\.")[0]));
                } catch (NumberFormatException e) {
                    // ignored
                }
            }
        }

        Collections.sort(available);
        return available;
    }

    public File getMappingsFile(int versionCode) {
        return new File(dir, versionCode + ".json");
    }

    public String get(int versionCode) {
        try {
            return StreamUtils.readFile(getMappingsFile(versionCode));
        } catch (IOException e) {
            return null;
        }
    }

    public void put(int versionCode, String content) {
        try {
            StreamUtils.writeFile(getMappingsFile(versionCode), content);
        } catch (IOException e) {
            XposedBridge.log("Error putting mappings in cache.");
            e.printStackTrace();
        }
    }
}
