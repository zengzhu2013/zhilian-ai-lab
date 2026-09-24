package ai.agent;

import com.alibaba.dashscope.embeddings.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;

import java.util.List;

/**
 * @author zengzhu
 * @create 2026-03-25 14:54
 */
public class Test1 {
    public static void main(String[] args) {
        try {
            MultiModalEmbeddingItemBase itemBase = new MultiModalEmbeddingItemText("我叫柱子");
            MultiModalEmbeddingParam param = MultiModalEmbeddingParam
                    .builder()
                    .model("multimodal-embedding-v1").apiKey(System.getenv("DASHSCOPE_API_KEY"))
                    .contents(List.of(itemBase))
                    .build();
            MultiModalEmbedding textEmbedding = new MultiModalEmbedding();
            MultiModalEmbeddingResult result = textEmbedding.call(param);
            System.out.println(result.getOutput().getEmbeddings().get(0).getEmbedding());

            List.of();

        } catch (ApiException | NoApiKeyException e) {
            System.out.println(e.getMessage());
        } catch (UploadFileException e) {
            System.out.println(e.getMessage());
        }
    }

}
