package ai.agent.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIDetectionHook;
import com.alibaba.cloud.ai.graph.agent.hook.pii.PIIType;
import com.alibaba.cloud.ai.graph.agent.hook.pii.RedactionStrategy;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import ai.agent.service.MessageSummarizationHook;
import ai.agent.utils.PythonTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@Slf4j
public class AgentConfig {

    @Autowired
    DataSource dataSource;

    protected record ToolConfigParam(String input) { }

    private  ToolCallback createSendEmailTool() {
        return FunctionToolCallback.builder("sendEmailTool", (ToolConfigParam input) -> "发送邮件成功，入参为："+input.input())
                .description("发送邮件")
                .inputType(ToolConfigParam.class)
                .build();
    }

    private  ToolCallback createDeleteDataTool() {
        return FunctionToolCallback.builder("deleteDataTool", (ToolConfigParam input) -> "数据删除成功，入参为："+input.input())
                .description("删除数据")
                .inputType(ToolConfigParam.class)
                .build();
    }

    @Bean(name = "testAgent") // 这里的名称必须与你加载时使用的名称匹配
    public ReactAgent testAgent(ChatModel chatModel) {

        // 用于总结的模型（可以是更便宜的模型） 也可以用com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
        ChatModel summaryModel = chatModel;
        MessageSummarizationHook summarizationHook = new MessageSummarizationHook(
                summaryModel,
                8000,  // 在 8000 tokens 时触发总结
                30     // 总结后保留最后 30 条消息
        );

        PIIDetectionHook pii = PIIDetectionHook.builder()
                .piiType(PIIType.EMAIL) //对邮箱进行脱敏 输出 ‘REDACTED_EMAIL’
                .strategy(RedactionStrategy.REDACT)
                .applyToInput(true)
                .applyToOutput(true)
                .build();

        ToolCallback sendEmailTool = createSendEmailTool();
        ToolCallback deleteDataTool = createDeleteDataTool();

        // 创建 Human-in-the-Loop Hook
        HumanInTheLoopHook humanReviewHook = HumanInTheLoopHook.builder()
                .approvalOn("sendEmailTool", ToolConfig.builder()
                        .description("确认发送邮件")
                        .build())
                .approvalOn("deleteDataTool", ToolConfig.builder()
                        .description("确认删除数据")
                        .build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("test1_agent")       //必须
                .model(chatModel)
                // 设置配置相信
                .chatOptions(DashScopeChatOptions.builder().multiModel(true).enableThinking(false).build())
                .systemPrompt("你是一个聪明的AI智能体")
                .hooks(ModelCallLimitHook.builder().runLimit(5).build())  // 限制模型调用次数为5次
                .hooks(summarizationHook)
                .hooks(pii)

                .tools(sendEmailTool, deleteDataTool)
                .hooks(humanReviewHook)

                //在将上下文发送给 LLM 之前对其进行修改，以注入、删除或修改信息。
                .interceptors(ContextEditingInterceptor.builder().trigger(120000).clearAtLeast(60000).build())

                // 存储访问跨对话, 将短期记忆保存到MySQL数据库中
                .saver(MysqlSaver.builder()
                        .dataSource(dataSource)
                        .build())
//                .instruction("""
//                         在回答问题时，请：
//                          1. 首先理解用户的核心需求
//                          2. 分析可能的技术方案
//                          3. 提供清晰的建议和理由
//                          4. 如果需要更多信息，主动询问
//                          保持专业、友好的语气。
//                        """)
                .build();
        return agent;
    }


    private static final String SKILLS_DIR = "skills";

    @Bean(name = "skillsAgent")
    public ReactAgent skillsAgent(ChatModel chatModel) {
        Path skillsPath = Path.of(SKILLS_DIR).toAbsolutePath();
        log.info("Skills directory: {}", skillsPath);

        if (!Files.exists(skillsPath)) {
            log.error("Skills directory not found at: {}", skillsPath);
            throw new IllegalStateException("Skills directory not found");
        }

        log.info("Skills directory exists, listing contents:");
        try {
            Files.list(skillsPath).forEach(p ->
                    log.info("  - {}", p.getFileName())
            );
        } catch (IOException e) {
            log.error("Failed to list directory", e);
        }

        SkillsAgentHook skillsHook = SkillsAgentHook.builder()
                .skillRegistry(FileSystemSkillRegistry.builder().projectSkillsDirectory(SKILLS_DIR).build())
                .build();

        ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
                .shellTool2(ShellTool2.builder(System.getProperty("user.dir")).build())
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("skillsAgent")
                .model(chatModel)
                .saver(new MemorySaver())
                .tools(PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION))
                .hooks(List.of(skillsHook, shellHook))
                .enableLogging(true)
                .build();
        return agent;
    }

}