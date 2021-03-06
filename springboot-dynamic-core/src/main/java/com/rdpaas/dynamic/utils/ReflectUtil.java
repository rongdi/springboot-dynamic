package com.rdpaas.dynamic.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射工具包
 * @author rongdi
 * @date 2021-03-06
 * @blog https://www.cnblogs.com/rongdi
 */
public class ReflectUtil {

    /**
     * 往父类直到查询到指定方法
     * @param object
     * @param methodName
     * @return
     */
    public static Method getMethod(Object object, String methodName,Class<?>... parameterTypes){
        Method method = null ;
        Class<?> clazz = object.getClass() ;
        for(; clazz != Object.class ; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName,parameterTypes) ;
                return method ;
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * 往父类直到查询到指定属性
     * @param object
     * @param fieldName
     * @return
     */
    public static Field getField(Object object, String fieldName){
        Field field = null ;
        Class<?> clazz = object.getClass() ;
        for(; clazz != Object.class ; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName) ;
                return field ;
            } catch (Exception e) {
            }
        }
        return null;
    }

}
