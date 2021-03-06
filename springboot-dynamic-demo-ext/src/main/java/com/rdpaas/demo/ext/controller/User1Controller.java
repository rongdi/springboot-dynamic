package com.rdpaas.demo.ext.controller;

import com.rdpaas.demo.ext.entity.User1;
import com.rdpaas.demo.ext.service.User1Service;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@ResponseBody
@RequestMapping("/user1")
@Api(value = "TestController", tags = "扩展用户")
public class User1Controller {

    @Autowired
    private User1Service user1Service;

    @GetMapping("/get")
    public User1 test(@RequestParam("id") Long id) {
        return user1Service.get(id);
    }

}
