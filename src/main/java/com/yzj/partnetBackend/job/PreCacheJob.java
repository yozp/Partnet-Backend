package com.yzj.partnetBackend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.yzj.partnetBackend.model.User;
import com.yzj.partnetBackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 定时缓存预热
 * 一定要记得释放锁，还有设置锁的过期时间
 * @author: shayu
 * @date: 2022/12/11
 * @ClassName: yupao-backend01
 * @Description:        数据预热
 */

@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 重点用户
    private List<Long> mainUserList = Arrays.asList(1L);//该方法是将数组转化成List集合的方法

    @Resource
    private RedissonClient redissonClient;

    // 每天执行，预热推荐用户
    @Scheduled(cron = "0 49 10 * * *")   //自己设置时间测试
    public void doCacheRecommendUser() {
        RLock lock=redissonClient.getLock("yzj:precachejob:docache:lock");

        try{
            //尝试获取锁，等待0秒（表示一天只抢一次，抢不到就走了），持有锁-1秒钟
            if(lock.tryLock(0,-1,TimeUnit.MILLISECONDS)){
                //Thread.currentThread()可以获取当前线程的引用,getId();获取该线程的标识符
                System.out.println("getLock: " + Thread.currentThread().getId());
                for(Long userId: mainUserList){
                    QueryWrapper queryWrapper=new QueryWrapper();
                    Page<User> userPage=userService.page(new Page<>(1,20),queryWrapper);

                    String redisKey=String.format("yzj:user:recommend:%s",mainUserList);
                    ValueOperations valueOperations=redisTemplate.opsForValue();

                    try{
                        valueOperations.set(redisKey, userPage, 30000, TimeUnit.MICROSECONDS);
                    }catch (Exception e){
                        log.error("redis set key error", e);
                    }
                }
            }
        }catch (InterruptedException e){
            log.error("doCacheRecommendUser error", e);
        }finally {
            // 是否是当前执行线程的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();//释放锁
            }
        }
    }

}