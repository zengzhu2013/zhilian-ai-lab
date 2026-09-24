
package ai.agent.controller;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeRerankProperties;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.rag.postretrieval.DashScopeRerankPostProcessor;
import ai.agent.common.ApplicationConstant;
import io.milvus.client.MilvusClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "AiRagController", description = "Rag接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/ai")
public class AiRagController {

    // 对话代理
    ChatClient chatClient;

    // 向量数据库
    VectorStore vectorStore;

    // 文本重排序模型
    RerankModel rerankModel;

    DashScopeRerankProperties dashScopeRerankProperties;

    DashScopeChatModel dashScopeChatModel;


    public AiRagController(ChatModel chatModel, ChatMemory chatMemory,
                           VectorStore vectorStore, MilvusClient milvusClient
            , RerankModel rerankModel, DashScopeRerankProperties dashScopeRerankProperties, DashScopeChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                // 默认系统提示词
                .defaultSystem("""
                        你是太和知识库系统的对话助手，请以乐于助人的方式进行对话，
                        今天的日期：{current_data}，
                        我的用户ID是{user_id}
                        """)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(chatMemory).build(),
                        SimpleLoggerAdvisor.builder().build() // 日志拦截
                )
                .build();
        this.vectorStore = vectorStore;
        this.rerankModel = rerankModel;
        this.dashScopeRerankProperties = dashScopeRerankProperties;
        this.dashScopeChatModel = dashScopeChatModel;
    }

    @GetMapping("/chat")
    public Flux<String> chat(
            @RequestParam(value = "question", defaultValue = "招标文件每套售价多少人民币？") String question, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Flux<String> content = ChatClient.builder(dashScopeChatModel).build().prompt()
                .user(question)
                .stream()
                .content();
        return content;
    }

    @GetMapping("/rag")
    public Flux<String> rag(
            @RequestParam(value = "question", defaultValue = "招标文件每套售价多少人民币？") String question, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Long userId = 100000000l;
        String currentDate = LocalDate.now().toString();
        Flux<String> content = chatClient.prompt()
                .system(a -> a.param("current_data", currentDate))
                .system(a -> a.param("user_id", userId))
                .user(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, userId))
                // 简单快速 rag  QuestionAnswerAdvisor实现思路
                .advisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(
                                SearchRequest.builder()
                                        .query(question)
                                        .similarityThreshold(0.4d).topK(2)
                                        .build()
                        )
                        .build())
                .stream()
                .content();

        return content;
    }

    @GetMapping("/advancedRag")
    public Flux<String> advancedRag(HttpServletResponse response
            , @RequestParam(value = "question", defaultValue = "开标时间和地点分别是？") String question) {
        response.setCharacterEncoding("UTF-8");

        // 查询增强
        ContextualQueryAugmenter queryAugmenter = ContextualQueryAugmenter.builder()
                // 用于增强输入查询的组件，提供额外的数据，用于为大型语言模型提供必要的上下文以回答用户查询。
                .allowEmptyContext(false) // 允许空上下文，避免在没有上下文时生成空查询
                .emptyContextPromptTemplate(PromptTemplate.builder().template("没有相关数据查询").build())
                .build();

        var compressionQueryTransformer = CompressionQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .build(); // 查询压缩 --使用大型语言模型将对话历史和后续查询压缩为捕获对话本质的独立查询

        var rewriteQueryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .targetSearchSystem("vector store")
                .build();  // 重写用户查询

        var translationQueryTransformer = TranslationQueryTransformer.builder()
                .chatClientBuilder(chatClient.mutate())
                .targetLanguage("english")
                .build();  // 查询翻译 --使用大型语言模型翻译用户查询为目标语言

        // Query Transformation 查询改写
        List<QueryTransformer> queryTransformers = List.of(
                rewriteQueryTransformer
                ,compressionQueryTransformer
                ,translationQueryTransformer
        );

        // Query Expansion 查询扩展
        MultiQueryExpander multiQueryExpander = MultiQueryExpander.builder()
                .numberOfQueries(2)
                .chatClientBuilder(chatClient.mutate())
                .build(); // 查询扩展 --使用大型语言模型扩展用户查询，生成多个相关查询

        // rerank 重排序
        DashScopeRerankPostProcessor dashScopeRerankPostProcessor = DashScopeRerankPostProcessor.builder()
                .rerankModel(rerankModel)
                .rerankOptions(dashScopeRerankProperties.getOptions())
                .build();

        // 创建RAG流程,基于模块化架构设计
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformers)
                .queryExpander(multiQueryExpander)
                .queryAugmenter(queryAugmenter)
                .documentRetriever(VectorStoreDocumentRetriever.builder() // Retrieval 检索器，--从向量数据库中检索与查询最相关的文档
                        .similarityThreshold(0.30).topK(2)
                        .vectorStore(vectorStore)
                        .build())
                .documentJoiner(new ConcatenationDocumentJoiner()) // 将从多个 query 和从多个数据源检索到的 Document 合并为一个 Document 集合；
                .documentPostProcessors(dashScopeRerankPostProcessor) // 文档后处理器，用于对检索到的文档进行索引，方便后续的检索和使用。
                .build();

        String answer = chatClient.prompt().system(a -> a.param("current_data", LocalDate.now().toString())).system(a -> a.param("user_id", 100000000L))
                .advisors(retrievalAugmentationAdvisor)
                .user(question)
                .call()
                .content();

        Flux<String> content = Flux.just(answer);
        return content;

        /**
         * 提升RAG召回率与精度的核心路径是：混合检索（语义+BM25）保召回 → 重排序（Cross-Encoder）提精度 → 语义分块与查询优化补细节。
         * 优先落地：混合检索+重排序可解决80%的常见问题
         * 进阶优化：需结合业务场景调整分块策略、查询改写规则，并建立量化评估闭环持续迭代。
         */
    }























    // RetrievalRerankAdvisor
