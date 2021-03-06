package com.rdpaas.dynamic.config;

import com.rdpaas.dynamic.core.ModuleApplication;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URL;

/**
 * 一切配置的入口
 * @author rongdi
 * @date 2021-03-06
 * @blog https://www.cnblogs.com/rongdi
 */
@Configuration
public class DynamicConfig implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(DynamicConfig.class);

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    private ApplicationContext applicationContext;

    @Value("${dynamic.jar:/}")
    private String dynamicJar;

    @Bean
    public ModuleApplication moduleApplication() throws Exception {
        return new ModuleApplication();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 随便找个事件ApplicationStartedEvent，用来reload外部的jar，其实直接在moduleApplication()方法也可以做
     * 这件事，但是为了验证容器初始化后再加载扩展包还可以生效，所以故意放在了这里。
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "dynamic",name = "jar")
    public ApplicationListener applicationListener1() {
        return (ApplicationListener<ApplicationStartedEvent>) event -> {
            try {
                /**
                 * 加载外部扩展jar
                 */
                moduleApplication().reloadJar(new URL(dynamicJar),applicationContext,sqlSessionFactory);
            } catch (Exception e) {
                logger.error("",e);
            }

        };
    }


}
