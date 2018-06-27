# springboot-session-redis
springboot+springsession+redis实现session共享
# springboot+springsession+redis
# 1、引入springsession和springredis的依赖
```
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.session</groupId>
            <artifactId>spring-session-data-redis</artifactId>
        </dependency>
```
# 2、在启动Application中添加@EnableRedisHttpSession注解
该注解的作用，就是引入springsession管理，同时实现是采用redis管理session的方式。
```
@SpringBootApplication
@EnableRedisHttpSession//增加redissession缓存支持
public class ServiceOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceOneApplication.class,args);
    }
}
```
# 3、修改application.yml，添加redis的配置信息
```
spring:
  redis:
    host: localhost
    port: 6379
server:
  port: 8080
```
# 4、测试controller，分别设置session属性和获取
```
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
```
启动两个实例，分别访问发现获取到的sessionid是一致的。

# 5、说明
经过源码分析，springsession+redis关键是通过@EnableRedisHttpSession注解引入的，主要是通过SessionRepositoryFilter进行session的预处理，整个过程还是通过
原生的Cookie中获取SessionID实现。因此我们需要对session做一些特殊操作的时候，需要考虑SessionRepositoryFilter的级别和顺序。
