package xyz.rodit.xposed.services;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface MappingCallback {

    void onMappingsReceived(@NotNull String mappings);
}
