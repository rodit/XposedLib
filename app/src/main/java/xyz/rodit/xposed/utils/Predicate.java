package xyz.rodit.xposed.utils;

@FunctionalInterface
public interface Predicate<T> {

    boolean test(T object);
}
