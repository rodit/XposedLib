package xyz.rodit.xposed.updates.model;

import java.io.File;

public class UpdatePackage {

    public final Release release;
    public final String mappingsUrl;
    public final String apkUrl;
    public final File mappingsFile;

    public UpdatePackage(Release release, String mappingsUrl, String apkUrl, File mappingsFile) {
        this.release = release;
        this.mappingsUrl = mappingsUrl;
        this.apkUrl = apkUrl;
        this.mappingsFile = mappingsFile;
    }
}
