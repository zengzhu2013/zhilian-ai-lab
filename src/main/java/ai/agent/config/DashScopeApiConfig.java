package ai.agent.config;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * @author zengzhu
 * @create 2026-05-12 17:29
 */
@Configuration
public class DashScopeApiConfig {

    @Bean
    public DashScopeApi dashScopeApi(DashScopeConnectionProperties connectionProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(60 * 1000);
        requestFactory.setReadTimeout(3 * 60 * 1000);
        return DashScopeApi.builder()
                .apiKey(connectionProperties.getApiKey())
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();
    }

}
