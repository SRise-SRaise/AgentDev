package com.springboot.model.dto.auth;

import java.io.Serializable;
import lombok.Data;

/**
 * 学生注册请求
 */
@Data
public class StudentRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;

    private String realName;

    private String password;

    private String checkPassword;

    private String phone;

    private String email;

    private String studentNo;

    private String gender;

    private String className;
}