//    MilvusClient milvusClient;
//        this.milvusClient = milvusClient;
//        initializeDataToMilvusCollection();
    /**
     * 构建知识库
     *
     * 从文档加载、分割、嵌入并存储到向量数据库
     */
    public void example1_buildKnowledgeBase() {
        // 1. 加载文档
        Resource resource = new FileSystemResource("path/to/document.txt");
        TextReader textReader = new TextReader(resource);
        List<Document> documents = textReader.get();

        // 2. 分割文档为块
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(documents);

        // 3. 将块添加到向量存储
        vectorStore.add(chunks);

        // 现在可以使用向量存储进行检索
        List<Document> results = vectorStore.similaritySearch("查询文本");

        System.out.println("知识库构建完成，检索到 " + results.size() + " 个相关文档");
    }

    /**
     * 初始化数据到库中
     */
    private void initializeDataToMilvusCollection() {
        try {
            List<Document> documents = List.of(
                    new Document("Spring Boot 是一个基于 Spring 的快速开发框架，它简化了 Spring 应用的配置和部署过程。", Map.of("category", "技术", "type", "Spring Boot")),
                    new Document("Spring AI 是 Spring 生态系统中的一个新项目，旨在帮助开发者更容易地集成人工智能功能。", Map.of("category", "技术", "type", "AI")),
                    new Document("机器学习是人工智能的一个分支，它使计算机能够在不被明确编程的情况下学习并改进任务。", Map.of("category", "技术", "type", "机器学习")),
                    new Document("向量数据库是一种专门设计用于存储和查询高维向量数据的数据库，常用于相似性搜索。", Map.of("category", "技术", "type", "数据库")),
                    new Document("Milvus 是一个开源的向量数据库，专为处理 AI 应用程序中的向量相似性搜索而设计。", Map.of("category", "技术", "type", "Milvus")),
                    new Document("Java 是一种广泛使用的面向对象编程语言，以其'一次编写，到处运行'的特性而闻名。", Map.of("category", "技术", "type", "Java")),
                    new Document("人工智能（AI）是指由机器展示的智能，通常与人类的自然智能相关联。", Map.of("category", "技术", "type", "AI概念")),
                    new Document("RAG（检索增强生成）是一种结合了信息检索和文本生成的技术，提高了 AI 回答的准确性。", Map.of("category", "技术", "type", "RAG")),
                    new Document("大语言模型（LLM）是具有大量参数的深度学习模型，能够理解和生成自然语言。", Map.of("category", "技术", "type", "LLM")),
                    new Document("微服务架构是一种将单个应用程序开发为一套小服务的方法，每个服务都运行在自己的进程中。", Map.of("category", "技术", "type", "架构")));
            // 将文档添加到 Milvus Vector Store
            vectorStore.add(documents);
            // 检索与查询相似的文档
            List<Document> results = this.vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(2).build());
            log.info("检索结果: {}", results);

        } catch (Exception e) {
            log.error("初始化 Milvus 集合时发生错误", e);
        }
    }

}