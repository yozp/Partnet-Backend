package com.yzj.partnetBackend.service;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yzj.partnetBackend.mapper.UserMapper;
import com.yzj.partnetBackend.model.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

//import javax.annotation.Resource;//jdk1.8版本

@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Test
    public void testAddUser(){
        User user=new User();
        user.setUsername("wangwu");
        user.setUserAccount("13672620892");
        user.setAvatarUrl("");
        user.setGender(0);
        user.setUserPassword("123456");
        user.setPhone("");
        user.setEmail("");
        user.setUserStatus(0);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);
        user.setUserRole(0);
        user.setPlanetCode("");

        userService.save(user);
    }

//    @Test
//    void userRegister() {
//        String userAccount = "yupi";
//        String userPassword = "";
//        String checkPassword = "123456";
//        long result = userService.userRegister(userAccount, userPassword, checkPassword);
//        Assertions.assertEquals(-1, result);
//
//        userAccount = "yu";
//        result = userService.userRegister(userAccount, userPassword, checkPassword);
//        Assertions.assertEquals(-1, result);
//
//        userAccount = "yupi";
//        userPassword = "123456";
//        result = userService.userRegister(userAccount, userPassword, checkPassword);
//        Assertions.assertEquals(-1, result);
//
//        userAccount = "yu pi";
//        userPassword = "12345678";
//        result = userService.userRegister(userAccount, userPassword, checkPassword);
//        Assertions.assertEquals(-1, result);
//
//        checkPassword = "123456789";
//        result = userService.userRegister(userAccount, userPassword, checkPassword);
//        Assertions.assertEquals(-1, result);
//
//        //到了这里会报错，因为数据库中没有dogYupi这个账户，设置与其返回值为-1，所有报错
//        //因为密码没有重新赋值，将上面的密码也带到这里来了
//        //就是说插入成功了，返回值不是-1，与期望值-1相违背，所以报错
//        userAccount = "dogyupi";
//        checkPassword = "12345678";
//        result = userService.userRegister(userAccount, userPassword, checkPassword);
//        Assertions.assertEquals(-1, result);
//
//        //这里也会报错，原因是跟上面的一样，都是没有将密码置空
//        userAccount = "yupi";
//        result = userService.userRegister(userAccount, userPassword, checkPassword);
//        Assertions.assertTrue(result > 0);
//    }

    @Test
    public void findUserTest(){
        User user=new User();
        LambdaQueryWrapper<User> lambdaQueryWrapper=new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(User::getUserAccount,"xiaoming");
        lambdaQueryWrapper.eq(User::getUserPassword,"372e3c856e9876c7da2af8169abb861c");
        user=userMapper.selectOne(lambdaQueryWrapper);
        System.out.println(user);
    }

    /**
     * 跟据tags查询用户
     */
    @Test
    void testSearchUsersByTags(){
        List<String > tagNameList= Arrays.asList("java","python");
        List<User> userList=userService.searchUsersByTags(tagNameList);
        System.out.println(userList);
    }

}