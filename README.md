## 使用文档

### 1、下载源码

### 2、安装依赖
	使用 mvn install将springboot-dynamic-core模块安装到本地maven仓库
### 3、引入依赖
	再需要扩展的springboot项目中引入如下依赖
	<dependency>
          <groupId>com.rdpaas</groupId>
          <artifactId>springboot-dynamic-core</artifactId>
          <version>1.0.0-BASE-SNAPSHOT</version>
    </dependency>

### 4、配置动态扩展包URL和扩展接口swagger扫描包
	
```
#这是扩展包的地址，是个URL
dynamic.jar=file:D:\\source\\github\\springboot-dynamic\\springboot-dynamic-demo-ext\\target\\springboot-dynamic-demo-ext-1.0.0-BASE-SNAPSHOT.jar
#这是扩展包的swagger接口扫描包
dynamic.swagger.doc.package=com.rdpaas.demo.ext.controller
```
### 5、打开扩展开关
	@EnableDynamic
### 6、模块说明
	springboot-dynamic-core 动态扩展核心依赖包
	springboot-dynamic-demo 待扩展的springboot示例项目
	springboot-dynamic-demo-ext 示例项目的示例扩展包
