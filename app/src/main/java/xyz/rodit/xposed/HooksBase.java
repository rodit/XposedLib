package xyz.rodit.xposed;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
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

public abstract class HooksBase implements IXposedHookLoadPackage {

    private static final int DEFAULT_SERVER_THREAD_POOL_SIZE = 8;

    private final Set<String> packageNames;
    private final String configurationPackageName;
    private final String configurationActionName;
    private final String contextHookClassName;
    private final String contextHookMethodName;

    protected XC_LoadPackage.LoadPackageParam lpparam;
    protected Context appContext;
    protected ConfigurationClient config;
    protected FileClient files;
    protected StreamServer server;

    private boolean firstConfig = true;

    public HooksBase(@NonNull Collection<String> packageNames) {
        this(packageNames, null, null, null, null);
    }

    public HooksBase(@NonNull Collection<String> packageNames, @Nullable String configurationPackageName, @Nullable String configurationActionName, @Nullable String contextHookClassName, @Nullable String contextHookMethodName) {
        this.packageNames = new HashSet<>(packageNames);
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
                    () -> {
                        this.onConfigLoaded(firstConfig);
                        firstConfig = false;
                    },
                    this::loadMappings);
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

    private void loadMappings(String content) {
        try (InputStream in = new ByteArrayInputStream(content.getBytes())) {
            Mappings.loadMappings(lpparam.classLoader, in);
        } catch (IOException e) {
            XposedBridge.log("Error loading mappings.");
            XposedBridge.log(e);
        }

        performHooksSafe();
    }

    protected void onPackageLoad() {

    }

    protected void onContextHook(Context contextObject) {

    }

    protected void onConfigLoaded(boolean first) {

    }

    private void performHooksSafe() {
        try {
            performHooks();
        } catch (Throwable t) {
            XposedBridge.log("Error performing user defined hooks.");
            XposedBridge.log(t);
        }
    }

    protected abstract void performHooks() throws Throwable;

    protected XC_MethodHook createContextHook() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.thisObject instanceof Context) {
                    Context context = (Context) param.thisObject;
                    appContext = context.getApplicationContext();
                    bindConfigurationService();
                    onContextHook(context);
                } else {
                    XposedBridge.log("Failed to acquire application context from context hook method.");
                }
            }
        };
    }
}
