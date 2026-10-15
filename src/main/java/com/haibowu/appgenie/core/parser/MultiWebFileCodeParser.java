package com.haibowu.appgenie.core.parser;

import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiWebFileCodeParser implements CodeParser {

    @Override
    public MultiWebFileCodeResult parserCode(String codeContent) {
        MultiWebFileCodeResult result = new MultiWebFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, CodePatternConstants.HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CodePatternConstants.CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, CodePatternConstants.JS_CODE_PATTERN);
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
