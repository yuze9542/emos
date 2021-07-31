package com.example.emos.wx.config;


import io.swagger.annotations.ApiOperation;
import io.swagger.models.Swagger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.ArrayList;
import java.util.List;

import static javax.print.attribute.standard.MediaSizeName.A;

/**
 * 用于创建swagger
 */
@Configuration //swagger第一步 配置类
@EnableSwagger2 // 生效！
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2);
        // ApiInfoBuilder 用于在Swagger界面上添加各种信息
        ApiInfoBuilder builder = new ApiInfoBuilder();
        builder.title("EMOS 在线办公"); // 项目描述信息
        ApiInfo info = builder.build(); // builder封装到 ApiInfo 里
        docket.apiInfo(info);

        //设置另外的信息  比如某个包某个方法封装到swagger上
        // ApiSelectorBuilder 用来设置哪些类中的方法会生成到REST API中
        ApiSelectorBuilder apiSelectorBuilder = docket.select();
        apiSelectorBuilder.paths(PathSelectors.any());//所有包的所有类
        //特定注解？？
        //使用@ApiOperation的方法会被提取到REST API中
        apiSelectorBuilder.apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class));
        docket = apiSelectorBuilder.build(); // 添加到docket里面

        // Swagger支持jwt
        /*
         * 下面的语句是开启对JWT的支持，当用户用Swagger调用受JWT认证保护的方法，
         * 必须要先提交参数（例如令牌）
         */
        // 令牌 因为这个项目设置了jwt 通不过jwt的不能访问相应功能吧？？
        ApiKey apiKey = new ApiKey("token", "token", "header");
        //存储用户必须提交的参数
        List<ApiKey> apiKeyList = new ArrayList<>();
        //规定用户需要输入什么参数
        apiKeyList.add(apiKey);
        docket.securitySchemes(apiKeyList); // 要放到数组里

        //令牌作用域
        //如果用户JWT认证通过，则在Swagger中全局有效
        AuthorizationScope scope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] scopes = {scope};
        //存储令牌和作用域
        SecurityReference reference = new SecurityReference("token", scopes);
        List refList = new ArrayList();
        refList.add(reference);
        SecurityContext context = SecurityContext.builder().securityReferences(refList).build();
        List cxtList = new ArrayList();
        cxtList.add(context);
        System.out.println(context);
        docket.securityContexts(cxtList); // 这一步完成了单点登录？没有！！ 只是swagger开启了jwt

        return docket;
    }
}
