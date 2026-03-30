package com.envision.bunny.module.aws.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

public class LoggingTransferListener implements TransferListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggingTransferListener.class);

    private final long totalBytes;

    public LoggingTransferListener(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        logger.info("Upload started");
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        long current = context.progressSnapshot().transferredBytes();

        int percent = (int) ((current * 100) / totalBytes);

        logger.info(
                "Upload progress: {}% ({} / {} bytes)",
                percent, current, totalBytes
        );
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        logger.info("Upload completed");
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        logger.error("Upload failed", context.exception());
    }
}
