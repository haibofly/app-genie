# app-genie

AI 应用生成器后端：用一句自然语言描述，生成可直接运行的网站代码。

基于 Spring Boot 4.1.1 + Java 21 + LangChain4j，对接 DeepSeek（OpenAI 兼容接口）。

## 核心能力

| 能力 | 说明 |
|---|---|
| 单文件模式 | 生成一个完整的 `index.html`，CSS/JS 全部内联 |
| 多文件模式 | 生成 `index.html` + `style.css` + `script.js` 三个文件 |
| 非流式生成 | 返回结构化对象，由 LangChain4j 按 JSON Schema 解析 |
| 流式生成 | 返回 `Flux<String>`，边生成边推送，结束后自动解析并落盘 |

生成结果按 `tmp/code_output/{类型}_{雪花ID}/` 目录保存。

## 技术栈

- Spring Boot 4.1.1、Java 21、Maven（含 Maven Wrapper）
- LangChain4j 1.22.0（AI Services、结构化输出、Reactor 流式）
- Knife4j / springdoc-openapi（接口文档）
- Hutool、Lombok、MySQL 驱动（暂无建表与数据访问层）

## 快速开始

### 1. 配置模型密钥

项目默认激活 `local` profile，配置写在 **`src/main/resources/application-local.yml`**（已被 `.gitignore` 忽略，需自行创建）：

```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      api-key: <你的 API Key>
      model-name: deepseek-flash
      strict-json-schema: true
      response-format: json_object
    streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: <你的 API Key>
      model-name: deepseek-flash
      custom-parameters:
        thinking:
          type: disabled   # 流式多文件模式必须关闭思考模式，详见下方说明
```

### 2. 启动

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

服务地址 `http://localhost:8123/api`：

- 健康检查：`GET /api/health/`
- 接口文档（Knife4j）：`http://localhost:8123/api/doc.html`
- OpenAPI JSON：`http://localhost:8123/api/v3/api-docs`

### 3. 测试

```bash
./mvnw test
```

> ⚠️ `AiCodeGeneratorServiceTest`、`AiCodeGeneratorFacadeTest` 会**真实调用大模型**，没有可用 Key 时会失败，且每次运行都会产生费用与较长耗时（实测整套约 5 分钟）。CI 环境建议先加 `@Disabled` 或环境变量守卫。

## 项目结构

```
src/main/java/com/haibowu/appgenie/
├── ai/                      # 模型接入层
│   ├── AiCodeGeneratorService(.java)   # AiServices 声明式接口：4 个生成方法
│   ├── AiCodeGeneratorServiceFactory   # 装配 ChatModel 与 StreamingChatModel
│   └── model/                          # HtmlCodeResult、MultiWebFileCodeResult、CodeGenTypeEnum
├── core/                    # 生成流程编排
│   ├── AiCodeGeneratorFacade           # 统一入口：生成 + 解析 + 落盘
│   ├── parser/                         # 解析侧：策略接口 + 执行器
│   └── saver/                          # 保存侧：模板方法 + 执行器
├── common/                  # 统一响应 BaseResponse、分页/删除请求对象
├── config/                  # 跨域配置
├── controller/              # 健康检查
└── exception/               # 错误码、业务异常、全局异常处理

src/main/resources/prompt/   # 四份系统提示词，按模式与返回类型拆分
```

## 关键设计

**提示词按「返回类型」拆分，而非按「接口」拆分。** 非流式方法返回 POJO，LangChain4j 会自动向请求注入 JSON Schema 格式约束，因此提示词里**不能**再写「输出 Markdown 代码块」，否则模型会输出 Markdown 导致 `OutputParsingException`；流式方法返回原始文本，格式完全由提示词决定：

| 文件 | 使用方 | 输出要求 |
|---|---|---|
| `codegen-html-system-prompt.txt` | 非流式 | 无格式要求，交由库约束 |
| `codegen-multi-web-file-system-prompt.txt` | 非流式 | 无格式要求，交由库约束 |
| `codegen-html-stream-system-prompt.txt` | 流式 | 纯 HTML，不加围栏 |
| `codegen-multi-web-file-stream-system-prompt.txt` | 流式 | 保留 ```html / ```css / ```javascript 围栏 |

多文件流式**必须保留围栏**：解析器依赖围栏区分三类代码，去掉后会因三段内容全部抽取失败而报错。

**解析与保存都采用「策略/模板方法 + 执行器」**，新增一种生成模式只需实现 `CodeParser<T>` 与 `CodeFileSaverTemplate<T>`，再在两个 Executor 的 `switch` 中登记，无需修改 `AiCodeGeneratorFacade`。

## 已知注意事项

- **流式模型不要配置 `response-format`**：它是模型级默认值，会给每个请求强制带上 `response_format={"type":"json_object"}`，而服务端要求提示词中出现 "json" 字样，否则直接返回 400。
- **`deepseek-flash` 是思考型模型**：流式（`stream=true`）时会把 CSS/JS 代码块草稿写进 `reasoning_content`，正式回答里只剩 ```` ```html ```` 一个代码块，导致 `style.css` / `script.js` 为空（非截断所致）。通过 `custom-parameters` 关闭思考模式后即可正常输出三个代码块。
- **`application-local.yml` 含密钥**，切勿提交；仓库仅保留不敏感的 `application.yaml`。
- 老版本的 `core/CodeParser`、`core/CodeFileSaver` 已标记 `@Deprecated`，新代码请使用 `core/parser`、`core/saver` 下的实现。
