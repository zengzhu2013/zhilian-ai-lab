package ai.agent.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * @author zengzhu
 * @create 2026-03-23 10:41
 */
@RestController
public class EmbaddingController {

    @Autowired
    EmbeddingModel embeddingModel;

    @GetMapping("/embedding")
    public void embedding() {
        float[] embedded = embeddingModel.embed("我叫柱子");
        System.out.println(embedded.length);
        System.out.println(Arrays.toString(embedded));


    }

}
