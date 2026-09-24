package ai.agent.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用消息总结
 * @author zengzhu
 * @create 2026-04-28 15:50
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class MessageSummarizationHook extends MessagesModelHook {

    private final ChatModel summaryModel;
    private final int maxTokensBeforeSummary;
    private final int messagesToKeep;

    public MessageSummarizationHook(
            ChatModel summaryModel,
            int maxTokensBeforeSummary,
            int messagesToKeep
    ) {
        this.summaryModel = summaryModel;
        this.maxTokensBeforeSummary = maxTokensBeforeSummary;
        this.messagesToKeep = messagesToKeep;
    }

    @Override
    public String getName() {
        return "message_summarization";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 估算 token 数量（简化版）
        int estimatedTokens = previousMessages.stream()
                .mapToInt(m -> m.getText().length() / 4)
                .sum();

        if (estimatedTokens < maxTokensBeforeSummary) {
            // 如果 token 数量未超过阈值，无需总结
            return new AgentCommand(previousMessages);
        }

        // 需要总结
        int messagesToSummarize = previousMessages.size() - messagesToKeep;
        if (messagesToSummarize <= 0) {
            // 如果消息数量不足以总结，无需更改
            return new AgentCommand(previousMessages);
        }

        List<Message> oldMessages = previousMessages.subList(0, messagesToSummarize);
        List<Message> recentMessages = previousMessages.subList(
                messagesToSummarize,
                previousMessages.size()
        );

        // 生成摘要
        String summary = generateSummary(oldMessages);

        // 创建摘要消息
        SystemMessage summaryMessage = new SystemMessage(
                "## 之前对话摘要:\n" + summary
        );

        // 构建新的消息列表：摘要消息 + 最近的消息
        List<Message> newMessages = new ArrayList<>();
        newMessages.add(summaryMessage);
        newMessages.addAll(recentMessages);

        // 使用 REPLACE 策略替换所有消息
        return new AgentCommand(newMessages, UpdatePolicy.REPLACE);
    }

    private String generateSummary(List<Message> messages) {
        StringBuilder conversation = new StringBuilder();
        for (Message msg : messages) {
            conversation.append(msg.getMessageType())
                    .append(": ")
                    .append(msg.getText())
                    .append("\n");
        }

        String summaryPrompt = "请简要总结以下对话:\n\n" + conversation;
        ChatResponse response = summaryModel.call(
                new Prompt(new UserMessage(summaryPrompt))
        );
        return response.getResult().getOutput().getText();
    }
}
