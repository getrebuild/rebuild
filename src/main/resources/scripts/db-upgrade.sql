-- Database upgrade scripts for rebuild 1.x
-- Each upgraded starts with `-- #VERSION`

-- #15 Widget in page (v1.6)
-- UNDO

-- #14 Name for LayoutConfig (v1.6)
alter table `layout_config`
    add column `CONFIG_NAME` varchar(100);

-- #13 MultiSelect supports by PickList (v1.6)
alter table `pick_list`
  add column `MASK_VALUE` bigint(20) default '0' comment 'MultiSelect专用',
  add index  `IX1_pick_list` (`BELONG_ENTITY`, `BELONG_FIELD`);

-- #12 Field can be repeated (v1.5)
alter table `meta_field`
    add column `REPEATABLE` char(1) default 'T';

-- #11 Audit (v1.5)
-- ************ Entity [RecycleBin] DDL ************
create table if not exists `recycle_bin` (
  `RECYCLE_ID`         char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '所属实体',
  `RECORD_ID`          char(20) not null comment 'ID字段值',
  `RECORD_NAME`        varchar(200) not null comment '名称字段值',
  `RECORD_CONTENT`     longtext not null comment '数据',
  `DELETED_BY`         char(20) not null comment '删除人',
  `DELETED_ON`         timestamp not null default current_timestamp comment '删除时间',
  `CHANNEL_WITH`       char(20) comment '删除渠道（空为直接删除，否则为关联删除）',
  primary key  (`RECYCLE_ID`)
)Engine=InnoDB;
alter table `recycle_bin`
  add index `IX1_recycle_bin` (`BELONG_ENTITY`, `RECORD_NAME`, `DELETED_BY`, `DELETED_ON`),
  add index `IX2_recycle_bin` (`RECORD_ID`, `CHANNEL_WITH`);
-- ************ Entity [RevisionHistory] DDL ************
create table if not exists `revision_history` (
  `REVISION_ID`        char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '所属实体',
  `RECORD_ID`          char(20) not null comment '记录ID',
  `REVISION_TYPE`      smallint(6) default '1' comment '变更类型',
  `REVISION_CONTENT`   longtext not null comment '变更数据',
  `REVISION_BY`        char(20) not null comment '操作人',
  `REVISION_ON`        timestamp not null default current_timestamp comment '操作时间',
  `CHANNEL_WITH`       char(20) comment '变更渠道（空为直接，否则为关联）',
  primary key  (`REVISION_ID`)
)Engine=InnoDB;
alter table `revision_history`
  add index `IX1_revision_history` (`BELONG_ENTITY`, `REVISION_TYPE`, `REVISION_BY`, `REVISION_ON`),
  add index `IX2_revision_history` (`RECORD_ID`, `CHANNEL_WITH`);

