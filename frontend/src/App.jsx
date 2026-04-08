import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Layout,
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
import { PlusOutlined, ReloadOutlined, SearchOutlined, UploadOutlined } from "@ant-design/icons";
import client from "./api/client";
import "./App.css";

const { Header, Content } = Layout;
const { Text } = Typography;

const FILE_CATEGORIES = [
  "BS",
  "PL",
  "BS_APPENDIX_TAX_PAYABLE",
  "PL_APPENDIX_2320",
  "PL_APPENDIX_PROJECT",
  "STAMP_TAX",
  "VAT_OUTPUT",
  "VAT_INPUT_CERT",
  "CUMULATIVE_PROJECT_TAX"
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

function ymToFiscalPeriod(yearMonth) {
  return `${yearMonth.replace("-", "")}1`;
}

function CompanyPanel({ onSelectCompany }) {
  const [form] = Form.useForm();
  const [rows, setRows] = useState([]);

  const load = async () => {
    try {
      const { data } = await client.get("/tax-ledger/companies");
      setRows(asArray(data));
    } catch {
      setRows([]);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <Card title="公司管理" extra={<Button onClick={load}>刷新</Button>} className="soft-card">
      <Form
        form={form}
        layout="inline"
        onFinish={async (values) => {
          await client.post("/tax-ledger/companies", values);
          message.success("公司保存成功");
          form.resetFields();
          load();
        }}
      >
        <Form.Item name="companyCode" rules={[{ required: true, message: "请输入公司代码" }]}>
          <Input placeholder="公司代码" />
        </Form.Item>
        <Form.Item name="companyName" rules={[{ required: true, message: "请输入公司名称" }]}>
          <Input placeholder="公司名称" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit">保存公司</Button>
        </Form.Item>
      </Form>

      <Table
        style={{ marginTop: 16 }}
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
                <Button
                  danger
                  onClick={async () => {
                    await client.delete(`/tax-ledger/companies/${row.id}`);
                    message.success("公司删除成功");
                    load();
                  }}
                >
                  删除
                </Button>
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

  const load = async () => {
    if (!companyCode) {
      setRows([]);
      return;
    }
    try {
      const { data } = await client.get("/tax-ledger/files", { params: { companyCode, yearMonth } });
      setRows(asArray(data));
    } catch {
      setRows([]);
    }
  };

  return (
    <Card
      title="文件管理"
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
          value={category}
          onChange={setCategory}
          options={FILE_CATEGORIES.map((item) => ({ value: item, label: item }))}
          style={{ width: 260 }}
        />
        <Upload
          showUploadList={false}
          customRequest={async ({ file, onSuccess, onError }) => {
            try {
              const formData = new FormData();
              formData.append("file", file);
              formData.append("companyCode", companyCode);
              formData.append("yearMonth", yearMonth);
              formData.append("fileCategory", category);
              await client.post("/tax-ledger/files/upload", formData, {
                headers: { "Content-Type": "multipart/form-data" }
              });
              message.success("文件上传成功");
              onSuccess?.();
              load();
            } catch (err) {
              onError?.(err);
            }
          }}
        >
          <Button icon={<UploadOutlined />}>上传文件</Button>
        </Upload>
        <Button
          onClick={async () => {
            await client.post("/tax-ledger/datalake/pull", {
              companyCode,
              yearMonth,
              fiscalYearPeriodStart: ymToFiscalPeriod(yearMonth),
              fiscalYearPeriodEnd: ymToFiscalPeriod(yearMonth)
            });
            message.success("数据湖拉取完成");
            load();
          }}
        >
          拉取数据湖
        </Button>
      </Space>

      <Table
        rowKey="id"
        dataSource={Array.isArray(rows) ? rows : []}
        columns={[
          { title: "类别", dataIndex: "fileCategory", render: (v) => <Tag>{v}</Tag> },
          { title: "来源", dataIndex: "fileSource" },
          { title: "文件名", dataIndex: "fileName" },
          {
            title: "操作",
            render: (_, row) => (
              <Button onClick={() => window.open(`/tax-ledger/files/${row.id}/download`, "_blank")}>下载</Button>
            )
          }
        ]}
      />
    </Card>
  );
}

function LedgerPanel({ companyCode }) {
  const [yearMonth, setYearMonth] = useState("2026-01");
  const [mode, setMode] = useState("AUTO");
  const [rows, setRows] = useState([]);

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
            await client.post("/tax-ledger/ledger/runs", { companyCode, yearMonth, mode });
            message.success("已发起台账生成");
            load();
          }}
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
              <Button onClick={() => window.open(`/tax-ledger/ledger/runs/${row.id}`, "_blank")}>查看</Button>
            )
          }
        ]}
      />
    </Card>
  );
}

