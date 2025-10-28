package com.yzj.partnetBackend.easyExcel;

import com.yzj.partnetBackend.model.User;
import com.yzj.partnetBackend.service.UserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户插入测试单元
 */
@SpringBootTest
class InsertUsersTest {

    @Resource
    private UserService userService;

    //线程设置
    private ExecutorService executorService = new ThreadPoolExecutor(16, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));

    /**
     * 循环插入用户  耗时：7260ms
     * 批量插入用户   1000  耗时： 4751ms
     */
    @Test
    public void doInsertUser() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
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

            userList.add(user);
        }
        userService.saveBatch(userList,100);
        stopWatch.stop();
        System.out.println( stopWatch.getStartTime());

    }

    /**
     * 并发批量插入用户
     * Concurrent:并发
     */
    @Test
    public void doConcurrencyInsertUser(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;

        //分组标记
        int j=0;// 分成10组
        //每组插入5000条数据
        int batchSize=5000;
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for(int i=0;i<INSERT_NUM/batchSize;i++){
            List<User> userList = new ArrayList<>();
            while(true){
                j++;

                User user = new User();
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

                userList.add(user);
                //够5000条就退出
                if(j%batchSize==0){
                    break;
                }
            }


            //异步执行
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->{
                System.out.println("ThreadName：" + Thread.currentThread().getName());
                userService.saveBatch(userList,batchSize);
            },executorService);
            futureList.add(future);
        }

        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println( stopWatch.getStartTime());
    }

}