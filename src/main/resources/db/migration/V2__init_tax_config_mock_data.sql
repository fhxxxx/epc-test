-- 税务台账 5张配置表初始化模拟数据（基于 overview.md + 需求文档）
-- 说明：
-- 1) 每表约20条数据；
-- 2) 关系口径：company_code_config -> 其余4表按 company_code 关联；
-- 3) 特殊条目仅给 2320/2355；基础条目采用“通用(NULL)+公司覆盖”混合。

SET NAMES utf8mb4;
START TRANSACTION;

-- =========================================================
-- 1) 公司代码配置表 t_company_code_config（20条）
-- =========================================================
DELETE FROM t_company_code_config WHERE id BETWEEN 910001 AND 910020;
DELETE FROM t_company_code_config
WHERE company_code IN (
  '2320','2355','3019','3021','3022','3023','3024','3025','3026','3027',
  '3028','3029','3030','3031','3032','3033','3034','3035','3036','3037'
);

INSERT INTO t_company_code_config (
  id, company_code, company_name, finance_bp_ad, finance_bp_name, finance_bp_email,
  create_time, create_by, create_by_name, update_time, update_by, update_by_name, is_deleted
) VALUES
(910001, '2320', '上海睿景能源科技有限公司', 'bp2320', '张敏', 'bp2320@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910002, '2355', '上海景程新能源有限公司', 'bp2355', '李娜', 'bp2355@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910003, '3019', '江阴青芦新能源有限公司', 'bp3019', '王磊', 'bp3019@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910004, '3021', '南京青芦新能源有限公司', 'bp3021', '赵云', 'bp3021@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910005, '3022', '苏州青芦新能源有限公司', 'bp3022', '陈杰', 'bp3022@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910006, '3023', '无锡青芦新能源有限公司', 'bp3023', '周倩', 'bp3023@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910007, '3024', '常州青芦新能源有限公司', 'bp3024', '吴涛', 'bp3024@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910008, '3025', '杭州青芦新能源有限公司', 'bp3025', '郑飞', 'bp3025@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910009, '3026', '宁波青芦新能源有限公司', 'bp3026', '钱琳', 'bp3026@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910010, '3027', '合肥青芦新能源有限公司', 'bp3027', '孙慧', 'bp3027@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910011, '3028', '芜湖青芦新能源有限公司', 'bp3028', '冯涛', 'bp3028@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910012, '3029', '武汉青芦新能源有限公司', 'bp3029', '何静', 'bp3029@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910013, '3030', '长沙青芦新能源有限公司', 'bp3030', '高峰', 'bp3030@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910014, '3031', '广州青芦新能源有限公司', 'bp3031', '林雪', 'bp3031@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910015, '3032', '深圳青芦新能源有限公司', 'bp3032', '蔡阳', 'bp3032@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910016, '3033', '佛山青芦新能源有限公司', 'bp3033', '蒋宁', 'bp3033@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910017, '3034', '成都青芦新能源有限公司', 'bp3034', '许哲', 'bp3034@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910018, '3035', '重庆青芦新能源有限公司', 'bp3035', '韩璐', 'bp3035@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910019, '3036', '西安青芦新能源有限公司', 'bp3036', '彭凯', 'bp3036@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(910020, '3037', '青岛青芦新能源有限公司', 'bp3037', '卢颖', 'bp3037@epc.local', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0);

-- =========================================================
-- 2) 税目配置表 t_tax_category_config（20条）
--   - 含通用规则(company_code=NULL)
--   - 含2320/2355专项规则
-- =========================================================
DELETE FROM t_tax_category_config WHERE id BETWEEN 920001 AND 920020;

