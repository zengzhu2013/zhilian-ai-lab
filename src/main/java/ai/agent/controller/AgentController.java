package ai.agent.controller;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController("myAgentController")
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private ReactAgent testAgent;

    public record StreamMessage(String outputType, String thinking, String content) {}

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StreamMessage>> streamChat(String query, HttpServletResponse response) throws GraphRunnerException {
        response.setCharacterEncoding("UTF-8");
        RunnableConfig config = RunnableConfig.builder()
                .threadId("test_user_001")
                .build();
        UserMessage userMessage = new UserMessage(query);
        Flux<NodeOutput> agentStream = testAgent.stream(userMessage, config);

        return agentStream
                .filter(output -> !output.isSTART() && !output.isEND()) // 过滤掉开始和结束事件
                .map(output -> {
                    // 处理流式输出
                    String outContent = "";
                    String thinking = "normal";
                    OutputType type = null;
                    if (output instanceof StreamingOutput<?> streamingOutput) {
                        type = streamingOutput.getOutputType();
                        Message message = streamingOutput.message();
                        // 处理模型推理的流式输出
                        if (type == OutputType.AGENT_MODEL_STREAMING) {
                            // 流式增量内容，逐步显示
                            MessageType messageType = message.getMessageType();
                            if (messageType == MessageType.ASSISTANT) {
                                Map<String, Object> reasoningContent = message.getMetadata();
                                if (reasoningContent != null && reasoningContent.containsKey("reasoningContent")
                                        && reasoningContent.get("reasoningContent") != null
                                        && !"".equals(reasoningContent.get("reasoningContent").toString())) {
                                    outContent = reasoningContent.get("reasoningContent").toString();
                                    thinking = "thinking";
                                } else {
                                    outContent = message.getText();
                                }
                            }
                            System.out.println(outContent);
                        } else if (type == OutputType.AGENT_MODEL_FINISHED) {
                            // 处理模型输出完成
                            outContent = "";
                            if (message instanceof AssistantMessage assistantMessage) {
                                if (assistantMessage.hasToolCalls()) {
                                    // 工具调用请求
                                    assistantMessage.getToolCalls().forEach(toolCall -> {
                                        System.out.println("[Tool Call] " + toolCall.name() + ": " + toolCall.arguments());
                                    });
                                } else {
                                    // 模型完整响应
                                    System.out.println("\n模型输出完成");
                                    String chunk = streamingOutput.chunk();
                                    if (chunk != null && !chunk.isEmpty()) {
                                        outContent = chunk;
                                    }
                                }
                            }
                        }

                        // 处理工具调用完成（目前不支持 STREAMING）
                        if (type == OutputType.AGENT_TOOL_FINISHED) {
                            System.out.println("工具调用完成: " + output.node());
                            if (message instanceof ToolResponseMessage toolResponse) {
                                toolResponse.getResponses().forEach(r -> {
                                    System.out.println("[Tool Result] " + r.name() + ": " + r.responseData());
                                });
                            }
                        }

                        // 对于 Hook 节点，通常只关注完成事件（如果Hook没有有效输出可以忽略）
                        if (type == OutputType.AGENT_HOOK_FINISHED) {
                            System.out.println("Hook 执行完成: " + output.node());
                        }

                    } else {
                        // 普通节点输出
                        String nodeId = output.node();
                        Map<String, Object> state = output.state().data();
                        System.out.println("节点 '" + nodeId + "'执行完成");
                        if (state.containsKey("result")) {
                            System.out.println("最终结果: " + state.get("result"));
                            outContent = state.get("result").toString();
                        }

                        if(output instanceof InterruptionMetadata interruptionMetadata){
                            System.out.println("中断元数据: " + interruptionMetadata);
                            outContent = interruptionMetadata.toString();
                        }
                    }

                    // 创建包含内容的消息
                    StreamMessage message = new StreamMessage(type != null ? type.name() : output.node(), thinking, outContent);
                    // 返回ServerSentEvent
                    return ServerSentEvent.<StreamMessage>builder()
                            .data(message)
                            .build();
                })
                .concatWith(Flux.just(ServerSentEvent.<StreamMessage>builder()
                        .data(new StreamMessage("END", "normal", "模型流式输出回答完成标识(END)"))
                        .build()))
                .doOnComplete(() -> System.out.println("流式输出完成"))
                .doOnError(error -> {
                    System.err.println("流处理错误: " + error.getMessage());
                });
    }



    public static void main(String[] args) throws GraphRunnerException {

    }

}
