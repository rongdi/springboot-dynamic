package com.rdpaas.demo.ext.mapper;

import com.rdpaas.demo.ext.entity.User1;
import org.springframework.stereotype.Repository;

@Repository
public interface User1Mapper {

    User1 get(Long id);
}
