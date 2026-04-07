package com.envision.epc.infrastructure.util;

import com.aspose.pdf.*;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author wenjun.gu
 * @since 2025/8/15-14:31
 */
@Slf4j
public class AsposeUtils {
    private static final String LICENSE_FILE_NAME = "Aspose.TotalforJava.lic";

    public static void loadLicense() {
        try (InputStream licenseStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(LICENSE_FILE_NAME)) {
            byte[] licenseBytes = Objects.requireNonNull(licenseStream, "未找到 Aspose.Total 许可证文件！").readAllBytes();
            try (ByteArrayInputStream wordLicenseStream = new ByteArrayInputStream(licenseBytes);
                 ByteArrayInputStream pdfLicenseStream = new ByteArrayInputStream(licenseBytes);
                 ByteArrayInputStream excelLicenseStream = new ByteArrayInputStream(licenseBytes);) {
                new com.aspose.words.License().setLicense(wordLicenseStream);
                log.info("Aspose.Words 许可证已成功加载！");
                new com.aspose.pdf.License().setLicense(pdfLicenseStream);
                log.info("Aspose.Pdf 许可证已成功加载！");
                new com.aspose.cells.License().setLicense(excelLicenseStream);
                log.info("Aspose.Cells 许可证已成功加载！");
            }
        } catch (Exception e) {
            log.error("Aspose.Total 许可证加载失败：", e);
        }
    }

    public static Document loadWord(InputStream inputStream) {
        try {
            long l1 = System.currentTimeMillis();
            Document doc = new Document(inputStream);
            long l2 = System.currentTimeMillis();
            log.debug("加载成功:{}ms", l2 - l1);
            return doc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static com.aspose.pdf.Document loadPDF(InputStream inputStream) {
        try {
            long l1 = System.currentTimeMillis();
            com.aspose.pdf.Document doc = new com.aspose.pdf.Document(inputStream);
            long l2 = System.currentTimeMillis();
            log.debug("加载成功:{}ms", l2 - l1);
            return doc;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static void  wordToPdf(String fileName, InputStream inputStream) {
        try {
            Document document = loadWord(inputStream);
//            NodeCollection comments = document.getChildNodes(NodeType.COMMENT, true);
//            comments.clear();
            document.save(fileName, SaveFormat.PDF);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static void imageToPdf(String fileName, InputStream inputStream) {
        try {
            byte[] imageBytes = inputStream.readAllBytes();

            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            int imgWidthPx = bufferedImage.getWidth();
            int imgHeightPx = bufferedImage.getHeight();

            double dpi = 200.0;

            double imgWidthPt = (imgWidthPx / dpi) * 72.0;
            double imgHeightPt = (imgHeightPx / dpi) * 72.0;

            com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document();
            Page page = pdfDocument.getPages().add();

            double pageWidth;
            double pageHeight;
            if (imgWidthPx > imgHeightPx) {
                pageWidth = PageSize.getA4().getHeight();
                pageHeight = PageSize.getA4().getWidth();
            } else {
                pageWidth = PageSize.getA4().getWidth();
                pageHeight = PageSize.getA4().getHeight();
            }
            page.setPageSize(pageWidth, pageHeight);

            double scale = Math.min(pageWidth / imgWidthPt, pageHeight / imgHeightPt);
            double finalWidth = imgWidthPt * scale;
            double finalHeight = imgHeightPt * scale;

            Image image = new Image();
            image.setImageStream(new ByteArrayInputStream(imageBytes));
            image.setFixWidth(finalWidth);
            image.setFixHeight(finalHeight);
            image.setHorizontalAlignment(HorizontalAlignment.Center);
            image.setVerticalAlignment(VerticalAlignment.Center);

            page.getParagraphs().add(image);

            pdfDocument.save(fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
