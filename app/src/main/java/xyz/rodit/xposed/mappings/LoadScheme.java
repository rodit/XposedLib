package xyz.rodit.xposed.mappings;

public enum LoadScheme {
    // Attempt to load latest mappings immediately on package load (very unsafe, no context available yet)
    CACHED_IMMEDIATE,
    // Attempt to load cached mappings as soon as context is acquired (by hooking user provided context class) and acquiring correct build number
    CACHED_ON_CONTEXT,
    // Load mappings when configuration service is bound
    SERVICE
}
