package ai.agent.config;

import ai.agent.service.LocationNamesService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

@Configuration
public class ToolsConfig {

    @Bean
    //必须  函数功能的描述，模型使用它来选择何时以及如何调用函数。
    @Description("用来获取某地区的名字重复数量")
    public Function<LocationNamesService.Request, LocationNamesService.Response> getLocationAndNum() {
        return new LocationNamesService();
    }


}
