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

import com.alibaba.cloud.ai.transformer.splitter.RecursiveCharacterTextSplitter;
import com.alibaba.cloud.ai.transformer.splitter.SentenceSplitter;
import ai.agent.model.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author zengzhu
 */
@RestController
@RequestMapping("/transformer")
public class TransformerController {

    private static final Logger logger = LoggerFactory.getLogger(TransformerController.class);

    private final List<Document> documents;
    private final ChatModel chatModel;

    public TransformerController(ChatModel chatModel) {
        this.chatModel = chatModel;
        logger.info("TransformerController --> start read text file");
        Resource resource = new DefaultResourceLoader().getResource(Constant.TEXT_FILE_PATH);
        TextReader textReader = new TextReader(resource); // 适用于文本数据
        this.documents = textReader.read();
    }

    /**
     * 文档拆分器
     * @return
     */
    @GetMapping("/token-text-splitter")
    public List<Document> tokenTextSplitter() {
        logger.info("start token text splitter");
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                // 每个文本块的目标token数量
                .withChunkSize(800)
                // 每个文本块的最小字符数
                .withMinChunkSizeChars(350)
                // 丢弃小于此长度的文本块
                .withMinChunkLengthToEmbed(5)
                // 文本中生成的最大块数
                .withMaxNumChunks(50)
                // 是否保留分隔符
                .withKeepSeparator(true).withPunctuationMarks(List.of('.', '?', '!', '\n', ';', ':', '。', '？', '！', '；'))
                .build();
        List<Document> documents = tokenTextSplitter.apply(this.documents);
        return documents;
    }


    /**
     * 语义相似度拆分器
     * @return
     */
    @GetMapping("/sentenceSplitter")
    public List<Document> sentenceSplitter() {
        logger.info("start sentence splitter");
        SentenceSplitter sentenceSplitter = new SentenceSplitter();
        List<Document> documents = sentenceSplitter.apply(this.documents);
        return documents;
    }

    /**
     * 递归字符文本拆分器
     * @return
     */
    @GetMapping("/recursiveCharacterTextSplitter")
    public List<Document> recursiveCharacterTextSplitter() {
        logger.info("start recursiveCharacterTextSplitter splitter");
        RecursiveCharacterTextSplitter sentenceSplitter = new RecursiveCharacterTextSplitter(1400,
                new String[]{"\n\n"});
        List<Document> documents = sentenceSplitter.apply(this.documents);
        return documents;
    }


    public List<Document> splitChineseText(List<Document> documents) {
        // Use Chinese punctuation marks
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(350)
                .withPunctuationMarks(List.of('。', '？', '！', '；'))  // Chinese punctuation
                .build();

        return splitter.apply(documents);
    }

    public List<Document> splitWithCustomMarks(List<Document> documents) {
        // Mix of English and other punctuation marks
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withPunctuationMarks(List.of('.', '?', '!', '\n', ';', ':', '。'))
                .build();

        return splitter.apply(documents);
    }









//    @GetMapping("/content-format-transformer")
//    public List<Document> contentFormatTransformer() {
//        logger.info("start content format transformer");
//        DefaultContentFormatter defaultContentFormatter = DefaultContentFormatter.defaultConfig();
//
//        ContentFormatTransformer contentFormatTransformer = new ContentFormatTransformer(defaultContentFormatter);
//
//        return contentFormatTransformer.apply(this.documents);
//    }
//
//    @GetMapping("/keyword-metadata-enricher")
//    public List<Document> keywordMetadataEnricher() {
//        logger.info("start keyword metadata enricher");
//        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(this.chatModel, 3);
//        return keywordMetadataEnricher.apply(this.documents);
//    }
//
//    @GetMapping("/summary-metadata-enricher")
//    public List<Document> summaryMetadataEnricher() {
//        logger.info("start summary metadata enricher");
//        List<SummaryMetadataEnricher.SummaryType> summaryTypes = List.of(
//                SummaryMetadataEnricher.SummaryType.NEXT,
//                SummaryMetadataEnricher.SummaryType.CURRENT,
//                SummaryMetadataEnricher.SummaryType.PREVIOUS);
//        SummaryMetadataEnricher summaryMetadataEnricher = new SummaryMetadataEnricher(this.chatModel, summaryTypes);
//
//        return summaryMetadataEnricher.apply(this.documents);
//    }
}
