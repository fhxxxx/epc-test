package com.envision.epc.module.taxledger.application.service;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.module.taxledger.application.dto.PrecheckSnapshotDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerParsedDataGateway;
import com.envision.epc.module.taxledger.application.ledger.LedgerParsedDataGatewayFactory;
import com.envision.epc.module.taxledger.application.ledger.LedgerWorkbookDataAssembler;
import com.envision.epc.module.taxledger.domain.LedgerRecord;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.excel.TaxLedgerExcelService;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunArtifactMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunStageMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunTaskMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TaxLedgerServiceContextBuildTest {
    private LedgerRecordMapper ledgerRecordMapper;
    private LedgerRunMapper ledgerRunMapper;
    private LedgerRunStageMapper stageMapper;
    private LedgerRunArtifactMapper artifactMapper;
    private LedgerRunTaskMapper taskMapper;
    private FileRecordMapper fileRecordMapper;
    private BlobStorageRemote blobStorageRemote;
    private FileParseOrchestratorService fileParseOrchestratorService;
    private ParsedResultReader parsedResultReader;
    private LedgerParsedDataGatewayFactory parsedDataGatewayFactory;
    private LedgerWorkbookDataAssembler workbookDataAssembler;
    private TaxLedgerExcelService excelService;
    private PermissionService permissionService;
    private TaskExecutor taskExecutor;
    private ObjectMapper objectMapper;
    private TaxLedgerService service;

    @BeforeEach
    void setUp() {
        ledgerRecordMapper = Mockito.mock(LedgerRecordMapper.class);
        ledgerRunMapper = Mockito.mock(LedgerRunMapper.class);
        stageMapper = Mockito.mock(LedgerRunStageMapper.class);
        artifactMapper = Mockito.mock(LedgerRunArtifactMapper.class);
        taskMapper = Mockito.mock(LedgerRunTaskMapper.class);
        fileRecordMapper = Mockito.mock(FileRecordMapper.class);
        blobStorageRemote = Mockito.mock(BlobStorageRemote.class);
        fileParseOrchestratorService = Mockito.mock(FileParseOrchestratorService.class);
        parsedResultReader = Mockito.mock(ParsedResultReader.class);
        parsedDataGatewayFactory = Mockito.mock(LedgerParsedDataGatewayFactory.class);
        workbookDataAssembler = Mockito.mock(LedgerWorkbookDataAssembler.class);
        excelService = Mockito.mock(TaxLedgerExcelService.class);
        permissionService = Mockito.mock(PermissionService.class);
        taskExecutor = Mockito.mock(TaskExecutor.class);
        objectMapper = new ObjectMapper();

        service = new TaxLedgerService(
                ledgerRecordMapper,
                ledgerRunMapper,
                stageMapper,
                artifactMapper,
                taskMapper,
                fileRecordMapper,
                blobStorageRemote,
                fileParseOrchestratorService,
                parsedResultReader,
                parsedDataGatewayFactory,
                workbookDataAssembler,
                excelService,
                permissionService,
                taskExecutor,
                objectMapper
        );
    }

    @Test
    void buildLedgerContext_shouldBuildWhenInputsComplete() {
        LedgerRun run = new LedgerRun();
        run.setId(101L);
        LedgerRecord ledger = new LedgerRecord();
        ledger.setCompanyCode("2320");
        ledger.setYearMonth("2026-04");

        LedgerParsedDataGateway gateway = Mockito.mock(LedgerParsedDataGateway.class);
        when(parsedDataGatewayFactory.create(any())).thenReturn(gateway);

        LedgerRunArtifact artifact = new LedgerRunArtifact();
        artifact.setBlobPath("tax-ledger/precheck.json");
        when(artifactMapper.selectOne(any())).thenReturn(artifact);
        PrecheckSnapshotDTO snapshot = new PrecheckSnapshotDTO();
        snapshot.setInputs(List.of(new PrecheckSnapshotDTO.InputItem()));
        when(parsedResultReader.readNodeOutputData(eq("tax-ledger/precheck.json"), eq(PrecheckSnapshotDTO.class)))
                .thenReturn(snapshot);

        LedgerBuildContext ctx = ReflectionTestUtils.invokeMethod(
                service,
                "buildLedgerContext",
                run,
                ledger,
                List.of(),
                List.of()
        );

        assertNotNull(ctx);
        assertEquals("2320", ctx.getCompanyCode());
        assertEquals("2026-04", ctx.getYearMonth());
        assertEquals("101", ctx.getTraceId());
        assertNotNull(ctx.getSnapshot());
        assertNotNull(ctx.getParsedDataGateway());
    }

    @Test
    void buildLedgerContext_shouldAllowSnapshotMissing() {
        LedgerRun run = new LedgerRun();
        run.setId(102L);
        LedgerRecord ledger = new LedgerRecord();
        ledger.setCompanyCode("2355");
        ledger.setYearMonth("2026-04");

        LedgerParsedDataGateway gateway = Mockito.mock(LedgerParsedDataGateway.class);
        when(parsedDataGatewayFactory.create(any())).thenReturn(gateway);
        when(artifactMapper.selectOne(any())).thenReturn(null);

        LedgerBuildContext ctx = ReflectionTestUtils.invokeMethod(
                service,
                "buildLedgerContext",
                run,
                ledger,
                List.of(),
                List.of()
        );

        assertNotNull(ctx);
        assertNull(ctx.getSnapshot());
        assertNotNull(ctx.getParsedDataGateway());
    }

    @Test
    void buildLedgerContext_shouldFailWhenCompanyCodeBlank() {
        LedgerRun run = new LedgerRun();
        run.setId(103L);
        LedgerRecord ledger = new LedgerRecord();
        ledger.setCompanyCode(" ");
        ledger.setYearMonth("2026-04");

        BizException ex = assertThrows(BizException.class, () -> ReflectionTestUtils.invokeMethod(
                service,
                "buildLedgerContext",
                run,
                ledger,
                List.of(),
                List.of()
        ));

        assertTrue(ex.getMessage().contains("companyCode"));
    }

    @Test
    void buildLedgerContext_shouldFailWhenYearMonthBlank() {
        LedgerRun run = new LedgerRun();
        run.setId(104L);
        LedgerRecord ledger = new LedgerRecord();
        ledger.setCompanyCode("2320");
        ledger.setYearMonth(" ");

        BizException ex = assertThrows(BizException.class, () -> ReflectionTestUtils.invokeMethod(
                service,
                "buildLedgerContext",
                run,
                ledger,
                List.of(),
                List.of()
        ));

        assertTrue(ex.getMessage().contains("yearMonth"));
    }
}
