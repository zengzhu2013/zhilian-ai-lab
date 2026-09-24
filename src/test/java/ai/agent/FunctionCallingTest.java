package ai.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author zengzhu
 * @create 2026-04-03 14:55
 */
public class FunctionCallingTest {


    public static void functionCalling() {
//        DashScopeApi dashScopeApi = DashScopeApi.builder()
//                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
//                .build();

//        ChatModel chatModel = DashScopeChatModel.builder()
//                .dashScopeApi(dashScopeApi)
//                .build();

        // 定义函数
        ToolCallback weatherFunction = FunctionToolCallback.builder("getWeather", (city) -> {
                    System.out.println("getWeather: " + city);
                    // 实际的天气查询逻辑
                    return "晴朗，25°C";
                })
                .description("获取指定城市的天气")
                .inputType(String.class)
                .build();

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .build();
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue())
                .temperature(0.4)
                .maxTokens(200).toolCallbacks(weatherFunction)
                .build();
        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(options)
                .build();

//


        // 使用函数
//        DashScopeChatOptions options = DashScopeChatOptions.builder()
//                .withToolCallbacks(List.of(weatherFunction))
//                .build();

        Prompt prompt = new Prompt("北京的天气怎么样?", options);
        ChatResponse response = chatModel.call(prompt);
        System.out.println(response.getResult().getOutput().getText());
	}

    public static void accessingContext() throws GraphRunnerException {
        ToolCallback accountTool = FunctionToolCallback
                .builder("get_account_info", (query, toolContext) -> {
                    System.out.println("get_account_info: " + query);
                    // 实际的账户查询逻辑
                    return "Account holder: Alice Johnson\nType: Premium\nBalance: $5000";
                })
                .description("获取当前账号信息")
                .inputType(Object.class)
                .build();

        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        /*ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions
                        .builder()
                        .model("qwen3.5-plus").multiModel(true)
                        .build())
                .build();*/

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .build();
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue())
                .temperature(0.4)
                .maxTokens(200).toolCallbacks(accountTool)
                .build();
        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(options)
                .build();

        System.out.println(chatModel.call("user_id为user123的账户信息是多少"));




        /*// 在 ReactAgent 中使用
        ReactAgent agent = ReactAgent.builder()
                .name("问题解决助手")
                .model(chatModel)
                .tools(accountTool)
                .systemPrompt("你是一名财务助理")
                .build();

        // 调用时传递上下文
        RunnableConfig config = RunnableConfig.builder()
                .addMetadata("user_id", "user123")
                .build();

        System.out.println(agent.call("user_id为user123的账户信息是多少", config).getText());*/



    }

    public static void toolUsage() throws GraphRunnerException {
/*        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
//                .defaultOptions(DashScopeChatOptions
//                        .builder()
//                        .model("qwen3.5-plus").multiModel(true)
//                        .build())
                .build();*/

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .build();
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue())
                .temperature(0.4)
                .maxTokens(200)
                .build();
        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(options)
                .build();

        // 创建工具回调
        ToolCallback searchTool = FunctionToolCallback
                .builder("搜索", new SearchTool())
                .description("搜索信息的工具")
                .inputType(String.class)
                .build();

        /*// 使用多个工具
        ReactAgent agent = ReactAgent.builder()
                .name("my_agent")
                .model(chatModel)
                .tools(searchTool)
                .build();
        System.out.println(agent.call("搜索").getText());*/

        ChatClient.StreamResponseSpec stream = ChatClient.create(chatModel)
                .prompt("What day is tomorrow?")
                .toolCallbacks(searchTool).stream();
        System.out.println(
                stream
        );


    }

    public static class SearchTool implements BiFunction<String, ToolContext, String> {
        @Override
        public String apply(
                @ToolParam(description = "搜索关键词") String query,
                ToolContext toolContext) {
            return "搜索结果：" + query;
        }
    }


    public static void main(String[] args) throws GraphRunnerException {
//        functionCalling();
        toolUsage();
    }
}
