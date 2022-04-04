package xyz.rodit.xposed.utils;

@FunctionalInterface
public interface Consumer<T> {

    void consume(T value);
}
