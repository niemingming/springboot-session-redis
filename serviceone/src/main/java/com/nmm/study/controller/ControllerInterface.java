package com.nmm.study.controller;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@FeignClient(name = "session",url = "http://localhost:8090")
public interface ControllerInterface {
    @RequestMapping("/setSession/{key}/{value}")
    public String setSession(@PathVariable("key") String key , @PathVariable("value") String value);
    @RequestMapping("/getSession/{key}")
    public String getSession(@PathVariable(name = "key") String key );
}
