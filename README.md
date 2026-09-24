# 智链实验室

> 重要声明
>
> 1. 本项目仅作为 AI 应用、RAG、Agent、Function Calling、技能机制等能力的示例代码，**仅供学习交流使用**。
> 2. **严禁将本项目及其衍生代码用于任何商业用途**，包括但不限于商业部署、付费服务、闭源集成、二次售卖、培训收费交付等。
> 3. 当前仓库更适合作为技术演示与实验样例，正式开源发布前请务必完成配置脱敏、许可证补充、样例数据校验与资源清理。

## 1. 项目介绍

**项目名称：智链实验室**

### 1.1 开发背景

随着大模型应用逐步从单纯对话扩展到知识库问答、工具调用、智能体协作和技能化执行，Java 技术栈也需要一套能够快速接入模型、向量库和业务能力的工程化样板。本项目基于 Spring Boot、Spring AI 与 Spring AI Alibaba 构建，围绕 DashScope 大模型平台、Milvus 向量数据库以及 Agent Framework，演示了从文档读取、文本切分、向量入库、RAG 检索问答，到 Function Calling、Agent 流式输出、技能加载与 Python 工具执行的完整链路。

### 1.2 设计目标

本项目的设计目标主要包括：

- 提供一个可直接参考的 Spring AI + DashScope + Milvus 集成样例；
- 演示基础问答、RAG、进阶 RAG、Agent、技能型 Agent 等多种 AI 应用形态；
- 展示文档读取、分块、嵌入、向量检索等知识库构建核心流程；
- 为后续扩展企业知识库、工作流自动化、工具调用与多技能路由提供参考骨架。

### 1.3 应用场景

本项目适用于以下学习与实验场景：

- Spring AI / Spring AI Alibaba 入门与能力验证；
- 基于 Milvus 的向量检索和 RAG 问答实验；
- DashScope 大模型接入、Embedding、Rerank、Function Calling 测试；
- Agent Hook、消息记忆、人工审批、人机协作场景演示；
- Skills 目录化管理、技能提示词设计与工具扩展实践；
- PDF / Markdown / HTML / JSON / TXT 等多格式文档处理示例。

### 1.4 核心价值

- 对 Java 开发者友好：围绕 Spring 体系构建，易于迁移到现有后端工程；
- 覆盖面完整：同时包含 LLM、Embedding、RAG、Agent、Skills、Tool Calling；
- 便于二次学习：控制器、配置类、测试类、技能目录均保留明确的样例结构；
- 能够作为 GitHub 演示仓库的基础内容模板，用于展示 AI 应用的典型工程组织方式。

### 1.5 项目定位

本项目的定位是**示例工程 / 学习样板 / 能力验证仓库**，并非生产级可直接交付系统。代码中保留了较多实验性实现、注释片段、测试类入口与技能素材，适合用于学习技术原理、验证组件能力、沉淀后续正式项目的初始脚手架。

## 2. 核心功能

本节基于项目源码、配置文件、测试代码与 `skills/` 目录进行梳理，按模块列出当前已实现或已保留的关键能力。

### 2.1 启动入口与工程骨架

- **应用启动**
  - 入口类：`src/main/java/ai/agent/AiApplication.java`
  - 实现方式：标准 `@SpringBootApplication` 启动，承载所有 Web、AI、Agent、向量库与工具配置。

- **通用返回与异常处理**
  - `BaseResponse`、`ResultUtils`：统一接口返回封装；
  - `ErrorCode`：统一错误码枚举；
  - `BusinessException`、`ThrowUtils`、`GlobalExceptionHandler`：提供业务异常抛出与全局兜底处理。

- **上下文与常量**
  - `ApplicationConstant`：定义默认系统提示词、基础 URL、API 版本前缀；
  - `BaseContext`：基于 `ThreadLocal` 保留简单上下文；
  - `StatusConstant`、`MessageConstant`：保留通用状态与消息常量。

### 2.2 基础对话与嵌入能力

