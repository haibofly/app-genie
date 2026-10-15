package com.haibowu.appgenie.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.haibowu.appgenie.ai.model.enums.CodeGenTypeEnum;
import com.haibowu.appgenie.exception.BusinessException;
import com.haibowu.appgenie.exception.ErrorCode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 代码文件保存器 - 模板方法模式
 *
 * @param <T>
 */
public abstract class CodeFileSaverTemplate<T> {

    protected static final Path FILE_SAVE_ROOT_DIR = Path.of(
            System.getProperty("user.dir"), "tmp", "code_output");

    public final File saveCodeFile(T result){
        try {
            validateInput(result);
            String baseDirPath = buildUniqueDir();
            saveFiles(result, baseDirPath);
         return new File(baseDirPath);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e.getMessage());
        }
    }

    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generated Code Cannot Be Null.");
        }
    }

    protected final String buildUniqueDir() {
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, IdUtil.getSnowflakeNextIdStr());
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }

    /**
     * 获取代码类型，具体由子类实现
     *
     */
    protected abstract CodeGenTypeEnum getCodeType();

    protected static void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }

    protected abstract void saveFiles(T result, String baseDirPath);
}
