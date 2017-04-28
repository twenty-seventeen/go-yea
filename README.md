# go-yea

# YEA
使用go-yea前，请先下载yea，并且执行yea/pom.xml
```bash
mvn clean install -Dmaven.test.skip=true
```
# 概述
GO-YEA是YEA的一个应用，它是一个快速启动分布式框架，致力于提供产品的快速启动以及后续的服务伸缩。
其核心部分包含：
- RPC服务：基于Netty4NIO框架、FST序列化、Ketama Hash算法提供的RPC服务，完成服务之间的非阻塞通讯。
- LOOKUP服务: 基于Zookeeper提供的注册中心，使地址透明，方便服务提供方平滑增加或减少机器。
- 认证授权：基于Shiro框架、Redis服务提供的权限管理，提供用户的认证服务和可配置的授权服务。
- 代码生成：通过生成工具，生成基于Mybatis的Sql-Mapping文件及相应的Entity、PK、Domain类(均是贫血对象)，降低开发人员的重复工作。
- Spring配置：透明化接入，通过Spring自身的注解机制无需额外编写代码即可为服务提供分布式能力。


# 启动

## 单机模式
### 1、准备数据库
- 建表：Postgresql，请执行go-yea/go-yea-launcher/src/main/sql/ddl_sql.sql；Mysql，请执行yea库里的yea/yea-shiro/src/sql/shiro_db.mwb（MysqlWorkbench打开）所生成的脚本。
- 初始化数据：请执行go-yea/go-yea-launcher/src/main/sql/init_sql.sql
### 2、Maven打包
执行go-yea/pom.xml：
```bash
mvn clean package -Dmaven.test.skip=true -Pdevelop
```
### 3、部署Web服务
复制go-yea-web/target/go-yea-web.war到应用服务器部署目录内，启动服务器
### 4、登录访问
- 浏览器输入http://host:port/go-yea-web/
- 登录用户:admin
- 登录密码:admin

## RPC模式
### 1、准备数据库
参见单机模式
### 2、安装zookeeper
### 3、Maven打包
执行go-yea/pom.xml：
```bash
mvn clean package -Dmaven.test.skip=true
```
### 4、启动APP服务
复制go-yea/go-yea-launcher/target/go-yea-launcher-0.0.1-release.tar.gz到执行目录
```bash
tar -xzvf go-yea-launcher-0.0.1-release.tar.gz;
cd launcher;
sh start.sh;
```
启动时会要求输入Netty服务端绑定端口，输入端口或直接回车
### 5、部署Web服务
复制go-yea-web/target/go-yea-web.war到应用服务器部署目录内，启动服务器
### 6、登录访问
- 浏览器输入http://host:port/go-yea-web/
- 登录用户:admin
- 登录密码:admin
### 注意
RPC模式下，启动Web服务时会调用APP服务初始化Shiro权限管理信息，启动顺序先APP后WEB。实际应用中可单独部署一台APP用于提供统一认证授权服务。