-- #10 Reports (v1.5)
-- ************ Entity [DataReportConfig] DDL ************
create table if not exists `data_report_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '应用实体',
  `NAME`               varchar(100) not null comment '报表名称',
  `TEMPLATE_FILE`      varchar(200) comment '模板文件',
  `TEMPLATE_CONTENT`   text(20000) comment '模板内容',
  `IS_DISABLED`        char(1) default 'F' comment '是否停用',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;

-- #9 Add name and isDisabled to RobotTriggerConfig (v1.4)
alter table `robot_trigger_config`
    add column `NAME` varchar(100) comment '触发器名称',
    add column `IS_DISABLED` char(1) default 'F' comment '是否停用';

-- #8 API (v1.4)
-- ************ Entity [RebuildApi] DDL ************
create table if not exists `rebuild_api` (
  `UNIQUE_ID`          char(20) not null,
  `APP_ID`             varchar(20) not null comment 'APPID',
  `APP_SECRET`         varchar(60) not null comment 'APPSECRET',
  `BIND_USER`          char(20) comment '绑定用户(权限)',
  `BIND_IPS`           varchar(300) comment 'IP白名单',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`UNIQUE_ID`),
  unique index `UIX1_rebuild_api` (`APP_ID`)
)Engine=InnoDB;
-- ************ Entity [RebuildApiRequest] DDL ************
create table if not exists `rebuild_api_request` (
  `REQUEST_ID`         char(20) not null,
  `APP_ID`             varchar(20) not null comment 'APPID',
  `REMOTE_IP`          varchar(100) not null comment '来源IP',
  `REQUEST_URL`        varchar(300) not null comment '请求URL',
  `REQUEST_BODY`       text(10000) comment '请求数据',
  `RESPONSE_BODY`      text(10000) not null comment '响应数据',
  `REQUEST_TIME`       timestamp not null default current_timestamp comment '请求时间',
  `RESPONSE_TIME`      timestamp not null default current_timestamp comment '响应时间',
  primary key  (`REQUEST_ID`)
)Engine=InnoDB;
alter table `rebuild_api_request`
  add index `IX1_rebuild_api_request` (`APP_ID`, `REMOTE_IP`, `REQUEST_URL`, `REQUEST_TIME`);

-- #7 Type of Notification
alter table `notification`
  add column `TYPE` smallint(6) default '0' comment '消息分类',
  add index `IX2_notification` (`TO_USER`, `TYPE`, `CREATED_ON`);

-- #6 Approval (v1.4)
-- ************ Entity [RobotApprovalConfig] DDL ************
create table if not exists `robot_approval_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '应用实体',
  `NAME`               varchar(100) not null comment '流程名称',
  `FLOW_DEFINITION`    text(21845) comment '流程定义',
  `IS_DISABLED`        char(1) default 'F' comment '是否停用',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_BY`         char(20) not null comment '创建人',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;
-- ************ Entity [RobotApprovalStep] DDL ************
create table if not exists `robot_approval_step` (
  `STEP_ID`            char(20) not null,
  `RECORD_ID`          char(20) not null comment '审批记录',
  `APPROVAL_ID`        char(20) not null comment '审批流程',
  `NODE`               varchar(100) not null comment '审批节点',
  `APPROVER`           char(20) not null comment '审批人',
  `STATE`              smallint(6) default '1' comment '审批结果',
  `REMARK`             varchar(600) comment '批注',
  `APPROVED_TIME`      timestamp null default null comment '审批时间',
  `PREV_NODE`          varchar(100) not null comment '上一审批节点',
  `IS_CANCELED`        char(1) default 'F' comment '是否取消',
  `IS_WAITING`         char(1) default 'F' comment '是否生效',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`STEP_ID`)
)Engine=InnoDB;
alter table `robot_approval_step`
  add index `IX1_robot_approval_step` (`RECORD_ID`, `APPROVAL_ID`, `NODE`, `IS_CANCELED`, `IS_WAITING`);

-- #5 Classification better (v1.3)
alter table `classification_data`
  add column `LEVEL` smallint(6) default '0',
  add column `IS_HIDE` char(1) default 'F';

-- #4 for AutoFillin/Trigger feature (v1.3)
-- ************ Entity [AutoFillinConfig] DDL ************
create table if not exists `auto_fillin_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null,
  `BELONG_FIELD`       varchar(100) not null,
  `SOURCE_FIELD`       varchar(100) not null comment '引用实体的字段',
  `TARGET_FIELD`       varchar(100) not null comment '当前实体的字段',
  `EXT_CONFIG`         varchar(700) comment '更多扩展配置, JSON格式KV',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;
-- ************ Entity [RobotTriggerConfig] DDL ************
create table if not exists `robot_trigger_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null,
  `WHEN`               int(11) default '0' comment '动作 (累加值)',
  `WHEN_FILTER`        text(21845) comment '附加过滤器',
  `ACTION_TYPE`        varchar(50) not null comment '预定义的触发操作类型',
  `ACTION_CONTENT`     text(21845) comment '预定义的触发操作类型, JSON KV 对',
  `PRIORITY`           int(11) default '1' comment '执行优先级, 越大越高(越先执行)',
  `NAME`               varchar(100) comment '触发器名称',
  `IS_DISABLED`        char(1) default 'F' comment '是否停用',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;

-- #3 Example User and Role (v1.2)
INSERT INTO `user` (`USER_ID`, `LOGIN_NAME`, `PASSWORD`, `FULL_NAME`, `DEPT_ID`, `ROLE_ID`, `IS_DISABLED`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`, `QUICK_CODE`)
  VALUES ('001-9000000000000001', 'rebuild', 'cf44886e54f424ce136dc38e4d9ef5b4b556d06060705262d6fcce02b4322539', 'RB示例用户', '002-9000000000000001', '003-9000000000000001', 'F', CURRENT_TIMESTAMP, '001-0000000000000000', CURRENT_TIMESTAMP, '001-0000000000000000', 'RBSLYH');
