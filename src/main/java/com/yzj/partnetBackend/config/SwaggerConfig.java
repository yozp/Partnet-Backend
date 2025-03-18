package com.yzj.partnetBackend.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.License;

/**
 * 自定义接口文档的配置
 */
//http://localhost:8080/api/doc.html#/home
//http://localhost:8081/api/doc.html#/home
@Configuration//声明配置类
//@EnableSwagger2WebMvc// 开启swagger的自动配置
@Profile({"dev", "test"})   //版本控制访问
public class SwaggerConfig {

//    @Bean
//    public Docket docket(){
//
//        // 创建一个 swagger 的 bean 实例
//        return new Docket(DocumentationType.SWAGGER_2)
//
//                // 配置接口信息
//                .select() // 设置扫描接口
//                // 配置如何扫描接口
//                .apis(RequestHandlerSelectors
//                                //.any() // 扫描全部的接口，默认
//                                //.none() // 全部不扫描
//                                .basePackage("com.yzj.partnetBackend.controller") // 扫描指定包下的接口，最为常用
//                        //.withClassAnnotation(RestController.class) // 扫描带有指定注解的类下所有接口
//                        //.withMethodAnnotation(PostMapping.class) // 扫描带有只当注解的方法接口
//
//                )
//                .paths(PathSelectors
//                                .any() // 满足条件的路径，该断言总为true
//                        //.none() // 不满足条件的路径，该断言总为false（可用于生成环境屏蔽 swagger）
//                        //.ant("/user/**") // 满足字符串表达式路径
//                        //.regex("") // 符合正则的路径
//                )
//                .build();
//    }
//
//    // 基本信息设置
//    private ApiInfo apiInfo() {
//        Contact contact = new Contact(
//                "yzj", // 作者姓名
//                "yzj.cn", // 作者网址
//                "yzj@qq.com"); // 作者邮箱
//        return new ApiInfoBuilder()
//                .title("伙伴匹配系统-接口文档") // 标题
//                .description("众里寻他千百度，慕然回首那人却在灯火阑珊处") // 描述
//                .termsOfServiceUrl("https://www.baidu.com") // 跳转连接
//                .version("1.0") // 版本
//                .license("Swagger-的使用(详细教程)")
//                .licenseUrl("https://blog.csdn.net/xhmico/article/details/125353535")
//                .contact(contact)
//                .build();
//    }

//    @Bean(value = "defaultApi2")
//    public Docket defaultApi2() {
//        return new Docket(DocumentationType.SWAGGER_2)
//                .apiInfo(apiInfo())
//                .select()
//                // 这里一定要标注你控制器的位置
//                .apis(RequestHandlerSelectors.basePackage("com.yzj.partnetBackend.controller"))
//                .paths(PathSelectors.any())
//                .build();
//    }
//
//    /**
//     * api 信息
//     * @return
//     */
//    private ApiInfo apiInfo() {
//        return new ApiInfoBuilder()
//                .title("鱼皮用户中心")
//                .description("鱼皮用户中心接口文档")
//                .termsOfServiceUrl("https://github.com/liyupi")
//                .contact(new Contact("shayu","https://shayuyu.cn/","shayu-yusha@qq.com"))
//                .version("1.0")
//                .build();
//    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                // 分组名称
                .group("app-api")
                // 接口请求路径规则
                .pathsToMatch("/**")
                .build();
    }

    /**
     * 配置基本信息
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        // 标题
                        .title("伙伴匹配系统 Api接口文档")
                        // 描述Api接口文档的基本信息
                        .description("Knife4j后端接口服务...")
                        // 版本
                        .version("v1.0.0")
                        // 设置OpenAPI文档的联系信息，姓名，邮箱。
                        .contact(new Contact().name("Hva").email("Hva@163.com"))
                        // 设置OpenAPI文档的许可证信息，包括许可证名称为"Apache 2.0"，许可证URL为"http://springdoc.org"。
                        .license(new License().name("Apache 2.0").url("http://springdoc.org"))
                );
    }


}
