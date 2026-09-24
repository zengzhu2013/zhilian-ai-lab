package ai.agent;

/**
 * @author zengzhu
 * @create 2026-04-27 17:03
 */
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
@SpringBootTest
public class TestChatModel {

    // 演示：基于ReactAgent设置模型配置项
    // 测试启动思考
    @Test
    public void test1(@Autowired DashScopeChatModel chatModel) throws GraphRunnerException {

        ReactAgent agent = ReactAgent.builder()
                .name("test1_agent")       //必须
                .model(chatModel)
                // 设置配置相信
                .chatOptions(DashScopeChatOptions.builder().multiModel(true).enableThinking(true).build())
                .systemPrompt("你是一个有帮助的AI助手")
                .instruction("""
                         在回答问题时，请：
                          1. 首先理解用户的核心需求
                          2. 分析可能的技术方案
                          3. 提供清晰的建议和理由
                          4. 如果需要更多信息，主动询问 
                          保持专业、友好的语气。
                        """)
                .build();

        // 调用 Agent
        AssistantMessage response = agent.call("你是谁");

        System.out.println(response.getMetadata());
        System.out.println(response.getMetadata().get("reasoningContent"));
        System.out.println(response.getText());
    }


}
