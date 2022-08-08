---
title: mybatis-plus 原理
date: 2022-8-8
desc:
keywords: mybatis mybatis-plus
categories: [mybatis]
---

### @MapperScan

@MapperScan的作用可以理解为@Import注解 + @MapperScan中配置的包名

```java

@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableFeignClients({"com.***.***.common.proxy.api"})
@EnableApolloConfig("application")
@MapperScan("com.***.***.backend.**.dao")
public class SopBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(SopBackendApplication.class);
    }
}
```

SpringBoot启动时会记录启动类上的注解信息，然后Spring会通过ConfigurationClassPostProcessor解析对应的注解bean，
当获取到@MapperScan注解中有@Import注解

```java

@Target(ElementType.TYPE)
@Documented
@Import(MapperScannerRegistrar.class)
@Repeatable(MapperScans.class)
public @interface MapperScan {
}
```

然后会通过ConfigurationClassPostProcessor类把@Import中的MapperScannerRegistrar加入到Spring的启动逻辑中，
因为MapperScannerRegistrar实现了Spring的ImportBeanDefinitionRegistrar接口，所以会被调用到registerBeanDefinitions()方法。
而registerBeanDefinitions()方法的逻辑会判断类上面是否有@MapperScan注解，
有的话就会通过MapperScannerConfigurer扫描@MapperScan配置的目录，
通过ClassPathMapperScanner类扫描@Mapperscan配置目录下的所有加了@Repository注解的mapper，
并生成对应的beandefinition信息保存到spring容器中，
其中这些beandefinition的beanclass被设置成了MapperFactoryBean。

```java
@Override
public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,BeanDefinitionRegistry registry){
        AnnotationAttributes mapperScanAttrs=AnnotationAttributes
        .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));
        if(mapperScanAttrs!=null){
        registerBeanDefinitions(mapperScanAttrs,registry,generateBaseBeanName(importingClassMetadata,0));
        }
        }
```

```java
private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions){
        GenericBeanDefinition definition;
        for(BeanDefinitionHolder holder:beanDefinitions){
        definition=(GenericBeanDefinition)holder.getBeanDefinition();
        String beanClassName=definition.getBeanClassName();
        LOGGER.debug(()->"Creating MapperFactoryBean with name '"+holder.getBeanName()+"' and '"+beanClassName
        +"' mapperInterface");

        // the mapper interface is the original class of the bean
        // but, the actual class of the bean is MapperFactoryBean
        definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59
        //设置beanclass为MappeFactoryBean
        definition.setBeanClass(this.mapperFactoryBeanClass);
        definition.getPropertyValues().add("addToConfig",this.addToConfig);

        boolean explicitFactoryUsed=false;
        if(StringUtils.hasText(this.sqlSessionFactoryBeanName)){
        definition.getPropertyValues().add("sqlSessionFactory",
        new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
        explicitFactoryUsed=true;
        }else if(this.sqlSessionFactory!=null){
        definition.getPropertyValues().add("sqlSessionFactory",this.sqlSessionFactory);
        explicitFactoryUsed=true;
        }
        }
        }
```

而MapperFactoryBean实现了spring的FactoryBean接口，当spring通过BeanDefinitionMap创建bean的过程中，
会调用MapperFactoryBean的getObject()方法生成xxxMapper接口的代理对象MybatisMapperProxy。
到此Mapper接口就可以有代理对象被调用了。

### xxxMapper.xml

MybatisPlusAutoConfiguration被spring解释的时候，通过调用sqlSessionFactory()方法，
加载默认路径下mapper.xml文件，保存到MappedStatement中，
包括查询类型，sql语句，参数信息等，通过namespace和mapper对应上。

```java

@Data
@Accessors(chain = true)
@ConfigurationProperties(prefix = Constants.MYBATIS_PLUS)
public class MybatisPlusProperties {

    private String[] mapperLocations = new String[]{"classpath*:/mapper/**/*.xml"};

    public Resource[] resolveMapperLocations() {
        return Stream.of(Optional.ofNullable(this.mapperLocations).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location)))
                .toArray(Resource[]::new);
    }

    private Resource[] getResources(String location) {
        try {
            return resourceResolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }
}
```

```java
public class MybatisSqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ApplicationEvent> {
    if(this.mapperLocations !=null)

    {
        if (this.mapperLocations.length == 0) {
            LOGGER.warn(() -> "Property 'mapperLocations' was specified but matching resources are not found.");
        } else {
            for (Resource mapperLocation : this.mapperLocations) {
                if (mapperLocation == null) {
                    continue;
                }
                try {
                    //解析xml文件，生成MapperStatement
                    XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
                            targetConfiguration, mapperLocation.toString(), targetConfiguration.getSqlFragments());
                    xmlMapperBuilder.parse();
                } catch (Exception e) {
                    throw new NestedIOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
                } finally {
                    ErrorContext.instance().reset();
                }
                LOGGER.debug(() -> "Parsed mapper file: '" + mapperLocation + "'");
            }
        }
    }
}
```

