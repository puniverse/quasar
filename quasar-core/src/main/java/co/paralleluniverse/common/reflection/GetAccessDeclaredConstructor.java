package co.paralleluniverse.common.reflection;

import java.lang.reflect.Constructor;
import java.security.PrivilegedExceptionAction;

public class GetAccessDeclaredConstructor<T> implements PrivilegedExceptionAction<Constructor<T>> {
    private final Class<T> clazz;
    private final Class<?>[] args;

    public GetAccessDeclaredConstructor(Class<T> clazz, Class<?>... args) {
        this.clazz = clazz;
        this.args = args;
    }

    @Override
    public Constructor<T> run() throws NoSuchMethodException {
        Constructor<T> constructor = clazz.getDeclaredConstructor(args);
        constructor.setAccessible(true);
        return constructor;
    }
}
