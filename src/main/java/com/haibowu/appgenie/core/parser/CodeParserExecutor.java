package com.haibowu.appgenie.core.parser;

import com.haibowu.appgenie.ai.model.enums.CodeGenTypeEnum;
import com.haibowu.appgenie.exception.BusinessException;
import com.haibowu.appgenie.exception.ErrorCode;

public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    private static final MultiWebFileCodeParser multiWebFileCodeParser = new MultiWebFileCodeParser();

    public static Object execute(String content, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> htmlCodeParser.parserCode(content);
            case MULTI_WEB_FILE ->  multiWebFileCodeParser.parserCode(content);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported CodeGenType: " + codeGenType);
        };
    }

}
