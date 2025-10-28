package com.yzj.partnetBackend.easyExcel;

import com.yzj.partnetBackend.mapper.UserMapper;
import com.yzj.partnetBackend.model.User;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 编写定时任务代码并进行测试（这里的定时取巧，尽量别用，注释掉。）
 */
@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    /**
     * 循环插入用户
     */
    public void doInsertUser(){
        StopWatch stopWatch=new StopWatch();
        stopWatch.start();
        final int INSERT_NUM=1000;
        for(int i=0;i<INSERT_NUM;i++){
            User user=new User();
            user.setUsername("ikun");
            user.setUserAccount("ikun");
            user.setAvatarUrl("https://s1.aigei.com/prevfiles/8007398ad64b495eb74a5d1e78de7169.jpg?e=2051020800&token=P7S2Xpzfz11vAkASLTkfHN7Fw-oOZBecqeJaxypL:5vcClcGvFWXQhkWsCOf7ZEws3b0=");
            user.setGender(1);
            user.setProfile("真爱粉");
            user.setUserPassword("12345678");
            user.setPhone("114514");
            user.setEmail("1919810@qq.com");
            user.setUserStatus(0);
            user.setUserRole(0);
            user.setPlanetCode("514");
            user.setTags("[]");

            userMapper.insert(user);
        }

        stopWatch.stop();
        System.out.println(stopWatch.getLastTaskTimeMillis());
    }

}
