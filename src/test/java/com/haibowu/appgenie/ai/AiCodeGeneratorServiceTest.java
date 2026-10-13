package com.haibowu.appgenie.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        String result = aiCodeGeneratorService.generateHtmlCode("做一个校园表白墙页面");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiWebFileCode() {
        String result = aiCodeGeneratorService.generateMultiWebFileCode("做一个K8s集群管理看板");
        Assertions.assertNotNull(result);
    }
}