# 开发
## APP服务
向外部系统提供APP服务接口，服务内部实现具体的业务场景。可参照go-yea的结构建立Maven项目结构：<br>
- 公共层Common
- 模型层Model
- 业务模块层，会有多个业务模块层
- 启动器层
### 1、建立通用Dao类
```java
@Repository
public class CommonDao<T> extends AbstractBaseDAO<T> {
  省略
}
```
- 通用Dao请参看go-yea/go-yea-common里的com.team.goyea.common.dao.CommonDao
- 若不使用通用Dao，则各业务所产生的Dao类请放置各个模块对应的Maven Module Project内
- 使用通用Dao的好处：将来可以将与DB的交互单独封装成服务，由该服务统一处理数据存储
### 2、建立Model层
- Model层将存放各模块所需要的模型类，它们将会被APP服务和WEB服务共同使用
### 3、建立模块对应的Maven Module Project
- 模块允许单独部署成APP服务，便于未来拆分服务
- 模块间调用分二种场景，非JVM内的将通过Netty-RPC完成，而JVM的调用将通过com.yea.core.remote.client.DefaultClient来完成
- 模块请参看权限模块go-yea/go-yea-permission和授权模块go-yea/go-yea-authorization
- 请注意：同JVM内模块间的调用务必使用DefaultClient来完成，未来若被调用方单独部署成服务，只需重新注入该服务的对应Client即可
### 4、建立启动器
- 每个单独部署的APP服务均需要启动器，启动器内将存放Main类、该APP服务所涉及的DB操作以及启动APP服务所需的Spring配置文件
- 启动器提供Netty服务端配置、DB配置，同时基于APP服务的实际调用情况酌情提供Netty客户端配置
- 启动器请参看go-yea/go-yea-launcher
- 运行在单机模式下的Netty配置内容:
```xml
	<!-- JVM内调用，注意，DefaultClient需设置lazy为false，以确保向ClientRegister注册本地客户端 -->
        <bean id="launcherClient" class="com.yea.core.remote.client.DefaultClient" lazy-init="false" init-method="connect" destroy-method="disconnect" ></bean>
```
- 运行在RPC模式下的Netty配置内容:
```xml
	<!-- JVM内调用，注意，DefaultClient需设置lazy为false，以确保向ClientRegister注册本地客户端 -->
        <bean id="launcherClient" class="com.yea.core.remote.client.DefaultClient" lazy-init="false" init-method="connect" destroy-method="disconnect" ></bean>
	
	<!-- Netty编解码的Handler实现 -->
	<bean id="nettyMessageDecoder" class="com.yea.remote.netty.codec.NettyMessageDecoder"></bean>
	<bean id="nettyMessageEncoder" class="com.yea.remote.netty.codec.NettyMessageEncoder"></bean>
	
	<!-- Netty心跳检测Handler实现 -->
	<bean id="heartBeatServerHandler" class="com.yea.remote.netty.server.handle.HeartBeatServerHandler">
		<constructor-arg index="0" type="int" value="60"/>
		<constructor-arg index="1" type="int" value="60"/>
	</bean>
	
	<!-- Netty服务处理Handler实现，所有业务操作均由该Handler处理 -->
	<bean id="serviceServerHandler" class="com.yea.remote.netty.server.handle.ServiceServerHandler"></bean>
	
	<!-- Netty异常处理Handler实现，Netty处理时若抛出异常，由该Handler封装异常并返回调用端 -->
	<bean id="exceptionHandler" class="com.yea.remote.netty.handle.ExceptionHandler"></bean>
	
	<!-- Zookeeper调度中心的配置 -->
	<bean id="zkDispatcher" class="com.yea.dispatcher.zookeeper.ZookeeperDispatcher" init-method="init">
		<property name="host" value="${zookeeper.host}" />
		<property name="port" value="${zookeeper.port}" />
	</bean>
	
	<!-- Netty服务端配置，Netty服务启动将在Main内调用(外部提供服务绑定端口)，启动服务后向调度中心发送服务注册请求 -->
	<bean id="nettyServer" class="com.yea.remote.netty.server.NettyServer" destroy-method="shutdown">
		<property name="registerName" value="${netty.server.register}" /><!-- 服务注册名，将在Zookeeper内注册 -->
		<property name="dispatcher" ref="zkDispatcher" />
		<property name="host" value="${netty.server.host}" /><!-- 该主机未设置时，系统将会读取本机IP自动设入 -->
		<property name="port" value="${netty.server.port}" /><!-- 该端口在执行start.sh脚本时，允许外部输入并替换 -->
	    <property name="listHandler">
            <list>
                <map>
	                <entry key="MessageDecoder">
	                    <ref bean="nettyMessageDecoder"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="MessageEncoder">
	                    <ref bean="nettyMessageEncoder"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="HeartBeatHandler">
	                    <ref bean="heartBeatServerHandler"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="ServiceHandler">
	                    <ref bean="serviceServerHandler"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="ExceptionHandler">
	                    <ref bean="exceptionHandler"/>
	                </entry>
	            </map>
            </list>
        </property>
	</bean>
	<!-- Netty服务端的配置 End -->
```
### 5、生成Mybatis的映射文件及相应的Domain
进入开发管理->生成工具
![Alt 生成工具](https://github.com/yiyongfei/picture/blob/master/go-yea/生成工具.tiff)
* Mybatis映射文件内包含单表常规操作
	* 生成的映射文件请复制到启动器内的src/main/resources/sqlmap
	* 样例请参看go-yea/go-yea-launcher/src/main/resources/sqlmap/authorization/personinfo-sqlmap-mapping.xml
* 模型类主要有：聚合类、实体类、主键类
	* 生成的模型类请复制到Model层
	* 实体类对应单表，通过实体类完成对表进行增、改操作，通过主键类完成基于主键的查、删操作
	* 聚合类默认由实体类和主键类组成，完成对表的查操作
	* 聚合类，将根据实际业务模型对聚合类进行属性扩充：一个聚合类可以对应一个或多个实体类，若存在一对多关系，对应的实体类应封装成集合
	* 样例请参看go-yea/go-yea-model里的com.team.goyea.authorization.model.PersonInfo，扩充属性的对比:UserInfo
* 生成工具使用说明：
	* 模块名：com.team.goyea.authorization.model.PersonInfo里，authorization是模块名
	* 根包路径：com.team.goyea.authorization.model.PersonInfo里，com.team.goyea是根包路径
	* 表名：数据库里的表名，例如t_person_info是表名，表名允许以%作后缀模糊匹配（如t_person%可以匹配t_person_info）
	* 公共DAO：勾选该选项后，生成的Mybatis映射文件namespace将使用所设置的Dao包路径和Dao名
### 6、Repository以及Service
### 7、Act(重点)
APP服务对外暴露的接口，客户端请求服务端时需提供Act名，Netty服务端收到请求后根据Act名找到相应的Bean并执行
- 无事务控制Act，针对查询请求，继承com.yea.core.act.AbstractAct并实现perform方法
- AbstractAct是一个RecursiveTask，复杂业务允许拆分成多个子任务做Fork Join
```java
@Service
public class QueryOperationAct extends AbstractAct {
	protected Object perform(Object[] messages) throws Throwable {
		省略
	}
}
```
- 事务控制Act，针对增删改请求，继承com.yea.core.act.AbstractTransactionAct并实现perform方法
- AbstractTransactionAct同样是一个RecursiveTask，复杂业务允许拆分成多个子任务做Fork Join
```java
@Service
public class SaveOperationAct extends AbstractTransactionAct {
	protected Object perform(Object[] messages) throws Throwable {
		省略
	}
}
```
## WEB服务
提供Web服务，主要完成参数封装、页面展示。可参照go-yea的结构建立Maven项目结构：<br>
- 模型层Model(与APP服务共用)
- 控制层Controller
- 页面资源WEB-INFO
### 1、pom.xml
- 运行在单机模式下:
```xml
<profile>
	<!-- 单机模式 -->
	<id>standalone</id>
	<properties>
	    <profiles.active>standalone</profiles.active>
	</properties>
	<dependencies>        
		<dependency>
			<groupId>com.team</groupId>
			<artifactId>go-yea-launcher</artifactId>
			<version>0.0.1</version>
		</dependency>
    </dependencies>
</profile>
```
- 运行在RPC模式下:
```xml
<profile>
	<!-- RPC模式 -->
	<id>rpc</id>
	<properties>
	    <profiles.active>rpc</profiles.active>
	</properties>
	<dependencies>        
		<dependency>
			<groupId>com.yea</groupId>
			<artifactId>yea-dispatcher</artifactId>
		</dependency>
    </dependencies>
</profile>
```
### 2、spring-bean.xml
- 运行在单机模式下:
```xml
    <import resource="classpath:/application-launcher.xml" />
```
- 运行在RPC模式下:
```xml
    <!-- Netty编解码的Handler实现 -->
	<bean id="nettyMessageDecoder" class="com.yea.remote.netty.codec.NettyMessageDecoder"></bean>
	<bean id="nettyMessageEncoder" class="com.yea.remote.netty.codec.NettyMessageEncoder"></bean>
	
	<!-- Netty异常处理Handler实现，Netty处理时若抛出异常，由该Handler封装异常并返回调用端 -->
	<bean id="exceptionHandler" class="com.yea.remote.netty.handle.ExceptionHandler"></bean>
	
	<!-- Netty心跳检测Handler实现 -->
	<bean id="heartBeatClientHandler" class="com.yea.remote.netty.client.handle.HeartBeatClientHandler">
		<constructor-arg index="0" type="int" value="60"/>
		<constructor-arg index="1" type="int" value="60"/>
	</bean>
	
	<!-- Netty客户端收到服务端响应后的处理Handler实现 -->
	<bean id="serviceClientHandler" class="com.yea.remote.netty.client.handle.ServiceClientHandler"></bean>
	
	<!-- Netty服务处理Handler实现，所有业务操作均由该Handler处理，配置该Handle是为了支持双向调用，由服务端主动发起请求，如果发送的动作均由客户端发起，该Handle可以不配置在客户端内 -->
    <bean id="serviceServerHandler" class="com.yea.remote.netty.server.handle.ServiceServerHandler"></bean>
    
	<!-- 调度中心的配置 -->
	<bean id="zkDispatcher" class="com.yea.dispatcher.zookeeper.ZookeeperDispatcher" init-method="init">
		<property name="host" value="${zookeeper.host}" />
		<property name="port" value="${zookeeper.port}" />
	</bean>
	
	<!-- Netty客户端配置，启动时将会根据服务注册名主动连接服务端 -->
        <bean id="nettyClient" class="com.yea.remote.netty.client.NettyClient" init-method="connect" destroy-method="disconnect">
		<property name="registerName" value="${netty.server.register}" /><!-- 服务注册名 -->
		<property name="dispatcher" ref="zkDispatcher" />
		<property name="host" value="${netty.client.host}" /><!-- 该主机未设置时，系统将会读取本机IP自动设入 -->
		<property name="port" value="${netty.client.port}" /><!-- 该端口在执行start.sh脚本时，允许外部输入并替换 -->
	    <property name="listHandler">
            <list>
                <map>
	                <entry key="MessageDecoder">
	                    <ref bean="nettyMessageDecoder"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="MessageEncoder">
	                    <ref bean="nettyMessageEncoder"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="HeartBeatHandler">
	                    <ref bean="heartBeatClientHandler"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="ServiceClientHandler">
	                    <ref bean="serviceClientHandler"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="ServiceServerHandler">
	                    <ref bean="serviceServerHandler"/>
	                </entry>
	            </map>
	            <map>
	                <entry key="ExceptionHandler">
	                    <ref bean="exceptionHandler"/>
	                </entry>
	            </map>
            </list>
        </property>
	</bean>
```
- Shiro的配置，请注意单机模式与RPC模式endpoint的设置(单机模式设置为DefaultClient，RPC模式设置成NettyClient):
```xml
    <bean id="sessionIdGenerator" class="org.apache.shiro.session.mgt.eis.JavaUuidSessionIdGenerator"/>  
    <bean id="sessionDAO" class="org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO">  
        <property name="activeSessionsCacheName" value="shiro-activeSessionCache"/>  
        <property name="sessionIdGenerator" ref="sessionIdGenerator"/>  
    </bean>
	
    <!-- 会话Cookie模板 -->  
    <bean id="sessionIdCookie" class="org.apache.shiro.web.servlet.SimpleCookie">
        <constructor-arg value="JEA_SESSIONID"/>
        <property name="httpOnly" value="true"/>
        <property name="maxAge" value="-1"/>    <!-- maxAge=-1，表示浏览器关闭时失效此Cookie -->
    </bean>
	
    <!-- 会话管理器 -->  
    <bean id="sessionManager" class="org.apache.shiro.web.session.mgt.DefaultWebSessionManager">  
        <property name="globalSessionTimeout" value="1800000"/>
        <property name="deleteInvalidSessions" value="true"/>
        <property name="sessionValidationSchedulerEnabled" value="true"/>
        <property name="sessionDAO" ref="sessionDAO"/>
        <property name="sessionIdCookieEnabled" value="true"/>
        <property name="sessionIdCookie" ref="sessionIdCookie"/>
    </bean>
	
    <!-- rememberMe的Cookie模板 -->
    <bean id="rememberMeCookie" class="org.apache.shiro.web.servlet.SimpleCookie">  
        <constructor-arg value="rememberMe"/>  
        <property name="httpOnly" value="true"/>  
        <property name="maxAge" value="604800"/><!-- 记住我的Cookie，保存时长7天 -->
    </bean>
    <!-- rememberMe管理器 -->
    <bean id="rememberMeManager" class="org.apache.shiro.web.mgt.CookieRememberMeManager">
         <property name="cipherKey" value="#{T(org.apache.shiro.codec.Base64).decode('9AVvhnFLuS3KTV8KprsdAg==')}" />
         <property name="cookie" ref="rememberMeCookie"/>
    </bean>
	
    <bean id="securityManager" class="com.yea.shiro.web.mgt.WebSecurityManager">
        <property name="endpoint" ref="nettyClient"/>
        <property name="sessionManager" ref="sessionManager"/>
        <property name="rememberMeManager" ref="rememberMeManager"/>
    </bean>
    
    <!-- 相当于调用SecurityUtils.setSecurityManager(securityManager) -->
    <bean class="org.springframework.beans.factory.config.MethodInvokingFactoryBean">
	<property name="staticMethod" value="org.apache.shiro.SecurityUtils.setSecurityManager"/>
        <property name="arguments" ref="securityManager"/>
    </bean>
	
    <!-- Shiro的Web过滤器 -->
    <bean id="shiroFilter" class="org.apache.shiro.spring.web.ShiroFilterFactoryBean" >
        <property name="securityManager" ref="securityManager"/>
        <property name="loginUrl" value="/login.html"/>
        <property name="successUrl" value="/index.html"/>
        <property name="unauthorizedUrl" value="/unauthorized.html"/>
    </bean>
    <!-- 对ShiroWeb过滤器进行包装，以初始化过滤器链（不允许延迟加载） -->
    <bean id="shiroFilterWrapper" class="com.yea.shiro.web.wrapper.ShiroFilterWrapper" init-method="init" lazy-init="false" >
        <property name="endpoint" ref="nettyClient"/>
        <property name="shiroFilter" ref="shiroFilter"/>
        <property name="authenticedUrl" value="/authenticed.html"/>
	<property name="logoutUrl" value="/logout.html"/>
    </bean>

    <!-- Shiro生命周期处理器-->
    <bean id="lifecycleBeanPostProcessor" class="org.apache.shiro.spring.LifecycleBeanPostProcessor"/>
```
- Shiro+Redis的配置，解决负载均衡模式下分布式Session共享问题:
```xml
    <!-- Redis池 -->
    <bean id="redisGeneralPool" class="com.yea.cache.jedis.pool.RedisGeneralPool" init-method="initPool" destroy-method="destroyPool">
        <property name="server" value="${redis.server}"/>
    </bean>
    
    <!-- RedisSessionDAO的支持，将Session放置在Redis服务上，可解决多Web服务器的Session同步问题 -->
    <bean id="sessionDAO" class="com.yea.shiro.session.mgt.redis.ShiroSessionDAO">
	<property name="cachePool" ref="redisGeneralPool"/>
    </bean>
```
### 3、spring-mvc.xml
- 配置拦截器，自动设置当前登录用户的用户信息及菜单信息
```xml
<mvc:interceptors>
    <bean class="com.yea.shiro.web.interceptor.ShiroInterceptor"></bean>
</mvc:interceptors>
```
### 4、Controller里的远程调用
* 通过NettyClient注册点发送
*
	* 发送请求到APP服务，欲请求的服务名(ActName)是服务方已注册的BeanName
```java
	CallAct act = new CallAct();
	act.setActName("queryOperationAct");
	Promise<List<OperationInfo>> promise = ClientRegister.<List<OperationInfo>>getInstance().send(act);
```
*
	* 如果发送时需要提供参数
```java
	CallAct act = new CallAct();
	act.setActName("saveOperationAct");
	Promise<List<OperationInfoPK>> promise = ClientRegister.<List<OperationInfoPK>>getInstance().send(act, operationDto);
```
*
	* 如果发送时需要提供多个参数
```java
	CallAct act = new CallAct();
	act.setActName("saveResourceIdentifierAct");
	Promise<ResourceIdentifierPK> promise = ClientRegister.<ResourceIdentifierPK>getInstance().send(act, identifier, resourceId, operationId);
```
*
	* 如果发送时指定的ActName在多个注册服务（RegisterName）里同时存在
```java
	CallAct act = new CallAct();
	act.setActName("重ActName");
	Promise<ResourceIdentifierPK> promise = ClientRegister.<ResourceIdentifierPK>getInstance().send("LAUNCHER", act, identifier, resourceId, operationId);
```
*
	* NIO发送，发送后可以执行其它步骤，如果要接收APP服务返回的对象可以在适合的时间点执行Promise.awaitObject方法获得结果
```java
	List<OperationInfo> listOperation = promise.awaitObject(10000);
```
* 通过注入NettyClient发送
*
	* 注入NettyClient
```java
	@Autowired
	private AbstractEndpoint nettyClient;
```
*
	* 发送请求到APP服务，欲请求的服务名(ActName)是服务方已注册的BeanName
```java
	CallAct act = new CallAct();
	act.setActName("queryOperationAct");
	Promise<List<OperationInfo>> promise = nettyClient.send(act);
```
* 通过ClientRegister，无需修改代码即能支持服务端所提供的Act服务的迁移，便于后续功能拆分或功能重新规划
### 5、历史项目的兼容
对于历史项目，通过反射调用机制(ReflectAct，有事务控制)提供RPC的兼容性，调用方的使用方式上与之前比较类似
```java
	CallReflect act = new CallReflect();
	act.setActName("authorizationService");
	act.setMethodName("addRolePermission");
	Promise<?> promise = ClientRegister.getInstance().send("LAUNCHER", act, roleId, resourceId, operationId);
```
* 通过反射机制不用额外提供Act类，但在Method的查找上存在一定隐患，性能上也会弱于直接调用，同时调用时需明确指明所注册的服务方
# 授权
## 匿名访问
若新添加的功能允许匿名用户访问，不需做任何授权配置即可，默设Shiro的Web访问控制是匿名访问，研发人员也可以在资源标识设置里配置Web访问是匿名访问。
## 登录访问或用户访问（记住我）
若新添加的功能需要用户登录才能访问，需要研发人员在资源标识设置（权限管理->资源标识设置）里配置该Web资源的访问控制为基于用户或登录验证。
![Alt 资源标识设置](https://github.com/yiyongfei/picture/blob/master/go-yea/资源标识设置.tiff)
## 授权访问
若新添加的功能需要授予用户权限后才能访问，此时：
### 1、新增对应功能的资源描述（权限管理->资源设置）
![Alt 资源设置](https://github.com/yiyongfei/picture/blob/master/go-yea/资源设置.tiff)
### 2、新增对应功能的权限描述（权限管理->权限设置）
![Alt 权限设置](https://github.com/yiyongfei/picture/blob/master/go-yea/权限设置.tiff)
### 3、资源标识设置时选择对应的权限（权限管理->资源标识设置）
![Alt 权限设置](https://github.com/yiyongfei/picture/blob/master/go-yea/资源标识设置-授权.tiff)

