package com.rdpaas.dynamic.core;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * mybatis的mapper.xml和@Mapper加载类
 * @author rongdi
 * @date 2021-03-06
 * @blog https://www.cnblogs.com/rongdi
 */
public class MapperLoader {

	private Logger logger = LoggerFactory.getLogger(MapperLoader.class);

    private Configuration configuration;

    /**
     * 刷新外部mapper，包括文件和@Mapper修饰的接口
     * @param sqlSessionFactory
     * @param xmlBytesMap
     * @return
     */
    public Map<String,Object> refresh(SqlSessionFactory sqlSessionFactory, Map<String, byte[]> xmlBytesMap) {
        Configuration configuration = sqlSessionFactory.getConfiguration();
        this.configuration = configuration;

        /**
         * 这里用来区分mybatis-plus和mybatis，mybatis-plus的Configuration是继承自mybatis的子类
         */
        boolean isSupper = configuration.getClass().getSuperclass() == Configuration.class;
        Map<String,Object> mapperMap = new HashMap<>();
        try {
            /**
             * 遍历外部传入的xml字节码map
             */
            for(Map.Entry<String,byte[]> entry:xmlBytesMap.entrySet()) {
                String resource = entry.getKey();
                byte[] bytes = entry.getValue();
                /**
                 * 使用反射强行拿出configuration中的loadedResources属性
                 */
                Field loadedResourcesField = isSupper
                        ? configuration.getClass().getSuperclass().getDeclaredField("loadedResources")
                        : configuration.getClass().getDeclaredField("loadedResources");
                loadedResourcesField.setAccessible(true);
                Set loadedResourcesSet = ((Set) loadedResourcesField.get(configuration));
                /**
                 * 加载mybatis中的xml
                 */
                XPathParser xPathParser = new XPathParser(new ByteArrayInputStream(bytes), true, configuration.getVariables(),
                        new XMLMapperEntityResolver());
                /**
                 * 解析mybatis的xml的根节点，
                 */
                XNode context = xPathParser.evalNode("/mapper");
                /**
                 * 拿到namespace，namespace就是指Mapper接口的全限定名
                 */
                String namespace = context.getStringAttribute("namespace");
                Field field = configuration.getMapperRegistry().getClass().getDeclaredField("knownMappers");
                field.setAccessible(true);

                /**
                 * 拿到存放Mapper接口和对应代理子类的映射map，
                 */
                Map mapConfig = (Map) field.get(configuration.getMapperRegistry());
                /**
                 * 拿到Mapper接口对应的class对象
                 */
                Class nsClass = Resources.classForName(namespace);

                /**
                 * 先删除各种
                 */
                mapConfig.remove(nsClass);
                loadedResourcesSet.remove(resource);
                configuration.getCacheNames().remove(namespace);

                /**
                 * 清掉namespace下各种缓存
                 */
                cleanParameterMap(context.evalNodes("/mapper/parameterMap"), namespace);
                cleanResultMap(context.evalNodes("/mapper/resultMap"), namespace);
                cleanKeyGenerators(context.evalNodes("insert|update|select|delete"), namespace);
                cleanSqlElement(context.evalNodes("/mapper/sql"), namespace);

                /**
                 * 加载并解析对应xml
                 */
                XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(new ByteArrayInputStream(bytes),
                        sqlSessionFactory.getConfiguration(), resource,
                        sqlSessionFactory.getConfiguration().getSqlFragments());
                xmlMapperBuilder.parse();

                /**
                 * 构造MapperFactoryBean，注意这里一定要传入sqlSessionFactory,
                 * 这块逻辑通过debug源码试验了很久
                 */
                MapperFactoryBean mapperFactoryBean = new MapperFactoryBean(nsClass);
                mapperFactoryBean.setSqlSessionFactory(sqlSessionFactory);
                /**
                 * 放入map，返回出去给ModuleApplication去加载
                 */
                mapperMap.put(namespace,mapperFactoryBean);
                logger.info("refresh: '" + resource + "', success!");

            }
            return mapperMap;
        } catch (Exception e) {
            logger.error("refresh error",e.getMessage());
        } finally {
            ErrorContext.instance().reset();
        }
        return null;
    }

    /**
     * 清理parameterMap
     *
     * @param list
     * @param namespace
     */
    private void cleanParameterMap(List<XNode> list, String namespace) {
        for (XNode parameterMapNode : list) {
            String id = parameterMapNode.getStringAttribute("id");
            configuration.getParameterMaps().remove(namespace + "." + id);
        }
    }

    /**
     * 清理resultMap
     *
     * @param list
     * @param namespace
     */
    private void cleanResultMap(List<XNode> list, String namespace) {
        for (XNode resultMapNode : list) {
            String id = resultMapNode.getStringAttribute("id", resultMapNode.getValueBasedIdentifier());
            configuration.getResultMapNames().remove(id);
            configuration.getResultMapNames().remove(namespace + "." + id);
            clearResultMap(resultMapNode, namespace);
        }
    }

    private void clearResultMap(XNode xNode, String namespace) {
        for (XNode resultChild : xNode.getChildren()) {
            if ("association".equals(resultChild.getName()) || "collection".equals(resultChild.getName())
                    || "case".equals(resultChild.getName())) {
                if (resultChild.getStringAttribute("select") == null) {
                    configuration.getResultMapNames()
                            .remove(resultChild.getStringAttribute("id", resultChild.getValueBasedIdentifier()));
                    configuration.getResultMapNames().remove(namespace + "."
                            + resultChild.getStringAttribute("id", resultChild.getValueBasedIdentifier()));
                    if (resultChild.getChildren() != null && !resultChild.getChildren().isEmpty()) {
                        clearResultMap(resultChild, namespace);
                    }
                }
            }
        }
    }

    /**
     * 清理selectKey
     *
     * @param list
     * @param namespace
     */
    private void cleanKeyGenerators(List<XNode> list, String namespace) {
        for (XNode context : list) {
            String id = context.getStringAttribute("id");
            configuration.getKeyGeneratorNames().remove(id + SelectKeyGenerator.SELECT_KEY_SUFFIX);
            configuration.getKeyGeneratorNames().remove(namespace + "." + id + SelectKeyGenerator.SELECT_KEY_SUFFIX);

            Collection<MappedStatement> mappedStatements = configuration.getMappedStatements();
            List<MappedStatement> objects = new ArrayList<>();
            Iterator<MappedStatement> it = mappedStatements.iterator();
            while (it.hasNext()) {
                Object object = it.next();
                if (object instanceof MappedStatement) {
                    MappedStatement mappedStatement = (MappedStatement) object;
                    if (mappedStatement.getId().equals(namespace + "." + id)) {
                        objects.add(mappedStatement);
                    }
                }
            }
            mappedStatements.removeAll(objects);
        }
    }

    /**
     * 清理sql节点缓存
     *
     * @param list
     * @param namespace
     */
    private void cleanSqlElement(List<XNode> list, String namespace) {
        for (XNode context : list) {
            String id = context.getStringAttribute("id");
            configuration.getSqlFragments().remove(id);
            configuration.getSqlFragments().remove(namespace + "." + id);
        }
    }

}