- **基础聊天**
  - 控制器：`AiRagController#chat`
  - 接口：`GET /ai/chat`
  - 实现逻辑：直接使用 `DashScopeChatModel` 构建 `ChatClient`，对用户问题执行流式输出。

- **Embedding 向量生成**
  - 控制器：`EmbaddingController#embedding`
  - 接口：`GET /embedding`
  - 实现逻辑：调用注入的 `EmbeddingModel` 对文本进行向量化，打印向量维度和内容。

- **多模态向量测试**
  - 测试类：`src/test/java/ai/agent/Test1.java`
  - 实现逻辑：通过 DashScope SDK 调用 `multimodal-embedding-v1` 生成文本向量，验证多模态嵌入能力。

### 2.3 文档读取模块

控制器：`src/main/java/ai/agent/controller/ReaderController.java`

已实现的文档读取能力如下：

- `GET /reader/text`
  - 使用 `TextReader` 读取纯文本文件；
- `GET /reader/json`
  - 使用 `JsonReader` 读取 JSON 文档；
- `GET /reader/pdf-page`
  - 使用 `PagePdfDocumentReader` 按页读取 PDF；
- `GET /reader/pdf-paragraph`
  - 使用 `ParagraphPdfDocumentReader` 按段读取 PDF；
- `GET /reader/markdown`
  - 使用 `MarkdownDocumentReader` 读取 Markdown；
- `GET /reader/html`
  - 使用 `JsoupDocumentReader` 读取 HTML；
- `GET /reader/tika`
  - 使用 `TikaDocumentReader` 进行通用文档解析。

实现特点：

- 统一从 `ai.agent.model.Constant` 指定的 `classpath:data/` 目录读取样例文件；
- 演示 Spring AI 不同 Reader 的差异化使用方式；
- 适合为知识库构建阶段提供多格式数据接入入口。

### 2.4 文档切分与预处理模块

控制器：`src/main/java/ai/agent/controller/TransformerController.java`

已实现的切分能力如下：

- `GET /transformer/token-text-splitter`
  - 使用 `TokenTextSplitter` 按 token 数量切分文本；
  - 自定义块大小、最小块长度、标点符号与最大块数；
- `GET /transformer/sentenceSplitter`
  - 使用 `SentenceSplitter` 按语义句子拆分；
- `GET /transformer/recursiveCharacterTextSplitter`
  - 使用 `RecursiveCharacterTextSplitter` 递归按字符与分隔符拆分。

附带方法：

- `splitChineseText`：针对中文标点的切分示例；
- `splitWithCustomMarks`：针对自定义标点集合的切分示例。

该模块展示了 RAG 中最关键的预处理步骤之一，即如何将长文本拆成适合向量化与召回的片段。

### 2.5 向量写入与检索模块

控制器：`src/main/java/ai/agent/controller/WriterController.java`

已实现能力如下：

- `GET /writer/writeVector`
  - 读取 PDF 前几页文档块并调用 `VectorStore.add` 写入向量库；

- `GET /writer/similaritySearch?msg=...`
  - 调用 `EmbeddingModel` 先生成查询向量，再通过 `VectorStore.similaritySearch` 检索相似文档；
  - 当前代码中会将入参覆盖为固定问题，主要用于演示检索流程；

- `GET /writer/writeCollectionVector`
  - 直接使用 Milvus 原生客户端；
  - 自动检查集合是否存在，不存在则创建；
  - 自定义字段包括：
    - `id`：主键；
    - `vector`：1536 维浮点向量；
    - `content`：文本内容；
    - `metadata`：JSON 元数据；
  - 创建 `IVF_FLAT + COSINE` 索引并加载集合；
  - 手工将文档内容和 metadata 组装后写入 Milvus。

实现价值：

- 同时演示了 Spring AI 的 `VectorStore` 抽象方式与 Milvus 原生 SDK 直连方式；
- 便于学习“快速接入”和“底层可控”两种实现思路。

### 2.6 RAG 检索增强问答模块

控制器：`src/main/java/ai/agent/controller/AiRagController.java`