INSERT INTO `department` (`DEPT_ID`, `NAME`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`, `QUICK_CODE`)
  VALUES ('002-9000000000000001', 'RB示例部门', CURRENT_TIMESTAMP, '001-0000000000000000', CURRENT_TIMESTAMP, '001-0000000000000000', 'RBSLBM');
INSERT INTO `role` (`ROLE_ID`, `NAME`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`, `QUICK_CODE`)
  VALUES ('003-9000000000000001', 'RB示例角色', CURRENT_TIMESTAMP, '001-0000000000000000', CURRENT_TIMESTAMP, '001-0000000000000000', 'RBSLJS');

-- #2 for Classification field (v1.1)
-- ************ Entity [Classification] DDL ************
create table if not exists `classification` (
  `DATA_ID`            char(20) not null,
  `NAME`               varchar(100) not null,
  `DESCRIPTION`        varchar(600),
  `IS_DISABLED`        char(1) default 'F',
  `OPEN_LEVEL`         smallint(6) default '0',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  primary key  (`DATA_ID`)
)Engine=InnoDB;
-- ************ Entity [ClassificationData] DDL ************
create table if not exists `classification_data` (
  `ITEM_ID`            char(20) not null,
  `DATA_ID`            char(20) not null,
  `NAME`               varchar(100) not null,
  `FULL_NAME`          varchar(300) not null comment '包括父级名称, 用点号分割',
  `PARENT`             char(20),
  `CODE`               varchar(50),
  `LEVEL`              smallint(6) default '0',
  `IS_HIDE`            char(1) default 'F',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  primary key  (`ITEM_ID`)
)Engine=InnoDB;
alter table `classification_data`
  add index `IX1_classification_data` (`DATA_ID`, `PARENT`),
  add index `IX2_classification_data` (`DATA_ID`, `FULL_NAME`);
INSERT INTO `classification` (`DATA_ID`, `NAME`, `DESCRIPTION`, `OPEN_LEVEL`, `IS_DISABLED`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`) 
  VALUES
  ('018-0000000000000001', '地区', NULL, 2, 'F', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001'),
  ('018-0000000000000002', '行业', NULL, 1, 'F', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001');
  
-- #1 Add LoginLog table (v1.1)
-- ************ Entity [LoginLog] DDL ************
create table if not exists `login_log` (
  `LOG_ID`             char(20) not null,
  `USER`               char(20) not null comment '登陆用户',
  `IP_ADDR`            varchar(100) comment 'IP地址',
  `USER_AGENT`         varchar(100) comment '客户端',
  `LOGIN_TIME`         timestamp not null default current_timestamp comment '登陆时间',
  `LOGOUT_TIME`        timestamp null default null comment '退出时间',
  primary key  (`LOG_ID`)
)Engine=InnoDB;
alter table `login_log`
  add index `IX1_login_log` (`USER`, `LOGIN_TIME`);
INSERT INTO `layout_config` (`CONFIG_ID`, `BELONG_ENTITY`, `CONFIG`, `APPLY_TYPE`, `SHARE_TO`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`)
  VALUES (CONCAT('013-',SUBSTRING(MD5(RAND()),1,16)), 'LoginLog', '[{"field":"user"},{"field":"loginTime"},{"field":"userAgent"},{"field":"ipAddr"},{"field":"logoutTime"}]', 'DATALIST', 'ALL', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001');
