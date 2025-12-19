1. 拦截来在API网关的请求，从请求头中提取用户上下文
2. 拦截发往其他微服务的Feign请求，添加用户上下文到请求头

1. 分支管理和version管理
 - 是为了解决什么问题
    - 兼顾开发的效率和生产的稳定
    - 发生bug或故障的时候能基于分支或版本号排查


关键工具：用 Maven 插件自动管理版本（避免手动改 pom）
手动改 pom 版本号易出错，推荐用versions-maven-plugin插件自动修改，配合分支关联规则：
1. 插件配置（common-core/pom.xml）
   xml
   <build>
   <plugins>
   <!-- 版本管理插件 -->
   <plugin>
   <groupId>org.codehaus.mojo</groupId>
   <artifactId>versions-maven-plugin</artifactId>
   <version>2.16.0</version>
   </plugin>
   </plugins>
   </build>
   https://www.atlassian.com/zh/git/tutorials/comparing-workflows/gitflow-workflow