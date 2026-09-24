package ai.agent;

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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author zengzhu
 * @create 2026-06-03 18:00
 */
public class HumanInTheLoopTest {

    record ToolInputParam(String input) { }

    // 初始化 ChatModel
    static DashScopeApi dashScopeApi = DashScopeApi.builder()
            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
            .build();

    static ChatModel chatModel = DashScopeChatModel.builder()
            .dashScopeApi(dashScopeApi).defaultOptions(DashScopeChatOptions.builder().multiModel(true)
                    .enableThinking(false).model("qwen3.6-plus").build())
            .build();

    public static void main(String[] args) throws Exception {


        MemorySaver memorySaver = new MemorySaver();

        ToolCallback poetTool = FunctionToolCallback.builder("poem", (arg) -> {
                    ToolInputParam arg1 = (ToolInputParam)arg;
                    return arg1.input() + "小小鸭子...";
                })
                .description("写诗工具")
                .inputType(ToolInputParam.class)
                .build();

        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("poem", ToolConfig.builder()
                        .description("请确认诗歌创作操作")
                        .build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("poet_agent")
                .model(chatModel)
                .tools(List.of(poetTool))
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();

        String threadId = "user-session-001";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 第一次调用 - 触发中断
        System.out.println("=== 第一次调用：期望中断 ===");
        Optional<NodeOutput> result = agent.invokeAndGetOutput(
                "帮我写一首10字左右的诗",
                config
        );

        // 检查中断并处理
        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            System.out.println("检测到中断，需要人工审批");
            List<InterruptionMetadata.ToolFeedback> toolFeedbacks =
                    interruptionMetadata.toolFeedbacks();

            for (InterruptionMetadata.ToolFeedback feedback : toolFeedbacks) {
                System.out.println("工具: " + feedback.getName());
                System.out.println("参数: " + feedback.getArguments());
                System.out.println("描述: " + feedback.getDescription());
            }

            // 构建批准反馈
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            // 对每个工具调用设置批准决策
            interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback approvedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
                                .description("不允许诗歌创作") // 如果为拒绝必须自定义拒绝原因
                                .build();
                feedbackBuilder.addToolFeedback(approvedFeedback);
            });

            InterruptionMetadata approvalMetadata = feedbackBuilder.build();

            // 使用批准决策恢复执行
            RunnableConfig resumeConfig = RunnableConfig.builder()
                    .threadId(threadId) // 相同的线程ID以恢复暂停的对话
                    .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvalMetadata)
                    .build();

            // 第二次调用以恢复执行
            System.out.println("\n=== 第二次调用：使用批准决策恢复 ===");
            Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);

            if (finalResult.isPresent()) {
                System.out.println("执行完成");
                NodeOutput nodeOutput = finalResult.get();
                ArrayList<Object> list = (ArrayList<Object>) nodeOutput.state().data().get("messages");
                for (int i = 0; i < list.size(); i++) {
                    if(list.get(i) instanceof AssistantMessage assistantMessage){
                        if (!assistantMessage.hasToolCalls()) {
                            System.out.println("最终结果: ");
                            System.out.println(assistantMessage.getText());
                        }
                    }
                }
            }
        }
        System.out.println("批准决策示例执行完成");



