package gg.topchdlc.api.scripts.api.integrate;

@FunctionalInterface
public interface IHook<T> {
    Object process(T val);
}
