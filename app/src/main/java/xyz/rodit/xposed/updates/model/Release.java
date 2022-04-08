package xyz.rodit.xposed.updates.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Release {

    public String url;
    public String tag_name;
    public String name;
    public String body;
    public Date created_at;
    public List<ReleaseAsset> assets = new ArrayList<>();

    public static class ReleaseAsset {

        public String name;
        public String browser_download_url;
    }
}
