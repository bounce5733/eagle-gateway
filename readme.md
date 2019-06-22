# 网关手册


<!-- @import "[TOC]" {cmd="toc" depthFrom=1 depthTo=6 orderedList=false} -->

<!-- code_chunk_output -->

* [网关手册](#网关手册)
	* [简介](#简介)
	* [特性](#特性)
	* [功能](#功能)
		* [安全组件](#安全组件)
			* [SQL注入](#sql注入)
			* [XSS注入](#xss注入)
			* [报文加密](#报文加密)
			* [请求防重](#请求防重)
		* [控制组件](#控制组件)
			* [会话认证](#会话认证)
			* [会话传递](#会话传递)
		* [服务管理](#服务管理)
		* [基础组件](#基础组件)
			* [日志管理](#日志管理)
			* [异常处理](#异常处理)
			* [请求熔断](#请求熔断)
			* [请求限流](#请求限流)
		* [管理功能](#管理功能)
			* [黑白名单](#黑白名单)
			* [路由规则](#路由规则)
	* [安装部署](#安装部署)
		* [环境配置](#环境配置)
			* [jdk1.8](#jdk18)
			* [redis](#redis)
			* [nacos](#nacos)

<!-- /code_chunk_output -->

---

## 简介

基于spring cloud gateway封装的微服务网关，通过nacos配置中心来管理应用路由

## 特性

- 动态配置
  - 基于nacos配置中心，各应用独立配置路由，实时刷新
- 插件机制
  - 组件支持通过配置热拔插，无需重启网关
- 弹性扩展
  - 网关节点支持横向动态扩容

## 功能

==spring cloud gateway 框架自带功能请参考官网==

### 安全组件
  
#### SQL注入

校验请求参数和请求体SQL注入关键字

  - 检查==请求参数==和==applicaiton/json类型body数据==中的sql注入风险
  &nbsp;
  - 配置代码
    ```yaml
        # Sql注入检测（依赖RequestDecrypt)
      - name: SqlInspect
        args:
          # 是否包括查询参数（true)
          includeQueryParams: true
          # 是否包括请求题（true）
          includeBody: true
    ```

#### XSS注入

todo...

#### 报文加密

对前端加密的请求参数和请求体数据在网关解密

  - ==请求参数==和==application/json类型body数据==解密
  &nbsp;
  - 加密算法： ==AES==，加密算法/加密模式／填充类型：==AES/ECB/Pkcs7==，加密密码：==app secret==，密钥偏移量：==abcdef0123456789==
  &nbsp;
  - 配置代码
    ```yaml
        # 请求解密
      - name: RequestDecrypt
        args:
          # 是否包括查询参数（true)
          includeQueryParams: true
          # 是否包括请求题（true）
          includeBody: true
    ```

#### 请求防重

防止中间人截获请求重放攻击

  - 请求唯一性检测，通过添加HTTP HEADER ==x-ca-reqid: uuid==
  &nbsp;
  - 请求过期检测，通过添加HTTP HEADER:==x-ca-reqtime: currentTimeMillis==
  &nbsp;
  - 配置代码
    ```yaml
        # 请求防重
      - name: ReplayAttack
        args:
          # 请求过期时间（秒）
          expiredTime: 900
    ```

### 控制组件

#### 会话认证

缓存oauth2 token，在网关本地认证

  - 配置代码
    ```yaml
      - AppAuth
    ```

#### 会话传递

封装HTTP HEADER(gw_session: 会话信息)给后端服务

  - 会话格式：
  ```json
    {
      sessionid: '',
      userid: '',
      username: ''
    }
  ```
  &nbsp;
  - 配置代码
    ```yaml
      - SessionHeader
    ```

### 服务管理

通过改造nacos注册中心管理

### 基础组件

#### 日志管理

todo


#### 异常处理

  - 网关自定义

    异常码 | 描述
    --- | ---
    1000 | 网关内部异常
    1001 | 认证失败
    1002 | 非法请求地址
    1004 | 非法安全请求头
    1005 | 解码数据出错
    1006 | 重放攻击出错
    1007 | 请求已被熔断
    1008 | 会话失效
    1009 | URI查询参数不合法
    1010 | 请求数据包含SQL注入风险

  - HTTP 异常码 ==参考网络==

#### 请求熔断

每个路由舱壁模式隔离，请求异常熔断，自动回复

- 配置代码
  
  ```yaml
      # 熔断
    - name: Hystrix
      args:
        name: appService1AddCmd
  ```

#### 请求限流

分布式令牌桶模式限流

- 配置代码
  
  ```yaml
      # 限流
    - name: RequestRateLimiter
      args:
        # 限流策略
        key-resolver: '#{@remoteAddrKeyResolver}'
        # 令牌桶每秒填充率
        redis-rate-limiter.replenishRate: 10
        # 令牌桶容量
        redis-rate-limiter.burstCapacity: 100
  ```

### 管理功能

#### 黑白名单

配置路由名单，白名单跳过认证，黑名单阻止访问

- 配置代码
  
  ```yaml
      # 黑白名单过滤
    - name: RBL
     args:
       blacklistUrl:
       - /echo222/**
       whitelistUrl:
       - /echo333/**
  ```

#### 路由规则

nacos注册中心配置

## 安装部署

### 环境配置

#### jdk1.8

#### redis

#### nacos

1. 修改启动脚本

    ```shell
    vi nacos\bin\startup.sh
    export JAVA_HOME="/opt/jdk"
    ```

2. 启动

    ```shell
    nacos\bin\startup.sh -m standalone &
    ```

3. 配置

   1. 登陆
      http://127.0.0.1:8848/nacos/#/login
      ==nacos/nacos==
    &nbsp;
   2. 创建命名空间如 ==sit==
   &nbsp;
   3. 创建网关基础配置 ==gateway_server.yaml==

    ```yaml
      server:
        port: 8080

      spring:
        application:
          name: gateway_server

        redis:
          host: 127.0.0.1
          port: 6379
          database: 0
          lettuce:
            pool:
              max-active: 200 #连接池最大连接数（使用负值表示没有限制）
              max-idle: 20 # 连接池中的最大空闲连接
              min-idle: 5 #连接池中的最小空闲连接
              max-wait: 1000 # 连接池最大阻塞等待时间（使用负值表示没有限制）

      # 熔断设置
      hystrix:
        command:
          default:
            fallback:
              # 关闭服务降级
              enabled: false
            execution:
              isolation:
                thread:
                  # 执行超时时间（1000）
                  timeoutInMilliseconds: 3000
            metrics:
              rollingStats:
                # 判断健康度的滚动时间窗长度（10000）
                timeInMilliseconds: 10000
            circuitBreaker:
              # 滚动时间窗内最小请求数，达到才会进入断路判断（20）
              requestVolumeThreshold: 20
              # 滚动时间窗内错误百分比，达到断路器打开（50）
              errorThresholdPercentage: 50
              # 休眠时间窗，休眠后断路器半开，尝试熔断后第一次请求
              sleepWindowInMilliseconds: 5000
            threadPool:
              # 命令线程池执行最大并发量（10）
              coreSize: 10

      # 是否开启监控端点        
      management:
        endpoints:
          web:
            exposure:
              include: "*"
        security:
          enabled: false
    ```

   &nbsp;
   4. 创建应用基础配置 ==app_base.yaml==

    ```yaml
      # 应用公共配置
      app:
        routes:
          # 路由文件列表
          filenames:
          - app_service1.yaml
        sqlInject:
          regex: (?:')|(?:--)|(/\*(?:.|[\n\r])*?\*/)|(\b(select|update|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\b)

      spring:
        cloud:
          gateway:
            default-filters:
              # 请求重放
            - name: ReplayAttack
              args:
                # 请求过期时间（秒）
                expiredTime: 900

              # 黑白名单过滤
            - name: RBL
              args:
                blacklistUrl:
                - /echo222/**
                whitelistUrl:
                - /echo333/**

              # 认证
            - AppAuth

              # 请求解密
            - name: RequestDecrypt
              args:
                # 是否包括查询参数（true)
                includeQueryParams: true
                # 是否包括请求题（true）
                includeBody: true
            
              # Sql注入检测（依赖RequestDecrypt)
            - name: SqlInspect
              args:
                # 是否包括查询参数（true)
                includeQueryParams: true
                # 是否包括请求题（true）
                includeBody: true

              # 添加会话
            - SessionHeader
    ```
    &nbsp;
   5. 创建应用服务路由配置 ==app_service1.yaml==

    ```yaml
      spring:
        cloud:
          gateway:
            routes:
            - id: appService1Echo
              uri: http://127.0.0.1:8081/echo
              predicates:
              - Cookie=SESSION,.*
              - Path=/service1/echo/**
              filters:
              - StripPrefix=1
                # 限流
              - name: RequestRateLimiter
                args:
                  # 限流策略
                  key-resolver: '#{@remoteAddrKeyResolver}'
                  # 令牌桶每秒填充率
                  redis-rate-limiter.replenishRate: 1
                  # 令牌桶容量
                  redis-rate-limiter.burstCapacity: 2
                # 熔断
              - name: Hystrix
                args:
                  name: appService1EchoCmd

            - id: appService1Add
              uri: http://127.0.0.1:8081/add
              predicates:
              - Cookie=SESSION,.*
              - Path=/service1/add
              filters:
              - StripPrefix=1
                # 限流
              - name: RequestRateLimiter
                args:
                  # 限流策略
                  key-resolver: '#{@remoteAddrKeyResolver}'
                  # 令牌桶每秒填充率
                  redis-rate-limiter.replenishRate: 10
                  # 令牌桶容量
                  redis-rate-limiter.burstCapacity: 100
                # 熔断
              - name: Hystrix
                args:
                  name: appService1AddCmd
                
    ```
