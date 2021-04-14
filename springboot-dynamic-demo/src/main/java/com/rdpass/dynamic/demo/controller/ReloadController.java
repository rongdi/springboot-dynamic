package com.rdpass.dynamic.demo.controller;

import com.rdpaas.dynamic.core.ModuleApplication;
import com.rdpass.dynamic.demo.service.UserService;
import com.sun.webkit.network.URLs;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "ReloadController", tags = "刷新扩展包Api")
@RestController
@RequestMapping("/reload")
public class ReloadController implements ApplicationContextAware {

    @Autowired
    private ModuleApplication moduleApplication;

    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    @Value("${dynamic.jar}")
    private String dynamicJar;

    private ApplicationContext applicationContext;

    @ApiOperation(nickname = "reload", value = "刷新容器")
    @GetMapping()
    public String get() throws Exception {
        moduleApplication.reloadJar(URLs.newURL(dynamicJar),applicationContext,sqlSessionFactory);
        return "ok";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
