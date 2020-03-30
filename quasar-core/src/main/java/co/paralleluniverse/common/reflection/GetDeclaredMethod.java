package co.paralleluniverse.common.reflection;

import java.lang.reflect.Method;
import java.security.PrivilegedExceptionAction;

public class GetDeclaredMethod implements PrivilegedExceptionAction<Method> {
    private final Class<?> clazz;
    private final String methodName;
    private final Class<?>[] args;

    public GetDeclaredMethod(Class<?> clazz, String methodName, Class<?>... args) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.args = args;
    }

    @Override
    public Method run() throws NoSuchMethodException {
        return clazz.getDeclaredMethod(methodName, args);
    }
}
