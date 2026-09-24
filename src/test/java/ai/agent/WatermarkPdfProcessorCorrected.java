package ai.agent;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 为 PDF 文件的每一页添加水印。
 * 流程：读取 -> 渲染为图片 -> 加载图片副本 -> 在副本上绘制水印 -> 将带水印的图片存回新的 PDF。
 */
public class WatermarkPdfProcessorCorrected {

    /**
     * 为 PDF 添加水印并生成新文件。
     *
     * @param inputPdfPath      输入的 PDF 文件路径 (classpath:/...)
     * @param watermarkText     要添加的水印文字
     * @param outputPdfPath     输出的带水印 PDF 文件路径
     * @param dpi               渲染图片时的分辨率
     * @throws IOException
     */
    public static void addWatermarkToPdf(String inputPdfPath, String watermarkText, String outputPdfPath, float dpi) throws IOException {
        Resource resource = new DefaultResourceLoader().getResource(inputPdfPath);
        File inputFile = resource.getFile();

        System.out.println("正在加载原始 PDF: " + inputFile.getAbsolutePath());

        try (PDDocument inputDoc = Loader.loadPDF(inputFile)) {
            // 创建一个新的 PDF 文档用于输出
            try (PDDocument outputDoc = new PDDocument()) {
                PDFRenderer renderer = new PDFRenderer(inputDoc);
                int pageCount = inputDoc.getNumberOfPages();

                System.out.println("开始处理 " + pageCount + " 页...");

                for (int i = 0; i < pageCount; i++) {
                    // 1. 渲染当前页为 BufferedImage
                    PDPage originalPage = inputDoc.getPage(i);
                    float scale = dpi / 72f;
                    BufferedImage pageImage = new BufferedImage(
                            (int) (originalPage.getMediaBox().getWidth() * scale),
                            (int) (originalPage.getMediaBox().getHeight() * scale),
                            BufferedImage.TYPE_INT_RGB
                    );
                    // 注意：这里的 g2d 是临时的，仅用于渲染页面内容
                    Graphics2D tempG2d = pageImage.createGraphics();
                    renderer.renderPageToGraphics(i, tempG2d, scale);
                    tempG2d.dispose(); // 渲染完成后立即释放临时 Graphics2D

                    // 2. 创建图片副本，并在副本上绘制水印
                    BufferedImage watermarkedImage = addWatermarkToImage(pageImage, watermarkText);

                    // 3. 将带水印的 BufferedImage 转换为 PDImageXObject 并添加到新 PDF 的页面
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(watermarkedImage, "png", baos);
                    byte[] imageBytes = baos.toByteArray();
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);

                    // 从字节数组创建 BufferedImage，然后转换为 PDImageXObject
                    BufferedImage imageForPdf = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    PDImageXObject pdImage = LosslessFactory.createFromImage(outputDoc, imageForPdf);

                    PDPage newPage = new PDPage(originalPage.getMediaBox());
                    outputDoc.addPage(newPage);

                    try (PDPageContentStream contentStream = new PDPageContentStream(outputDoc, newPage)) {
                        // 计算缩放因子，确保图片填满页面
                        PDRectangle pageSize = newPage.getMediaBox();
                        float scale_x = pageSize.getWidth() / pdImage.getWidth();
                        float scale_y = pageSize.getHeight() / pdImage.getHeight();

                        contentStream.drawImage(pdImage, 0, 0, pdImage.getWidth() * scale_x, pdImage.getHeight() * scale_y);
                    }

                    System.out.println("  - 已处理并添加水印到第 " + (i + 1) + " 页。");
                }

                // 4. 保存新的 PDF
                outputDoc.save(outputPdfPath);
                System.out.println("\n带水印的 PDF 已保存至: " + outputPdfPath);
            }
        }
    }

    /**
     * 在给定的 BufferedImage 上绘制水印，并返回一个新的带水印的图片。
     */
    private static BufferedImage addWatermarkToImage(BufferedImage originalImage, String watermarkText) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 创建一个新的 BufferedImage 作为副本，类型与原图一致
        BufferedImage watermarkedImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D g2d = watermarkedImage.createGraphics();

        // 首先绘制原始图片内容
        g2d.drawImage(originalImage, 0, 0, null);

        // --- 开始绘制水印 ---
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 设置水印字体和颜色
        Font font = new Font("微软雅黑", Font.BOLD, Math.max(60, (width + height) / 30)); // 根据图片大小动态调整字体大小
        g2d.setColor(new Color(255, 0, 0, 128)); // 半透明红色 (R, G, B, Alpha)
        g2d.setFont(font);

        // 计算水印文字的位置
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(watermarkText, frc);
        double textWidth = bounds.getWidth();
        double textHeight = bounds.getHeight();

        // 将水印放置在图片中心
        int x = (int) ((width - textWidth) / 2);
        int y = (int) ((height - textHeight) / 2) + (int) (textHeight / 2); // y 是基线，调整使其垂直居中

        // 可选：旋转水印
        g2d.rotate(Math.toRadians(-20), width / 2.0, height / 2.0);
        g2d.drawString(watermarkText, x, y);
        // --- 水印绘制结束 ---

        g2d.dispose(); // 释放 Graphics2D 资源

        return watermarkedImage; // 返回带有水印的新图片
    }


    public static void main(String[] args) {
        String inputPath = "classpath:/data/RAG.pdf"; // 输入 PDF 路径
        String watermark = "柱子哥制造"; // 水印文字
        String outputPath = "watermarked_output.pdf"; // 输出 PDF 路径
        float dpi = 150; // 渲染分辨率

        try {
            addWatermarkToPdf(inputPath, watermark, outputPath, dpi);
        } catch (IOException e) {
            System.err.println("处理 PDF 时发生错误:");
            e.printStackTrace();
        }
    }
}