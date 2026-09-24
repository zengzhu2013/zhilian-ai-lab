package ai.agent;

import com.google.common.io.Files;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.assertj.core.util.Paths;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 从 PDF 文件中提取图片。
 * 此工具通过将 PDF 的每一页渲染为图像来实现提取。
 */
public class PdfImageExtractor {

    /**
     * 存储提取结果的简单类
     */
    public static class ExtractionResult {
        private final String textContent;
        private final List<String> savedImagePaths;

        public ExtractionResult(String textContent, List<String> savedImagePaths) {
            this.textContent = textContent;
            this.savedImagePaths = savedImagePaths;
        }

        public String getTextContent() {
            return textContent;
        }

        public List<String> getSavedImagePaths() {
            return savedImagePaths;
        }
    }

    /**
     * 从指定类路径下的 PDF 文件中同时提取文字和图片。
     *
     * @param pdfClasspath 类路径下的 PDF 文件路径
     * @param dpi          输出图片的分辨率 (Dots Per Inch)
     * @return 包含提取的文字和图片路径的 ExtractionResult 对象
     */
    public static ExtractionResult extractAll(String pdfClasspath, float dpi) throws IOException {
        Resource resource = new DefaultResourceLoader().getResource(pdfClasspath);
        File pdfFile = resource.getFile();

        StringBuilder fullText = new StringBuilder();
        List<String> imagePaths = new ArrayList<>();

        System.out.println("正在加载 PDF 文件: " + pdfFile.getAbsolutePath());

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int pageCount = document.getNumberOfPages();
            System.out.printf("PDF 加载成功，共 %d 页。开始处理...\n", pageCount);

            // --- 1. 提取文字 ---
            System.out.println("正在提取文字...");
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 保持文字的逻辑顺序
            fullText.append(stripper.getText(document)); // 直接从文档获取全部文字
            System.out.println("文字提取完成。\n");

            // --- 2. 提取图片 ---
            System.out.println("正在渲染页面为图片...");
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);

                // 计算图片尺寸
                float scale = dpi / 72f;
                BufferedImage image = new BufferedImage(
                        (int) (page.getMediaBox().getWidth() * scale),
                        (int) (page.getMediaBox().getHeight() * scale),
                        BufferedImage.TYPE_INT_RGB
                );

                // 渲染页面
                pdfRenderer.renderPageToGraphics(i, image.createGraphics(), scale);

                // 保存图片
                String outputFileName = String.format("extracted_page_img_%03d.png", i);
                File outputFile = new File("pic/" + outputFileName);
                ImageIO.write(image, "PNG", outputFile);

                imagePaths.add(outputFile.getAbsolutePath());
                System.out.println("  - 已渲染并保存第 " + (i + 1) + " 页为: " + outputFile.getAbsolutePath());
            }
            System.out.println("\n所有页面均已渲染为图片。");

        } catch (IOException e) {
            System.err.println("处理 PDF 文件时发生错误:");
            e.printStackTrace();
            throw e; // 抛出异常，让调用者处理
        }

        return new ExtractionResult(fullText.toString(), imagePaths);
    }

    public static void main(String[] args) {
        String pdfPathInClasspath = "classpath:/data/RAG实践手册.pdf";
        float resolutionDpi = 200;

        try {
            // 执行提取
            ExtractionResult result = extractAll(pdfPathInClasspath, resolutionDpi);

            // 打印结果摘要
            System.out.println("\n========== 提取完成 ==========");
            System.out.printf("提取到的文字总长度: %d 字符\n", result.getTextContent().length());
            System.out.printf("提取到的图片数量: %d 张\n", result.getSavedImagePaths().size());

            System.out.println("\n生成的图片列表:");
            for (String path : result.getSavedImagePaths()) {
                System.out.println("  - " + path);
            }

            System.out.println("提取到文字:" + result.getTextContent());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}