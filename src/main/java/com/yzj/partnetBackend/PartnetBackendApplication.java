package com.yzj.partnetBackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@MapperScan("com.yzj.partnetBackend.mapper")
@EnableRedisHttpSession
public class PartnetBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PartnetBackendApplication.class, args);
    }

}
