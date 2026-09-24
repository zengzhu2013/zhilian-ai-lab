package ai.agent.controller;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * @author zengzhu
 * @create 2026-05-12 17:31
 */
@RestController("/api/skillsAgent")
public class SkillsAgentController {

    @Autowired
    private ReactAgent skillsAgent;

//    private final SkillsAgent skillsAgent;
//    private final ChatModel chatModel;

    /*public SkillsAgentController(SkillsAgent skillsAgent, ChatModel chatModel) {
        this.skillsAgent = skillsAgent;
        this.chatModel = chatModel;
    }*/

    @GetMapping("/chat")
    public String chat(String message) throws GraphRunnerException {
        RunnableConfig config = RunnableConfig.builder()
                .threadId("test_user_002")
                .build();
        UserMessage userMessage = new UserMessage(message);
        return String.valueOf(skillsAgent.call(userMessage, config));
    }

}
