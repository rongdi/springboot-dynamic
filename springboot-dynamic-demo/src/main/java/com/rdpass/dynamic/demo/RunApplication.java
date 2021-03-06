package com.rdpass.dynamic.demo;

import com.rdpaas.dynamic.anno.EnableDynamic;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.rdpass.dynamic.demo.mapper")
@SpringBootApplication
@EnableDynamic
public class RunApplication {
    public static void main(String[] args) {
        SpringApplication.run(RunApplication.class);
    }
}
