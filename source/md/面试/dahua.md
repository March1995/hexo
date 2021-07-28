### 1.linux常用命令例如:查看XXX端口是否被占用,cpu占用率等等
netstat -anp|grep 8080

netstat  -nultp

ps –ef|grep java

kill

top

jobs

tar –zxvf 

chmod 文件权限

chown文件拥有者和群组

### 2.Nginx技术SSl配置,跨域访问负载均衡(几种策略)

跨域访问: 

allow-origin:
allow-methods:
当跨域请求需要携带cookie是，请求头中需要设置Access-Control-Allow-Credentials:true。
Access-Control-Allow-Credentials值为true时，Access-Control-Allow-Origin必须有明确的值，不能是通配符(*)
> 负载均衡: https://segmentfault.com/a/1190000022440540

使用upstream模块定义后端服务器
策略：
轮询 默认方式
weight 权重，
ip_hash
least_conn

### 3 接口访问慢问题排查。 

### 4 线程池的使用,几大参数,参数解析 

### 5 Springcloud使用过哪些组件,在项目中起什么作用。 

### 6 Springboot相互依赖问题的解决原理,几种注入注解的区别@Autowired@Resource. 

spring生命周期，在bean初始化的时候，填充属性的时候，去加载需要依赖的对象，这个时候发现存在循环依赖。在A对象初始化的时候，先在二级缓存中提前暴露自己，
以便在B对象需要A的时候能获得引用。 spring生命周期提供了很多修改bean的接入点(beanPostProcessor)，三级缓存是为了解决bean的引用被替换的问题，提前获取对象的引用，提前把对象给替换了AOP。

@Autowire 由spring提供，默认根据type注入，当同一个类型有多个实现类时，会造成无法选择具体的注入，一般配合@Qualifier使用，根据名称注入。
@Resource 由java提供，默认按照byName注入，也提供byType注入。
@Resource装配顺序
1. 如果同时指定了name和type，则从Spring上下文中找到唯一匹配的bean进行装配，找不到则抛出异常
2. 如果指定了name，则从上下文中查找名称（id）匹配的bean进行装配，找不到则抛出异常
3. 如果指定了type，则从上下文中找到类型匹配的唯一bean进行装配，找不到或者找到多个，都会抛出异常
4. 如果既没有指定name，又没有指定type，则自动按照byName方式进行装配；如果没有匹配，则回退为一个原始类型进行匹配，如果匹配则自动装配；

### 7 Redis几种存储类型,redis分布式锁实现原理.redis的持久化策略 

存储类型:
String, List, Set,Hash, Zset, 

redis分布式锁实现原理:利用redis string操作的setnx,

### 8 垃圾回收器:新生代几种,老年代几种,常用的组合。项目上用的是哪种?有什么优点 

G1   region防止全堆扫描 每个region单独回收  整体是标记整理算法 局部是标记复制算法

新生代
Serial(单线程)  ParNew(Serial多线程版本)  Parallel Scavenge(多线程，吞吐量优先)
老年代
Serial Old     CMS（标记清除算法，容易导致内存碎片,造成full GC）  Parallel Old 收集器       

常用的组合

ParNew + CMS
Parallel Scavenge +  Parallel Old

### 9 synchronized锁升级原理,Look锁,独占与共享

synchronized jvm层面

synchronized 1.6之前是重锁，调用操作系统的函数，1.7之后做了优化，在jvm升级 偏向锁，轻量锁，

### 10 对AQS的理解

核心思想是，如果被请求的共享资源空闲，则将当前请求资源的线程设置为有效的工作线程，并且将共享资源设置为锁定状态。
如果被请求的共享资源被占用，那么就需要一套线程阻塞等待以及被唤醒时锁分配的机制，这个机制AQS是用CLH队列锁实现的，即将暂时获取不到锁的线程加入到队列中。

### 11 Dubbo的实现原理

### 12 几种Mq的优缺点,项目上为什么用XXXMq

### 13 mysql索引有几种,底层数据结构是什么,为什么会用到这种数据结构,优点是什么? 

### 14 针对项目上使用的技术随机发问

