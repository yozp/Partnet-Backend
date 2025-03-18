package com.yzj.partnetBackend;

import com.yzj.partnetBackend.mapper.UserMapper;
import com.yzj.partnetBackend.model.User;
import  org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.util.List;

@SpringBootTest
public class PartnetBackendApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    public void contextLoads() {

        List<User> userList=userMapper.selectList(null);
        userList.forEach(System.out::println);
    }

    @Test
    void testDigest(){
        String s = DigestUtils.md5DigestAsHex(("123456" + "mypassword").getBytes());
        System.out.println("s = " + s);

    }

}