function PermissionPanel({ companyCode }) {
  const [rows, setRows] = useState([]);
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

  useEffect(() => {
    load();
  }, [companyCode]);

  return (
    <Card title="权限管理" className="soft-card" extra={<Button onClick={load}>刷新</Button>}>
      <Form
        form={form}
        layout="inline"
        onFinish={async (values) => {
          await client.post("/tax-ledger/permissions", values);
          message.success("授权成功");
          form.resetFields();
          load();
        }}
      >
        <Form.Item name="userId" rules={[{ required: true, message: "请输入 userId" }]}>
          <Input placeholder="userId" />
        </Form.Item>
        <Form.Item name="userName" rules={[{ required: true, message: "请输入 userName" }]}>
          <Input placeholder="userName" />
        </Form.Item>
        <Form.Item name="employeeId" rules={[{ required: true, message: "请输入 employeeId" }]}>
          <Input placeholder="employeeId" />
        </Form.Item>
        <Form.Item name="permissionLevel" rules={[{ required: true, message: "请选择权限级别" }]}>
          <Select
            style={{ width: 170 }}
            options={[
              { value: "SUPER_ADMIN", label: "SUPER_ADMIN" },
              { value: "COMPANY_ADMIN", label: "COMPANY_ADMIN" },
              { value: "COMPANY_USER", label: "COMPANY_USER" }
            ]}
          />
        </Form.Item>
        <Form.Item name="companyCode">
          <Input placeholder="companyCode(超管可空)" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit">授权</Button>
        </Form.Item>
      </Form>

      <Table
        style={{ marginTop: 16 }}
        rowKey="id"
        dataSource={Array.isArray(rows) ? rows : []}
        columns={[
          { title: "工号", dataIndex: "employeeId" },
          { title: "姓名", dataIndex: "userName" },
          { title: "级别", dataIndex: "permissionLevel", render: (v) => <Tag>{v}</Tag> },
          { title: "公司代码", dataIndex: "companyCode" },
          {
            title: "操作",
            render: (_, row) => (
              <Button
                danger
                onClick={async () => {
                  await client.delete("/tax-ledger/permissions", {
                    params: { employeeId: row.employeeId, companyCode: row.companyCode }
                  });
                  message.success("撤销成功");
                  load();
                }}
              >
                撤销
              </Button>
            )
          }
        ]}
      />
    </Card>
  );
}

function ConfigPanel({ companyCode }) {
  const [activeKey, setActiveKey] = useState("category");
  const [rows, setRows] = useState([]);
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState(null);
  const [formVisible, setFormVisible] = useState(false);
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
    setFormVisible(false);
    setEditing(null);
    form.resetFields();
    setQuery("");
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
                setFormVisible(true);
                form.setFieldsValue(row);
              }}
            >
              编辑
            </Button>
            <Button
              danger
              size="small"
              onClick={async () => {
                await client.delete(`${activeMeta.endpoint}/${row.id}`);
                message.success("删除成功");
                load();
              }}
            >
              删除
            </Button>
          </Space>
        )
      }
    ];
  }, [activeMeta, form]);

  return (
    <div className="config-shell">
      <Card className="config-left soft-card" bordered={false}>
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
        bordered={false}
        title={
          <div className="right-title-row">
            <div>
              <div className="right-title">{activeMeta.label}</div>
              <div className="right-subtitle">当前共 {filteredRows.length} 条记录</div>
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
                  setFormVisible(true);
                  form.resetFields();
                }}
              >
                新增
              </Button>
            </Space>
          </div>
        }
      >
        {formVisible && (
          <div className="config-form-wrap">
            <Form
              form={form}
              layout="vertical"
              onFinish={async (values) => {
                const payload = editing ? { ...editing, ...values } : values;
                await client.post(activeMeta.endpoint, payload);
                message.success(editing ? "更新成功" : "新增成功");
                setFormVisible(false);
                setEditing(null);
                form.resetFields();
                load();
              }}
            >
              <Row gutter={12}>
                {activeMeta.fields.map((f) => (
                  <Col xs={24} sm={12} md={8} lg={6} key={f.name}>
                    <Form.Item
                      name={f.name}
                      label={f.label}
                      rules={f.required ? [{ required: true, message: `请输入${f.label}` }] : []}
                    >
                      <Input placeholder={f.label} />
                    </Form.Item>
                  </Col>
                ))}
              </Row>
              <Space>
                <Button type="primary" htmlType="submit">保存</Button>
                <Button
                  onClick={() => {
                    setFormVisible(false);
                    setEditing(null);
                    form.resetFields();
                  }}
                >
                  取消
                </Button>
              </Space>
            </Form>
          </div>
        )}

        <Table
          rowKey="id"
          dataSource={Array.isArray(filteredRows) ? filteredRows : []}
          columns={columns}
          pagination={{ pageSize: 10, showSizeChanger: false }}
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
        resp.data = normalizePayload(resp.data);
        return resp;
      },
      (error) => {
        const msg = error?.response?.data?.message || error?.message || "request failed";
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
  );
}
