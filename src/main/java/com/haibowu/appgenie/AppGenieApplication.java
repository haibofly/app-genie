package com.haibowu.appgenie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class AppGenieApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppGenieApplication.class, args);
    }

}
