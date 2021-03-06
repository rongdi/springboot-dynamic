package com.rdpaas.demo.ext.service;

import com.rdpaas.demo.ext.entity.User1;
import com.rdpaas.demo.ext.mapper.User1Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class User1Service {

    @Autowired
    private User1Mapper user1Mapper;

    @Transactional
    public User1 get(Long id) {
        return user1Mapper.get(id);
    }

}
