package com.springboot.module.auth.service.impl;

import static com.springboot.constant.UserConstant.USER_LOGIN_STATE;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.springboot.common.ErrorCode;
import com.springboot.common.enums.UserRoleEnum;
import com.springboot.constant.UserConstant;
import com.springboot.exception.BusinessException;
import com.springboot.mapper.system.ClassInfoMapper;
import com.springboot.mapper.system.StudentMapper;
import com.springboot.mapper.system.UserMapper;
import com.springboot.model.dto.auth.StudentRegisterRequest;
import com.springboot.model.entity.system.ClassInfo;
import com.springboot.model.entity.system.Student;
import com.springboot.model.entity.system.User;
import com.springboot.model.vo.auth.LoginUserVO;
import com.springboot.model.vo.auth.UserVO;
import com.springboot.module.auth.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    public static final String SALT = "springboot";

    private static final int MIN_USERNAME_LENGTH = 4;

    private static final int MIN_PASSWORD_LENGTH = 6;

    @Resource
    private StudentMapper studentMapper;

    @Resource
    private ClassInfoMapper classInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        validateCredentialInput(userAccount, userPassword, checkPassword);
        ensureUsernameUnique(userAccount);

        User user = new User();
        user.setId(IdWorker.getId());
        user.setUsername(userAccount);
        user.setPasswordHash(encryptPassword(userPassword));
        user.setRealName(userAccount);
        user.setRole(UserConstant.DEFAULT_ROLE);
        user.setStatus(1);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long registerStudent(StudentRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        validateCredentialInput(request.getUsername(), request.getPassword(), request.getCheckPassword());
        if (StringUtils.isBlank(request.getRealName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "真实姓名不能为空");
        }
        if (StringUtils.isBlank(request.getStudentNo())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号不能为空");
        }
        if (StringUtils.isBlank(request.getClassName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "班级不能为空");
        }

        ensureUsernameUnique(request.getUsername());
        ensureStudentNoUnique(request.getStudentNo());

        QueryWrapper<ClassInfo> classQueryWrapper = new QueryWrapper<>();
        classQueryWrapper.eq("class_name", request.getClassName());
        ClassInfo classInfo = classInfoMapper.selectOne(classQueryWrapper);
        if (classInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "班级不存在，请联系教师确认班级信息");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(encryptPassword(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(UserConstant.STUDENT_ROLE);
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        boolean userSaved = this.save(user);
        if (!userSaved) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建用户失败");
        }

        Student student = new Student();
        student.setUserId(user.getId());
        student.setStudentNo(request.getStudentNo());
        student.setStudentName(request.getRealName());
        student.setGender(StringUtils.trimToNull(request.getGender()));
        student.setClassId(classInfo.getId());
        int insertResult = studentMapper.insert(student);
        if (insertResult <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建学生信息失败");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String username, String password, HttpServletRequest request) {
        if (StringUtils.isAnyBlank(username, password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null || !encryptPassword(password).equals(user.getPasswordHash())) {
            log.info("user login failed, username or password not match, username={}", username);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用");
        }

        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        currentUser = this.getById(currentUser.getId());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        return this.getById(currentUser.getId());
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getRole());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    private void validateCredentialInput(String username, String password, String checkPassword) {
        if (StringUtils.isAnyBlank(username, password, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (username.length() < MIN_USERNAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || checkPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!password.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
    }

    private void ensureUsernameUnique(String username) {
        synchronized (username.intern()) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
        }
    }

    private void ensureStudentNoUnique(String studentNo) {
        QueryWrapper<Student> studentQueryWrapper = new QueryWrapper<>();
        studentQueryWrapper.eq("student_no", studentNo);
        long count = studentMapper.selectCount(studentQueryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "学号已存在");
        }
    }

    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes());
    }
}
