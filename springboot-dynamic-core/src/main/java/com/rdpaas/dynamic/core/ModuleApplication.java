package com.rdpaas.dynamic.core;

import com.rdpaas.dynamic.utils.ReflectUtil;
import com.rdpaas.dynamic.utils.SpringUtil;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.plugin.core.PluginRegistry;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseMessageBuilder;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.ResponseMessage;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.DocumentationPlugin;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.DocumentationPluginsBootstrapper;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 基于spring的应用上下文提供一些工具方法
 * @author rongdi
 * @date 2021-03-06
 * @blog https://www.cnblogs.com/rongdi
 */
public class ModuleApplication {

    private final static String SINGLETON = "singleton";

    private final static String DYNAMIC_DOC_PACKAGE = "dynamic.swagger.doc.package";

    private Set<RequestMappingInfo> extMappingInfos = new HashSet<>();

    private ApplicationContext applicationContext;

    /**
     * 使用spring上下文拿到指定beanName的对象
     */
    public <T> T getBean(String beanName) {
        return (T) ((ConfigurableApplicationContext) applicationContext).getBeanFactory().getBean(beanName);
    }

    /**
     * 使用spring上下文拿到指定类型的对象
     */
    public <T> T getBean(Class<T> clazz) {
        return (T) ((ConfigurableApplicationContext) applicationContext).getBeanFactory().getBean(clazz);
    }

    /**
     * 加载一个外部扩展jar，包括springmvc接口资源，mybatis的@mapper和mapper.xml和spring bean等资源
     * @param url jar url
     * @param applicationContext spring context
     * @param sqlSessionFactory mybatis的session工厂
     */
    public void reloadJar(URL url, ApplicationContext applicationContext,SqlSessionFactory sqlSessionFactory) throws Exception {
        this.applicationContext = applicationContext;
        URL[] urls = new URL[]{url};
        /**
         * 这里实际上是将spring的ApplicationContext的类加载器当成parent传给了自定义类加载器，很明自定义的子类加载器自己加载
         * 的类，parent类加载器直接是获取不到的，所以在自定义类加载器做了特殊的骚操作
         */
        ModuleClassLoader moduleClassLoader = new ModuleClassLoader(applicationContext.getClassLoader(), urls);
        /**
         * 使用模块类加载器加载url资源的jar包，直接返回类的全限定名和Class对象的映射，这些Class对象是
         * jar包里所有.class结尾的文件加载后的结果,同时mybatis的xml加载后，无奈的放入了
         * moduleClassLoader.getXmlBytesMap()，不是很优雅
         */
        Map<String, Class> classMap = moduleClassLoader.load();

        MapperLoader mapperLoader = new MapperLoader();

        /**
         * 刷新mybatis的xml和Mapper接口资源，Mapper接口其实就是xml的namespace
         */
        Map<String, Object> extObjMap = mapperLoader.refresh(sqlSessionFactory, moduleClassLoader.getXmlBytesMap());
        /**
         * 将各种资源放入spring容器
         */
        registerBeans(applicationContext, classMap, extObjMap);
    }

    /**
     * 装载bean到spring中
     *
     * @param applicationContext
     * @param cacheClassMap
     */
    public void registerBeans(ApplicationContext applicationContext, Map<String, Class> cacheClassMap,Map<String,Object> extObjMap) throws Exception {
        /**
         * 将applicationContext转换为ConfigurableApplicationContext
         */
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        /**
         * 获取bean工厂并转换为DefaultListableBeanFactory
         */
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();

        /**
         * 有一些对象想给spring管理，则放入spring中，如mybatis的@Mapper修饰的接口的代理类
         */
        if(extObjMap != null && !extObjMap.isEmpty()) {
            extObjMap.forEach((beanName,obj) ->{
                /**
                 * 如果已经存在，则销毁之后再注册
                 */
                if(defaultListableBeanFactory.containsSingleton(beanName)) {
                    defaultListableBeanFactory.destroySingleton(beanName);
                }
                defaultListableBeanFactory.registerSingleton(beanName,obj);
            });
        }

        for (Map.Entry<String, Class> entry : cacheClassMap.entrySet()) {
            String className = entry.getKey();
            Class<?> clazz = entry.getValue();
            if (SpringUtil.isSpringBeanClass(clazz)) {
                //将变量首字母置小写
                String beanName = StringUtils.uncapitalize(className);
                beanName = beanName.substring(beanName.lastIndexOf(".") + 1);
                beanName = StringUtils.uncapitalize(beanName);

               /**
                 * 已经在spring容器就删了
                 */
                if (defaultListableBeanFactory.containsBeanDefinition(beanName)) {
                    defaultListableBeanFactory.removeBeanDefinition(beanName);
                }
                /**
                 * 使用spring的BeanDefinitionBuilder将Class对象转成BeanDefinition
                 */
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
                BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
                //设置当前bean定义对象是单利的
                beanDefinition.setScope(SINGLETON);
                /**
                 * 以指定beanName注册上面生成的BeanDefinition
                 */
                defaultListableBeanFactory.registerBeanDefinition(beanName, beanDefinition);
            }

        }

        /**
         * 刷新springmvc，让新增的接口生效
         */
        refreshMVC((ConfigurableApplicationContext) applicationContext);

    }

