import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
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
import { PlusOutlined, ReloadOutlined, SearchOutlined, UploadOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import client from "./api/client";
import "./App.css";

const { Header, Content } = Layout;
const { Text } = Typography;

const FILE_CATEGORIES = [
  { value: "BS", label: "资产负债表（BS）" },
  { value: "PL", label: "利润表（PL）" },
  { value: "BS_APPENDIX_TAX_PAYABLE", label: "BS附表-应交税费" },
  { value: "PL_APPENDIX_2320", label: "PL附表（2320/2355）" },
  { value: "PL_APPENDIX_PROJECT", label: "PL附表（项目公司）" },
  { value: "STAMP_TAX", label: "印花税明细" },
  { value: "VAT_OUTPUT", label: "增值税销项" },
  { value: "VAT_INPUT_CERT", label: "增值税进项认证清单" },
  { value: "CUMULATIVE_PROJECT_TAX", label: "累计项目税收明细表" }
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
  const [pullModalOpen, setPullModalOpen] = useState(false);
  const [pullSubmitting, setPullSubmitting] = useState(false);
  const [companyOptions, setCompanyOptions] = useState([]);
  const [companyLoading, setCompanyLoading] = useState(false);
  const [pullForm] = Form.useForm();

  const periodOptions = useMemo(
    () => Array.from({ length: 16 }, (_, i) => {
      const period = String(i + 1).padStart(2, "0");
      return { value: period, label: period };
    }),
    []
  );

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
      const makeFiscalPeriod = (monthValue) => `${fiscalYear}${String(monthValue).padStart(2, "0")}1`;
      const results = await Promise.allSettled(
        values.companyCodes.map((code) =>
          client.post("/tax-ledger/datalake/pull", {
            companyCode: code,
            yearMonth: requestYearMonth,
            fiscalYearPeriodStart: makeFiscalPeriod(startMonth),
            fiscalYearPeriodEnd: makeFiscalPeriod(endMonth)
          })
        )
      );
      const successCount = results.filter((item) => item.status === "fulfilled").length;
      const failedCount = results.length - successCount;
      if (failedCount === 0) {
        message.success(`数据湖拉取完成，共 ${successCount} 家公司`);
      } else if (successCount > 0) {
        message.warning(`部分完成：成功 ${successCount} 家，失败 ${failedCount} 家`);
      } else {
        message.error("数据湖拉取失败，请检查后重试");
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
        <Select
          value={category}
          onChange={setCategory}
          options={FILE_CATEGORIES}
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
        <Button onClick={onOpenPullModal}>拉取数据湖</Button>
      </Space>

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
            try {
              await client.post("/tax-ledger/ledger/runs", { companyCode, yearMonth, mode });
              message.success("已发起台账生成");
              load();
            } catch {
              // 失败提示已由全局拦截器处理
            }
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
  const permissionLevel = Form.useWatch("permissionLevel", form);

  const levelOptions = [
    { value: "SUPER_ADMIN", label: "超级管理员" },
    { value: "COMPANY_ADMIN", label: "公司管理员" },
    { value: "COMPANY_USER", label: "公司用户" }
  ];

  const levelTextMap = {
    SUPER_ADMIN: "超级管理员",
    COMPANY_ADMIN: "公司管理员",
    COMPANY_USER: "公司用户"
  };

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
              const permissionLevel = values.permissionLevel;
              const companyCodeValue = permissionLevel === "SUPER_ADMIN" ? null : values.companyCode;
              if (permissionLevel !== "SUPER_ADMIN" && !companyCodeValue) {
                message.warning("公司级权限必须选择公司代码");
                return;
              }
              const payload = selectedUsers.map((item) => ({
                userId: item.account || item.userCode,
                userName: item.username,
                employeeId: item.userCode,
                permissionLevel,
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
            <Col span={12}>
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
            <Col span={6}>
              <Form.Item
                name="permissionLevel"
                label="权限级别"
                rules={[{ required: true, message: "请选择权限级别" }]}
              >
                <Select options={levelOptions} placeholder="请选择级别" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="companyCode" label="公司代码">
                <Select
                  showSearch
                  allowClear
                  placeholder="超级管理员可留空"
                  loading={companyLoading}
                  disabled={permissionLevel === "SUPER_ADMIN"}
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
          { title: "工号", dataIndex: "employeeId" },
          { title: "姓名", dataIndex: "userName" },
          { title: "级别", dataIndex: "permissionLevel", render: (v) => <Tag>{levelTextMap[v] || v}</Tag> },
          { title: "公司代码", dataIndex: "companyCode" },
          {
            title: "操作",
            render: (_, row) => (
              <Popconfirm
                overlayClassName="pretty-popconfirm"
                title="确认撤销该权限？"
                description={`将撤销员工 ${row.employeeId} 的权限绑定。`}
                okText="确认撤销"
                cancelText="取消"
                okButtonProps={{ danger: true }}
                onConfirm={async () => {
                  try {
                    await client.delete("/tax-ledger/permissions", {
                      params: { employeeId: row.employeeId, companyCode: row.companyCode }
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
              title={`确认删除该${activeMeta.label}？`}
              description="删除后将立即生效，如需恢复请重新新增。"
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
                      <Input placeholder={f.label} />
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
