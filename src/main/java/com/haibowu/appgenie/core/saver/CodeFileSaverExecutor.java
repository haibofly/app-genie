package com.haibowu.appgenie.core.saver;

import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;
import com.haibowu.appgenie.ai.model.enums.CodeGenTypeEnum;
import com.haibowu.appgenie.exception.BusinessException;
import com.haibowu.appgenie.exception.ErrorCode;

import java.io.File;

public class CodeFileSaverExecutor {

    private static HtmlCodeFileSaver htmlCodeFileSaver = new HtmlCodeFileSaver();
    private static MultiWebCodeFileSaver multiWebCodeFileSaver = new MultiWebCodeFileSaver();

    public static File execute(Object codeResult, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCodeFile((HtmlCodeResult)  codeResult);
            case MULTI_WEB_FILE -> multiWebCodeFileSaver.saveCodeFile((MultiWebFileCodeResult) codeResult);
            default ->  throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unknown CodeGenType:" +codeGenType );
        };
    }
}
