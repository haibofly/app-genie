package com.haibowu.appgenie.core.parser;

/**
 * 代码解析器策略接口
 */
public interface CodeParser<T> {

    T parserCode(String codeContent);

}
