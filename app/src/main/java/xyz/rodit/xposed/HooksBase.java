package xyz.rodit.xposed;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.rodit.dexsearch.client.Mappings;
import xyz.rodit.xposed.client.ConfigurationClient;
import xyz.rodit.xposed.client.FileClient;
import xyz.rodit.xposed.client.http.StreamServer;
import xyz.rodit.xposed.mappings.LoadScheme;
import xyz.rodit.xposed.mappings.MappingsManager;
import xyz.rodit.xposed.utils.BuildUtils;

public abstract class HooksBase implements IXposedHookLoadPackage {

    private static final int DEFAULT_SERVER_THREAD_POOL_SIZE = 8;

    private final Set<String> packageNames;
    private final EnumSet<LoadScheme> mappingsLoadSchemes;
    private final String configurationPackageName;
    private final String configurationActionName;
    private final String contextHookClassName;
    private final String contextHookMethodName;

    protected XC_LoadPackage.LoadPackageParam lpparam;
    protected Context appContext;
    protected ConfigurationClient config;
    protected FileClient files;
    protected StreamServer server;

    protected MappingsManager mappingsManager;
    protected int versionCode;

    protected boolean contextHooked;
    protected boolean mappingsLoaded;
    private boolean firstConfig = true;

    public HooksBase(@NonNull Collection<String> packageNames) {
        this(packageNames, EnumSet.of(LoadScheme.CACHED_ON_CONTEXT, LoadScheme.SERVICE));
    }

    public HooksBase(@NonNull Collection<String> packageNames, @NonNull EnumSet<LoadScheme> mappingsLoadSchemes) {
        this(packageNames, mappingsLoadSchemes, null, null, null, null);
    }

    public HooksBase(@NonNull Collection<String> packageNames, @NonNull EnumSet<LoadScheme> mappingsLoadSchemes, @Nullable String configurationPackageName, @Nullable String configurationActionName, @Nullable String contextHookClassName, @Nullable String contextHookMethodName) {
        this.packageNames = new HashSet<>(packageNames);
        this.mappingsLoadSchemes = mappingsLoadSchemes;
        this.configurationPackageName = configurationPackageName;
        this.configurationActionName = configurationActionName;
        this.contextHookClassName = contextHookClassName;
        this.contextHookMethodName = contextHookMethodName;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!packageNames.contains(lpparam.packageName)) {
            return;
        }

        this.lpparam = lpparam;
        onPackageLoad();

        if (mappingsLoadSchemes.contains(LoadScheme.CACHED_IMMEDIATE)) {
            mappingsManager = new MappingsManager(new File("/data/data/" + lpparam.packageName + "/files/mappings"));
            List<Integer> available = mappingsManager.getAvailable();
            if (!available.isEmpty()) {
                // Try and load latest available mappings
                loadMappings(mappingsManager.get(available.get(available.size() - 1)));
            } else {
                XposedBridge.log("Could not immediately load latest cached mappings. No available mappings files were found.");
            }
        }

        if (contextHookClassName != null) {
            Class<?> contextClass = XposedHelpers.findClass(contextHookClassName, lpparam.classLoader);
            XposedBridge.hookAllMethods(contextClass, contextHookMethodName, createContextHook());
        } else {
            performHooksSafe();
        }
    }

    protected void bindConfigurationService() {
        if (configurationPackageName != null && configurationActionName != null) {
            config = new ConfigurationClient(appContext,
                    configurationPackageName,
                    configurationActionName,
                    this::handleServiceConfiguration,
                    this::handleServiceMappings);
            config.bind();
        }
    }

    protected boolean requireFileService(@NonNull String downloadActionName) {
        if (appContext != null && configurationPackageName != null) {
            files = new FileClient(appContext, configurationPackageName, downloadActionName);
            return files.bind();
        }

        return false;
    }

    protected boolean requireStreamServer(int port) {
        try {
            server = new StreamServer(port, DEFAULT_SERVER_THREAD_POOL_SIZE);
            server.start();
            return true;
        } catch (IOException e) {
            XposedBridge.log("Error starting stream server.");
            XposedBridge.log(e);
        }

        return false;
    }

    private void handleServiceMappings(String content) {
        // Store latest service mappings in cache
        mappingsManager.put(versionCode, content);
        // Load service mappings if that is one of the schemes.
        if (mappingsLoadSchemes.contains(LoadScheme.SERVICE)) {
            loadMappings(content);
        }
    }

    private void handleServiceConfiguration() {
        this.onConfigLoaded(firstConfig);
        firstConfig = false;
    }

    private void loadMappings(String content) {
        // Only load mappings if they were not successfully loaded at a previous stage.
        if (mappingsLoaded) {
            return;
        }

        try (InputStream in = new ByteArrayInputStream(content.getBytes())) {
            Mappings.loadMappings(lpparam.classLoader, in);
        } catch (IOException e) {
            XposedBridge.log("Error loading mappings.");
            XposedBridge.log(e);
        }

        performHooksSafe();
        mappingsLoaded = true;
    }

    private void performHooksSafe() {
        try {
            performHooks();
        } catch (Throwable t) {
            XposedBridge.log("Error performing user defined hooks.");
            XposedBridge.log(t);
        }
    }

    private XC_MethodHook createContextHook() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log("Context hook " + param.thisObject + ", " + Arrays.toString(param.args));
                if (contextHooked) {
                    return;
                }

                if (param.thisObject instanceof Context) {
                    Context context = (Context) param.thisObject;
                    appContext = context.getApplicationContext();
                    if (appContext == null) {
                        appContext = context;
                    }

                    versionCode = BuildUtils.getBuildVersion(appContext);
                    bindConfigurationService();
                    onContextHook(context);

                    mappingsManager = new MappingsManager(appContext);
                    if (mappingsLoadSchemes.contains(LoadScheme.CACHED_ON_CONTEXT)) {
                        String cached = mappingsManager.get(versionCode);
                        if (cached != null) {
                            loadMappings(cached);
                            XposedBridge.log("Loaded cached mappings for build " + versionCode + ".");
                        }
                    }

                    contextHooked = true;
                } else {
                    XposedBridge.log("Failed to acquire application context from context hook method.");
                }
            }
        };
    }

    // User module callbacks
    protected void onPackageLoad() {

    }

    protected void onContextHook(Context contextObject) {

    }

    protected void onConfigLoaded(boolean first) {

    }

    protected abstract void performHooks() throws Throwable;
}