#### 2.6.1 基础 RAG

- 接口：`GET /ai/rag`
- 实现逻辑：
  - 构建带系统提示词的 `ChatClient`；
  - 注入 `PromptChatMemoryAdvisor`，支持对话记忆；
  - 使用 `QuestionAnswerAdvisor` 从向量库检索相关文档；
  - 基于 `SearchRequest` 设置 `similarityThreshold=0.4`、`topK=2`；
  - 以流式方式返回回答内容。

#### 2.6.2 进阶 RAG

- 接口：`GET /ai/advancedRag`
- 实现逻辑：
  - 使用 `ContextualQueryAugmenter` 进行上下文增强；
  - 使用 `CompressionQueryTransformer`、`RewriteQueryTransformer`、`TranslationQueryTransformer` 进行查询压缩、改写、翻译；
  - 使用 `MultiQueryExpander` 扩展多个查询；
  - 使用 `VectorStoreDocumentRetriever` 从向量库检索；
  - 使用 `DashScopeRerankPostProcessor` 对召回文档重排序；
  - 使用 `RetrievalAugmentationAdvisor` 将上述模块按 RAG 管线方式编排；
  - 最终同步生成回答，再以 `Flux.just(answer)` 返回。

#### 2.6.3 知识库构建示例

`AiRagController` 中还保留了两个内部示例方法：

- `example1_buildKnowledgeBase`
  - 演示从文本读取、切分、入库到相似检索的最小闭环；
- `initializeDataToMilvusCollection`
  - 演示使用内置样例文档初始化向量数据并测试召回。

整体来看，该模块既覆盖了“开箱即用的简单 RAG”，也覆盖了“带查询改写、扩展、重排的增强 RAG”。

### 2.7 Function Calling 与工具调用模块

- **函数注册**
  - 配置类：`src/main/java/ai/agent/config/ToolsConfig.java`
  - 注册 Bean：`getLocationAndNum`
  - 对应实现：`src/main/java/ai/agent/service/LocationNamesService.java`
  - 实现逻辑：定义 `Request/Response` 结构，模拟“查询某地某姓名重名数量”的工具响应。

- **Function Calling 接口**
  - 控制器：`FunctionCallController`
  - 接口：`GET /ai/fc`
  - 实现逻辑：通过 `DashScopeChatOptions.builder().toolName("getLocationAndNum")` 指定工具，让模型根据问题自动发起函数调用。

- **Function Calling 测试样例**
  - `FunctionCallingTest`
  - 覆盖能力：
    - 自定义天气工具调用；
    - 访问上下文信息；
    - 通过 `BiFunction` 构造工具；
    - 使用 DeepSeek 模型验证工具回调行为。

### 2.8 Agent 智能体模块

#### 2.8.1 Agent 核心配置

配置类：`src/main/java/ai/agent/config/AgentConfig.java`

已配置两个核心 Agent：

- `testAgent`
  - 使用 `ReactAgent` 构建；
  - 绑定模型调用上限 Hook `ModelCallLimitHook(runLimit=5)`；
  - 绑定自定义消息总结 Hook `MessageSummarizationHook`；
  - 绑定 PII 脱敏 Hook，对邮箱做脱敏处理；
  - 注册 `sendEmailTool`、`deleteDataTool` 两个演示工具；
  - 绑定 `HumanInTheLoopHook`，对敏感工具调用要求人工审批；
  - 绑定 `ContextEditingInterceptor` 处理超长上下文；
  - 使用 `MysqlSaver` 将短期记忆持久化到 MySQL。

- `skillsAgent`
  - 自动扫描项目根目录 `skills/`；
  - 使用 `FileSystemSkillRegistry` 注册技能；
  - 绑定 `SkillsAgentHook` 和 `ShellToolAgentHook`；
  - 额外挂载 `python_tool`，支持通过 GraalVM 执行 Python 代码；
  - 使用 `MemorySaver` 做内存级会话保存。

#### 2.8.2 Agent 流式接口

