package xyz.rodit.xposed.utils;

@FunctionalInterface
public interface Supplier<T> {

    T get();
}
