package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.service.ParsedResultReader;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LedgerParsedDataGatewayFactory {
    private final ParsedResultReader parsedResultReader;
    private final BlobStorageRemote blobStorageRemote;
    private final ObjectMapper objectMapper;

    public LedgerParsedDataGateway create(List<FileRecord> files) {
        return new LedgerParsedDataGateway(parsedResultReader, blobStorageRemote, objectMapper, files);
    }
}