控制器：`src/main/java/ai/agent/controller/AgentController.java`

- 接口：`GET /api/agent/stream`
- 实现逻辑：
  - 调用 `testAgent.stream(...)` 获取节点输出流；
  - 过滤 `START` / `END` 节点；
  - 区分模型流式推理、模型完成、工具完成、Hook 完成等不同 `OutputType`；
  - 对 reasoning 内容和正式内容分别输出；
  - 使用 SSE (`ServerSentEvent`) 向前端持续推送。

该接口适合用来演示 Agent 的中间推理过程、工具调用事件与最终回答的分阶段输出。

#### 2.8.3 技能型 Agent 接口

控制器：`src/main/java/ai/agent/controller/SkillsAgentController.java`

- 接口：`GET /chat`
- 实现逻辑：
  - 使用 `skillsAgent.call(userMessage, config)` 处理请求；
  - 为技能型提示词路由、Shell 执行与 Python 工具调用提供统一入口。

> 注意：该控制器的 `@RestController("/api/skillsAgent")` 写法并不会形成类级路径前缀，实际映射以方法上的 `@GetMapping("/chat")` 为准。

### 2.9 消息记忆与对话压缩模块

- 配置类：`ChatMemoryConfig`
  - 使用 `JdbcChatMemoryRepository` + `MessageWindowChatMemory(maxMessages=100)` 构建窗口型对话记忆；

- 自定义 Hook：`MessageSummarizationHook`
  - 在 `BEFORE_MODEL` 阶段执行；
  - 依据历史消息长度粗略估算 token；
  - 当达到阈值后，调用总结模型归纳旧消息；
  - 用摘要系统消息替换前置历史，保留最近若干条消息。

该实现能够帮助 Agent 在长对话过程中控制上下文长度，并保留关键历史语义。

### 2.10 DashScope API 与模型配置模块

- `DashScopeApiConfig`
  - 为 DashScope HTTP 客户端设置超时时间；
- `application.properties`
  - 配置模型名称、Embedding 模型、Rerank 模型；
  - 配置 Milvus、MySQL、服务端口、日志级别等；
- `logback-spring.xml`
  - 定义控制台日志、信息日志、错误日志与滚动策略；
  - 默认日志目录为 `/data/cloud2/logs/ai`。

### 2.11 技能系统模块

项目根目录 `skills/` 当前包含以下技能样例：

- `arxiv-search`
  - 提供 arXiv 学术搜索技能说明；
  - 附带 `arxiv_search.py` 脚本，通过 Python 查询 arXiv 并返回标题与摘要；

- `web-search`
  - 提供面向复杂 Web 调研任务的技能说明，强调计划、子任务拆分与结果汇总；

- `skill-creator`
  - 提供技能创建规范；
  - `scripts/init_skills.py`：初始化新技能目录模板；
  - `scripts/quick_validate.py`：快速校验 `SKILL.md` Frontmatter 结构；

- `zhangxuefeng-skill`
  - 提供一个内容较完整的角色型技能案例；
  - 包含 `assets/`、`examples/`、`references/research/` 等素材，适合学习复杂技能的组织方式。

该目录说明本项目不仅演示后端 AI 调用，还尝试演示“基于文件系统组织技能资产”的能力。

### 2.12 Python 工具执行模块

- 实现类：`src/main/java/ai/agent/utils/PythonTool.java`
- 实现逻辑：
  - 基于 GraalVM Polyglot 创建 Python 执行上下文；
  - 接收 `PythonRequest.code`；
  - 在受限上下文中执行 Python 代码；
  - 将字符串、数值、布尔、数组等结果转成文本返回；
  - 捕获并返回 Python 执行异常信息。

该模块主要服务于 `skillsAgent`，让 Agent 能够在技能流程中执行轻量 Python 片段。

### 2.13 搜索工具预留模块

- `SearchUtils`
  - 封装对 Tavily 搜索接口的调用；
  - 使用 OkHttp + Jackson 发送 HTTP 请求并解析结果；
  - 当前为预留组件，尚未在控制器或 Agent 中完整接入。

