package com.haibowu.appgenie.core.saver;

import cn.hutool.core.util.StrUtil;
import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.enums.CodeGenTypeEnum;
import com.haibowu.appgenie.exception.BusinessException;
import com.haibowu.appgenie.exception.ErrorCode;

public class HtmlCodeFileSaver extends CodeFileSaverTemplate<HtmlCodeResult>{

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        if(StrUtil.isBlank(result.getHtmlCode())){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML Code Cannot Be Empty");
        }
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }
}