    /**
     * 刷新springMVC,这里花了大量时间调试，找不到开放的方法，只能取个巧，在更新RequestMappingHandlerMapping前先记录之前
     * 所有RequestMappingInfo，记得这里一定要copy一下，然后刷新后再记录一次，计算出差量存放在成员变量Set中，然后每次开头判断
     * 差量那里是否有内容，有就先unregiester掉
     */
    private void refreshMVC(ConfigurableApplicationContext applicationContext) throws Exception {


        Map<String, RequestMappingHandlerMapping> map = applicationContext.getBeanFactory().getBeansOfType(RequestMappingHandlerMapping.class);
        /**
         * 先拿到RequestMappingHandlerMapping对象
         */
        RequestMappingHandlerMapping mappingHandlerMapping = map.get("requestMappingHandlerMapping");

        /**
         * 重新注册mapping前先判断是否存在了，存在了就先unregister掉
         */
        if(!extMappingInfos.isEmpty()) {
            for(RequestMappingInfo requestMappingInfo:extMappingInfos) {
                mappingHandlerMapping.unregisterMapping(requestMappingInfo);
            }
        }

        /**
         * 获取刷新前的RequestMappingInfo
         */
        Map<RequestMappingInfo, HandlerMethod> preMappingInfoHandlerMethodMap = mappingHandlerMapping.getHandlerMethods();
        /**
         * 这里注意一定要拿到拷贝，不然刷新后内容就一致了，就没有差量了
         */
        Set<RequestMappingInfo> preRequestMappingInfoSet = new HashSet(preMappingInfoHandlerMethodMap.keySet());

        /**
         * 这里是刷新springmvc上下文
         */
        applicationContext.getBeanFactory().getBeansOfType(RequestMappingHandlerMapping.class)
        .forEach((key,value) ->{
            value.afterPropertiesSet();
        });

        /**
         * 获取刷新后的RequestMappingInfo
         */
        Map<RequestMappingInfo, HandlerMethod> afterMappingInfoHandlerMethodMap = mappingHandlerMapping.getHandlerMethods();
        Set<RequestMappingInfo> afterRequestMappingInfoSet = afterMappingInfoHandlerMethodMap.keySet();

        /**
         * 填充差量部分RequestMappingInfo
          */
        fillSurplusRequestMappingInfos(preRequestMappingInfoSet,afterRequestMappingInfoSet);

        /**
         * 这里真的是不讲武德了，每次调用value.afterPropertiesSet();如下urlLookup都会产生重复，暂时没找到开放方法去掉重复，这里重复会导致
         * 访问的时候报错Ambiguous handler methods mapped for
         * 目标是去掉RequestMappingHandlerMapping -> RequestMappingInfoHandlerMapping -> AbstractHandlerMethodMapping
         * -> mappingRegistry -> urlLookup重复的RequestMappingInfo,这里的.getClass().getSuperclass().getSuperclass()相信会
         * 很懵逼，如果单独通过getClass().getDeclaredMethod("getMappingRegistry",new Class[]{})是无论如何都拿不到父类的非public非
         * protected方法的，因为这个方法不属于子类，只有父类才可以访问到，只有你拿得到你才有资格不讲武德的使用method.setAccessible(true)强行
         * 访问
         */
        Method method = ReflectUtil.getMethod(mappingHandlerMapping,"getMappingRegistry",new Class[]{});
        method.setAccessible(true);
        Object mappingRegistryObj = method.invoke(mappingHandlerMapping,new Object[]{});
        Field field = mappingRegistryObj.getClass().getDeclaredField("urlLookup");
        field.setAccessible(true);
        MultiValueMap<String, RequestMappingInfo> multiValueMap = (MultiValueMap)field.get(mappingRegistryObj);
        multiValueMap.forEach((key,list) -> {
            clearMultyMapping(list);
        });

        /**
         * 刷新swagger文档
         */
        refreshSwagger(applicationContext);
    }


