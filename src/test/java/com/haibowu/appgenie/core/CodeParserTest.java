package com.haibowu.appgenie.core;

import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeParserTest {

    @Test
    void parseHtmlCode() {
        String codeContent = """
                随便写一段描述：
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>测试页面</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                ```
                随便写一段描述
                """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
    }

    @Test
    void parseHtmlCodeWithCodeFence() {
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
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertEquals("""
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>""", result.getHtmlCode());
    }

    @Test
    void parseHtmlCodeWithFenceWithoutLineBreak() {
        // 语言标签后不换行，直接跟 HTML 标签
        String codeContent = "```html<!DOCTYPE html>\n<html></html>```";
        assertEquals("<!DOCTYPE html>\n<html></html>", CodeParser.parseHtmlCode(codeContent).getHtmlCode());
    }

    @Test
    void parseHtmlCodeWithCrlfFence() {
        // Windows 换行符
        String codeContent = "说明\r\n```html\r\n<html>\r\n<body>Hi</body>\r\n</html>\r\n```";
        assertEquals("<html>\r\n<body>Hi</body>\r\n</html>", CodeParser.parseHtmlCode(codeContent).getHtmlCode());
    }

    @Test
    void parseHtmlCodeWithUnclosedFence() {
        // 模型漏写结尾围栏（或流式输出被截断）
        String codeContent = "说明\n```html\n<html><body>Hi</body></html>";
        assertEquals("<html><body>Hi</body></html>", CodeParser.parseHtmlCode(codeContent).getHtmlCode());
    }

    @Test
    void parseHtmlCodeWithUppercaseTagAndBlankLine() {
        String codeContent = "```HTML\n\n    <div>内容</div>\n```";
        assertEquals("<div>内容</div>", CodeParser.parseHtmlCode(codeContent).getHtmlCode());
    }

    @Test
    void parseHtmlCodeShouldIgnoreInlineFenceMention() {
        // 正文里提到 ```html 但没有真正的代码块，不应被当成 HTML 代码
        String codeContent = "请把代码放在 ```html 代码块中，不要写别的";
        // 未匹配到代码块时会走兜底逻辑，整段内容原样返回
        assertEquals(codeContent, CodeParser.parseHtmlCode(codeContent).getHtmlCode());
    }

    @Test
    void parseMultiFileCodeWithCodeFences() {
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
        MultiWebFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
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
    void parseCssCodeWithoutLineBreakAfterTag() {
        // 语言标签后不换行，直接跟标点开头的 CSS
        String codeContent = "```css@media (max-width: 600px) {\n    body { font-size: 14px; }\n}```";
        assertEquals("@media (max-width: 600px) {\n    body { font-size: 14px; }\n}",
                CodeParser.parseMultiFileCode(codeContent).getCssCode());
    }

    @Test
    void parseJsCodeWithoutLineBreakAfterTag() {
        // 语言标签后不换行，直接跟标点开头的 JS
        String codeContent = "```js// 初始化\nconsole.log('init');```";
        assertEquals("// 初始化\nconsole.log('init');",
                CodeParser.parseMultiFileCode(codeContent).getJsCode());
    }

    @Test
    void parseMultiFileCodeWithUnclosedFence() {
        // 两个代码块都漏写结尾围栏：CSS 不应吞掉后面的 JS
        String codeContent = "```css\nh1 { color: red; }\n```js\nconsole.log('done');";
        MultiWebFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertEquals("h1 { color: red; }", result.getCssCode());
        assertEquals("console.log('done');", result.getJsCode());
    }

    @Test
    void parseCssAndJsCodeWithCrlfFence() {
        String codeContent = "```css\r\nh1 { color: red; }\r\n```\r\n```js\r\nconsole.log(1);\r\n```";
        MultiWebFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertEquals("h1 { color: red; }", result.getCssCode());
        assertEquals("console.log(1);", result.getJsCode());
    }

    @Test
    void parseMultiFileCodeShouldIgnoreInlineFenceMention() {
        // 正文里提到围栏但没有真正的代码块，不应被当成代码
        MultiWebFileCodeResult withSpace =
                CodeParser.parseMultiFileCode("请把样式放在 ```css 代码块中，把脚本放在 ```js 代码块中");
        assertNull(withSpace.getCssCode());
        assertNull(withSpace.getJsCode());

        MultiWebFileCodeResult withoutSpace =
                CodeParser.parseMultiFileCode("请把样式写进```css代码块，脚本写进```js代码块");
        assertNull(withoutSpace.getCssCode());
        assertNull(withoutSpace.getJsCode());
    }

    @Test
    void parseMultiFileCodeShouldNotTreatUnknownTagAsJs() {
        // ```json / ```jsx 不是 JS 代码块（js 是它们标签的前缀）
        assertNull(CodeParser.parseMultiFileCode("```json\n{\"name\": \"app\"}\n```").getJsCode());
        assertNull(CodeParser.parseMultiFileCode("```jsx\nconst el = <div>hi</div>;\n```").getJsCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
                创建一个完整的网页：
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>多文件示例</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>欢迎使用</h1>
                    <script src="script.js"></script>
                </body>
                </html>
                ```
                
                ```css
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```
                
                ```js
                console.log('页面加载完成');
                ```
                文件创建完成！
                """;
        MultiWebFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());
    }
}
