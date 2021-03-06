package com.rdpass.dynamic.demo.controller;

import com.rdpass.dynamic.demo.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
