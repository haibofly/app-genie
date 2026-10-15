package com.haibowu.appgenie.core.parser;

import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;
import com.haibowu.appgenie.ai.model.enums.CodeGenTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解析器策略与执行器的单元测试：不联网，直接验证
 * {@link HtmlCodeParser} / {@link MultiWebFileCodeParser} / {@link CodeParserExecutor} 的解析行为。
 */
class CodeParserExecutorTest {

    // ==================== HTML 模式 ====================

    @Test
    void executeHtmlShouldExtractHtmlCodeBlock() {
        String codeContent = """
                这是一段说明：
                ```html
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                ```
                生成完成""";
        HtmlCodeResult result = (HtmlCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.HTML);
        assertEquals("""
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>""", result.getHtmlCode());
    }

    @Test
    void executeHtmlShouldFallBackToWholeContentWhenNoFence() {
        String codeContent = "抱歉，我无法生成代码。";
        HtmlCodeResult result = (HtmlCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.HTML);
        assertEquals(codeContent, result.getHtmlCode());
    }

    @Test
    void executeHtmlShouldHandleNoLineBreakAfterTag() {
        // 语言标签后不换行，直接跟 HTML 标签
        String codeContent = "```html<!DOCTYPE html>\n<html></html>```";
        assertEquals("<!DOCTYPE html>\n<html></html>",
                ((HtmlCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.HTML)).getHtmlCode());
    }

    @Test
    void executeHtmlShouldHandleCrlfFence() {
        String codeContent = "说明\r\n```html\r\n<html>\r\n<body>Hi</body>\r\n</html>\r\n```";
        assertEquals("<html>\r\n<body>Hi</body>\r\n</html>",
                ((HtmlCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.HTML)).getHtmlCode());
    }

    @Test
    void executeHtmlShouldHandleUnclosedFence() {
        // 模型漏写结尾围栏（或流式输出被截断）
        String codeContent = "说明\n```html\n<html><body>Hi</body></html>";
        assertEquals("<html><body>Hi</body></html>",
                ((HtmlCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.HTML)).getHtmlCode());
    }

    @Test
    void executeHtmlShouldHandleUppercaseTagAndBlankLine() {
        String codeContent = "```HTML\n\n    <div>内容</div>\n```";
        assertEquals("<div>内容</div>",
                ((HtmlCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.HTML)).getHtmlCode());
    }

    @Test
    void executeHtmlShouldIgnoreInlineFenceMention() {
        // 正文里提到 ```html 但没有真正的代码块，走兜底逻辑整段返回
        String codeContent = "请把代码放在 ```html 代码块中，不要写别的";
        assertEquals(codeContent,
                ((HtmlCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.HTML)).getHtmlCode());
    }

    // ==================== 多文件模式 ====================

    @Test
    void executeMultiFileShouldExtractHtmlCssJs() {
        String codeContent = """
                创建一个完整的网页：
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                </head>
                <body>
                    <h1>欢迎使用</h1>
                </body>
                </html>
                ```
                ```css
                h1 {
                    color: blue;
                }
                ```
                ```js
                console.log('页面加载完成');
                ```""";
        MultiWebFileCodeResult result = (MultiWebFileCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.MULTI_WEB_FILE);
        assertEquals("""
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                </head>
                <body>
                    <h1>欢迎使用</h1>
                </body>
                </html>""", result.getHtmlCode());
        assertEquals("""
                h1 {
                    color: blue;
                }""", result.getCssCode());
        assertEquals("console.log('页面加载完成');", result.getJsCode());
    }

    @Test
    void executeMultiFileShouldExtractCssWithoutLineBreakAfterTag() {
        String codeContent = "```css@media (max-width: 600px) {\n    body { font-size: 14px; }\n}```";
        assertEquals("@media (max-width: 600px) {\n    body { font-size: 14px; }\n}",
                ((MultiWebFileCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.MULTI_WEB_FILE)).getCssCode());
    }

    @Test
    void executeMultiFileShouldExtractJsWithoutLineBreakAfterTag() {
        String codeContent = "```js// 初始化\nconsole.log('init');```";
        assertEquals("// 初始化\nconsole.log('init');",
                ((MultiWebFileCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.MULTI_WEB_FILE)).getJsCode());
    }

    @Test
    void executeMultiFileShouldHandleUnclosedFence() {
        // 两个代码块都漏写结尾围栏：CSS 不应吞掉后面的 JS
        String codeContent = "```css\nh1 { color: red; }\n```js\nconsole.log('done');";
        MultiWebFileCodeResult result = (MultiWebFileCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.MULTI_WEB_FILE);
        assertEquals("h1 { color: red; }", result.getCssCode());
        assertEquals("console.log('done');", result.getJsCode());
    }

    @Test
    void executeMultiFileShouldHandleCrlfFence() {
        String codeContent = "```css\r\nh1 { color: red; }\r\n```\r\n```js\r\nconsole.log(1);\r\n```";
        MultiWebFileCodeResult result = (MultiWebFileCodeResult) CodeParserExecutor.execute(codeContent, CodeGenTypeEnum.MULTI_WEB_FILE);
        assertEquals("h1 { color: red; }", result.getCssCode());
        assertEquals("console.log(1);", result.getJsCode());
    }

    @Test
    void executeMultiFileShouldIgnoreInlineFenceMention() {
        MultiWebFileCodeResult withSpace = (MultiWebFileCodeResult) CodeParserExecutor.execute(
                "请把样式放在 ```css 代码块中，把脚本放在 ```js 代码块中", CodeGenTypeEnum.MULTI_WEB_FILE);
        assertNull(withSpace.getCssCode());
        assertNull(withSpace.getJsCode());

        MultiWebFileCodeResult withoutSpace = (MultiWebFileCodeResult) CodeParserExecutor.execute(
                "请把样式写进```css代码块，脚本写进```js代码块", CodeGenTypeEnum.MULTI_WEB_FILE);
        assertNull(withoutSpace.getCssCode());
        assertNull(withoutSpace.getJsCode());
    }

    @Test
    void executeMultiFileShouldNotTreatUnknownTagAsJs() {
        // ```json / ```jsx 不是 JS 代码块（js 是它们标签的前缀）
        assertNull(((MultiWebFileCodeResult) CodeParserExecutor.execute(
                "```json\n{\"name\": \"app\"}\n```", CodeGenTypeEnum.MULTI_WEB_FILE)).getJsCode());
        assertNull(((MultiWebFileCodeResult) CodeParserExecutor.execute(
                "```jsx\nconst el = <div>hi</div>;\n```", CodeGenTypeEnum.MULTI_WEB_FILE)).getJsCode());
    }

    // ==================== 解析器策略直接调用 ====================

    @Test
    void htmlCodeParserShouldParseDirectly() {
        HtmlCodeParser parser = new HtmlCodeParser();
        HtmlCodeResult result = parser.parserCode("```html\n<p>hi</p>\n```");
        assertEquals("<p>hi</p>", result.getHtmlCode());
    }

    @Test
    void multiWebFileCodeParserShouldParseDirectly() {
        MultiWebFileCodeParser parser = new MultiWebFileCodeParser();
        MultiWebFileCodeResult result = parser.parserCode("""
                ```html
                <p>hi</p>
                ```
                ```css
                p { color: red; }
                ```
                ```javascript
                console.log(1);
                ```""");
        assertEquals("<p>hi</p>", result.getHtmlCode());
        assertEquals("p { color: red; }", result.getCssCode());
        assertEquals("console.log(1);", result.getJsCode());
    }
}
