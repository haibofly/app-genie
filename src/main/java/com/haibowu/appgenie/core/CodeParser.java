package com.haibowu.appgenie.core;

import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析器
 * 提供静态方法解析不同类型的代码内容
 *
 */
public class CodeParser {

    /**
     * HTML 代码块匹配模式
     * <p>
     * 1. 语言标签后允许任意空白（含换行），不再强制要求紧跟换行符，兼容 ```html<!DOCTYPE html> 这类写法；
     * 2. 用 (?=<) 要求代码块以 HTML 标签开头，避免正文里提到 "```html" 时被误判成代码块；
     * 3. 结束围栏允许缺失（\z），兼容模型漏写结尾 ``` 或流式输出被截断的情况。
     */
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*(?=<)([\\s\\S]*?)(?:```|\\z)", Pattern.CASE_INSENSITIVE);
    /**
     * CSS / JS 代码块匹配模式
     * <p>
     * 1. 语言标签后可以是换行（标准写法），也可以直接跟代码（如 ```css.body{...}、```js// 注释），
     *    直接跟代码时首个字符必须是标点：因为 ```cssh1{} 与 ```css3、```json、```jsx 这类标签无法区分，
     *    用 \p{Punct} 把这种歧义挡在门外（字母/数字开头的不换行写法保持原行为，即不识别）；
     * 2. 标签后不允许是"空白 + 正文"，因此正文里提到 "```css 代码块" 或 "```css代码块" 都不会被误判成代码块；
     * 3. 结束围栏允许缺失（\z），兼容模型漏写结尾 ``` 或流式输出被截断的情况。
     */
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css(?:[ \\t]*\\R|(?=\\p{Punct}))\\s*([\\s\\S]*?)(?:```|\\z)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)(?:[ \\t]*\\R|(?=\\p{Punct}))\\s*([\\s\\S]*?)(?:```|\\z)", Pattern.CASE_INSENSITIVE);

    /**
     * 解析 HTML 单文件代码
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果没有找到代码块，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 解析多文件代码（HTML + CSS + JS）
     */
    public static MultiWebFileCodeResult parseMultiFileCode(String codeContent) {
        MultiWebFileCodeResult result = new MultiWebFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    /**
     * 提取HTML代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private static String extractHtmlCode(String content) {
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

