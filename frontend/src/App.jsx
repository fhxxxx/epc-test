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
  Upload
} from "antd";
import { UploadOutlined } from "@ant-design/icons";
import client from "./api/client";

const { Header, Content } = Layout;
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

function CompanyPanel({ onSelectCompany }) {
  const [form] = Form.useForm();
  const [rows, setRows] = useState([]);

  const load = async () => {
    const { data } = await client.get("/tax-ledger/companies");
    setRows(data || []);
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <Card title="公司管理" extra={<Button onClick={load}>刷新</Button>}>
      <Form
        form={form}
        layout="inline"
        onFinish={async (values) => {
          await client.post("/tax-ledger/companies", values);
          form.resetFields();
          load();
        }}
      >
        <Form.Item name="companyCode" rules={[{ required: true }]}>
          <Input placeholder="公司代码" />
        </Form.Item>
        <Form.Item name="companyName" rules={[{ required: true }]}>
          <Input placeholder="公司名称" />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit">
            保存公司
          </Button>
        </Form.Item>
      </Form>
      <Table
        style={{ marginTop: 16 }}
        rowKey="id"
        dataSource={rows}
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
    if (!companyCode) return;
    const { data } = await client.get("/tax-ledger/files", { params: { companyCode, yearMonth } });
    setRows(data || []);
  };

  return (
    <Card
      title="文件管理"
      extra={
        <Space>
          <Input value={yearMonth} onChange={(e) => setYearMonth(e.target.value)} style={{ width: 120 }} />
          <Button onClick={load}>刷新</Button>
        </Space>
      }
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
              onSuccess?.();
              load();
            } catch (e) {
              onError?.(e);
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
              fiscalYearPeriodStart: yearMonth.replace("-", "") + "1",
              fiscalYearPeriodEnd: yearMonth.replace("-", "") + "1"
            });
            load();
          }}
        >
          拉取数据湖
        </Button>
      </Space>
      <Table
        rowKey="id"
        dataSource={rows}
        columns={[
          { title: "类别", dataIndex: "fileCategory", render: (v) => <Tag>{v}</Tag> },
          { title: "来源", dataIndex: "fileSource" },
          { title: "文件名", dataIndex: "fileName" },
          {
            title: "操作",
            render: (_, row) => <Button onClick={() => window.open(`/tax-ledger/files/${row.id}/download`)}>下载</Button>
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
    if (!companyCode) return;
    const { data } = await client.get(`/tax-ledger/ledger/${companyCode}/${yearMonth}/runs`);
    setRows(data || []);
  };

  return (
    <Card
      title="台账运行"
      extra={
        <Space>
          <Input value={yearMonth} onChange={(e) => setYearMonth(e.target.value)} style={{ width: 120 }} />
          <Button onClick={load}>刷新</Button>
        </Space>
      }
    >
      <Space style={{ marginBottom: 16 }}>
        <Select
          value={mode}
          onChange={setMode}
          options={[
            { value: "AUTO", label: "AUTO" },
            { value: "GATED", label: "GATED" }
          ]}
          style={{ width: 120 }}
        />
        <Button
          type="primary"
          onClick={async () => {
            await client.post("/tax-ledger/ledger/runs", { companyCode, yearMonth, mode });
            load();
          }}
        >
          发起生成
        </Button>
        <Button onClick={() => window.open(`/tax-ledger/ledger/${companyCode}/${yearMonth}/download`)}>
          下载最终台账
        </Button>
      </Space>
      <Table
        rowKey="id"
        dataSource={rows}
        columns={[
          { title: "RunNo", dataIndex: "runNo" },
          { title: "状态", dataIndex: "status", render: (v) => <Tag>{v}</Tag> },
          { title: "批次", dataIndex: "currentBatch" },
          {
            title: "详情",
            render: (_, row) => <Button onClick={() => window.open(`/tax-ledger/ledger/runs/${row.id}`)}>查看</Button>
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
    const { data } = await client.get("/tax-ledger/permissions", { params: { companyCode } });
    setRows(data || []);
  };

  useEffect(() => {
    load();
  }, [companyCode]);

  return (
    <Card title="权限管理" extra={<Button onClick={load}>刷新</Button>}>
      <Form
        form={form}
        layout="inline"
        onFinish={async (values) => {
          await client.post("/tax-ledger/permissions", values);
          form.resetFields();
          load();
        }}
      >
        <Form.Item name="userId" rules={[{ required: true }]}><Input placeholder="userId" /></Form.Item>
        <Form.Item name="userName" rules={[{ required: true }]}><Input placeholder="userName" /></Form.Item>
        <Form.Item name="employeeId" rules={[{ required: true }]}><Input placeholder="employeeId" /></Form.Item>
        <Form.Item name="permissionLevel" rules={[{ required: true }]}><Select style={{ width: 150 }} options={[{ value: "SUPER_ADMIN" }, { value: "COMPANY_ADMIN" }, { value: "COMPANY_USER" }]} /></Form.Item>
        <Form.Item name="companyCode"><Input placeholder="companyCode" /></Form.Item>
        <Form.Item><Button type="primary" htmlType="submit">授权</Button></Form.Item>
      </Form>
      <Table
        style={{ marginTop: 16 }}
        rowKey="id"
        dataSource={rows}
        columns={[
          { title: "employeeId", dataIndex: "employeeId" },
          { title: "userName", dataIndex: "userName" },
          { title: "level", dataIndex: "permissionLevel", render: (v) => <Tag>{v}</Tag> },
          { title: "companyCode", dataIndex: "companyCode" },
          {
            title: "操作",
            render: (_, row) => (
              <Button
                danger
                onClick={async () => {
                  await client.delete("/tax-ledger/permissions", { params: { employeeId: row.employeeId, companyCode: row.companyCode } });
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
  const [rows, setRows] = useState([]);
  const [form] = Form.useForm();

  const load = async () => {
    const { data } = await client.get("/tax-ledger/config/category", { params: { companyCode } });
    setRows(data || []);
  };

  useEffect(() => {
    load();
  }, [companyCode]);

  return (
    <Card title="税目配置（示例）" extra={<Button onClick={load}>刷新</Button>}>
      <Form
        form={form}
        layout="inline"
        onFinish={async (values) => {
          await client.post("/tax-ledger/config/category", values);
          form.resetFields();
          load();
        }}
      >
        <Form.Item name="seqNo" rules={[{ required: true }]}><Input placeholder="seqNo" /></Form.Item>
        <Form.Item name="companyCode"><Input placeholder="companyCode" /></Form.Item>
        <Form.Item name="taxType" rules={[{ required: true }]}><Input placeholder="taxType" /></Form.Item>
        <Form.Item name="taxCategory"><Input placeholder="taxCategory" /></Form.Item>
        <Form.Item><Button type="primary" htmlType="submit">保存</Button></Form.Item>
      </Form>
      <Table
        style={{ marginTop: 16 }}
        rowKey="id"
        dataSource={rows}
        columns={[
          { title: "seqNo", dataIndex: "seqNo" },
          { title: "companyCode", dataIndex: "companyCode" },
          { title: "taxType", dataIndex: "taxType" },
          { title: "taxCategory", dataIndex: "taxCategory" },
          {
            title: "操作",
            render: (_, row) => (
              <Button
                danger
                onClick={async () => {
                  await client.delete(`/tax-ledger/config/category/${row.id}`);
                  load();
                }}
              >
                删除
              </Button>
            )
          }
        ]}
      />
    </Card>
  );
}

export default function App() {
  const [companyCode, setCompanyCode] = useState("");

  useEffect(() => {
    client.interceptors.response.use(
      (resp) => resp,
      (error) => {
        console.error(error?.response?.data?.message || "request failed");
        return Promise.reject(error);
      }
    );
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
    <Layout style={{ minHeight: "100vh" }}>
      <Header style={{ color: "white", fontSize: 18 }}>
        EPC 进项税报表系统 {companyCode ? `| 当前公司: ${companyCode}` : ""}
      </Header>
      <Content style={{ padding: 24 }}>
        <Row gutter={16}>
          <Col span={24}>
            <Tabs items={tabs} />
          </Col>
        </Row>
      </Content>
    </Layout>
  );
}
