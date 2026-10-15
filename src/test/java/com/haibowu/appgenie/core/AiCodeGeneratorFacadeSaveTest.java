package com.haibowu.appgenie.core;

import cn.hutool.core.io.FileUtil;
import com.haibowu.appgenie.ai.AiCodeGeneratorService;
import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;
import com.haibowu.appgenie.ai.model.enums.CodeGenTypeEnum;
import com.haibowu.appgenie.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AiCodeGeneratorFacade} 流式"解析 + 落盘"逻辑的单元测试。
 *
 * <p>用假的模型流（手写 {@link AiCodeGeneratorService} 实现）驱动，不联网、不依赖 api-key，
 * 因此可以严格断言：流内容原样透传给调用方、流结束后按约定落盘、多文件模式三个文件内容都必须正确。
 * 真实模型能否按约定输出，由 {@link AiCodeGeneratorFacadeTest}（集成测试）负责。
 */
class AiCodeGeneratorFacadeSaveTest {

    private static final Path OUTPUT_ROOT = Paths.get(System.getProperty("user.dir"), "tmp", "code_output");

    /**
     * 本用例自己产生的输出目录，测试结束后清理，避免污染 tmp/code_output
     */
    private final List<Path> generatedDirs = new ArrayList<>();

    @AfterEach
    void cleanUpGeneratedDirs() {
        generatedDirs.forEach(dir -> FileUtil.del(dir.toFile()));
        generatedDirs.clear();
    }

