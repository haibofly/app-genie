package com.haibowu.appgenie.core.parser;

import java.util.regex.Pattern;

public final class CodePatternConstants {
    /**
     * HTML 代码块匹配模式
     * <p>
     * 1. 语言标签后允许任意空白（含换行），不再强制要求紧跟换行符，兼容 ```html<!DOCTYPE html> 这类写法；
     * 2. 用 (?=<) 要求代码块以 HTML 标签开头，避免正文里提到 "```html" 时被误判成代码块；
     * 3. 结束围栏允许缺失（\z），兼容模型漏写结尾 ``` 或流式输出被截断的情况。
     */
    public static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*(?=<)([\\s\\S]*?)(?:```|\\z)", Pattern.CASE_INSENSITIVE);
    /**
     * CSS / JS 代码块匹配模式
     * <p>
     * 1. 语言标签后可以是换行（标准写法），也可以直接跟代码（如 ```css.body{...}、```js// 注释），
     *    直接跟代码时首个字符必须是标点：因为 ```cssh1{} 与 ```css3、```json、```jsx 这类标签无法区分，
     *    用 \p{Punct} 把这种歧义挡在门外（字母/数字开头的不换行写法保持原行为，即不识别）；
     * 2. 标签后不允许是"空白 + 正文"，因此正文里提到 "```css 代码块" 或 "```css代码块" 都不会被误判成代码块；
     * 3. 结束围栏允许缺失（\z），兼容模型漏写结尾 ``` 或流式输出被截断的情况。
     */
    public static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css(?:[ \\t]*\\R|(?=\\p{Punct}))\\s*([\\s\\S]*?)(?:```|\\z)", Pattern.CASE_INSENSITIVE);
    public static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)(?:[ \\t]*\\R|(?=\\p{Punct}))\\s*([\\s\\S]*?)(?:```|\\z)", Pattern.CASE_INSENSITIVE);
}