### 2.14 测试与样例验证模块

`src/test/java/ai/agent/` 下包含多个实验类：

- `AiRagApplicationTests`
  - 演示 `TokenTextSplitter` 对文档分块的基础行为；
- `TestChatModel`
  - 演示开启模型思考能力后，通过 `ReactAgent` 获取 reasoning 内容；
- `FunctionCallingTest`
  - 演示多种工具回调与上下文传递方式；
- `HumanInTheLoopTest`
  - 演示工具审批中断、编辑、拒绝三种人机协作决策流程；
- `PdfImageExtractor`
  - 将 PDF 渲染成图片并提取全文文本；
- `WatermarkPdfProcessorCorrected`
  - 将 PDF 页面先渲染成图片后叠加水印，再重建输出 PDF；
- `test.md`、`ma.md`
  - 保留测试过程生成的文本内容或样例输出。

### 2.15 当前工程结构概览

```text
ai
├─ src
│  ├─ main
│  │  ├─ java/ai/agent
│  │  │  ├─ common          # 通用响应、错误码、分页等
│  │  │  ├─ config          # 模型、记忆、Agent、工具配置
│  │  │  ├─ constant        # 常量定义
│  │  │  ├─ context         # 上下文工具
│  │  │  ├─ controller      # Web 接口入口
│  │  │  ├─ exception       # 统一异常处理
│  │  │  ├─ model           # 资源路径等模型常量
│  │  │  ├─ service         # Hook、工具服务
│  │  │  └─ utils           # PythonTool、SearchUtils
│  │  └─ resources
│  │     ├─ application.properties
│  │     ├─ logback-spring.xml
│  │     └─ data            # 文本、HTML、PDF 样例数据
│  └─ test
│     └─ java/ai/agent      # 功能实验类与测试样例
├─ skills                   # 技能目录与脚本资产
├─ pic                      # PDF 图片提取输出目录
├─ pom.xml
└─ 说明.md
```

## 3. 技术栈

以下版本以 `pom.xml` 中显式声明和 BOM 管理结果为准：

### 3.1 后端基础框架

- Java `17`
- Spring Boot `3.5.2`
- Spring Web `3.5.x`（随 Spring Boot 统一管理）
- Lombok `1.18.40`

### 3.2 AI / LLM / Agent 相关

- Spring AI BOM `1.1.3`
- Spring AI Alibaba BOM `1.1.2.2`
- Spring AI Alibaba DashScope Starter `1.1.2.2`
- Spring AI Alibaba RAG `1.1.2.2`
- Spring AI Alibaba Agent Framework `1.1.2.2`
- Spring AI Alibaba Studio `1.1.2.2`
- Spring AI DeepSeek `1.1.3`
- DashScope Java SDK `2.22.12`

### 3.3 向量检索与知识库

- Milvus Vector Store Starter `1.1.x`（由 Spring AI BOM 管理）
- Milvus 服务端建议版本：`2.x`
- Rerank：DashScope Rerank 模型能力（通过 Spring AI Alibaba 接入）

### 3.4 文档处理与转换

- Spring AI Jsoup Document Reader `1.1.x`
- Spring AI Markdown Document Reader `1.1.x`
- Spring AI PDF Document Reader `1.1.x`
- Spring AI Tika Document Reader `1.1.x`
- PDFBox 相关能力：由 Spring AI Reader 与测试代码间接使用

### 3.5 数据存储与记忆

- MySQL Connector/J `8.0.28`
- JDBC Chat Memory Repository `1.1.x`
- MySQL：建议 `8.0+`

### 3.6 工具与扩展能力

- GraalVM Polyglot `24.2.1`
- GraalVM Python Community `24.2.1`
- OkHttp（通过代码直接使用，版本由依赖树解析）
- Jackson（Spring Boot 默认集成）

### 3.7 构建与运行工具

- Maven `3.9+` 建议
- IDE：IntelliJ IDEA 2023+ 或等效 Java IDE

