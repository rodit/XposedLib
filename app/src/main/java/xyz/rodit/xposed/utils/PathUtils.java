package xyz.rodit.xposed.utils;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PathUtils {

    public static List<File> getPossibleMappingFiles(Context context, String name) {
        List<File> files = new ArrayList<>();
        files.add(new File(context.getFilesDir(), name));
        files.add(new File(context.getExternalFilesDir(null), name));
        return files;
    }
}
