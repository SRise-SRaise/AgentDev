package com.springboot.service.impl;

import com.springboot.mapper.system.UserMapper;
import com.springboot.model.entity.system.User;
import com.springboot.module.auth.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void userRegisterShouldGenerateUserNameWithId() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        long userId = userService.userRegister("testAccount", "12345678", "12345678");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        Assertions.assertNotNull(savedUser.getId());
        Assertions.assertEquals("用户" + savedUser.getId(), savedUser.getUserName());
        Assertions.assertEquals(savedUser.getId(), userId);
    }
}
