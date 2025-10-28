package com.yzj.partnetBackend.service;

import com.yzj.partnetBackend.model.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedisTest {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Test
    void test01(){
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("shayuString", "fish");
        valueOperations.set("shayuInt", 1);
        valueOperations.set("shayuDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("shayu");
        valueOperations.set("shayuUser", user);

        // 查
        Object shayu = valueOperations.get("shayuString");
        Assertions.assertTrue("fish".equals((String) shayu));
        shayu = valueOperations.get("shayuInt");
        Assertions.assertTrue(1 == (Integer) shayu);
        shayu = valueOperations.get("shayuDouble");
        Assertions.assertTrue(2.0 == (Double) shayu);
        System.out.println(valueOperations.get("shayuUser"));
        valueOperations.set("shayuString", "fish");

        //删
//        redisTemplate.delete("shayuString");
    }

    @Test
    void test() {
        // list，数据存在本地 JVM 内存中
        List<String> list = new ArrayList<>();
        list.add("yupi");
        System.out.println("list:" + list.get(0));

        list.remove(0);

        // 数据存在 redis 的内存中
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("yupi");
        System.out.println("rlist:" + rList.get(0));
        rList.remove(0);

        // map
        Map<String, Integer> map = new HashMap<>();
        map.put("yupi", 10);
        map.get("yupi");

        RMap<Object, Object> map1 = redissonClient.getMap("test-map");

        // set

        // stack
    }

    /**
     * 开一个监听线程，如果方法还没执行完，就帮你重置 redis 锁的过期时间
     */
    @Test
    void testWatchDog() {
        RLock lock = redissonClient.getLock("yzj:precachejob:docache:lock");
        try {
            // 只有一个线程能获取到锁
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                //使当前线程休眠，进入阻塞状态（暂停执行）
                Thread.sleep(300000);//todo 实际要执行的代码
                System.out.println("getLock: " + Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
