import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
  Checkbox,
  Col,
  ConfigProvider,
  DatePicker,
  Form,
  Input,
  Layout,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
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
  { value: "BS_APPENDIX_TAX_PAYABLE", label: "BS附表-应交税费科目余额表" },
  { value: "PL_APPENDIX_PROJECT", label: "PL附表（项目公司）" },
  { value: "STAMP_TAX", label: "印花税明细" },
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
  const [companyOptions, setCompanyOptions] = useState([]);
  const [companyLoading, setCompanyLoading] = useState(false);
  const [uploadFileCategoryOptions, setUploadFileCategoryOptions] = useState(DEFAULT_UPLOAD_FILE_CATEGORIES);
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

  const loadUploadFileCategoryOptions = async () => {
    try {
      const { data } = await client.get("/tax-ledger/files/categories", {
        params: { manualUpload: true }
      });
      const list = asArray(data);
      if (list.length > 0) {
        setUploadFileCategoryOptions(
          list.map((item) => ({
            value: item.value,
            label: item.label
          }))
        );
      } else {
        setUploadFileCategoryOptions(DEFAULT_UPLOAD_FILE_CATEGORIES);
      }
    } catch {
      setUploadFileCategoryOptions(DEFAULT_UPLOAD_FILE_CATEGORIES);
    }
  };

  useEffect(() => {
    loadUploadFileCategoryOptions();
  }, []);

  const onOpenUploadModal = () => {
    setUploadModalOpen(true);
    setUploadRows([]);
    setBatchCompanyCode(companyCode || undefined);
    setBatchFileCategory(category);
    loadCompanyOptions();
    loadUploadFileCategoryOptions();
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

  const applyBatchSettings = () => {
    if (!uploadRows.length) {
      message.warning("请先选择需要上传的文件");
      return;
    }
    if (!batchCompanyCode && !batchFileCategory) {
      message.warning("请先选择要批量应用的公司代码或类型");
      return;
    }
    setUploadRows((prev) => prev.map((row) => ({
      ...row,
      ...(batchCompanyCode ? { companyCode: batchCompanyCode } : {}),
      ...(batchFileCategory ? { fileCategory: batchFileCategory } : {})
    })));
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
            onChange={setBatchCompanyCode}
          />
          <Select
            value={batchFileCategory}
            placeholder="请选择类型"
            style={{ width: 240 }}
            options={uploadFileCategoryOptions}
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
                  onChange={(value) => updateUploadRow(row.uid, { companyCode: value })}
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
                  options={uploadFileCategoryOptions}
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
    </Card>
  );
}

function LedgerPanel({ companyCode }) {
  const [yearMonth, setYearMonth] = useState("2026-01");
  const [mode, setMode] = useState("AUTO");
  const [rows, setRows] = useState([]);
  const [detailOpen, setDetailOpen] = useState(false);
  const [runDetail, setRunDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const load = async () => {
    if (!companyCode) {
      setRows([]);
      return;
    }
    try {
      const { data } = await client.get(`/tax-ledger/ledger/${companyCode}/${yearMonth}/runs`);
      setRows(asArray(data));
    } catch {
      setRows([]);
    }
  };

  useEffect(() => {
    load();
  }, [companyCode, yearMonth]);

  useEffect(() => {
    if (!rows.some((item) => item.status === "RUNNING" || item.status === "PAUSED")) {
      return undefined;
    }
    const timer = setInterval(() => {
      load();
    }, 5000);
    return () => clearInterval(timer);
  }, [rows, companyCode, yearMonth]);

  const openRunDetail = async (runId) => {
    try {
      setDetailLoading(true);
      const { data } = await client.get(`/tax-ledger/ledger/runs/${runId}`);
      setRunDetail(data);
      setDetailOpen(true);
    } catch {
      setRunDetail(null);
    } finally {
      setDetailLoading(false);
    }
  };

  return (
    <Card
      title="台账运行"
      className="soft-card"
      extra={(
        <Space>
          <Input value={yearMonth} onChange={(e) => setYearMonth(e.target.value)} style={{ width: 120 }} />
          <Button onClick={load}>刷新</Button>
        </Space>
      )}
    >
      <Space style={{ marginBottom: 16 }}>
        <Select
          value={mode}
          onChange={setMode}
          options={[{ value: "AUTO", label: "AUTO" }, { value: "GATED", label: "GATED" }]}
          style={{ width: 120 }}
        />
        <Button
          type="primary"
          onClick={async () => {
            try {
              await client.post("/tax-ledger/ledger/runs", { companyCode, yearMonth, mode });
              message.success("已发起台账生成");
              load();
            } catch {
              // 失败提示已由全局拦截器处理
            }
          }}
          disabled={!companyCode}
        >
          发起生成
        </Button>
        <Button onClick={() => window.open(`/tax-ledger/ledger/${companyCode}/${yearMonth}/download`, "_blank")}>
          下载最终台账
        </Button>
      </Space>

      <Table
        rowKey="id"
        dataSource={Array.isArray(rows) ? rows : []}
        columns={[
          { title: "RunNo", dataIndex: "runNo" },
          { title: "状态", dataIndex: "status", render: (v) => <Tag>{v}</Tag> },
          { title: "当前批次", dataIndex: "currentBatch" },
          {
            title: "详情",
            render: (_, row) => (
              <Space>
                <Button onClick={() => openRunDetail(row.id)}>查看</Button>
                {row.status === "PAUSED" ? (
                  <Button
                    type="primary"
                    onClick={async () => {
                      try {
                        await client.post(`/tax-ledger/ledger/runs/${row.id}/confirm`, {
                          batchNo: row.currentBatch
                        });
                        message.success("已继续执行");
                        load();
                        if (detailOpen) {
                          openRunDetail(row.id);
                        }
                      } catch {
                        // 失败提示已由全局拦截器处理
                      }
                    }}
                  >
                    继续执行
                  </Button>
                ) : null}
              </Space>
            )
          }
        ]}
      />
      <Modal
        title="运行详情"
        open={detailOpen}
        width={960}
        footer={null}
        onCancel={() => setDetailOpen(false)}
      >
        {detailLoading ? (
          <Text>加载中...</Text>
        ) : (
          <Space direction="vertical" style={{ width: "100%" }}>
            <Space>
              <Text>RunId: {runDetail?.runId ?? "-"}</Text>
              <Tag>{runDetail?.status ?? "-"}</Tag>
              <Text>当前批次: {runDetail?.currentBatch ?? "-"}</Text>
            </Space>
            {runDetail?.blockingManualAction ? (
              <Card size="small" title="阻塞人工动作">
                <div>动作: {runDetail.blockingManualAction.actionCode}</div>
                <div>提示: {runDetail.blockingManualAction.hint}</div>
                <div>必填字段: {(runDetail.blockingManualAction.requiredFields || []).join(", ")}</div>
              </Card>
            ) : null}
            <Table
              rowKey={(item) => `${item.nodeCode}-${item.batchNo}`}
              size="small"
              pagination={false}
              dataSource={Array.isArray(runDetail?.tasks) ? runDetail.tasks : []}
              columns={[
                { title: "节点", dataIndex: "nodeCode" },
                { title: "批次", dataIndex: "batchNo" },
                { title: "状态", dataIndex: "status", render: (v) => <Tag>{v}</Tag> },
                { title: "依赖", dataIndex: "dependsOn" },
                { title: "错误", dataIndex: "errorMsg" }
              ]}
            />
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
