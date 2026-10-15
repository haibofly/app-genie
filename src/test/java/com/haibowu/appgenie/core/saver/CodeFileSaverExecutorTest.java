package com.haibowu.appgenie.core.saver;

import cn.hutool.core.io.FileUtil;
import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;
import com.haibowu.appgenie.ai.model.enums.CodeGenTypeEnum;
import com.haibowu.appgenie.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 保存器模板与执行器的单元测试：不联网，验证
 * {@link HtmlCodeFileSaver} / {@link MultiWebCodeFileSaver} / {@link CodeFileSaverExecutor} 的落盘与校验行为。
 */
class CodeFileSaverExecutorTest {

    /**
     * 代码保存根目录，与 {@link CodeFileSaverTemplate#FILE_SAVE_ROOT_DIR} 保持一致
     */
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

    // ==================== HTML 模式 ====================

    @Test
    @DisplayName("HTML 保存：创建目录并写入 index.html，内容一致")
    void executeHtmlShouldSaveIndexHtml() {
        String html = "<!DOCTYPE html>\n<html><body><h1>Hi</h1></body></html>";
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode(html);

        File savedDir = CodeFileSaverExecutor.execute(result, CodeGenTypeEnum.HTML);
        register(savedDir);

        assertSavedDir(savedDir, CodeGenTypeEnum.HTML);
        assertEquals(html, readFile(savedDir.toPath().resolve("index.html")));
    }

    @Test
    @DisplayName("HTML 保存：htmlCode 为空时抛 BusinessException，且不创建目录")
    void executeHtmlWithBlankHtmlShouldThrow() {
        HtmlCodeResult result = new HtmlCodeResult();
        result.setHtmlCode("   ");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> CodeFileSaverExecutor.execute(result, CodeGenTypeEnum.HTML));
        assertEquals("HTML Code Cannot Be Empty", exception.getMessage());
    }

    // ==================== 多文件模式 ====================

    @Test
    @DisplayName("多文件保存：写入 index.html / style.css / script.js 三个文件")
    void executeMultiFileShouldSaveThreeFiles() {
        String html = "<!DOCTYPE html>\n<html><body><h1>Hi</h1></body></html>";
        String css = "h1 { color: red; }";
        String js = "console.log('hi');";
        MultiWebFileCodeResult result = new MultiWebFileCodeResult();
        result.setHtmlCode(html);
        result.setCssCode(css);
        result.setJsCode(js);

        File savedDir = CodeFileSaverExecutor.execute(result, CodeGenTypeEnum.MULTI_WEB_FILE);
        register(savedDir);

        assertSavedDir(savedDir, CodeGenTypeEnum.MULTI_WEB_FILE);
        Path dir = savedDir.toPath();
        assertEquals(html, readFile(dir.resolve("index.html")));
        assertEquals(css, readFile(dir.resolve("style.css")));
        assertEquals(js, readFile(dir.resolve("script.js")));
    }

    @Test
    @DisplayName("多文件保存：CSS/JS 为空时只写 index.html，不产生空文件")
    void executeMultiFileShouldSkipBlankCssJs() {
        MultiWebFileCodeResult result = new MultiWebFileCodeResult();
        result.setHtmlCode("<html><body>hi</body></html>");

        File savedDir = CodeFileSaverExecutor.execute(result, CodeGenTypeEnum.MULTI_WEB_FILE);
        register(savedDir);

        Path dir = savedDir.toPath();
        assertTrue(Files.isRegularFile(dir.resolve("index.html")));
        assertFalse(Files.exists(dir.resolve("style.css")));
        assertFalse(Files.exists(dir.resolve("script.js")));
    }

    @Test
    @DisplayName("多文件保存：htmlCode 为空时抛 BusinessException（即使 CSS/JS 存在）")
    void executeMultiFileWithBlankHtmlShouldThrow() {
        MultiWebFileCodeResult result = new MultiWebFileCodeResult();
        result.setCssCode("h1{color:red}");
        result.setJsCode("console.log(1)");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> CodeFileSaverExecutor.execute(result, CodeGenTypeEnum.MULTI_WEB_FILE));
        assertEquals("HTML Code Cannot Be Empty", exception.getMessage());
    }

    // ==================== 通用校验 ====================

    @Test
    @DisplayName("保存：结果为 null 时抛 BusinessException")
    void executeWithNullResultShouldThrow() {
        BusinessException htmlException = assertThrows(BusinessException.class,
                () -> CodeFileSaverExecutor.execute(null, CodeGenTypeEnum.HTML));
        assertEquals("Generated Code Cannot Be Null.", htmlException.getMessage());

        BusinessException multiException = assertThrows(BusinessException.class,
                () -> CodeFileSaverExecutor.execute(null, CodeGenTypeEnum.MULTI_WEB_FILE));
        assertEquals("Generated Code Cannot Be Null.", multiException.getMessage());
    }

    // ==================== 辅助方法 ====================

    private void register(File savedDir) {
        generatedDirs.add(savedDir.toPath());
    }

    private static void assertSavedDir(File savedDir, CodeGenTypeEnum codeGenType) {
        assertNotNull(savedDir, "保存目录不应为空");
        assertTrue(savedDir.isDirectory(), "保存结果应为目录：" + savedDir);
        assertTrue(savedDir.getName().startsWith(codeGenType.getValue() + "_"),
                "目录名应以业务类型为前缀，实际为：" + savedDir.getName());
        Path path = savedDir.toPath().toAbsolutePath().normalize();
        assertTrue(path.startsWith(OUTPUT_ROOT.toAbsolutePath().normalize()),
                "文件应保存在 " + OUTPUT_ROOT + " 下，实际为：" + path);
    }

    private static String readFile(Path path) {
        assertTrue(Files.isRegularFile(path), "文件不存在：" + path);
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("读取文件失败：" + path, e);
        }
    }
}
