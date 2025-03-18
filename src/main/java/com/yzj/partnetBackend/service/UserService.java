package com.yzj.partnetBackend.service;

import com.yzj.partnetBackend.model.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author 杨钲键
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-19 10:23:14
*/
public interface UserService extends IService<User> {

    //用户登录态键
    String USER_LOGIN_STATE="已登录";

    long userRegister(String userAccount ,String userPassword,String checkPassword,String planetCode);

    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    //用户脱敏公共接口
    User getSafetyUser(User originUser);

    int userLogout(HttpServletRequest request);

    List<User> searchUsersByTags(List<String> tagNameList);

    User getLogininUser(HttpServletRequest request);

    int userUpdate(User user, User loginUser);

    boolean isAdmin(HttpServletRequest request);

    boolean isAdmin(User loginUser);
}