//        example3_editDecision();
//        example4_rejectDecision();
    }

    public static void example3_editDecision() throws Exception {
        MemorySaver memorySaver = new MemorySaver();

        ToolCallback executeSqlTool = FunctionToolCallback.builder("execute_sql", (args) -> "SQL执行结果")
                .description("执行SQL语句")
                .inputType(ToolInputParam.class)
                .build();

        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("execute_sql", ToolConfig.builder()
                        .description("SQL执行操作需要审批")
                        .build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("sql_agent")
                .model(chatModel)
                .tools(executeSqlTool)
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();

        String threadId = "sql-session-001";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 第一次调用 - 触发中断
        Optional<NodeOutput> result = agent.invokeAndGetOutput(
                "DELETE FROM records",
                config
        );

        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            // 构建编辑反馈
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
                // 修改工具参数
                String editedArguments = toolFeedback.getArguments()
                        .replace("DELETE FROM records", "DELETE FROM old_records");

                InterruptionMetadata.ToolFeedback editedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .arguments(editedArguments)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.EDITED)
                                .build();
                feedbackBuilder.addToolFeedback(editedFeedback);
            });

            InterruptionMetadata editMetadata = feedbackBuilder.build();

            // 使用编辑决策恢复执行
            RunnableConfig resumeConfig = RunnableConfig.builder()
                    .threadId(threadId)
                    .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, editMetadata)
                    .build();

            Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);
            if (finalResult.isPresent()) {
                System.out.println("执行完成");
                NodeOutput nodeOutput = finalResult.get();
                ArrayList<Object> list = (ArrayList<Object>) nodeOutput.state().data().get("messages");
                for (int i = 0; i < list.size(); i++) {
                    if(list.get(i) instanceof AssistantMessage assistantMessage){
                        if (!assistantMessage.hasToolCalls()) {
                            System.out.println("最终结果: ");
                            System.out.println(assistantMessage.getText());
                        }
                    }
                }
            }
            System.out.println("编辑决策示例执行完成");
        }
    }

    public static void example4_rejectDecision() throws Exception {
        MemorySaver memorySaver = new MemorySaver();

        ToolCallback deleteTool = FunctionToolCallback.builder("delete_data", (args) -> "数据已删除")
                .description("删除数据")
                .inputType(String.class)
                .build();

        HumanInTheLoopHook humanInTheLoopHook = HumanInTheLoopHook.builder()
                .approvalOn("delete_data", ToolConfig.builder()
                        .description("删除操作需要审批")
                        .build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("delete_agent")
                .model(chatModel)
                .tools(deleteTool)
                .hooks(List.of(humanInTheLoopHook))
                .saver(memorySaver)
                .build();

        String threadId = "delete-session-001";
        RunnableConfig config = RunnableConfig.builder()
                .threadId(threadId)
                .build();

        // 第一次调用 - 触发中断
        Optional<NodeOutput> result = agent.invokeAndGetOutput(
                "删除所有用户数据",
                config
        );

        if (result.isPresent() && result.get() instanceof InterruptionMetadata) {
            InterruptionMetadata interruptionMetadata = (InterruptionMetadata) result.get();

            // 构建拒绝反馈
            InterruptionMetadata.Builder feedbackBuilder = InterruptionMetadata.builder()
                    .nodeId(interruptionMetadata.node())
                    .state(interruptionMetadata.state());

            interruptionMetadata.toolFeedbacks().forEach(toolFeedback -> {
                InterruptionMetadata.ToolFeedback rejectedFeedback =
                        InterruptionMetadata.ToolFeedback.builder(toolFeedback)
                                .result(InterruptionMetadata.ToolFeedback.FeedbackResult.REJECTED)
                                .description("不允许删除操作，请使用归档功能代替。")
                                .build();
                feedbackBuilder.addToolFeedback(rejectedFeedback);
            });

            InterruptionMetadata rejectMetadata = feedbackBuilder.build();

            // 使用拒绝决策恢复执行
            RunnableConfig resumeConfig = RunnableConfig.builder()
                    .threadId(threadId)
                    .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, rejectMetadata)
                    .build();

            Optional<NodeOutput> finalResult = agent.invokeAndGetOutput("", resumeConfig);
            if (finalResult.isPresent()) {
                System.out.println("执行完成");
                NodeOutput nodeOutput = finalResult.get();
                ArrayList<Object> list = (ArrayList<Object>) nodeOutput.state().data().get("messages");
                for (int i = 0; i < list.size(); i++) {
                    if(list.get(i) instanceof AssistantMessage assistantMessage){
                        if (!assistantMessage.hasToolCalls()) {
                            System.out.println("最终结果: ");
                            System.out.println(assistantMessage.getText());
                        }
                    }
                }
            }
            System.out.println("拒绝决策示例执行完成");
        }
    }

}
