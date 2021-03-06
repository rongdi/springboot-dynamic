package com.rdpass.dynamic.demo.config;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {


    @Value("${swagger.basePackage:}")
    private String swaggerBasePackage;


    /**
     * 构建api文档的详细信息函数
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                //页面标题
                .title("SpringBoot动态扩展")
                //创建人
                .contact(new Contact("rongdi", "https://www.cnblogs.com/rongdi", "495194630@qq.com"))
                //版本号
                .version("1.0")
                //描述
                .description("api管理").build();
    }

    /**
     * 支持多包扫描
     *
     * @return
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo()).select()
                .apis(basePackage(swaggerBasePackage))
                .paths(PathSelectors.any())
                .build();
    }


    public static Predicate<RequestHandler> basePackage(final String basePackage) {
        return new Predicate<RequestHandler>() {
            @Override
            public boolean apply(RequestHandler input) {
                return declaringClass(input).transform(handlerPackage(basePackage)).or(true);
            }
        };
    }

    private static Function<Class<?>, Boolean> handlerPackage(final String basePackage) {
        String[] arrays = basePackage.split(";");
        return input -> {
            for (String array : arrays) {
                if (input.getPackage() != null && input.getPackage().getName().startsWith(array)) {
                    return true;
                }
            }
            return false;
        };
    }


    private static Optional<? extends Class<?>> declaringClass(RequestHandler input) {
        return Optional.fromNullable(input.declaringClass());
    }


}