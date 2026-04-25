import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
  Checkbox,
  Col,
  ConfigProvider,
  DatePicker,
  Empty,
  Form,
  Input,
  Layout,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Upload,
  message,
  Typography
} from "antd";
import zhCN from "antd/locale/zh_CN";
import { DeleteOutlined, FilterFilled, InboxOutlined, PlusOutlined, ReloadOutlined, SearchOutlined, UploadOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import client from "./api/client";
import "./App.css";

const { Header, Content } = Layout;
const { Text } = Typography;

const DEFAULT_UPLOAD_FILE_CATEGORIES = [
  { value: "BS", label: "资产负债表（BS）" },
  { value: "PL", label: "利润表（PL）" },
  { value: "BS_APPENDIX_TAX_PAYABLE", label: "BS附表" },
  { value: "PL_APPENDIX_PROJECT", label: "PL附表（项目公司）" },
  { value: "STAMP_TAX", label: "印花税明细-2320、2355" },
  { value: "VAT_OUTPUT", label: "增值税销项" },
  { value: "VAT_INPUT_CERT", label: "增值税进项认证清单" },
  { value: "CUMULATIVE_PROJECT_TAX", label: "累计项目税收明细表" },
  { value: "VAT_CHANGE_APPENDIX", label: "增值税变动表附表" },
  { value: "CONTRACT_STAMP_DUTY_LEDGER", label: "合同印花税明细台账" },
  { value: "MONTHLY_SETTLEMENT_TAX", label: "睿景景程月结数据表-报税" },
  { value: "DL_INCOME", label: "收入明细" },
  { value: "DL_OUTPUT", label: "销项明细" },
  { value: "DL_INPUT", label: "进项明细" },
  { value: "DL_INCOME_TAX", label: "所得税明细" },
  { value: "DL_OTHER", label: "其他科目明细" }
];

const CONFIG_META = [
  {
    key: "company-code",
    label: "公司代码配置",
    endpoint: "/tax-ledger/config/company-code",
    fields: [
      { name: "companyCode", label: "公司代码", required: true },
      { name: "companyName", label: "公司名称", required: true },
      { name: "financeBpAd", label: "财务BP AD" },
      { name: "financeBpName", label: "财务BP姓名" },
      { name: "financeBpEmail", label: "财务BP邮箱" }
    ],
    columns: [
      { title: "公司代码", dataIndex: "companyCode" },
      { title: "公司名称", dataIndex: "companyName" },
      { title: "财务BP AD", dataIndex: "financeBpAd" },
      { title: "财务BP姓名", dataIndex: "financeBpName" },
      { title: "财务BP邮箱", dataIndex: "financeBpEmail" }
    ]
  },
  {
    key: "category",
    label: "税目配置",
    endpoint: "/tax-ledger/config/category",
    fields: [
      { name: "seqNo", label: "序号", required: true },
      { name: "companyCode", label: "公司代码" },
      { name: "taxType", label: "税种", required: true },
      { name: "taxCategory", label: "税目" },
      { name: "taxBasis", label: "计税依据" },
      { name: "collectionRatio", label: "征收比例" },
      { name: "taxRate", label: "税率" },
      { name: "accountSubject", label: "会计科目" }
    ],
    columns: [
      { title: "序号", dataIndex: "seqNo" },
      { title: "公司代码", dataIndex: "companyCode" },
      { title: "税种", dataIndex: "taxType" },
      { title: "税目", dataIndex: "taxCategory" },
      { title: "计税依据", dataIndex: "taxBasis" },
      { title: "税率", dataIndex: "taxRate" }
    ]
  },
  {
    key: "project",
    label: "项目配置",
    endpoint: "/tax-ledger/config/project",
    fields: [
      { name: "companyCode", label: "公司代码", required: true },
      { name: "taxType", label: "税种", required: true },
      { name: "taxCategory", label: "税目" },
      { name: "projectName", label: "项目名称" },
      { name: "preferentialPeriod", label: "所属优惠期", required: true }
    ],
    columns: [
      { title: "公司代码", dataIndex: "companyCode" },
      { title: "税种", dataIndex: "taxType" },
      { title: "税目", dataIndex: "taxCategory" },
      { title: "项目名称", dataIndex: "projectName" },
      { title: "所属优惠期", dataIndex: "preferentialPeriod" }
    ]
  },
  {
    key: "vat-basic",
    label: "增值税基础条目",
    endpoint: "/tax-ledger/config/vat-basic",
    fields: [
      { name: "itemSeq", label: "条目序号", required: true },
      { name: "companyCode", label: "公司代码" },
      { name: "basicItem", label: "基础条目", required: true },
      { name: "isSplit", label: "是否拆分(Y/N)", required: true },
      { name: "isDisplay", label: "是否显示(Y/N)" }
    ],
    columns: [
      { title: "条目序号", dataIndex: "itemSeq" },
      { title: "公司代码", dataIndex: "companyCode" },
      { title: "基础条目", dataIndex: "basicItem" },
      { title: "是否拆分", dataIndex: "isSplit" },
      { title: "是否显示", dataIndex: "isDisplay" }
    ]
  },
  {
    key: "vat-special",
    label: "增值税特殊条目",
    endpoint: "/tax-ledger/config/vat-special",
    fields: [
      { name: "itemSeq", label: "条目序号", required: true },
      { name: "companyCode", label: "公司代码", required: true },
      { name: "specialItem", label: "特殊条目", required: true },
      { name: "isDisplay", label: "是否显示(Y/N)", required: true }
    ],
    columns: [
      { title: "条目序号", dataIndex: "itemSeq" },
      { title: "公司代码", dataIndex: "companyCode" },
      { title: "特殊条目", dataIndex: "specialItem" },
      { title: "是否显示", dataIndex: "isDisplay" }
    ]
  }
];

function asArray(payload) {
  if (Array.isArray(payload)) return payload;
  if (payload && Array.isArray(payload.data)) return payload.data;
  if (payload && payload.data && Array.isArray(payload.data.data)) return payload.data.data;
  return [];
}

function normalizePayload(payload) {
  if (payload == null) return null;
  if (payload && Object.prototype.hasOwnProperty.call(payload, "data")) return payload.data;
  return payload;
}

function CompanyPanel({ onSelectCompany }) {
  const [rows, setRows] = useState([]);

  const load = async () => {
    try {
      const { data } = await client.get("/tax-ledger/config/company-code");
      setRows(asArray(data));
    } catch {
      setRows([]);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <Card title="公司管理" className="soft-card">
      <Table
        rowKey="id"
        dataSource={Array.isArray(rows) ? rows : []}
        columns={[
          { title: "公司代码", dataIndex: "companyCode" },
          { title: "公司名称", dataIndex: "companyName" },
          {
            title: "操作",
            render: (_, row) => (
              <Space>
                <Button onClick={() => onSelectCompany(row.companyCode)}>选择</Button>
              </Space>
            )
          }
        ]}
      />
    </Card>
  );
}

function FilePanel({ companyCode }) {
  const [rows, setRows] = useState([]);
  const [yearMonth, setYearMonth] = useState("2026-01");
  const [category, setCategory] = useState("BS");
  const [pageCurrent, setPageCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [uploadSubmitting, setUploadSubmitting] = useState(false);
  const [uploadRows, setUploadRows] = useState([]);
  const [batchCompanyCode, setBatchCompanyCode] = useState();
  const [batchFileCategory, setBatchFileCategory] = useState();
  const [pullModalOpen, setPullModalOpen] = useState(false);
  const [pullSubmitting, setPullSubmitting] = useState(false);
  const [finalLedgerUploading, setFinalLedgerUploading] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState("");
  const [previewPayload, setPreviewPayload] = useState(null);
  const [previewFileName, setPreviewFileName] = useState("");
  const [companyOptions, setCompanyOptions] = useState([]);
  const [companyLoading, setCompanyLoading] = useState(false);
  const [uploadFileCategoryOptions, setUploadFileCategoryOptions] = useState(DEFAULT_UPLOAD_FILE_CATEGORIES);
  const [uploadCategoryOptionsByCompany, setUploadCategoryOptionsByCompany] = useState({});
  const [pullForm] = Form.useForm();
  const formatFileSize = (bytes) => {
    const size = Number(bytes);
    if (!Number.isFinite(size) || size < 0) return "-";
    return `${(size / 1024).toFixed(2)} KB`;
  };
  const formatDateTime = (value) => {
    if (!value) return "-";
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.format("YYYY-MM-DD HH:mm:ss") : String(value);
  };
  const categoryLabelMap = useMemo(
    () => Object.fromEntries(uploadFileCategoryOptions.map((item) => [item.value, item.label])),
    [uploadFileCategoryOptions]
  );
  const previewTableData = useMemo(() => {
    if (!Array.isArray(previewPayload)) {
      return [];
    }
    return previewPayload.map((item, index) => ({
      ...(item && typeof item === "object" ? item : { value: item }),
      __rowKey: index
    }));
  }, [previewPayload]);
  const previewTableColumns = useMemo(() => {
    if (!previewTableData.length) {
      return [];
    }
    return Object.keys(previewTableData[0])
      .filter((key) => key !== "__rowKey")
      .map((key) => ({
        title: key,
        dataIndex: key,
        render: (value) => {
          if (value == null) return "-";
          if (typeof value === "object") return JSON.stringify(value);
          return String(value);
        }
      }));
  }, [previewTableData]);
  const previewCopyText = useMemo(() => {
    if (previewPayload == null) {
      return "";
    }
    if (typeof previewPayload === "string") {
      return previewPayload;
    }
    try {
      return JSON.stringify(previewPayload, null, 2);
    } catch {
      return String(previewPayload);
    }
  }, [previewPayload]);

  const periodOptions = useMemo(
    () => Array.from({ length: 16 }, (_, i) => {
      const period = String(i + 1).padStart(2, "0");
      return { value: period, label: period };
    }),
    []
  );

  const load = async () => {
    try {
      const params = { yearMonth };
      if (companyCode) {
        params.companyCode = companyCode;
      }
      const { data } = await client.get("/tax-ledger/files", { params });
      setRows(asArray(data));
    } catch {
      setRows([]);
    }
  };

  useEffect(() => {
    setPageCurrent(1);
    load();
  }, [companyCode, yearMonth]);

  const loadCompanyOptions = async (keyword = "") => {
    try {
      setCompanyLoading(true);
      const { data } = await client.get("/tax-ledger/config/company-code");
      const list = asArray(data);
      const normalizedKeyword = keyword.trim().toLowerCase();
      const filtered = normalizedKeyword
        ? list.filter((item) => {
            const code = String(item?.companyCode ?? "").toLowerCase();
            const name = String(item?.companyName ?? "").toLowerCase();
            return code.includes(normalizedKeyword) || name.includes(normalizedKeyword);
          })
        : list;
      setCompanyOptions(
        filtered.map((item) => ({
          value: item.companyCode,
          label: `${item.companyCode}${item.companyName ? ` - ${item.companyName}` : ""}`
        }))
      );
    } catch {
      setCompanyOptions([]);
    } finally {
      setCompanyLoading(false);
    }
  };

  const fetchUploadFileCategoryOptions = async (selectedCompanyCode) => {
    try {
      const params = { manualUpload: true };
      if (selectedCompanyCode) {
        params.companyCode = selectedCompanyCode;
      }
      const { data } = await client.get("/tax-ledger/files/categories", { params });
      const list = asArray(data);
      if (list.length > 0) {
        return list.map((item) => ({
          value: item.value,
          label: item.label
        }));
      }
      return DEFAULT_UPLOAD_FILE_CATEGORIES;
    } catch {
      return DEFAULT_UPLOAD_FILE_CATEGORIES;
    }
  };

  const loadUploadFileCategoryOptions = async () => {
    const options = await fetchUploadFileCategoryOptions();
    setUploadFileCategoryOptions(options);
  };

  const ensureCompanyUploadCategoryOptions = async (selectedCompanyCode) => {
    if (!selectedCompanyCode) {
      return uploadFileCategoryOptions;
    }
    const cached = uploadCategoryOptionsByCompany[selectedCompanyCode];
    if (cached && cached.length) {
      return cached;
    }
    const options = await fetchUploadFileCategoryOptions(selectedCompanyCode);
    setUploadCategoryOptionsByCompany((prev) => ({ ...prev, [selectedCompanyCode]: options }));
    return options;
  };

  const getUploadCategoryOptionsByCompany = (selectedCompanyCode) => {
    if (!selectedCompanyCode) {
      return uploadFileCategoryOptions;
    }
    return uploadCategoryOptionsByCompany[selectedCompanyCode] || [];
  };

  const isCategoryAllowedForCompany = (selectedCompanyCode, selectedCategory) => {
    if (!selectedCompanyCode || !selectedCategory) {
      return false;
    }
    return getUploadCategoryOptionsByCompany(selectedCompanyCode).some((item) => item.value === selectedCategory);
  };

  useEffect(() => {
    loadUploadFileCategoryOptions();
  }, []);

  const onOpenUploadModal = () => {
    setUploadModalOpen(true);
    setUploadRows([]);
    setBatchCompanyCode(companyCode || undefined);
    setBatchFileCategory(undefined);
    setUploadCategoryOptionsByCompany({});
    loadCompanyOptions();
    loadUploadFileCategoryOptions();
    if (companyCode) {
      ensureCompanyUploadCategoryOptions(companyCode);
    }
  };

  const addUploadFile = (file) => {
    const filename = file?.name || "";
    if (!filename.toLowerCase().endsWith(".xlsx")) {
      message.error("仅支持上传 .xlsx 文件");
      return false;
    }
    setUploadRows((prev) => {
      if (prev.some((row) => row.uid === file.uid)) {
        return prev;
      }
      return [
        ...prev,
        {
          uid: file.uid,
          file,
          fileName: filename,
          companyCode: companyCode || undefined,
          fileCategory: category,
          status: "待上传"
        }
      ];
    });
    return false;
  };

  const removeUploadRow = (uid) => {
    setUploadRows((prev) => prev.filter((item) => item.uid !== uid));
  };

  const updateUploadRow = (uid, patch) => {
    setUploadRows((prev) => prev.map((item) => (item.uid === uid ? { ...item, ...patch } : item)));
  };

  const submitUpload = async () => {
    if (!uploadRows.length) {
      message.warning("请先选择需要上传的文件");
      return;
    }
    const invalidRow = uploadRows.find((row) => !row.companyCode || !row.fileCategory);
    if (invalidRow) {
      message.warning("请为每个文件选择公司代码和文件类别");
      return;
    }
    const companyCodeSet = Array.from(new Set(uploadRows.map((row) => row.companyCode).filter(Boolean)));
    await Promise.all(companyCodeSet.map((code) => ensureCompanyUploadCategoryOptions(code)));
    const mismatchRow = uploadRows.find((row) => !isCategoryAllowedForCompany(row.companyCode, row.fileCategory));
    if (mismatchRow) {
      message.warning("存在公司与文件类型不匹配的记录，请调整后再上传");
      return;
    }
    try {
      setUploadSubmitting(true);
      let successCount = 0;
      for (const row of uploadRows) {
        updateUploadRow(row.uid, { status: "上传中" });
        try {
          const formData = new FormData();
          formData.append("file", row.file);
          formData.append("companyCode", row.companyCode);
          formData.append("yearMonth", yearMonth);
          formData.append("fileCategory", row.fileCategory);
          await client.post("/tax-ledger/files/upload", formData, {
            headers: { "Content-Type": "multipart/form-data" }
          });
          successCount += 1;
          updateUploadRow(row.uid, { status: "成功" });
        } catch {
          updateUploadRow(row.uid, { status: "失败" });
        }
      }
      if (successCount === uploadRows.length) {
        message.success(`上传成功，共 ${successCount} 个文件`);
        setUploadModalOpen(false);
        setUploadRows([]);
      } else if (successCount > 0) {
        message.warning(`部分成功：${successCount}/${uploadRows.length}`);
      } else {
        message.error("上传失败，请检查后重试");
      }
      if (successCount > 0) {
        load();
      }
    } finally {
      setUploadSubmitting(false);
    }
  };

  const applyBatchSettings = async () => {
    if (!uploadRows.length) {
      message.warning("请先选择需要上传的文件");
      return;
    }
    if (!batchCompanyCode && !batchFileCategory) {
      message.warning("请先选择要批量应用的公司代码或类型");
      return;
    }
    const targetCodes = new Set(
      uploadRows
        .map((row) => (batchCompanyCode ? batchCompanyCode : row.companyCode))
        .filter(Boolean)
    );
    await Promise.all(Array.from(targetCodes).map((code) => ensureCompanyUploadCategoryOptions(code)));

    let skippedCount = 0;
    setUploadRows((prev) =>
      prev.map((row) => {
        const nextCompanyCode = batchCompanyCode || row.companyCode;
        let nextCategory = row.fileCategory;
        if (batchFileCategory) {
          const allowed = isCategoryAllowedForCompany(nextCompanyCode, batchFileCategory);
          if (allowed) {
            nextCategory = batchFileCategory;
          } else {
            skippedCount += 1;
          }
        }
        return {
          ...row,
          ...(batchCompanyCode ? { companyCode: batchCompanyCode } : {}),
          fileCategory: nextCategory
        };
      })
    );
    if (batchFileCategory && skippedCount > 0) {
      message.warning(`批量应用完成，${skippedCount} 条因公司与类型不匹配未变更类型`);
    }
  };

  const handleUploadRowCompanyChange = async (uid, selectedCompanyCode) => {
    const options = await ensureCompanyUploadCategoryOptions(selectedCompanyCode);
    const row = uploadRows.find((item) => item.uid === uid);
    const currentCategory = row?.fileCategory;
    const keepCategory = options.some((item) => item.value === currentCategory);
    setUploadRows((prev) =>
      prev.map((item) =>
        item.uid === uid
          ? {
              ...item,
              companyCode: selectedCompanyCode,
              fileCategory: keepCategory ? item.fileCategory : undefined
            }
          : item
      )
    );
    if (currentCategory && !keepCategory) {
      message.warning("该公司不支持当前文件类型，已清空，请重新选择");
    }
  };

  const handleBatchCompanyChange = async (selectedCompanyCode) => {
    setBatchCompanyCode(selectedCompanyCode);
    if (!selectedCompanyCode) {
      return;
    }
    const options = await ensureCompanyUploadCategoryOptions(selectedCompanyCode);
    if (!options.some((item) => item.value === batchFileCategory)) {
      setBatchFileCategory(undefined);
    }
  };

  const onOpenPullModal = () => {
    const [year, month] = yearMonth.split("-");
    const period = String(Number(month || "1")).padStart(2, "0");
    pullForm.setFieldsValue({
      companyCodes: companyCode ? [companyCode] : [],
      fiscalYear: dayjs(`${year}-01-01`),
      periodStartMonth: period,
      periodEndMonth: period
    });
    setPullModalOpen(true);
    loadCompanyOptions();
  };

  const onSubmitPullDataLake = async () => {
    try {
      const values = await pullForm.validateFields();
      const startMonth = Number(values.periodStartMonth);
      const endMonth = Number(values.periodEndMonth);
      if (startMonth > endMonth) {
        message.warning("会计期间开始不能大于结束");
        return;
      }
      setPullSubmitting(true);
      const fiscalYear = values.fiscalYear.year();
      const requestMonth = String(Math.min(endMonth, 12)).padStart(2, "0");
      const requestYearMonth = `${fiscalYear}-${requestMonth}`;
      // 规则：四位年份 + 0 + 两位月份，例如 2025 + 0 + 02 = 2025002
      const makeFiscalPeriod = (monthValue) => `${fiscalYear}0${String(monthValue).padStart(2, "0")}`;
      const { data } = await client.post("/tax-ledger/datalake/pull", {
        companyCodeList: values.companyCodes,
        yearMonth: requestYearMonth,
        fiscalYearPeriodStart: makeFiscalPeriod(startMonth),
        fiscalYearPeriodEnd: makeFiscalPeriod(endMonth)
      });
      const successCount = Number(data?.successCount ?? 0);
      const failedCount = Number(data?.failCount ?? 0);
      const errorList = Array.isArray(data?.errors) ? data.errors : [];
      if (failedCount === 0) {
        message.success(`数据湖拉取完成，共 ${successCount} 家公司`);
      } else if (successCount > 0) {
        message.warning(`部分完成：成功 ${successCount} 家，失败 ${failedCount} 家`);
      } else {
        message.error("数据湖拉取失败，请检查后重试");
      }
      if (errorList.length > 0) {
        message.warning(errorList.slice(0, 3).join("；"));
      }
      if (successCount > 0) {
        setYearMonth(requestYearMonth);
        load();
      }
      if (failedCount === 0) {
        setPullModalOpen(false);
      }
    } catch {
      // 校验失败或接口异常提示已处理
    } finally {
      setPullSubmitting(false);
    }
  };

  const uploadFinalLedgerFile = async (file) => {
    if (!companyCode) {
      message.warning("请先选择公司");
      return;
    }
    Modal.confirm({
      title: "确认上传为最终台账？",
      content: `该操作会覆盖公司 ${companyCode} 在 ${yearMonth} 的当前最终台账，并用于后续台账生成。`,
      okText: "确认覆盖",
      okButtonProps: { danger: true },
      cancelText: "取消",
      async onOk() {
        const fileName = file?.name || "";
        if (!fileName.toLowerCase().endsWith(".xlsx")) {
          message.error("仅支持上传 .xlsx 文件");
          return;
        }
        const formData = new FormData();
        formData.append("file", file);
        formData.append("companyCode", companyCode);
        formData.append("yearMonth", yearMonth);
        try {
          setFinalLedgerUploading(true);
          await client.post("/tax-ledger/ledger/final-ledger/upload", formData, {
            headers: { "Content-Type": "multipart/form-data" }
          });
          message.success("最终台账已上传并生效");
          await load();
        } finally {
          setFinalLedgerUploading(false);
        }
      }
    });
  };

  const closePreview = () => {
    setPreviewOpen(false);
    setPreviewLoading(false);
    setPreviewError("");
    setPreviewPayload(null);
    setPreviewFileName("");
  };

  const parsePreviewPayload = (payload) => {
    const normalized = normalizePayload(payload);
    if (typeof normalized !== "string") {
      return normalized;
    }
    const text = normalized.trim();
    if (!text) {
      return null;
    }
    if (text.startsWith("{") || text.startsWith("[")) {
      try {
        return JSON.parse(text);
      } catch {
        return normalized;
      }
    }
    return normalized;
  };

  const openPreview = async (row) => {
    setPreviewOpen(true);
    setPreviewFileName(row?.fileName || "");
    setPreviewLoading(true);
    setPreviewError("");
    setPreviewPayload(null);
    try {
      const { data } = await client.get(`/tax-ledger/files/${row.id}/parsed-result`);
      setPreviewPayload(parsePreviewPayload(data));
    } catch (e) {
      setPreviewError(e?.response?.data?.msg || e?.message || "加载解析结果失败");
    } finally {
      setPreviewLoading(false);
    }
  };

  const copyPreviewResult = async () => {
    if (!previewCopyText) {
      message.warning("暂无可复制的解析结果");
      return;
    }
    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(previewCopyText);
      } else {
        const textarea = document.createElement("textarea");
        textarea.value = previewCopyText;
        textarea.style.position = "fixed";
        textarea.style.opacity = "0";
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand("copy");
        document.body.removeChild(textarea);
      }
      message.success("解析结果已复制");
    } catch {
      message.error("复制失败，请重试");
    }
  };

  return (
    <Card
      title="文件管理"
      className="soft-card"
      extra={(
        <Space>
          <DatePicker
            picker="month"
            allowClear={false}
            format="YYYY-MM"
            value={dayjs(yearMonth, "YYYY-MM")}
            onChange={(_, dateString) => setYearMonth(dateString)}
          />
          <Button onClick={load}>刷新</Button>
        </Space>
      )}
    >
      <Space style={{ marginBottom: 16 }}>
        <Upload
          openFileDialogOnClick={false}
        >
          <Button icon={<UploadOutlined />} onClick={onOpenUploadModal}>上传文件</Button>
        </Upload>
        <Upload
          showUploadList={false}
          accept=".xlsx"
          maxCount={1}
          beforeUpload={(file) => {
            uploadFinalLedgerFile(file);
            return Upload.LIST_IGNORE;
          }}
        >
          <Button danger loading={finalLedgerUploading} disabled={!companyCode}>
            上传最终台账
          </Button>
        </Upload>
        <Button onClick={onOpenPullModal}>拉取数据湖</Button>
      </Space>

      <Modal
        title="上传结果excel"
        open={uploadModalOpen}
        destroyOnHidden
        width={980}
        onCancel={() => {
          setUploadModalOpen(false);
          setUploadRows([]);
        }}
        onOk={submitUpload}
        okText="确定"
        cancelText="取消"
        confirmLoading={uploadSubmitting}
      >
        <Upload.Dragger
          multiple
          accept=".xlsx"
          showUploadList={false}
          beforeUpload={addUploadFile}
          className="upload-dragger"
        >
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽文件上传</p>
          <p className="ant-upload-hint">支持 .xlsx 格式文件</p>
        </Upload.Dragger>

        <div className="upload-batch-bar">
          <Text className="upload-batch-label">批量设置</Text>
          <Select
            value={batchCompanyCode}
            showSearch
            placeholder="请选择公司代码"
            style={{ width: 280 }}
            loading={companyLoading}
            options={companyOptions}
            filterOption={(input, option) => String(option?.label ?? "").toLowerCase().includes(input.toLowerCase())}
            onSearch={loadCompanyOptions}
            onChange={handleBatchCompanyChange}
          />
          <Select
            value={batchFileCategory}
            placeholder="请选择类型"
            style={{ width: 240 }}
            options={getUploadCategoryOptionsByCompany(batchCompanyCode)}
            onChange={setBatchFileCategory}
          />
          <Button
            onClick={applyBatchSettings}
            disabled={!uploadRows.length || (!batchCompanyCode && !batchFileCategory)}
          >
            批量应用
          </Button>
        </div>

        <Table
          style={{ marginTop: 16 }}
          rowKey="uid"
          pagination={false}
          locale={{ emptyText: "暂无待上传文件" }}
          dataSource={uploadRows}
          columns={[
            { title: "文件名", dataIndex: "fileName" },
            {
              title: "公司代码",
              dataIndex: "companyCode",
              render: (_, row) => (
                <Select
                  value={row.companyCode}
                  showSearch
                  placeholder="请选择公司代码"
                  style={{ width: "100%" }}
                  loading={companyLoading}
                  options={companyOptions}
                  filterOption={(input, option) => String(option?.label ?? "").toLowerCase().includes(input.toLowerCase())}
                  onSearch={loadCompanyOptions}
                  onChange={(value) => handleUploadRowCompanyChange(row.uid, value)}
                />
              )
            },
            {
              title: "类型",
              dataIndex: "fileCategory",
              render: (_, row) => (
                <Select
                  value={row.fileCategory}
                  placeholder="请选择类型"
                  style={{ width: "100%" }}
                  options={getUploadCategoryOptionsByCompany(row.companyCode)}
                  onOpenChange={(open) => {
                    if (open && row.companyCode) {
                      ensureCompanyUploadCategoryOptions(row.companyCode);
                    }
                  }}
                  onChange={(value) => updateUploadRow(row.uid, { fileCategory: value })}
                />
              )
            },
            { title: "进度", dataIndex: "status" },
            {
              title: "操作",
              width: 80,
              render: (_, row) => (
                <Button
                  danger
                  type="text"
                  icon={<DeleteOutlined />}
                  onClick={() => removeUploadRow(row.uid)}
                />
              )
            }
          ]}
        />
      </Modal>

      <Modal
        title="数据湖拉取"
        open={pullModalOpen}
        destroyOnHidden
        onCancel={() => setPullModalOpen(false)}
        onOk={onSubmitPullDataLake}
        okText="开始拉取"
        cancelText="取消"
        confirmLoading={pullSubmitting}
      >
        <Form form={pullForm} layout="vertical">
          <Form.Item
            name="companyCodes"
            label="公司代码"
            rules={[{ required: true, message: "请至少选择一个公司代码" }]}
          >
            <Select
              mode="multiple"
              showSearch
              allowClear
              placeholder="请选择公司代码"
              loading={companyLoading}
              options={companyOptions}
              filterOption={false}
              onSearch={loadCompanyOptions}
              onOpenChange={(open) => {
                if (open) {
                  loadCompanyOptions();
                }
              }}
            />
          </Form.Item>
          <Form.Item
            name="fiscalYear"
            label="会计年度"
            rules={[{ required: true, message: "请选择会计年度" }]}
          >
            <DatePicker picker="year" style={{ width: "100%" }} placeholder="请选择年度" />
          </Form.Item>
          <Space style={{ width: "100%" }} align="start">
            <Form.Item
              name="periodStartMonth"
              label="会计期间开始"
              rules={[{ required: true, message: "请选择会计期间开始" }]}
              style={{ minWidth: 180 }}
            >
              <Select options={periodOptions} placeholder="请选择" />
            </Form.Item>
            <Form.Item
              name="periodEndMonth"
              label="会计期间结束"
              rules={[{ required: true, message: "请选择会计期间结束" }]}
              style={{ minWidth: 180 }}
            >
              <Select options={periodOptions} placeholder="请选择" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      <Table
        rowKey="id"
        dataSource={Array.isArray(rows) ? rows : []}
        columns={[
          {
            title: "类别",
            dataIndex: "fileCategory",
            render: (v, row) => <Tag>{row?.fileCategoryName || categoryLabelMap[v] || v}</Tag>,
            filterDropdown: ({ setSelectedKeys, selectedKeys, confirm, clearFilters }) => {
              const checkedValues = Array.isArray(selectedKeys) ? selectedKeys : [];
              const options = uploadFileCategoryOptions.map((item) => ({ value: item.value, label: item.label }));

              return (
                <div style={{ width: 220, padding: 10 }}>
                  <div style={{ marginBottom: 8, fontWeight: 600 }}>类型</div>
                  <Checkbox.Group
                    value={checkedValues}
                    style={{ width: "100%", maxHeight: 220, overflowY: "auto" }}
                    onChange={(vals) => setSelectedKeys(Array.isArray(vals) ? vals : [])}
                  >
                    <Space direction="vertical" size={6} style={{ width: "100%" }}>
                      {options.map((item) => (
                        <Checkbox key={item.value} value={item.value} style={{ width: "100%" }}>
                          {item.label}
                        </Checkbox>
                      ))}
                    </Space>
                  </Checkbox.Group>
                  <div style={{ display: "flex", justifyContent: "space-between", marginTop: 12 }}>
                    <Button
                      size="small"
                      onClick={() => {
                        clearFilters?.();
                        setSelectedKeys([]);
                      }}
                    >
                      重置
                    </Button>
                    <Button type="primary" size="small" onClick={() => confirm()}>
                      确定
                    </Button>
                  </div>
                </div>
              );
            },
            filterIcon: (filtered) => <FilterFilled style={{ color: filtered ? "#1677ff" : undefined }} />,
            onFilter: (value, record) => String(record?.fileCategory ?? "") === String(value ?? "")
          },
          { title: "公司代码", dataIndex: "companyCode", render: (v) => v || "-" },
          { title: "文件名", dataIndex: "fileName" },
          { title: "文件大小", dataIndex: "fileSize", render: (v) => formatFileSize(v) },
          { title: "创建人", dataIndex: "createByName", render: (v, row) => v || row?.createBy || "-" },
          { title: "创建时间", dataIndex: "createTime", render: (v) => formatDateTime(v) },
          {
            title: "操作",
            render: (_, row) => (
              <Space>
                <Button onClick={() => openPreview(row)}>预览</Button>
                <Button onClick={() => window.open(`/tax-ledger/files/${row.id}/download`, "_blank")}>下载</Button>
                <Popconfirm
                  title="确认删除该文件？"
                  description="删除后不可恢复"
                  okText="确认"
                  cancelText="取消"
                  onConfirm={async () => {
                    try {
                      await client.delete(`/tax-ledger/files/${row.id}`);
                      message.success("删除成功");
                      load();
                    } catch {
                      // 失败提示已由全局拦截器处理
                    }
                  }}
                >
                  <Button danger>删除</Button>
                </Popconfirm>
              </Space>
            )
          }
        ]}
        pagination={{
          current: pageCurrent,
          pageSize,
          total: Array.isArray(rows) ? rows.length : 0,
          showSizeChanger: true,
          pageSizeOptions: ["10", "20", "50", "100"],
          showTotal: (total) => `共 ${total} 条`,
          locale: { items_per_page: "/页" },
          onChange: (current, size) => {
            setPageCurrent(current);
            setPageSize(size);
          },
          position: ["bottomRight"]
        }}
      />
      <Modal
        title={`解析结果预览${previewFileName ? ` - ${previewFileName}` : ""}`}
        open={previewOpen}
        onCancel={closePreview}
        footer={null}
        width={1000}
        destroyOnHidden
      >
        <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 8 }}>
          <Button size="small" onClick={copyPreviewResult} disabled={previewLoading || !previewCopyText}>
            复制结果
          </Button>
        </div>
        {previewLoading ? (
          <div style={{ padding: "32px 0", textAlign: "center" }}>
            <Spin />
          </div>
        ) : previewError ? (
          <div style={{ color: "#ff4d4f", whiteSpace: "pre-wrap" }}>{previewError}</div>
        ) : Array.isArray(previewPayload) ? (
          previewPayload.length > 0 ? (
            <Table
              rowKey="__rowKey"
              size="small"
              dataSource={previewTableData}
              columns={previewTableColumns}
              pagination={{ pageSize: 20, showSizeChanger: false }}
              scroll={{ x: "max-content" }}
            />
          ) : (
            <Empty description="暂无解析结果" />
          )
        ) : previewPayload == null ? (
          <Empty description="暂无解析结果" />
        ) : (
          <pre
            style={{
              margin: 0,
              maxHeight: 560,
              overflow: "auto",
              background: "#fafafa",
              border: "1px solid #f0f0f0",
              borderRadius: 8,
              padding: 12,
              fontSize: 12
            }}
          >
            {typeof previewPayload === "string"
              ? previewPayload
              : JSON.stringify(previewPayload, null, 2)}
          </pre>
        )}
      </Modal>
    </Card>
  );
}

