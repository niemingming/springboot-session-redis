package com.nmm.study;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * @author nmm 2018/6/26
 * @description
 */
@SpringBootApplication
@EnableRedisHttpSession//增加redissession缓存支持
@EnableFeignClients//增加feign支持，引入feign注解，feign扫描路径可以单独指定(basePackages = ),默认是spring的扫描路径
public class ServiceOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceOneApplication.class,args);
    }
}