## 4. 环境要求

### 4.1 操作系统要求

- Windows 10 / 11、Linux、macOS 均可运行；
- 若直接使用当前日志配置，Linux 环境需保证 `/data/cloud2/logs/ai` 可写；
- Windows 环境建议根据实际情况调整日志路径与本地资源路径。

### 4.2 开发环境要求

- JDK `17`
- Maven `3.9+`
- 可访问外部 Maven 仓库或已配置可用镜像仓库
- 可访问 DashScope / DeepSeek 等外部模型服务

### 4.3 中间件与外部依赖

- MySQL `8.0+`
  - 用于 JDBC Chat Memory Repository、MysqlSaver 等持久化能力；
- Milvus `2.x`
  - 用于向量写入与检索；
- DashScope API Key
  - 用于聊天模型、Embedding、Rerank、Agent 等核心能力；
- 可选：DeepSeek API Key
  - 仅部分测试类会使用。

### 4.4 样例资源要求

当前资源目录已包含：

- `text.txt`
- `text.md`
- `text.json`
- `spring-ai.html`
- `RAG实践手册.pdf`
- `google-ai-agents-whitepaper.pdf`

注意事项：

- `ai.agent.model.Constant` 中当前 `PDF_FILE_PATH` 指向 `classpath:data//xxxx招标文件.pdf`；
- 仓库现有资源目录中**并不存在**该文件名；
- 若要正常体验 PDF 读取、PDF 分块、向量入库相关接口，请先执行以下任一处理：
  - 将实际 PDF 文件命名为 `xxxx招标文件.pdf` 后放入 `src/main/resources/data/`；
  - 或直接修改 `Constant.PDF_FILE_PATH` 指向现有 PDF，例如 `RAG实践手册.pdf`。

## 5. 快速开始

### 5.1 克隆与准备工程

```bash
git clone <your-repo-url>
cd ai
```

如果当前目录还未初始化 Git 仓库，可先在发布前执行：

```bash
git init
```

### 5.2 配置运行参数

建议不要直接把真实密钥与数据库账号写入仓库，正式发布前请替换为环境变量或示例值。

当前 `application.properties` 涉及以下关键配置项：

```properties
server.port=8080

spring.ai.dashscope.api-key=${DASHSCOPE_API_KEY}
spring.ai.dashscope.chat.options.model=qwen3.6-plus
spring.ai.dashscope.embedding.options.model=text-embedding-v1
spring.ai.dashscope.embedding.options.dimensions=1536
spring.ai.dashscope.rerank.options.model=qwen3-vl-rerank

spring.ai.vectorstore.milvus.client.host=<your-milvus-host>
spring.ai.vectorstore.milvus.client.port=19530
spring.ai.vectorstore.milvus.collectionName=ragtest

spring.datasource.url=jdbc:mysql://<your-mysql-host>:3306/<your-db>
spring.datasource.username=<your-username>
spring.datasource.password=<your-password>
```

推荐发布前处理方式：

1. 将真实地址、用户名、密码替换为占位值；
2. 通过环境变量注入 API Key；
3. 为 MySQL、Milvus 提供单独的示例配置说明；
4. 避免将真实内网地址与敏感账号直接提交到 GitHub。

### 5.3 初始化 MySQL 记忆表

项目中启用了 JDBC Chat Memory 与 MysqlSaver，因此需要准备数据库。

可选方式：

1. 使用 Spring AI 官方 JDBC Chat Memory Schema 初始化表结构；
2. 打开配置中的以下选项，让应用自动初始化：

```properties
spring.ai.chat.memory.repository.jdbc.initialize-schema=always
spring.ai.chat.memory.repository.jdbc.platform=mysql
spring.ai.chat.memory.repository.jdbc.schema=classpath:org/springframework/ai/chat/memory/repository/jdbc/schema-mysql.sql
```

### 5.4 准备 Milvus

确保 Milvus 服务已启动，并且向量维度与配置保持一致：

