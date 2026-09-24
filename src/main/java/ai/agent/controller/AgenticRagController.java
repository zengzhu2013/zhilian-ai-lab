//
//package ai.agent.controller;
//
//import com.alibaba.cloud.ai.graph.NodeOutput;
//import com.alibaba.cloud.ai.graph.OverAllState;
//import com.alibaba.cloud.ai.graph.agent.ReactAgent;
//import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
//import ai.agent.common.ApplicationConstant;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.messages.AbstractMessage;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.tool.ToolCallback;
//import org.springframework.ai.tool.function.FunctionToolCallback;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//@Tag(name = "AgenticRagController", description = "Rag接口")
//@Slf4j
//@RestController
//@RequestMapping(ApplicationConstant.API_VERSION + "/ai")
//public class AgenticRagController {
//
//
//    // 对话代理
//    ChatModel chatModel;
//
//    // 向量数据库
//    VectorStore vectorStore;
//
//
//    public AgenticRagController(ChatModel chatModel,
//                                VectorStore vectorStore) {
//        this.chatModel = chatModel;
//        this.vectorStore = vectorStore;
//    }
//
//    // 创建文档检索工具
//    class DocumentSearchTool {
//        public Response search(Request request) {
//            // 从向量存储检索相关文档
//            List<Document> docs = vectorStore.similaritySearch(request.query());
//
//            // 合并文档内容
//            String combinedContent = docs.stream()
//                    .map(Document::getText)
//                    .collect(Collectors.joining("\n\n"));
//            return new Response(combinedContent);
//        }
//
//        public record Request(String query) { }
//
//        public record Response(String content) { }
//    }
//
//
//    @GetMapping("/agenticRag")
//    public String agenticRag(
//            @RequestParam(value = "question", defaultValue = "招标文件每套售价多少人民币？") String question, HttpServletResponse response) throws GraphRunnerException {
//        response.setCharacterEncoding("UTF-8");
//
//        DocumentSearchTool searchTool = new DocumentSearchTool();
//
//        // 创建工具回调
//        ToolCallback searchCallback = FunctionToolCallback.builder("search_documents",
//                        (Function<DocumentSearchTool.Request, DocumentSearchTool.Response>)
//                                request -> searchTool.search(request))
//                .description("搜索文档以查找相关信息")
//                .inputType(DocumentSearchTool.Request.class)
//                .build();
//
//        // 创建带有检索工具的Agent
//        ReactAgent ragAgent = ReactAgent.builder()
//                .name("ragAgent")
//                .model(chatModel)
//                .instruction("你是一个智能助手。当需要查找信息时，使用search_documents工具。" +
//                        "基于检索到的信息回答用户的问题，并引用相关片段。")
//                .tools(searchCallback)
//                .build();
//
//        NodeOutput result = ragAgent.invokeAndGetOutput(question).orElse(null);
//        return extractResponse(result);
//    }
//
//    private String extractResponse(NodeOutput result) {
//        if (result == null) {
//            return "No response generated.";
//        }
//
//        OverAllState state = result.state();
//
//        // Try "output" key first (common for ReactAgent)
//        Optional<Object> output = state.value("output");
//        if (output.isPresent()) {
//            return String.valueOf(output.get());
//        }
//
//        // Fallback to "messages" key
//        Optional<List<AbstractMessage>> messages = state.value("messages");
//        if (messages.isPresent() && !messages.get().isEmpty()) {
//            List<AbstractMessage> msgList = messages.get();
//            return msgList.get(msgList.size() - 1).getText();
//        }
//
//        // Last resort: return state string representation
//        return state.toString();
//	}
//
//
//}