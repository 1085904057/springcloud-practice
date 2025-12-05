package com.spring.cloud.practice.service.user.controller;

import com.spring.cloud.practice.user.api.contract.UserContract;
import com.spring.cloud.practice.user.api.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserInternalController implements UserContract {


    private static final Logger log = LoggerFactory.getLogger(UserInternalController.class);

    @Override
    public UserDTO getUserById(@PathVariable Long id) {
        log.info("getUserById: {}", id);
        UserDTO userDTO = new UserDTO();
        userDTO.setId(id);
        userDTO.setName("Robin");
        return userDTO;
    }
}