- Embedding 维度：`1536`
- Milvus 向量字段维度：`1536`

若维度不一致，向量写入会失败。

### 5.5 调整 PDF 资源路径

如果需要测试以下接口：

- `/reader/pdf-page`
- `/reader/pdf-paragraph`
- `/writer/writeVector`
- `/writer/writeCollectionVector`

请先确认 `Constant.PDF_FILE_PATH` 指向一个真实存在的 PDF 文件。

### 5.6 启动项目

```bash
mvn spring-boot:run
```

或在 IDE 中直接运行 `AiApplication`。

### 5.7 访问验证

服务启动后，可通过以下接口进行验证：

- 基础聊天

```text
GET http://localhost:8080/ai/chat?question=你好，请介绍一下Spring%20AI
```

- 基础 RAG

```text
GET http://localhost:8080/ai/rag?question=这份文档的主要内容是什么
```

- 进阶 RAG

```text
GET http://localhost:8080/ai/advancedRag?question=请总结文档中的关键要求
```

- 文档读取

```text
GET http://localhost:8080/reader/text
GET http://localhost:8080/reader/html
GET http://localhost:8080/reader/markdown
```

- 文本切分

```text
GET http://localhost:8080/transformer/token-text-splitter
GET http://localhost:8080/transformer/sentenceSplitter
```

- 向量写入与检索

```text
GET http://localhost:8080/writer/writeVector
GET http://localhost:8080/writer/similaritySearch?msg=招标文件发售开始时间
```

- Function Calling

```text
GET http://localhost:8080/ai/fc?message=深圳有多少个叫pillar的人
```

- Agent 流式输出

```text
GET http://localhost:8080/api/agent/stream?query=帮我规划一个简单的知识库问答方案
```

### 5.8 常见问题排查

#### 问题 1：Maven 无法解析依赖或父 POM

现象：

- `spring-boot-starter-parent` 无法下载；
- 本地仓库路径异常；
- 镜像仓库不可用。

排查建议：

1. 检查 Maven `settings.xml` 是否配置了可用镜像；
2. 检查本地仓库目录是否存在且可写；
3. 确认网络可访问 Maven Central / 阿里云镜像；
4. 删除损坏的 `.lastUpdated` / `.part.lock` 文件后重试。

#### 问题 2：DashScope API Key 未生效

现象：

- 调用模型接口时报认证失败；
- 启动后聊天能力不可用。

排查建议：

1. 确认已设置环境变量 `DASHSCOPE_API_KEY`；
2. 确认账号具备对应模型权限；
3. 检查是否存在代理、网络出口或证书问题。

#### 问题 3：Milvus 检索或写入失败

现象：

- 向量写入报维度不匹配；
- 检索结果为空；
- 集合创建失败。

排查建议：

1. 确认 Embedding 模型维度为 `1536`；
2. 确认 Milvus 集合字段维度也为 `1536`；
3. 确认集合名称、数据库名、端口配置正确；
4. 确认 Milvus 服务已正常启动。

#### 问题 4：PDF 文件找不到

现象：

- 调用 PDF 读取或写入接口时报资源不存在。

排查建议：

1. 检查 `Constant.PDF_FILE_PATH`；
2. 确认实际 PDF 文件已放入 `src/main/resources/data/`；
3. 优先将常量改为现有的 `RAG实践手册.pdf` 进行验证。

#### 问题 5：Chat Memory / MysqlSaver 初始化失败

现象：

- 启动时报 JDBC 记忆表不存在；
- Agent 无法持久化历史消息。

排查建议：

1. 初始化 Spring AI JDBC Chat Memory 所需表结构；
2. 检查数据源连通性与数据库权限；
3. 确认 MySQL 版本兼容。

#### 问题 6：日志文件无法写入

现象：

- 启动后日志文件目录报权限错误；
- Windows 环境无法创建 `/data/cloud2/logs/ai`。

排查建议：

1. 修改 `logback-spring.xml` 中的 `LOG_HOME`；
2. 或提前创建目标目录并赋予写权限。

