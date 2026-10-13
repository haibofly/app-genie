package com.haibowu.appgenie.ai;

import dev.langchain4j.service.SystemMessage;

public interface AiCodeGeneratorService {

    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    String generateHtmlCode(String userMessage);

    @SystemMessage(fromResource = "prompt/codegen-multi-web-file-system-prompt.txt")
    String generateMultiWebFileCode(String userMessage);

}