    @Test
    @DisplayName("流式 HTML：流内容原样透传，流结束后保存解析出的 HTML（去掉说明文字和代码围栏）")
    void streamHtmlCodeShouldSaveParsedHtml() {
        String explanation = "好的，下面是校园表白墙的代码：";
        String html = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <body><h1>校园表白墙</h1></body>
                </html>""";
        String tail = "```html\n" + html + "\n```\n生成完成，直接保存为 index.html 即可。";
        FakeAiCodeGeneratorService service = FakeAiCodeGeneratorService.htmlStream(Flux.just(explanation, tail));
        AiCodeGeneratorFacade facade = facadeWith(service);
        Set<String> dirsBefore = snapshotOutputDirs();

        List<String> streamed = facade.generateAndSaveCodeStream("生成一个校园表白墙页面", CodeGenTypeEnum.HTML)
                .collectList().block();

        // 1. 调用方拿到的流内容必须是模型原文，不能被解析逻辑改动
        assertEquals(List.of(explanation, tail), streamed);
        // 2. 流结束后应把解析出的 HTML 落盘（说明文字、```html 围栏都不应写进文件）
        assertEquals(html, readFile(awaitNewOutputDir(dirsBefore, CodeGenTypeEnum.HTML).resolve("index.html")));
    }

    @Test
    @DisplayName("流式多文件：三个代码块必须分别落盘到 index.html / style.css / script.js")
    void streamMultiFileCodeShouldSaveThreeFiles() {
        String html = """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head><link rel="stylesheet" href="style.css"></head>
                <body><h1>个人主页</h1><script src="script.js"></script></body>
                </html>""";
        String css = "h1 {\n    color: #2563eb;\n}";
        String js = "console.log('个人主页加载完成');";
        FakeAiCodeGeneratorService service = FakeAiCodeGeneratorService.multiFileStream(Flux.just(
                "这是三个文件：\n```html\n" + html + "\n```\n",
                "```css\n" + css + "\n```\n",
                "```javascript\n" + js + "\n```\n完成。"));
        AiCodeGeneratorFacade facade = facadeWith(service);
        Set<String> dirsBefore = snapshotOutputDirs();

        List<String> streamed = facade.generateAndSaveCodeStream("生成一个个人主页", CodeGenTypeEnum.MULTI_WEB_FILE)
                .collectList().block();

        assertNotNull(streamed, "流式结果不应为空");
        assertFalse(streamed.isEmpty(), "流式结果不应为空");
        Path savedDir = awaitNewOutputDir(dirsBefore, CodeGenTypeEnum.MULTI_WEB_FILE);
        assertEquals(html, readFile(savedDir.resolve("index.html")));
        assertEquals(css, readFile(savedDir.resolve("style.css")));
        assertEquals(js, readFile(savedDir.resolve("script.js")));
    }

    @Test
    @DisplayName("流式 HTML：模型返回不含代码块的内容时走兜底逻辑，整段内容写入 index.html")
    void streamHtmlCodeWithoutFenceShouldFallBackToWholeContent() {
        String content = "抱歉，我无法生成代码。";
        FakeAiCodeGeneratorService service = FakeAiCodeGeneratorService.htmlStream(Flux.just(content));
        AiCodeGeneratorFacade facade = facadeWith(service);
        Set<String> dirsBefore = snapshotOutputDirs();

        facade.generateAndSaveCodeStream("生成一个页面", CodeGenTypeEnum.HTML).collectList().block();

        assertEquals(content, readFile(awaitNewOutputDir(dirsBefore, CodeGenTypeEnum.HTML).resolve("index.html")));
    }

    @Test
    @DisplayName("流式生成失败：异常向上抛出，且不会落盘任何文件")
    void streamErrorShouldPropagateAndSaveNothing() {
        FakeAiCodeGeneratorService service = FakeAiCodeGeneratorService
                .htmlStream(Flux.error(new IllegalStateException("模型服务不可用")));
        AiCodeGeneratorFacade facade = facadeWith(service);
        Set<String> dirsBefore = snapshotOutputDirs();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> facade.generateAndSaveCodeStream("生成一个页面", CodeGenTypeEnum.HTML).collectList().block());

        assertEquals("模型服务不可用", exception.getMessage());
        assertEquals(dirsBefore, snapshotOutputDirs(), "生成失败时不应产生输出目录");
    }

    @Test
    @DisplayName("生成类型为 null：两个入口都抛 BusinessException，且不会调用模型")
    void nullCodeGenTypeShouldBeRejectedWithoutCallingModel() {
        FakeAiCodeGeneratorService service = FakeAiCodeGeneratorService.htmlStream(Flux.just("不会用到"));
        AiCodeGeneratorFacade facade = facadeWith(service);

        BusinessException syncException = assertThrows(BusinessException.class,
                () -> facade.generateAndSaveCode("生成一个页面", null));
        BusinessException streamException = assertThrows(BusinessException.class,
                () -> facade.generateAndSaveCodeStream("生成一个页面", null));

        assertEquals("生成类型为空", syncException.getMessage());
        assertEquals("生成类型为空", streamException.getMessage());
        assertEquals(0, service.invocationCount(), "参数校验失败时不应调用模型");
    }

    // ==================== 辅助方法 ====================

    private AiCodeGeneratorFacade facadeWith(AiCodeGeneratorService service) {
        AiCodeGeneratorFacade facade = new AiCodeGeneratorFacade();
        ReflectionTestUtils.setField(facade, "aiCodeGeneratorService", service);
        return facade;
    }

    private static String readFile(Path path) {
        assertTrue(Files.isRegularFile(path), "文件不存在：" + path);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("读取文件失败：" + path, e);
        }
    }

    private static Set<String> snapshotOutputDirs() {
        if (!Files.isDirectory(OUTPUT_ROOT)) {
            return Set.of();
        }
        try (Stream<Path> paths = Files.list(OUTPUT_ROOT)) {
            return paths.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException("读取代码输出目录失败：" + OUTPUT_ROOT, e);
        }
    }

    /**
     * 找到本次新增的输出目录并登记，测试结束后由 {@link #cleanUpGeneratedDirs()} 清理
     */
    private Path awaitNewOutputDir(Set<String> dirsBefore, CodeGenTypeEnum codeGenType) {
        Set<String> newDirs = new HashSet<>(snapshotOutputDirs());
        newDirs.removeAll(dirsBefore);
        newDirs.removeIf(name -> !name.startsWith(codeGenType.getValue() + "_"));
        assertEquals(1, newDirs.size(), "应生成且只生成一个输出目录，实际：" + newDirs);
        Path savedDir = OUTPUT_ROOT.resolve(newDirs.iterator().next());
        generatedDirs.add(savedDir);
        return savedDir;
    }

    /**
     * 手写的假 {@link AiCodeGeneratorService}：流式方法返回给定内容，非流式方法不会被本用例调用；
     * 同时记录调用次数，用于断言"参数校验失败时不会调用模型"。
     */
    private static final class FakeAiCodeGeneratorService implements AiCodeGeneratorService {

        private final Flux<String> htmlCodeStream;
        private final Flux<String> multiWebFileCodeStream;
        private final AtomicInteger invocationCount = new AtomicInteger();

        private FakeAiCodeGeneratorService(Flux<String> htmlCodeStream, Flux<String> multiWebFileCodeStream) {
            this.htmlCodeStream = htmlCodeStream;
            this.multiWebFileCodeStream = multiWebFileCodeStream;
        }

        static FakeAiCodeGeneratorService htmlStream(Flux<String> htmlCodeStream) {
            return new FakeAiCodeGeneratorService(htmlCodeStream, Flux.empty());
        }

        static FakeAiCodeGeneratorService multiFileStream(Flux<String> multiWebFileCodeStream) {
            return new FakeAiCodeGeneratorService(Flux.empty(), multiWebFileCodeStream);
        }

        int invocationCount() {
            return invocationCount.get();
        }

        @Override
        public HtmlCodeResult generateHtmlCode(String userMessage) {
            invocationCount.incrementAndGet();
            throw new UnsupportedOperationException("本用例只验证流式链路");
        }

        @Override
        public MultiWebFileCodeResult generateMultiWebFileCode(String userMessage) {
            invocationCount.incrementAndGet();
            throw new UnsupportedOperationException("本用例只验证流式链路");
        }

        @Override
        public Flux<String> generateHtmlCodeStream(String userMessage) {
            invocationCount.incrementAndGet();
            return htmlCodeStream;
        }

        @Override
        public Flux<String> generateMultiWebFileCodeStream(String userMessage) {
            invocationCount.incrementAndGet();
            return multiWebFileCodeStream;
        }
    }
}