## 6. 贡献指南

欢迎基于学习目的对本项目进行改进、补充示例或修正文档，但请遵循以下规范：

### 6.1 贡献范围建议

- 补充更完整的 README / Wiki / API 示例；
- 优化 RAG、Agent、Function Calling、Skills 模块示例；
- 完善配置脱敏、环境变量化与样例配置文件；
- 增加启动脚本、Docker 化支持、测试说明；
- 修复样例资源路径错误、日志路径兼容性等问题。

### 6.2 提交流程建议

1. Fork 仓库并创建个人分支；
2. 提交前先确保代码可以编译或至少完成静态校对；
3. 对新增功能补充必要说明；
4. 提交 Pull Request 时说明改动背景、实现方式与验证方法；
5. 不要提交真实密钥、数据库密码、内网地址、商业文档或侵权资源。

### 6.3 代码与文档建议

- 保持目录结构清晰，优先遵循现有包结构；
- 新增示例时请明确其用途与适用场景；
- 文档优先写清“如何运行、依赖什么、限制是什么”；
- 若新增技能，请同时补充对应 `SKILL.md` 与必要脚本说明。

### 6.4 发布前建议检查清单

- [ ] 替换 `application.properties` 中的真实敏感配置；
- [ ] 校对 `Constant.PDF_FILE_PATH` 与样例资源；
- [ ] 校对日志目录在当前系统下是否可用；
- [ ] 检查是否误提交测试输出文件、临时图片或大体积资料；
- [ ] 明确许可证文本与仓库首页声明一致；
- [ ] 补充 `.gitignore`、`LICENSE`、`README.md`（如计划面向公众发布）。

## 7. 许可证声明

### 7.1 当前许可方式

本项目当前建议以**自定义“仅供学习交流、禁止商用”声明**发布。建议在 GitHub 仓库中补充独立 `LICENSE` 文件，并至少包含以下约束：

- 允许个人学习、技术研究、二次阅读与非商业性质的参考使用；
- 允许基于学习目的进行修改、演示与提交改进建议；
- 禁止任何形式的商业使用、商业部署、商用集成、收费培训、闭源售卖、SaaS 服务化输出；
- 禁止去除原始声明后重新分发并宣称为可商用版本；
- 若需商业授权，应由原作者另行书面授权。

### 7.2 重要说明

需要特别说明的是：

- **“禁止商业使用”的许可证并不属于严格意义上的 OSI 开源许可证**；
- 因此，本项目更准确的表述应为：**源码公开 / 学习示例项目 / Source Available 项目**；
- 如果未来希望完全符合标准开源社区定义，可考虑改为 `MIT`、`Apache-2.0`、`BSD-3-Clause` 等标准许可证；
- 但只要仍保留“禁止商用”要求，就应明确说明该项目是“公开源码学习项目”，而非严格开放源代码许可项目。

### 7.3 建议加入仓库首页的简版声明

```text
本项目仅作为示例代码与学习资料使用，仅供学习交流，禁止任何商业用途。
This project is provided as sample code for learning and research only.
Commercial use is strictly prohibited.
```

## 8. 补充说明

### 8.1 当前仓库的已知特点

- 代码以示例演示为主，部分模块偏实验性质；
- 部分配置和资源路径仍需整理后更适合公开发布；
- `application.properties` 当前存在真实环境风格配置，正式发布前应完成脱敏；
- `skills/` 目录内容较丰富，适合作为技能系统演示案例；
- 项目根目录当前未见标准 Git 仓库元数据，若准备上传 GitHub，请先初始化并整理发布文件。

### 8.2 推荐后续完善方向

- 增加标准 `README.md` 与英文版简介；
- 提供 `application-example.properties`；
- 为 MySQL / Milvus 提供 Docker Compose；
- 补充接口清单与测试用例；
- 对 Skill 系统、Agent 事件流、RAG 流程绘制架构图；
- 增加正式 `LICENSE`、`.gitignore`、`CHANGELOG.md` 与发布说明。