    /**
     * 刷新swagger文档
     * @param applicationContext
     * @throws Exception
     */
    private void refreshSwagger(ConfigurableApplicationContext applicationContext) throws Exception {
        /**
         * 获取扩展包swagger的地址接口扫描包，如果有配置则执行文档刷新操作
         */
        String extSwaggerDocPackage = applicationContext.getEnvironment().getProperty(DYNAMIC_DOC_PACKAGE);
        if (!StringUtils.isEmpty(extSwaggerDocPackage)) {
            /**
             * 拿到swagger解析文档的入口类，真的不想这样，主要是根本不提供刷新和重新加载文档的方法，只能不讲武德了
             */
            DocumentationPluginsBootstrapper bootstrapper = applicationContext.getBeanFactory().getBean(DocumentationPluginsBootstrapper.class);
            /**
             * 不管愿不愿意，强行拿到属性得到documentationPluginsManager对象
             */
            Field field1 = bootstrapper.getClass().getDeclaredField("documentationPluginsManager");
            field1.setAccessible(true);
            DocumentationPluginsManager documentationPluginsManager = (DocumentationPluginsManager) field1.get(bootstrapper);

            /**
             * 继续往下层拿documentationPlugins属性
             */
            Field field2 = documentationPluginsManager.getClass().getDeclaredField("documentationPlugins");
            field2.setAccessible(true);
            PluginRegistry<DocumentationPlugin, DocumentationType> pluginRegistrys = (PluginRegistry<DocumentationPlugin, DocumentationType>) field2.get(documentationPluginsManager);
            /**
             * 拿到最关键的文档插件集合，所有逻辑文档解析逻辑都在插件中
             */
            List<DocumentationPlugin> dockets = pluginRegistrys.getPlugins();
            /**
             * 真的不能怪我，好端端，你还搞个不能修改的集合，强行往父类递归拿到unmodifiableList的list属性
             */
            Field unModList = ReflectUtil.getField(dockets,"list");
            unModList.setAccessible(true);
            List<DocumentationPlugin> modifyerList = (List<DocumentationPlugin>) unModList.get(dockets);
            /**
             * 清掉已经存在的dynamic分组
             */
            removeGroup(modifyerList,"dynamic");
            /**
             * 这下老实了吧，把自己的Docket加入进去，这里的groupName为dynamic
             */
            modifyerList.add(createRestApi(extSwaggerDocPackage));
            /**
             * 清空罪魁祸首DocumentationCache缓存，不然就算再加载一次，获取文档还是从这个缓存中拿，不会完成更新
             */
            bootstrapper.stop();
            /**
             * 手动执行重新解析swagger文档
             */
            bootstrapper.start();
        }
    }

    public Docket createRestApi(String basePackage) {
        List<ResponseMessage> responseMessageList = new ArrayList<>();
        responseMessageList.add(new ResponseMessageBuilder().code(200).message("成功").responseModel(new ModelRef("Payload")).build());
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName("dynamic")
                .globalResponseMessage(RequestMethod.GET,responseMessageList)
                .globalResponseMessage(RequestMethod.DELETE,responseMessageList)
                .globalResponseMessage(RequestMethod.POST,responseMessageList)
                .apiInfo(apiInfo()).select()
                //为当前包路径
                .apis(RequestHandlerSelectors.basePackage(basePackage)).paths(PathSelectors.any()).build();
        return docket;
    }

    /**
     * 删除集合中指定group
     * @param dockets
     * @param groupName
     */
    private void removeGroup(List<DocumentationPlugin> dockets, String groupName) {
        List<DocumentationPlugin> deletingPlugins = new ArrayList<>();
        for(DocumentationPlugin docket:dockets) {
            if(docket.getGroupName().equals(groupName)) {
                deletingPlugins.add(docket);
            }
        }
        dockets.removeAll(deletingPlugins);
    }

    /**
     * 构建api文档的详细信息函数
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                //页面标题
                .title("SpringBoot动态扩展")
                //创建人
                .contact(new Contact("rongdi", "https://www.cnblogs.com/rongdi", "495194630@qq.com"))
                //版本号
                .version("1.0")
                //描述
                .description("api管理").build();
    }

    /**
     * 填充差量的RequestMappingInfo，因为已经重写过hashCode和equals方法所以可以直接用对象判断是否存在
     * @param preRequestMappingInfoSet
     * @param afterRequestMappingInfoSet
     */
    private void fillSurplusRequestMappingInfos(Set<RequestMappingInfo> preRequestMappingInfoSet,Set<RequestMappingInfo> afterRequestMappingInfoSet) {
        for(RequestMappingInfo requestMappingInfo:afterRequestMappingInfoSet) {
            if(!preRequestMappingInfoSet.contains(requestMappingInfo)) {
                extMappingInfos.add(requestMappingInfo);
            }
        }
    }

    /**
     * 简单的逻辑，删除List里重复的RequestMappingInfo，已经写了toString，直接使用mappingInfo.toString()就可以区分重复了
     * @param mappingInfos
     */
    private void clearMultyMapping(List<RequestMappingInfo> mappingInfos) {
        Set<String> containsList = new HashSet<>();
        for(Iterator<RequestMappingInfo> iter = mappingInfos.iterator();iter.hasNext();) {
            RequestMappingInfo mappingInfo = iter.next();
            String flag = mappingInfo.toString();
            if(containsList.contains(flag)) {
                iter.remove();
            } else {
                containsList.add(flag);
            }
        }
    }

}
