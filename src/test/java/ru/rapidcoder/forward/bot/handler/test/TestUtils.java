package ru.rapidcoder.forward.bot.handler.test;

import java.lang.reflect.Method;

public class TestUtils {

    public static Object callPrivateMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass()
                .getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
