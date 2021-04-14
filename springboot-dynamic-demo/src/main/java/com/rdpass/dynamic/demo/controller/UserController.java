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
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;

@Api(value = "UserController", tags = "用户管理Api")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    @ApiOperation(nickname = "get", value = "根据ID获取用户")
    @GetMapping("get")
    public String get(@RequestParam Long id){
        return userService.get(id).toString();
    }

}
