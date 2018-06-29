package com.nmm.study.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @author nmm 2018/6/26
 * @description
 */
@Controller
public class TestController {
    @Autowired
    private ControllerInterface controllerInterface;

    /**
     * session设置
     * @param key
     * @param value
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("/setSession/{key}/{value}")
    public String setSession(@PathVariable String key , @PathVariable String value,
                             HttpServletRequest request){
        request.getSession().setAttribute(key,value);
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()){
            String name = headers.nextElement();
            System.out.println(name + ":"+ request.getHeader(name));
        }
        System.out.println(request.getSession().getId());
        return request.getSession().getId();
    }

    /**
     * 读取session
     * @param key
     * @param request
     * @return
     */
    @ResponseBody
    @RequestMapping("/getSession/{key}")
    public String getSession(@PathVariable String key ,HttpServletRequest request){
        return request.getSession().getAttribute(key) + "---- sessionId:" + request.getSession().getId() ;
    }

    /**
     * 测试feign的session问题。
     * @param key
     * @return
     */
    @ResponseBody
    @RequestMapping("/testFeign/{key}")
    public String testFeign(@PathVariable String key,HttpServletRequest request) {
        return controllerInterface.getSession(key);
    }
}
