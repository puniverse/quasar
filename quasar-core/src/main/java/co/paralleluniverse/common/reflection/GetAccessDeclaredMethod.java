package co.paralleluniverse.common.reflection;

import java.lang.reflect.Method;

public class GetAccessDeclaredMethod extends GetDeclaredMethod {

    public GetAccessDeclaredMethod(Class<?> clazz, String methodName, Class<?>... args) {
        super(clazz, methodName, args);
    }

    @Override
    public Method run() throws NoSuchMethodException {
        Method method = super.run();
        method.setAccessible(true);
        return method;
    }
}