INSERT INTO t_tax_category_config (
  id, seq_no, company_code, tax_type, tax_category, tax_basis, collection_ratio, tax_rate, account_subject,
  create_time, create_by, create_by_name, update_time, update_by, update_by_name, is_deleted
) VALUES
(920001, '1',   NULL,  '增值税',   '销项税额',         '不含税销售额', 100.00, 0.130000, '应交税费-应交增值税(销项税额)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920002, '1.1', NULL,  '增值税',   '进项税额',         '认证进项税额', 100.00, 0.130000, '应交税费-应交增值税(进项税额)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920003, '2',   NULL,  '附加税',   '城建税',           '当期应纳增值税', 100.00, 0.070000, '税金及附加-城建税', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920004, '3',   NULL,  '附加税',   '教育费附加',       '当期应纳增值税', 100.00, 0.030000, '税金及附加-教育费附加', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920005, '4',   NULL,  '附加税',   '地方教育附加',     '当期应纳增值税', 100.00, 0.020000, '税金及附加-地方教育附加', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920006, '5',   NULL,  '印花税',   '购销合同印花税',   '购销合同金额', 100.00, 0.000300, '税金及附加-印花税', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920007, '6',   NULL,  '企业所得税', '当期应纳税所得额', '利润总额调整后', 100.00, 0.250000, '所得税费用-当期所得税', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920008, '7',   NULL,  '土地使用税', '土地使用税',     '占地面积', 100.00, 0.000000, '税金及附加-土地使用税', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920009, '8',   NULL,  '房产税',   '房产原值税',       '房产原值扣减后', 100.00, 0.012000, '税金及附加-房产税', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920010, '9',   NULL,  '水利建设基金', '水利建设基金',  '销售收入', 100.00, 0.000100, '税金及附加-水利基金', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920011, '10',  '2320', '增值税',  '专票-13%',         'PL附表拆分依据', 100.00, 0.130000, '应交税费-销项税(13%)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920012, '11',  '2320', '增值税',  '专票-9%',          'PL附表拆分依据', 100.00, 0.090000, '应交税费-销项税(9%)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920013, '12',  '2320', '增值税',  '专票-6%',          'PL附表拆分依据', 100.00, 0.060000, '应交税费-销项税(6%)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920014, '13',  '2320', '增值税',  '普票-13%',         'PL附表拆分依据', 100.00, 0.130000, '应交税费-销项税(13%普票)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920015, '14',  '2320', '增值税',  '普票-9%',          'PL附表拆分依据', 100.00, 0.090000, '应交税费-销项税(9%普票)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920016, '15',  '2320', '增值税',  '普票-6%',          'PL附表拆分依据', 100.00, 0.060000, '应交税费-销项税(6%普票)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920017, '16',  '2355', '增值税',  '专票-13%',         'PL附表拆分依据', 100.00, 0.130000, '应交税费-销项税(13%)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920018, '17',  '2355', '增值税',  '专票-9%',          'PL附表拆分依据', 100.00, 0.090000, '应交税费-销项税(9%)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920019, '18',  '2355', '增值税',  '普票-13%',         'PL附表拆分依据', 100.00, 0.130000, '应交税费-销项税(13%普票)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(920020, '19',  '2355', '增值税',  '普票-9%',          'PL附表拆分依据', 100.00, 0.090000, '应交税费-销项税(9%普票)', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0);

-- =========================================================
-- 3) 项目配置表 t_project_config（20条）
--   - company_code 与 company_code_config 对齐
--   - tax_type/tax_category 与 tax_category_config 口径一致
-- =========================================================
DELETE FROM t_project_config WHERE id BETWEEN 930001 AND 930020;

INSERT INTO t_project_config (
  id, company_code, tax_type, tax_category, project_name, preferential_period,
  create_time, create_by, create_by_name, update_time, update_by, update_by_name, is_deleted
) VALUES
(930001, '2320', '增值税', '专票-13%', '临港储能EPC项目', '2026-01~2028-12', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930002, '2320', '增值税', '普票-9%',  '奉贤分布式光伏项目', '非风场项目，无优惠期', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930003, '2355', '增值税', '专票-13%', '景程综合能源站A', '2026-01~2027-12', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930004, '2355', '增值税', '普票-9%',  '景程综合能源站B', '非风场项目，无优惠期', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930005, '3019', '增值税', '销项税额', '江阴青芦工商业储能一期', '2026-01~2026-12', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930006, '3021', '增值税', '销项税额', '南京园区储能项目', '2026-01~2026-12', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930007, '3022', '增值税', '销项税额', '苏州零碳园区项目', '2026-01~2026-12', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930008, '3023', '增值税', '销项税额', '无锡用户侧储能项目', '2026-01~2026-12', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930009, '3024', '增值税', '销项税额', '常州工业园储能项目', '2026-01~2026-12', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930010, '3025', '增值税', '销项税额', '杭州工厂微网项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930011, '3026', '增值税', '销项税额', '宁波港区储能项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930012, '3027', '增值税', '销项税额', '合肥经开区储能项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930013, '3028', '增值税', '销项税额', '芜湖船厂储能项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930014, '3029', '增值税', '销项税额', '武汉园区综合能源项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930015, '3030', '增值税', '销项税额', '长沙零碳工厂项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930016, '3031', '增值税', '销项税额', '广州商储示范项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930017, '3032', '增值税', '销项税额', '深圳数据中心储能项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930018, '3033', '增值税', '销项税额', '佛山制造园区储能项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930019, '3034', '增值税', '销项税额', '成都高新区综合能源项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(930020, '3035', '增值税', '销项税额', '重庆工商业储能项目', '2026-01~2027-06', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0);

-- =========================================================
-- 4) 增值税基础条目配置表 t_vat_basic_item_config（20条）
--   - 前12条通用(NULL)
--   - 后8条为2320/2355覆盖条目
-- =========================================================
DELETE FROM t_vat_basic_item_config WHERE id BETWEEN 940001 AND 940020;

