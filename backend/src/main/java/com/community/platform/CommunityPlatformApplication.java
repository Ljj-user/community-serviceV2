package com.community.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({
        "com.community.platform.generated.mapper"
})
public class CommunityPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityPlatformApplication.class, args);
    }
}

