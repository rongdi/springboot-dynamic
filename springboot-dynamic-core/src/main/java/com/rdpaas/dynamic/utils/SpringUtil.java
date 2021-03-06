package com.rdpaas.dynamic.utils;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Modifier;

/**
 * spring工具类
 * @author rongdi
 * @date 2021-03-06
 * @blog https://www.cnblogs.com/rongdi
 */
public class SpringUtil {

    /**
     * 判断class对象是否带有spring的注解
     */
    public static boolean isSpringBeanClass(Class<?> cla) {
        /**
         * 如果为空或是接口或是抽象类直接返回false
         */
        if (cla == null || cla.isInterface() || Modifier.isAbstract(cla.getModifiers())) {
            return false;
        }

        Class targetClass = cla;
        while(targetClass != null) {
            /**
             * 如果包含spring注解则返回true
             */
            if (targetClass.isAnnotationPresent(Component.class) ||
                    targetClass.isAnnotationPresent(Repository.class) ||
                    targetClass.isAnnotationPresent(Service.class) ||
                    targetClass.isAnnotationPresent(Configuration.class) ||
                    targetClass.isAnnotationPresent(Controller.class) ||
                    targetClass.isAnnotationPresent(RestController.class)) {
                return true;
            }
            targetClass = targetClass.getSuperclass();
        }

        return false;
    }

}
