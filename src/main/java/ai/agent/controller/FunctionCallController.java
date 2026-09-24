package ai.agent.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FunctionCallController {

    private final DashScopeChatModel chatClient;


    @GetMapping("/ai/fc")
    public String fc(@RequestParam(value = "message", defaultValue = "深圳有多少个叫pillar的人?") String message){
        UserMessage userMessage =new UserMessage(message);

        ChatResponse response = chatClient.call(new Prompt(
                List.of(userMessage),
                DashScopeChatOptions.builder().toolName("getLocationAndNum").multiModel(true)
                        .model("qwen3.6-plus")
                        .build()));

        return response.getResult().getOutput().getText();
    }
}
