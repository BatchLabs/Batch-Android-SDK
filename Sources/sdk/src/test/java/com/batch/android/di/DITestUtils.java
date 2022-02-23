package com.batch.android.di;

import java.lang.reflect.Method;
import org.mockito.Mockito;

public class DITestUtils {

    /**
     * Method to mock a singleton dependency
     * Must be used at the beginning of tests or after {@link DI#reset()}
     * Must be in com.batch.android.di package to access DI protected methods and fields
     *
     * @param clazz
     * @param params
     * @param <T>
     * @return
     */
    public static <T> T mockSingletonDependency(Class<T> clazz, Class<?>[] paramsType, Object... params) {
        try {
            // Try to call the static get method
            Class providerClass = Class.forName("com.batch.android.di.providers." + clazz.getSimpleName() + "Provider");
            Method getMethod;
            if (params.length > 0) {
                getMethod = providerClass.getMethod("get", paramsType);
            } else {
                getMethod = providerClass.getMethod("get");
            }

            T newInstance = (T) getMethod.invoke(null, params);

            T mock = Mockito.spy(newInstance);
            DI.getInstance().addSingletonInstance(clazz, mock);
            return mock;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Can't mock " + clazz.getName() + ": it's not annotated with @Module: ");
        }
    }
}
