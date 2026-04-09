package com.envision.epc.infrastructure.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * Aspose license loader (Excel only).
 */
@Slf4j
public class AsposeUtils {
    private static final String LICENSE_FILE_NAME = "Aspose.TotalforJava.lic";

    public static void loadLicense() {
        try (InputStream licenseStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(LICENSE_FILE_NAME)) {
            byte[] licenseBytes = Objects.requireNonNull(licenseStream, "Aspose license file not found").readAllBytes();
            try (ByteArrayInputStream excelLicenseStream = new ByteArrayInputStream(licenseBytes)) {
                new com.aspose.cells.License().setLicense(excelLicenseStream);
                log.info("Aspose.Cells license loaded");
            }
        } catch (Exception e) {
            log.error("Aspose.Cells license load failed", e);
        }
    }
}

