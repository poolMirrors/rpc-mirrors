package com.mirrors.client.config;

import com.mirrors.client.communicate.RpcClient;
import com.mirrors.client.communicate.http.HttpRpcClient;
import com.mirrors.client.communicate.netty.NettyRpcClient;
import com.mirrors.client.communicate.socket.SocketRpcClient;
import com.mirrors.client.proxy.ClientProxyFactory;
import com.mirrors.client.spring.RpcClientBeanPostProcessor;
import com.mirrors.client.spring.RpcClientExitDisposableBean;
import com.mirrors.core.discover.ServiceDiscover;
import com.mirrors.core.discover.nacos.NacosServiceDiscover;
import com.mirrors.core.discover.zookeeper.ZooKeeperServiceDiscover;
import com.mirrors.core.loadbalance.LoadBalance;
import com.mirrors.core.loadbalance.impl.ConsistentHashLoadBalance;
import com.mirrors.core.loadbalance.impl.RandomLoadBalance;
import com.mirrors.core.loadbalance.impl.RoundRobinLoadBalance;
import com.mirrors.core.registry.zookeeper.ZooKeeperServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 客户端自动装配类
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
 * @date 2023/12/15 9:41
 */
@Configuration
@EnableConfigurationProperties(RpcClientProperties.class)
public class RpcClientAutoConfiguration {

    /**
     * 配置属性类对象
     */
    @Autowired
    RpcClientProperties properties;

    // ---------------------------------------负载均衡---------------------------------------------------

    /**
     * 配置负载均衡算法，当不指定属性值，则值默认为当前创建的类（随机策略）
     *
     * @return
     */
    @Bean(name = "loadBalance")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "loadBalance", havingValue = "random")
    public LoadBalance randomLoadBalance() {
        return new RandomLoadBalance();
    }

    /**
     * 配置负载均衡算法（轮询策略）
     *
     * @return
     */
    @Bean(name = "loadBalance")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "loadBalance", havingValue = "roundRobin", matchIfMissing = true)
    public LoadBalance roundRobinLoadBalance() {
        return new RoundRobinLoadBalance();
    }

    /**
     * 配置负载均衡算法（一致性哈希策略）
     *
     * @return
     */
    @Bean(name = "loadBalance")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "loadBalance", havingValue = "consistentHash")
    public LoadBalance consistentHashLoadBalance() {
        return new ConsistentHashLoadBalance();
    }

    // ---------------------------------------服务发现---------------------------------------------------

    /**
     * 配置服务发现（zookeeper）
     *
     * @return
     */
    @Bean(name = "serviceDiscover")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnBean(LoadBalance.class)
    @ConditionalOnProperty(prefix = "rpc.client", name = "registry", havingValue = "zookeeper")
    public ServiceDiscover zookeeperServiceDiscover(@Autowired LoadBalance loadBalance) {
        return new ZooKeeperServiceDiscover(properties.getRegistryIpAndPort(), loadBalance);
    }

    /**
     * 配置服务发现（nacos）
     *
     * @return
     */
    @Bean(name = "serviceDiscover")
    @ConditionalOnMissingBean
    @ConditionalOnBean(LoadBalance.class)
    @ConditionalOnProperty(prefix = "rpc.client", name = "registry", havingValue = "nacos")
    public ServiceDiscover nacosServiceDiscover(@Autowired LoadBalance loadBalance) {
        return new NacosServiceDiscover(properties.getRegistryIpAndPort(), loadBalance);
    }

    // ---------------------------------------客户端连接---------------------------------------------------

    /**
     * 配置客户端通信连接（基于netty）
     *
     * @return
     */
    @Bean(name = "rpcClient")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "communicate", havingValue = "netty", matchIfMissing = true)
    public RpcClient nettyRpcClient() {
        // 返回netty客户端
        return new NettyRpcClient();
    }

    /**
     * 配置客户端通信连接（基于http）
     *
     * @return
     */
    @Bean(name = "rpcClient")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "communicate", havingValue = "http")
    public RpcClient httpRpcClient() {
        // 返回http客户端
        return new HttpRpcClient();
    }

    /**
     * 配置客户端通信连接（基于socket）
     *
     * @return
     */
    @Bean(name = "rpcClient")
    @Primary
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "rpc.client", name = "communicate", havingValue = "socket")
    public RpcClient socketRpcClient() {
        // 返回socket客户端
        return new SocketRpcClient();
    }

    // ---------------------------------------代理工厂---------------------------------------------------

    /**
     * 创建并配置 代理对象
     *
     * @param serviceDiscover
     * @param rpcClient
     * @param rpcClientProperties
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({ServiceDiscover.class, RpcClient.class})
    public ClientProxyFactory clientProxyFactory(@Autowired ServiceDiscover serviceDiscover,
                                                 @Autowired RpcClient rpcClient,
                                                 @Autowired RpcClientProperties rpcClientProperties) {
        return new ClientProxyFactory(serviceDiscover, rpcClient, rpcClientProperties);
    }

    // ---------------------------------------bean后置处理器---------------------------------------------------

    /**
     * 创建配置 bean后置处理器
     *
     * @param clientProxyFactory
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RpcClientBeanPostProcessor rpcClientBeanPostProcessor(@Autowired ClientProxyFactory clientProxyFactory) {
        return new RpcClientBeanPostProcessor(clientProxyFactory);
    }

    /**
     * 创建配置 客户端退后的额外操作类
     *
     * @param serviceDiscover
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RpcClientExitDisposableBean rpcClientExitDisposableBean(@Autowired ServiceDiscover serviceDiscover) {
        return new RpcClientExitDisposableBean(serviceDiscover);
    }
}
