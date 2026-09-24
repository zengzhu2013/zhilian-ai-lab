/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.agent.controller;

import ai.agent.model.Constant;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 读取pdf文件，将文件内容写入到向量数据库中
 */
@RestController
@RequestMapping("/writer")
public class WriterController {

    private static final Logger logger = LoggerFactory.getLogger(WriterController.class);

    private final List<Document> documents;

    private final VectorStore vectorStore;

    private final EmbeddingModel embeddingModel;

    public WriterController(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        logger.info("WriterController --> start read pdf file by page");
        Resource resource = new DefaultResourceLoader().getResource(Constant.PDF_FILE_PATH);
        PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(resource); // 只可以传pdf格式文件
        //pagePdfDocumentReader.read();
        this.documents = pagePdfDocumentReader.get().subList(0, 5); // query 5 pages
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }


    @GetMapping("/writeVector")
    public void writeVector() {
        logger.info("Writing vector...");
        vectorStore.add(documents);
    }

    @GetMapping("/similaritySearch")
    public List<Document> similaritySearch(@RequestParam("msg") String msg) {
        msg = "招标文件发售开始时间";
        logger.info("start search data: {}", msg);
        float[] embed = embeddingModel.embed(msg);
        logger.info("embed: {}", embed);
        return vectorStore.similaritySearch(SearchRequest
                .builder()
                .query(msg)
                .topK(2).similarityThreshold(0.4)
                .build());
    }




    @GetMapping("/writeCollectionVector")
    public void writeCollectionVector() {
        logger.info("Writing collection vector...");

        // 直接使用 Milvus 原生客户端
        Optional<MilvusServiceClient> nativeClient = vectorStore.getNativeClient();
        if (nativeClient.isPresent()) {
            // 为每个文档生成嵌入向量并准备插入数据
            for (Document document : documents) {
                document.getMetadata().put("size", document.getText().length());
            }
            MilvusServiceClient client = nativeClient.get();
            try {
                // 确保集合存在（不存在则自动创建）
                ensureCollectionExists(client);
                // 插入文档
                for (Document document : documents) {
                    insertDocuments(client, embeddingModel, document);
                }
                System.out.println("数据插入完成！");
            } catch (Exception e) {
                System.err.println("操作失败: " + e.getMessage());
            } finally {
            }
        } else {
            logger.error("Milvus native client is not available");
        }
    }

    public static void ensureCollectionExists(MilvusServiceClient client) {
        // 检查集合是否存在
        HasCollectionParam hasParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        R<Boolean> hasResp = client.hasCollection(hasParam);

        if (hasResp.getStatus() != 0) {
            throw new RuntimeException("检查集合失败: " + hasResp.getMessage());
        }

        if (hasResp.getData()) {
            System.out.println("集合 " + COLLECTION_NAME + " 已存在，直接使用");
            return;
        }

        // 创建集合
        System.out.println("集合 " + COLLECTION_NAME + " 不存在，开始创建...");

        // 定义字段结构
        List<FieldType> fieldTypes = new ArrayList<>();

        // id 字段：主键，自动生成
        FieldType idField = FieldType.newBuilder()
                .withName("id")
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build();
        fieldTypes.add(idField);

        // vector 字段：浮点向量，维度 1024
        FieldType vectorField = FieldType.newBuilder()
                .withName("vector")
                .withDataType(DataType.FloatVector)
                .withDimension(VECTOR_DIMENSION)
                .build();
        fieldTypes.add(vectorField);

        // content 字段：字符串，最大长度 1024
        FieldType contentField = FieldType.newBuilder()
                .withName("content")
                .withDataType(DataType.VarChar)
                .withMaxLength(CONTENT_MAX_LENGTH)
                .build();
        fieldTypes.add(contentField);

        FieldType metadataField = FieldType.newBuilder()
                .withName("metadata")
                .withDataType(DataType.JSON)
                .build();
        fieldTypes.add(metadataField);


        // 创建集合
        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDescription("RAG 测试集合，存储文档向量和内容")
                .withFieldTypes(fieldTypes)
                .build();

        client.createCollection(createParam);
        System.out.println("集合 " + COLLECTION_NAME + " 创建成功");

        CreateIndexParam indexParam = CreateIndexParam.newBuilder().withCollectionName(COLLECTION_NAME)
                .withFieldName("vector").withIndexName("vector_index").withIndexType(IndexType.IVF_FLAT)
                .withMetricType(MetricType.COSINE)
                .build();
        client.createIndex(indexParam);
        System.out.println("集合 " + COLLECTION_NAME + " 创建索引vector_index成功");

        //  Load the collection
        LoadCollectionParam loadCollectionReq = LoadCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        client.loadCollection(loadCollectionReq);
        System.out.println("集合 " + COLLECTION_NAME + " 加载成功");
    }


    public void insertDocuments(MilvusServiceClient client,
                                EmbeddingModel embeddingModel,
                                       Document document) {
        if (document == null) {
            return;
        }

        // 单条转换
        String content = document.getText();
        float[] vectorArray = embeddingModel.embed(content);

        List<Float> vectorList = new ArrayList<>(vectorArray.length);
        for (float v : vectorArray) vectorList.add(v);

        // 4. 将 metadata 转换为 Gson JsonObject
        com.google.gson.JsonObject metadataJson = new com.google.gson.JsonObject();
        // 4.1 假设 document.getMetadata() 返回 Map<String, Object>
        Map<String, Object> metadataMap = document.getMetadata();
        for (Map.Entry<String, Object> entry : metadataMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                metadataJson.addProperty(key, (String) value);
            } else if (value instanceof Number) {
                metadataJson.addProperty(key, (Number) value);
            } else if (value instanceof Boolean) {
                metadataJson.addProperty(key, (Boolean) value);
            } else if (value instanceof List) {
                // 将 List 转为 JsonArray
                com.google.gson.JsonArray array = new com.google.gson.JsonArray();
                for (Object item : (List<?>) value) {
                    array.add(String.valueOf(item)); // 根据实际类型调整
                }
                metadataJson.add(key, array);
            } else if (value instanceof Map) {
                // 将嵌套 Map 转为 JsonObject（递归）
                com.google.gson.JsonObject nested = new com.google.gson.JsonObject();
                for (Map.Entry<?, ?> nestedEntry : ((Map<?, ?>) value).entrySet()) {
                    nested.addProperty(String.valueOf(nestedEntry.getKey()), String.valueOf(nestedEntry.getValue()));
                }
                metadataJson.add(key, nested);
            } else {
                metadataJson.addProperty(key, String.valueOf(value));
            }
        }

        List<InsertParam.Field> fields = Arrays.asList(
                new InsertParam.Field("vector", Collections.singletonList(vectorList)),
                new InsertParam.Field("content", Collections.singletonList(content)),
                new InsertParam.Field("metadata", Collections.singletonList(metadataJson))
        );

        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFields(fields)
                .build();
        R<MutationResult> response = client.insert(insertParam);
        if (response.getStatus() != 0) {
            throw new RuntimeException("插入失败: " + response.getMessage());
        }

    }

    private static final String COLLECTION_NAME = "diy_rag_test";
    private static final int VECTOR_DIMENSION = 1536;
    private static final int CONTENT_MAX_LENGTH = 65535;

}
