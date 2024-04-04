package com.mirrors.server.config;

import com.mirrors.core.registry.ServiceRegistry;
import com.mirrors.core.registry.nacos.NacosServiceRegistry;
import com.mirrors.core.registry.zookeeper.ZooKeeperServiceRegistry;
import com.mirrors.server.communicate.RpcServer;
import com.mirrors.server.communicate.http.HttpRpcServer;
import com.mirrors.server.communicate.netty.NettyRpcServer;
import com.mirrors.server.communicate.socket.SocketRpcServer;
import com.mirrors.server.spring.RpcServerBeanPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * server端的自动装配类，使得provider端的配置生效
 * <p>
 * 使得 @ConfigurationProperties 注解生效，将该类注入到 IOC 容器中,交由 IOC 容器进行管理
 * <pre>
 *     1. ConditionalOnBean：是否存在某个某类或某个名字的Bean
 *     2. ConditionalOnMissingBean：是否缺失某个某类或某个名字的Bean
 *     3. ConditionalOnSingleCandidate：是否符合指定类型的Bean只有⼀个
 *     4. ConditionalOnClass：是否存在某个类
 *     5. ConditionalOnMissingClass：是否缺失某个类
 *     6. ConditionalOnExpression：指定的表达式返回的是true还是false
 *     7. ConditionalOnJava：判断Java版本
 *     8. ConditionalOnJndi：JNDI指定的资源是否存在
 *     9. ConditionalOnWebApplication：当前应⽤是⼀个Web应⽤
 *     10. ConditionalOnNotWebApplication：当前应⽤不是⼀个Web应⽤
 *     11. ConditionalOnProperty：Environment中是否存在某个属性
 *     12. ConditionalOnResource：指定的资源是否存在
 *     13. ConditionalOnWarDeployment：当前项⽬是不是以War包部署的⽅式运⾏
 *     14. ConditionalOnCloudPlatform：是不是在某个云平台上
 * </pre>
 *
 * @author mirrors
 * @version 1.0
 * @Configuration 交给spring管理（有@Component）
 * @date 2023/12/13 21:00
 */
@Configuration
@EnableConfigurationProperties(RpcServerProperties.class)
public class RpcServerAutoConfiguration {

    /**
     * 配置类对象
     */
    @Autowired
    RpcServerProperties properties;

    // -----------------------------服务注册------------------------------------------------------------------

    /**
     * 创建ServiceRegistry实例bean，没有配置时返回zookeeper
     *
     * @return
     */
    @Bean(name = "serviceRegistry") // bean的名称
    @Primary // 有多个相同类型的bean时，使用@Primary来赋予bean更高的优先级
    @ConditionalOnMissingBean // 保证Spring容器中只有一个Bean类型的实例
    @ConditionalOnProperty(prefix = "rpc.server", name = "registry", havingValue = "zookeeper", matchIfMissing = true)
    // @ConditionalOnProperty：根据属性值来控制类或某个方法是否需要加载。它既可以放在类上也可以放在方法上
    public ServiceRegistry zookeeperServiceRegistry() {
        return new ZooKeeperServiceRegistry(properties.getRegistryIpAndPort());
    }

    /**
     * 创建ServiceRegistry实例bean，配置为nacos时
     *
     * @return
     */
    @Bean(name = "serviceRegistry")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "registry", havingValue = "nacos")
    public ServiceRegistry nacosServiceRegistry() {
        return new NacosServiceRegistry(properties.getRegistryIpAndPort());
    }

    // -----------------------------连接方式------------------------------------------------------------------

    /**
     * 创建通信方式，没有配置时就是用netty
     *
     * @return
     */
    @Bean(name = "rpcServer")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "communicate", havingValue = "netty", matchIfMissing = true)
    public RpcServer nettyRpcServer() {
        //返回netty通信方式
        return new NettyRpcServer();
    }

    /**
     * 创建通信方式，配置http
     *
     * @return
     */
    @Bean(name = "rpcServer")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "communicate", havingValue = "socket")
    public RpcServer socketRpcServer() {
        //返回socket通信方式
        return new SocketRpcServer();
    }

    /**
     * 创建通信方式，配置socket
     *
     * @return
     */
    @Bean(name = "rpcServer")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.server", name = "communicate", havingValue = "http")
    // @ConditionalOnClass标识在 Bean方法 上，只有只有存在@ConditionalOnClass中value/name配置的类方法才会生效
    // @ConditionalOnClass(name = {"org.apache.catalina.startup.Tomcat"})
    public RpcServer httpRpcServer() {
        //返回http通信方式
        return new HttpRpcServer();
    }

    // -----------------------------bean后置处理------------------------------------------------------------------

    /**
     * 配置这个类（RpcServerBeanPostProcessor使得 被@RpcService标注的类 暴露）
     *
     * @param registry
     * @param rpcServer
     * @param properties
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ServiceRegistry.class, RpcServer.class})
    public RpcServerBeanPostProcessor rpcServerBeanPostProcessor(@Autowired ServiceRegistry registry,
                                                                 @Autowired RpcServer rpcServer,
                                                                 @Autowired RpcServerProperties properties) {
        return new RpcServerBeanPostProcessor(registry, rpcServer, properties);
    }
}