function LedgerPanel({ companyCode }) {
  const [yearMonth, setYearMonth] = useState("2026-01");
  const [rows, setRows] = useState([]);
  const [pageCurrent, setPageCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [detailOpen, setDetailOpen] = useState(false);
  const [jobDetail, setJobDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [publishingJobId, setPublishingJobId] = useState(null);

  const runningStatuses = ["VALIDATING", "GENERATING"];
  const failedStatuses = ["VALIDATION_FAILED", "FAILED"];
  const statusTextMap = {
    PENDING: "排队中",
    VALIDATING: "校验中",
    VALIDATION_FAILED: "校验失败",
    GENERATING: "生成中",
    SUCCESS: "成功",
    FAILED: "失败"
  };
  const statusColorMap = {
    PENDING: "default",
    VALIDATING: "processing",
    VALIDATION_FAILED: "error",
    GENERATING: "processing",
    SUCCESS: "success",
    FAILED: "error"
  };
  const formatDateTime = (value) => {
    if (!value) return "-";
    const parsed = dayjs(value);
    return parsed.isValid() ? parsed.format("YYYY-MM-DD HH:mm:ss") : String(value);
  };

  const load = async () => {
    if (!companyCode) {
      setRows([]);
      setTotal(0);
      return;
    }
    try {
      const { data } = await client.get("/tax-ledger/ledger/jobs", {
        params: { companyCode, yearMonth, page: pageCurrent, size: pageSize }
      });
      const items = Array.isArray(data?.items) ? data.items : [];
      setRows(items);
      setTotal(Number(data?.total ?? items.length));
    } catch {
      setRows([]);
      setTotal(0);
    }
  };

  useEffect(() => {
    load();
  }, [companyCode, yearMonth, pageCurrent, pageSize]);

  useEffect(() => {
    if (!rows.some((item) => runningStatuses.includes(item?.status))) {
      return undefined;
    }
    const timer = setInterval(() => {
      load();
    }, 5000);
    return () => clearInterval(timer);
  }, [rows, companyCode, yearMonth, pageCurrent, pageSize]);

  const openJobDetail = async (jobId) => {
    try {
      setDetailLoading(true);
      const { data } = await client.get(`/tax-ledger/ledger/jobs/${jobId}`);
      setJobDetail(data);
      setDetailOpen(true);
    } catch {
      setJobDetail(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const createJob = async () => {
    if (!companyCode) {
      message.warning("请先选择公司");
      return;
    }
    try {
      setCreating(true);
      await client.post("/tax-ledger/ledger/jobs", { companyCode, yearMonth });
      message.success("已创建台账任务");
      setPageCurrent(1);
      await load();
    } finally {
      setCreating(false);
    }
  };

  const retryJob = async (jobId) => {
    try {
      await client.post(`/tax-ledger/ledger/jobs/${jobId}/retry`);
      message.success("已发起重试");
      setPageCurrent(1);
      await load();
    } catch {
      // 失败提示已由全局拦截器处理
    }
  };

  const publishFinalLedger = async (jobId) => {
    try {
      setPublishingJobId(jobId);
      await client.post(`/tax-ledger/ledger/jobs/${jobId}/publish-final-ledger`);
      message.success("已设为最终台账，将用于后续台账生成");
      await load();
    } catch {
      // 失败提示已由全局拦截器处理
    } finally {
      setPublishingJobId(null);
    }
  };

  const hasRunningJob = rows.some((item) => runningStatuses.includes(item?.status));

  return (
    <Card
      title="台账任务"
      className="soft-card"
      extra={(
        <Space>
          <DatePicker
            picker="month"
            allowClear={false}
            format="YYYY-MM"
            value={dayjs(yearMonth, "YYYY-MM")}
            onChange={(_, dateString) => {
              setYearMonth(dateString);
              setPageCurrent(1);
            }}
          />
          <Button onClick={load}>刷新</Button>
        </Space>
      )}
    >
      <Space style={{ marginBottom: 16 }}>
        <Button
          type="primary"
          onClick={createJob}
          loading={creating}
          disabled={!companyCode || hasRunningJob}
        >
          生成台账
        </Button>
      </Space>

      <Table
        rowKey="id"
        dataSource={Array.isArray(rows) ? rows : []}
        columns={[
          { title: "任务ID", dataIndex: "id", width: 120 },
          { title: "公司代码", dataIndex: "companyCode", width: 120 },
          { title: "月份", dataIndex: "yearMonth", width: 120 },
          {
            title: "状态",
            dataIndex: "status",
            width: 120,
            render: (v) => <Tag color={statusColorMap[v] || "default"}>{statusTextMap[v] || v}</Tag>
          },
          { title: "发起人", dataIndex: "createByName", render: (v, row) => v || row?.createBy || "-" },
          { title: "开始时间", dataIndex: "startedAt", render: (v) => formatDateTime(v) },
          { title: "结束时间", dataIndex: "endedAt", render: (v) => formatDateTime(v) },
          {
            title: "操作",
            width: 220,
            render: (_, row) => (
              <Space>
                <Button onClick={() => openJobDetail(row.id)}>查看</Button>
                <Button
                  disabled={row.status !== "SUCCESS"}
                  onClick={() => window.open(`/tax-ledger/ledger/jobs/${row.id}/download`, "_blank")}
                >
                  下载
                </Button>
                <Popconfirm
                  title="确认设为最终台账？"
                  description="将覆盖当前公司+月份的最终台账，并用于后续台账生成。"
                  okText="确认覆盖"
                  okButtonProps={{ danger: true }}
                  cancelText="取消"
                  onConfirm={() => publishFinalLedger(row.id)}
                >
                  <Button
                    disabled={row.status !== "SUCCESS" || publishingJobId === row.id}
                    loading={publishingJobId === row.id}
                  >
                    设为最终台账
                  </Button>
                </Popconfirm>
                <Button
                  disabled={!failedStatuses.includes(row.status)}
                  onClick={() => retryJob(row.id)}
                >
                  重试
                </Button>
              </Space>
            )
          }
        ]}
        pagination={{
          current: pageCurrent,
          pageSize,
          total,
          showSizeChanger: true,
          pageSizeOptions: ["10", "20", "50", "100"],
          showTotal: (count) => `共 ${count} 条`,
          locale: { items_per_page: "/页" },
          onChange: (current, size) => {
            setPageCurrent(current);
            setPageSize(size);
          },
          position: ["bottomRight"]
        }}
      />
      <Modal
        title="任务详情"
        open={detailOpen}
        width={720}
        footer={null}
        onCancel={() => setDetailOpen(false)}
      >
        {detailLoading ? (
          <Text>加载中...</Text>
        ) : (
          <Space direction="vertical" style={{ width: "100%" }}>
            <Space>
              <Text>任务ID: {jobDetail?.id ?? "-"}</Text>
              <Tag color={statusColorMap[jobDetail?.status] || "default"}>
                {statusTextMap[jobDetail?.status] || jobDetail?.status || "-"}
              </Tag>
            </Space>
            <Text>公司代码: {jobDetail?.companyCode || "-"}</Text>
            <Text>月份: {jobDetail?.yearMonth || "-"}</Text>
            <Text>RunId: {jobDetail?.runId || "-"}</Text>
            <Text>开始时间: {formatDateTime(jobDetail?.startedAt)}</Text>
            <Text>结束时间: {formatDateTime(jobDetail?.endedAt)}</Text>
            <Text type={jobDetail?.errorMsg ? "danger" : undefined}>
              {jobDetail?.errorMsg || "-"}
            </Text>
          </Space>
        )}
      </Modal>
    </Card>
  );
}

function PermissionPanel({ companyCode }) {
  const [rows, setRows] = useState([]);
  const [pageCurrent, setPageCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [userOptions, setUserOptions] = useState([]);
  const [userLoading, setUserLoading] = useState(false);
  const [userPageNum, setUserPageNum] = useState(1);
  const [userHasMore, setUserHasMore] = useState(true);
  const [userKeyword, setUserKeyword] = useState("");
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [userPool, setUserPool] = useState({});
  const [companyOptions, setCompanyOptions] = useState([]);
  const [companyLoading, setCompanyLoading] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    try {
      const params = companyCode ? { companyCode } : undefined;
      const { data } = await client.get("/tax-ledger/permissions", { params });
      setRows(asArray(data));
    } catch {
      setRows([]);
    }
  };

  const loadUsers = async ({ keyword = "", pageNum = 1, append = false } = {}) => {
    if (userLoading) {
      return;
    }
    try {
      setUserLoading(true);
      const { data } = await client.get("/user/list", {
        params: { username: keyword || "", pageNum, pageSize: 20 }
      });
      const items = asArray(data?.items ?? data);
      const hasTotalCount = data && Object.prototype.hasOwnProperty.call(data, "totalCount");
      const totalCount = Number(data?.totalCount ?? 0);
      const merged = append ? [...userOptions, ...items] : items;
      const uniqMap = new Map();
      merged.forEach((item) => {
        if (item?.userCode) {
          uniqMap.set(item.userCode, item);
        }
      });
      const uniqList = Array.from(uniqMap.values());
      setUserOptions(uniqList);
      setUserPageNum(pageNum);
      setUserKeyword(keyword);
      setUserHasMore(hasTotalCount ? uniqList.length < totalCount : items.length >= 20);
      setUserPool((prev) => {
        const next = { ...prev };
        items.forEach((item) => {
          if (item?.userCode) {
            next[item.userCode] = item;
          }
        });
        return next;
      });
    } catch {
      if (!append) {
        setUserOptions([]);
      }
      setUserHasMore(false);
    } finally {
      setUserLoading(false);
    }
  };

  const searchUser = async (keyword) => {
    await loadUsers({ keyword, pageNum: 1, append: false });
  };

  const loadMoreUsers = async () => {
    if (!userHasMore || userLoading) {
      return;
    }
    await loadUsers({ keyword: userKeyword, pageNum: userPageNum + 1, append: true });
  };

  const onOpenAddModal = () => {
    setModalOpen(true);
    setSelectedUsers([]);
    form.resetFields();
    if (companyCode) {
      form.setFieldValue("companyCode", companyCode);
    }
    setUserOptions([]);
    setUserPool({});
    setUserPageNum(1);
    setUserHasMore(true);
    setUserKeyword("");
    loadUsers({ keyword: "", pageNum: 1, append: false });
    loadCompanyOptions();
  };

  const loadCompanyOptions = async (keyword = "") => {
    try {
      setCompanyLoading(true);
      const { data } = await client.get("/tax-ledger/config/company-code");
      const rows = asArray(data);
      const filteredRows = keyword
        ? rows.filter((item) => String(item?.companyCode ?? "").toLowerCase().includes(keyword.toLowerCase()))
        : rows;
      setCompanyOptions(
        filteredRows.map((item) => ({
          value: item.companyCode,
          label: `${item.companyCode}${item.companyName ? ` - ${item.companyName}` : ""}`
        }))
      );
    } catch {
      setCompanyOptions([]);
    } finally {
      setCompanyLoading(false);
    }
  };

  useEffect(() => {
    load();
    setPageCurrent(1);
  }, [companyCode]);

  return (
    <Card
      title="权限管理"
      className="soft-card"
      extra={(
        <Space>
          <Button onClick={load}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={onOpenAddModal}>添加权限</Button>
        </Space>
      )}
    >
      <Modal
        title="添加权限"
        open={modalOpen}
        destroyOnHidden
        forceRender
        width={820}
        onCancel={() => {
          setModalOpen(false);
          setSelectedUsers([]);
          form.resetFields();
        }}
        footer={null}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={async (values) => {
            try {
              setSubmitting(true);
              if (!selectedUsers.length) {
                message.warning("请至少选择一名员工");
                return;
              }
              const companyCodeValue = values.companyCode;
              if (!companyCodeValue) {
                message.warning("请选择公司代码");
                return;
              }
              const payload = selectedUsers.map((item) => ({
                userId: item.userCode,
                userName: item.username,
                companyCode: companyCodeValue
              }));
              await client.post("/tax-ledger/permissions/batch", payload);
              message.success(`批量授权成功，共 ${payload.length} 人`);
              setModalOpen(false);
              setSelectedUsers([]);
              form.resetFields();
              load();
            } catch {
              // 失败提示已由全局拦截器处理
            } finally {
              setSubmitting(false);
            }
          }}
        >
          <Row gutter={12}>
            <Col span={14}>
              <Form.Item
                name="employeePick"
                label="选择员工"
                rules={[{ required: true, message: "请输入姓名/工号/域账号检索员工" }]}
              >
                <Select
                  mode="multiple"
                  showSearch
                  allowClear
                  placeholder="输入姓名/工号/域账号搜索员工"
                  filterOption={false}
                  loading={userLoading}
                  onSearch={searchUser}
                  onPopupScroll={(event) => {
                    const target = event?.target;
                    if (!target) return;
                    const nearBottom = target.scrollTop + target.clientHeight >= target.scrollHeight - 20;
                    if (nearBottom) {
                      loadMoreUsers();
                    }
                  }}
                  dropdownRender={(menu) => (
                    <div>
                      {menu}
                      <div style={{ padding: "6px 12px", textAlign: "center", color: "#8c8c8c", fontSize: 12 }}>
                        {userLoading ? "加载中..." : userHasMore ? "下滑加载更多" : "没有更多数据了"}
                      </div>
                    </div>
                  )}
                  onOpenChange={(open) => {
                    if (open && !userOptions.length) {
                      loadUsers({ keyword: "", pageNum: 1, append: false });
                    }
                  }}
                  onChange={(vals) => {
                    const users = (vals || [])
                      .map((val) => userPool[val])
                      .filter(Boolean);
                    setSelectedUsers(users);
                  }}
                  options={userOptions.map((item) => ({
                    value: item.userCode,
                    label: `${item.username}（${item.userCode} / ${item.account}）`
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item
                name="companyCode"
                label="公司代码"
                rules={[{ required: true, message: "请选择公司代码" }]}
              >
                <Select
                  showSearch
                  allowClear
                  placeholder="请选择公司代码"
                  loading={companyLoading}
                  options={companyOptions}
                  filterOption={false}
                  onSearch={loadCompanyOptions}
                  onOpenChange={(open) => {
                    if (open) loadCompanyOptions();
                  }}
                />
              </Form.Item>
            </Col>
          </Row>
          <Text type="secondary">支持多选员工后一次性授权；若存在重复记录，系统将按同员工同公司覆盖授权。</Text>
          <div style={{ marginTop: 16 }}>
            <Space>
              <Button type="primary" htmlType="submit" loading={submitting}>确认</Button>
              <Button
                onClick={() => {
                  setModalOpen(false);
                  setSelectedUsers([]);
                  form.resetFields();
                }}
              >
                取消
              </Button>
            </Space>
          </div>
        </Form>
      </Modal>

      <Table
        style={{ marginTop: 16 }}
        rowKey="id"
        dataSource={Array.isArray(rows) ? rows : []}
        columns={[
          { title: "用户编码", dataIndex: "userId" },
          { title: "姓名", dataIndex: "userName" },
          { title: "公司代码", dataIndex: "companyCode" },
          {
            title: "操作",
            render: (_, row) => (
              <Popconfirm
                overlayClassName="pretty-popconfirm"
                title="确认撤销该权限？"
                description={`将撤销用户 ${row.userId} 的权限绑定。`}
                okText="确认撤销"
                cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={async () => {
                  try {
                    await client.delete("/tax-ledger/permissions", {
                      params: { userId: row.userId, companyCode: row.companyCode }
                    });
                    message.success("撤销成功");
                    load();
                  } catch {
                    // 失败提示已由全局拦截器处理
                  }
                }}
              >
                <Button danger>撤销</Button>
              </Popconfirm>
            )
          }
        ]}
        pagination={{
          current: pageCurrent,
          pageSize,
          total: rows.length,
          showSizeChanger: true,
          pageSizeOptions: ["10", "20", "50", "100"],
          showTotal: (total) => `共 ${total} 条`,
          locale: { items_per_page: "/页" },
          onChange: (current, size) => {
            setPageCurrent(current);
            setPageSize(size);
          },
          position: ["bottomRight"]
        }}
      />
    </Card>
  );
}

function ConfigPanel({ companyCode }) {
  const [activeKey, setActiveKey] = useState("category");
  const [rows, setRows] = useState([]);
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [pageCurrent, setPageCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [companyOptions, setCompanyOptions] = useState([]);
  const [companyLoading, setCompanyLoading] = useState(false);
  const [form] = Form.useForm();

  const activeMeta = useMemo(
    () => CONFIG_META.find((item) => item.key === activeKey) ?? CONFIG_META[0],
    [activeKey]
  );

  const load = async () => {
    try {
      const params = companyCode && activeMeta.key !== "company-code" ? { companyCode } : undefined;
      const { data } = await client.get(activeMeta.endpoint, { params });
      setRows(asArray(data));
    } catch {
      setRows([]);
    }
  };

  useEffect(() => {
    load();
    setModalOpen(false);
    setEditing(null);
    form.resetFields();
    setQuery("");
    setPageCurrent(1);
  }, [activeMeta.endpoint, companyCode]);

  const filteredRows = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return rows;
    const fields = activeMeta.fields.map((f) => f.name);
    return rows.filter((row) =>
      fields.some((field) => String(row?.[field] ?? "").toLowerCase().includes(q))
    );
  }, [rows, query, activeMeta.fields]);

  const columns = useMemo(() => {
    return [
      ...activeMeta.columns,
      {
        title: "操作",
        width: 180,
        render: (_, row) => (
          <Space>
            <Button
              size="small"
              onClick={() => {
                setEditing(row);
                setModalOpen(true);
                form.setFieldsValue(row);
                if (activeMeta.key !== "company-code") {
                  loadCompanyOptions();
                }
              }}
            >
              编辑
            </Button>
            <Popconfirm
              overlayClassName="pretty-popconfirm"
              title={activeMeta.key === "company-code" ? "删除公司将清理全部关联数据，是否继续？" : `确认删除该${activeMeta.label}？`}
              description={
                activeMeta.key === "company-code"
                  ? "删除后将同步清除该公司关联的文件记录（含上传与数据湖文件）、台账运行与产物、配置覆盖项、权限授权数据。该操作不可撤销，如需恢复只能重新新增并重新导入。"
                  : "删除后将立即生效，如需恢复请重新新增。"
              }
              okText="确认删除"
              cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={async () => {
                  try {
                    await client.delete(`${activeMeta.endpoint}/${row.id}`);
                    message.success("删除成功");
                    load();
                  } catch {
                    // 失败提示已由全局拦截器处理
                  }
                }}
              >
              <Button danger size="small">删除</Button>
            </Popconfirm>
          </Space>
        )
      }
    ];
  }, [activeMeta, form]);

  const loadCompanyOptions = async (keyword = "") => {
    try {
      setCompanyLoading(true);
      const { data } = await client.get("/tax-ledger/config/company-code");
      const rows = asArray(data);
      const filteredRows = keyword
        ? rows.filter((item) => String(item?.companyCode ?? "").toLowerCase().includes(keyword.toLowerCase()))
        : rows;
      setCompanyOptions(
        filteredRows.map((item) => ({
          value: item.companyCode,
          label: `${item.companyCode}${item.companyName ? ` - ${item.companyName}` : ""}`
        }))
      );
    } catch {
      setCompanyOptions([]);
    } finally {
      setCompanyLoading(false);
    }
  };

  return (
    <div className="config-shell">
      <Card className="config-left soft-card" variant="borderless">
        <Text className="left-title">配置导航</Text>
        <div className="left-menu">
          {CONFIG_META.map((item) => (
            <button
              type="button"
              key={item.key}
              className={`left-menu-item ${item.key === activeKey ? "active" : ""}`}
              onClick={() => setActiveKey(item.key)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </Card>

      <Card
        className="config-right soft-card"
        variant="borderless"
        title={
          <div className="right-title-row">
            <div>
              <div className="right-title">{activeMeta.label}</div>
            </div>
            <Space>
              <Input
                allowClear
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="搜索当前配置字段"
                prefix={<SearchOutlined />}
                style={{ width: 240 }}
              />
              <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  setEditing(null);
                  setModalOpen(true);
                  form.resetFields();
                  if (activeMeta.key !== "company-code") {
                    loadCompanyOptions();
                  }
                }}
              >
                新增
              </Button>
            </Space>
          </div>
        }
      >
        <Modal
          title={editing ? `编辑${activeMeta.label}` : `新增${activeMeta.label}`}
          open={modalOpen}
          destroyOnHidden
          forceRender
          width={920}
          onCancel={() => {
            setModalOpen(false);
            setEditing(null);
            form.resetFields();
          }}
          footer={null}
        >
          <Form
            form={form}
            layout="vertical"
            onFinish={async (values) => {
              try {
                setSubmitting(true);
                const payload = editing ? { ...editing, ...values } : values;
                if (editing && activeMeta.key === "company-code") {
                  payload.companyCode = editing.companyCode;
                }
                await client.post(activeMeta.endpoint, payload);
                message.success(editing ? "更新成功" : "新增成功");
                setModalOpen(false);
                setEditing(null);
                form.resetFields();
                load();
              } catch {
                // 失败提示已由全局拦截器处理
              } finally {
                setSubmitting(false);
              }
            }}
          >
            <Row gutter={12}>
              {activeMeta.fields.map((f) => (
                <Col xs={24} sm={12} md={8} key={f.name}>
                  <Form.Item
                    name={f.name}
                    label={f.label}
                    rules={f.required ? [{ required: true, message: `${f.name === "companyCode" && activeMeta.key !== "company-code" ? "请选择" : "请输入"}${f.label}` }] : []}
                  >
                    {f.name === "companyCode" && activeMeta.key !== "company-code" ? (
                      <Select
                        showSearch
                        allowClear
                        placeholder={`请选择${f.label}`}
                        loading={companyLoading}
                        options={companyOptions}
                        filterOption={false}
                        onSearch={loadCompanyOptions}
                        onOpenChange={(open) => {
                          if (open) loadCompanyOptions();
                        }}
                      />
                    ) : (
                      <Input
                        placeholder={f.label}
                        disabled={Boolean(editing && activeMeta.key === "company-code" && f.name === "companyCode")}
                      />
                    )}
                  </Form.Item>
                </Col>
              ))}
            </Row>
            <Space>
              <Button type="primary" htmlType="submit" loading={submitting}>保存</Button>
              <Button
                onClick={() => {
                  setModalOpen(false);
                  setEditing(null);
                  form.resetFields();
                }}
              >
                取消
              </Button>
            </Space>
          </Form>
        </Modal>

        <Table
          rowKey="id"
          dataSource={Array.isArray(filteredRows) ? filteredRows : []}
          columns={columns}
          pagination={{
            current: pageCurrent,
            pageSize,
            total: filteredRows.length,
            showSizeChanger: true,
            showQuickJumper: true,
            pageSizeOptions: ["10", "20", "50", "100"],
            showTotal: (total) => `共 ${total} 条`,
            locale: { items_per_page: "/页" },
            onChange: (current, size) => {
              setPageCurrent(current);
              setPageSize(size);
            },
            position: ["bottomRight"]
          }}
        />
      </Card>
    </div>
  );
}

export default function App() {
  const [companyCode, setCompanyCode] = useState("");

  useEffect(() => {
    const id = client.interceptors.response.use(
      (resp) => {
        const raw = resp.data;
        if (raw && typeof raw === "object" && Object.prototype.hasOwnProperty.call(raw, "code")) {
          const code = Number(raw.code);
          if (!Number.isNaN(code) && code !== 0) {
            message.error(raw.msg || "请求失败");
            return Promise.reject({
              response: { data: raw },
              message: raw.msg || "请求失败"
            });
          }
          resp.data = raw.data;
          return resp;
        }
        const payload = normalizePayload(raw);
        resp.data = payload;
        return resp;
      },
      (error) => {
        const msg = error?.response?.data?.msg || error?.response?.data?.message || error?.message || "request failed";
        message.error(msg);
        return Promise.reject(error);
      }
    );
    return () => client.interceptors.response.eject(id);
  }, []);

  const tabs = useMemo(
    () => [
      { key: "company", label: "公司", children: <CompanyPanel onSelectCompany={setCompanyCode} /> },
      { key: "files", label: "文件", children: <FilePanel companyCode={companyCode} /> },
      { key: "ledger", label: "台账", children: <LedgerPanel companyCode={companyCode} /> },
      { key: "permission", label: "权限", children: <PermissionPanel companyCode={companyCode} /> },
      { key: "config", label: "配置", children: <ConfigPanel companyCode={companyCode} /> }
    ],
    [companyCode]
  );

  return (
    <ConfigProvider locale={zhCN}>
      <Layout className="app-layout">
        <Header className="app-header">
          <div className="brand-title">EPC 进项税报表系统</div>
          <div className="brand-subtitle">
            {companyCode ? `当前公司：${companyCode}` : "请先在公司页选择目标公司"}
          </div>
        </Header>
        <Content className="app-content">
          <Tabs className="main-tabs" items={tabs} />
        </Content>
      </Layout>
    </ConfigProvider>
  );
}
