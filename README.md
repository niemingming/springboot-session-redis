# springboot-session-redis
springboot+springsession+redis实现session共
* [1、springboot+springsession+redis](#session)
* [2、feign框架导致session共享失效](#feign)

<h2 id="session">springboot+springsession+redis</h2>

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

<h2 id="feign">feign框架导致session丢失问题</h2>

# 1、引入feign依赖
```
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-feign</artifactId>
</dependency>
```
# 2、声明feignrpc接口
注意：这里feign接口声明只需要能够表达出来调用的url/path/params等信息就可以了，不要携带额外的信息，否则会报错。因为feign本身
是一个rpc框架，目的是为了实现rpc调用，与dubbo等不同，不是接口声明式的，它是restful风格的，因此它要模拟的是浏览器，不是javabean
```aidl
//这里不要出现与调用无关的任何信息，哪怕这些信息是真正实现时要用到的。
@FeignClient(name = "session",url = "http://localhost:8090")
public interface ControllerInterface {
    @RequestMapping("/setSession/{key}/{value}")
    public String setSession(@PathVariable("key") String key , @PathVariable("value") String value);
    @RequestMapping("/getSession/{key}")
    public String getSession(@PathVariable(name = "key") String key );
}

```
# 3、在application中增加@EnableFeignClients注解，支持feign
```aidl
@SpringBootApplication
@EnableRedisHttpSession//增加redissession缓存支持
@EnableFeignClients//增加feign支持，引入feign注解，feign扫描路径可以单独指定(basePackages = ),默认是spring的扫描路径
public class ServiceOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceOneApplication.class,args);
    }
}
```
# 4、在用到的地方，像注入bean一样使用它
```aidl
  @Autowired
    private ControllerInterface controllerInterface;
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
```
到这一步，一个普通的feign调用示例就结束了，因为它倾向于restful风格的，因此默认的是无状态的，不会向下游携带额外状态信息，实际开发中我们又需要携带如登录
信息一样的状态信息，该怎么办呢？Feign早就想到了，我们看下源码中的RequestInterceptor怎么说的
```aidl
/**
* <font color="#FF0000">
 * Zero or more {@code RequestInterceptors} may be configured for purposes such as adding headers to
 * all requests.  No guarantees are give with regards to the order that interceptors are applied.
 * Once interceptors are applied, {@link Target#apply(RequestTemplate)} is called to create the
 * immutable http request sent via {@link Client#execute(Request, feign.Request.Options)}. <br> <br>
 * For example: <br>
 * <pre>
 * public void apply(RequestTemplate input) {
 *     input.replaceHeader(&quot;X-Auth&quot;, currentToken);
 * }
 * </font>
 * </pre>
 * <br> <br><b>Configuration</b><br> <br> {@code RequestInterceptors} are configured via {@link
 * Feign.Builder#requestInterceptors}. <br> <br><b>Implementation notes</b><br> <br> Do not add
 * parameters, such as {@code /path/{foo}/bar } in your implementation of {@link
 * #apply(RequestTemplate)}. <br> Interceptors are applied after the template's parameters are
 * {@link RequestTemplate#resolve(java.util.Map) resolved}.  This is to ensure that you can
 * implement signatures are interceptors. <br> <br><br><b>Relationship to Retrofit 1.x</b><br> <br>
 * This class is similar to {@code RequestInterceptor.intercept()}, except that the implementation
 * can read, remove, or otherwise mutate any part of the request template.
 */
public interface RequestInterceptor {

  /**
   * Called for every request. Add data using methods on the supplied {@link RequestTemplate}.
   */
  void apply(RequestTemplate template);
}
```
注意标红的部分，大意是我们有时候需要一个或者多个RequestInterceptor去配置诸如请求头信息，还给出了授权的头部配置。
到这里我们就明白了，我们需要实现一个RequestInterceptor，在里面将原来的请求头信息付给下游请求，实际上就是Cookie信息，
这样sessionId就传到下游了，也就实现了共享。
# 5、实现RequestInterceptor
 ```aidl

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * 实现RequestInterceptor，用于设置feign全局请求模板
 */
@Component
public class FeignRequestIntercepter implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        //通过RequestContextHolder获取本地请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null){
            return;
        }
        //获取本地线程绑定的请求对象
        HttpServletRequest request = ((ServletRequestAttributes)requestAttributes).getRequest();
        //给请求模板附加本地线程头部信息，主要是cookie信息
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()){
            String name = headerNames.nextElement();
            requestTemplate.header(name,request.getHeader(name));
        }

    }
}

```
# 6、验证
在浏览器中分别输入
http://localhost:8080/setSession/key/hello
http://localhost:8090/getSession/key
http://localhost:8080/testFeign/key
可以看到，打出的sessionID是相同的，而且都可以获取到session属性。
到这一步就基本解决了session丢失问题。至于网上说的熔断的异步化，导致session信息获取不到的问题，我们这里不做熔断，因此可以不考虑。
