package com.rdpass.dynamic.demo.mapper;

import com.rdpass.dynamic.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User get(Long id);
}
