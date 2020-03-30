package co.paralleluniverse.common.reflection;

import java.lang.reflect.Field;

public class GetAccessDeclaredField extends GetDeclaredField {

    public GetAccessDeclaredField(Class<?> clazz, String fieldName) {
        super(clazz, fieldName);
    }

    @Override
    public Field run() throws NoSuchFieldException {
        Field field = super.run();
        field.setAccessible(true);
        return field;
    }
}
