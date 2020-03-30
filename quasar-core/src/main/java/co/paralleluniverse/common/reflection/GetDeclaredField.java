package co.paralleluniverse.common.reflection;

import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;

public class GetDeclaredField implements PrivilegedExceptionAction<Field> {
    private final Class<?> clazz;
    private final String fieldName;

    public GetDeclaredField(Class<?> clazz, String fieldName) {
        this.clazz = clazz;
        this.fieldName = fieldName;
    }

    @Override
    public Field run() throws NoSuchFieldException {
        return clazz.getDeclaredField(fieldName);
    }
}