### BaseMapper<T>

BaseMapper，继承了该接口后，无需编写mapper.xml文件，即可以获得CRUD功能，那么MyBatis-Plus又是如何做到无需配置mapper.xml就能获得CRUD功能呢？
答案就是通过sql注入器AbstractSqlInjector自己生成sql。

### @TableName

在AbstractSqlInject中inspectInject方法会获取BaseMapper<T>中的泛型类型，然后获取Entity上@tableName配置的表名以及@TableField注解字段的信息。

```java
public enum SqlMethod {
    /**
     * 插入
     */
    INSERT_ONE("insert", "插入一条数据（选择字段插入）", "<script>\nINSERT INTO %s %s VALUES %s\n</script>"),
}
```

```java
public abstract class AbstractSqlInjector implements ISqlInjector {

    private static final Log logger = LogFactory.getLog(AbstractSqlInjector.class);

    @Override
    public void inspectInject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
        //  获取BaseMapper中的泛型类型，，通过TableName获取mapper关联的是哪个表
        Class<?> modelClass = extractModelClass(mapperClass);
        if (modelClass != null) {
            String className = mapperClass.toString();
            Set<String> mapperRegistryCache = GlobalConfigUtils.getMapperRegistryCache(builderAssistant.getConfiguration());
            if (!mapperRegistryCache.contains(className)) {
                List<AbstractMethod> methodList = this.getMethodList(mapperClass);
                if (CollectionUtils.isNotEmpty(methodList)) {
                    TableInfo tableInfo = TableInfoHelper.initTableInfo(builderAssistant, modelClass);
                    // 循环注入自定义方法
                    methodList.forEach(m -> m.inject(builderAssistant, mapperClass, modelClass, tableInfo));
                } else {
                    logger.debug(mapperClass.toString() + ", No effective injection method was found.");
                }
                mapperRegistryCache.add(className);
            }
        }
    }
}
```

```java
public class TableInfoHelper {
    /**
     * <p>
     * 实体类反射获取表信息【初始化】
     * </p>
     *
     * @param clazz 反射实体类
     * @return 数据库表反射信息
     */
    public synchronized static TableInfo initTableInfo(MapperBuilderAssistant builderAssistant, Class<?> clazz) {
        TableInfo tableInfo = TABLE_INFO_CACHE.get(clazz);
        if (tableInfo != null) {
            if (builderAssistant != null) {
                tableInfo.setConfiguration(builderAssistant.getConfiguration());
            }
            return tableInfo;
        }

        /* 没有获取到缓存信息,则初始化 */
        tableInfo = new TableInfo(clazz);
        GlobalConfig globalConfig;
        if (null != builderAssistant) {
            tableInfo.setCurrentNamespace(builderAssistant.getCurrentNamespace());
            tableInfo.setConfiguration(builderAssistant.getConfiguration());
            globalConfig = GlobalConfigUtils.getGlobalConfig(builderAssistant.getConfiguration());
        } else {
            // 兼容测试场景
            globalConfig = GlobalConfigUtils.defaults();
        }

        /* 初始化表名相关 */
        initTableName(clazz, globalConfig, tableInfo);

        /* 初始化字段相关 */
        initTableFields(clazz, globalConfig, tableInfo);

        /* 放入缓存 */
        TABLE_INFO_CACHE.put(clazz, tableInfo);

        /* 缓存 lambda */
        LambdaUtils.installCache(tableInfo);

        /* 自动构建 resultMap */
        tableInfo.initResultMapIfNeed();

        return tableInfo;
    }

    /**
     * <p>
     * 初始化 表数据库类型,表名,resultMap
     * </p>
     *
     * @param clazz        实体类
     * @param globalConfig 全局配置
     * @param tableInfo    数据库表反射信息
     */
    private static void initTableName(Class<?> clazz, GlobalConfig globalConfig, TableInfo tableInfo) {
        /* 数据库全局配置 */
        GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();
        TableName table = clazz.getAnnotation(TableName.class);

        String tableName = clazz.getSimpleName();
        String tablePrefix = dbConfig.getTablePrefix();
        String schema = dbConfig.getSchema();
        boolean tablePrefixEffect = true;

        if (table != null) {
            if (StringUtils.isNotEmpty(table.value())) {
                tableName = table.value();
                if (StringUtils.isNotEmpty(tablePrefix) && !table.keepGlobalPrefix()) {
                    tablePrefixEffect = false;
                }
            } else {
                tableName = initTableNameWithDbConfig(tableName, dbConfig);
            }
            ...
        }
    }
}
```

