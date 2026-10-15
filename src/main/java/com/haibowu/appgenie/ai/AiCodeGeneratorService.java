package com.haibowu.appgenie.ai;

import com.haibowu.appgenie.ai.model.HtmlCodeResult;
import com.haibowu.appgenie.ai.model.MultiWebFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    @SystemMessage(fromResource = "prompt/codegen-multi-web-file-system-prompt.txt")
    MultiWebFileCodeResult generateMultiWebFileCode(String userMessage);

    @SystemMessage(fromResource = "prompt/codegen-html-stream-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    @SystemMessage(fromResource = "prompt/codegen-multi-web-file-stream-system-prompt.txt")
    Flux<String> generateMultiWebFileCodeStream(String userMessage);

}