INSERT INTO t_vat_basic_item_config (
  id, item_seq, company_code, basic_item, is_split, is_display,
  create_time, create_by, create_by_name, update_time, update_by, update_by_name, is_deleted
) VALUES
(940001,  1,  NULL,  '期初未交增值税',              'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940002,  2,  NULL,  '本期销项税额',                'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940003,  3,  NULL,  '本期进项税额',                'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940004,  4,  NULL,  '进项税额转出',                'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940005,  5,  NULL,  '当期应纳税额',                'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940006,  6,  NULL,  '已交税金',                    'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940007,  7,  NULL,  '期末未交增值税',              'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940008,  8,  NULL,  '留抵税额',                    'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940009,  9,  NULL,  '加计抵减额',                  'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940010, 10,  NULL,  '免抵退税额',                  'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940011, 11,  NULL,  '本期可抵扣固定资产进项',      'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940012, 12,  NULL,  '本期认证待抵扣进项',          'N', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940013, 13, '2320', '专票销项税(13%)',             'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940014, 14, '2320', '专票销项税(9%)',              'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940015, 15, '2320', '普票销项税(13%)',             'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940016, 16, '2320', '普票销项税(9%)',              'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940017, 13, '2355', '专票销项税(13%)',             'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940018, 14, '2355', '专票销项税(9%)',              'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940019, 15, '2355', '普票销项税(13%)',             'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(940020, 16, '2355', '普票销项税(9%)',              'Y', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0);

-- =========================================================
-- 5) 增值税特殊条目配置表 t_vat_special_item_config（20条）
--   - 按需求仅给 2320 / 2355
-- =========================================================
DELETE FROM t_vat_special_item_config WHERE id BETWEEN 950001 AND 950020;

INSERT INTO t_vat_special_item_config (
  id, item_seq, company_code, special_item, is_display,
  create_time, create_by, create_by_name, update_time, update_by, update_by_name, is_deleted
) VALUES
(950001,  1, '2320', '异地预缴抵减',           'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950002,  2, '2320', '即征即退',               'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950003,  3, '2320', '留抵退税',               'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950004,  4, '2320', '加计抵减调减',           'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950005,  5, '2320', '转出未交增值税(历史追溯)', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950006,  6, '2320', '进项税额结构调整',       'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950007,  7, '2320', '项目预征补差',           'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950008,  8, '2320', '红字发票冲减调整',       'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950009,  9, '2320', '税控服务费抵减',         'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950010, 10, '2320', '汇总申报差异调节',       'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950011,  1, '2355', '异地预缴抵减',           'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950012,  2, '2355', '即征即退',               'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950013,  3, '2355', '留抵退税',               'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950014,  4, '2355', '加计抵减调减',           'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950015,  5, '2355', '转出未交增值税(历史追溯)', 'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950016,  6, '2355', '进项税额结构调整',       'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950017,  7, '2355', '项目预征补差',           'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950018,  8, '2355', '红字发票冲减调整',       'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950019,  9, '2355', '税控服务费抵减',         'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0),
(950020, 10, '2355', '汇总申报差异调节',       'Y', NOW(), 'seed_script', '初始化脚本', NOW(), 'seed_script', '初始化脚本', 0);

COMMIT;

