package com.rdpass.dynamic.demo.service;

import com.rdpass.dynamic.demo.entity.User;
import com.rdpass.dynamic.demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    public User get(Long id){
        return userMapper.get(id);
    }
}
