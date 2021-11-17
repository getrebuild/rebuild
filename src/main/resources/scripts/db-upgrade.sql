-- Database upgrade scripts for rebuild 1.x and 2.x
-- Each upgraded starts with `-- #VERSION`

-- #41 (v2.7)
-- ************ Entity [NreferenceItem] DDL ************
create table if not exists `nreference_item` (
  `ITEM_ID`            char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '哪个实体',
  `RECORD_ID`          char(20) not null comment '记录 ID',
  `REFERENCE_ID`       char(20) not null comment '引用 ID',
  primary key  (`ITEM_ID`),
  index IX0_nreference_item (`BELONG_ENTITY`, `RECORD_ID`, `REFERENCE_ID`)
)Engine=InnoDB;

-- #40 (v2.7)
alter table `role_privileges`
  change column `DEFINITION` `DEFINITION` VARCHAR(2000) NULL DEFAULT NULL COMMENT '权限定义';

-- #39 (v2.6)
alter table `project_config`
  add column `STATUS` smallint(6) default '1' comment '状态 (1=正常 2=归档)';

-- #38 (v2.6)
alter table `data_report_config`
  add column `TEMPLATE_TYPE` smallint(6) default '1' comment '模板类型 (1=记录, 2=列表)';

-- #37 (v2.5)
-- ************ Entity [ExternalUser] DDL ************
create table if not exists `external_user` (
  `USER_ID`            char(20) not null,
  `APP_USER`           varchar(100) not null,
  `APP_ID`           varchar(100) not null,
  `BIND_USER`          char(20) not null,
  primary key  (`USER_ID`),
  unique index UIX0_external_user (`APP_USER`, `APP_ID`)
)Engine=InnoDB;

-- #36 (v2.4)
-- ************ Entity [FrontjsCode] DDL ************
create table if not exists `frontjs_code` (
  `CODE_ID`            char(20) not null,
  `NAME`               varchar(100) not null comment '名称',
  `APPLY_PATH`         varchar(200) comment '匹配路径',
  `CODE`               text(21845) comment '代码',
  `ES5_CODE`           text(21845) comment 'ES5 代码',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`CODE_ID`)
)Engine=InnoDB;

-- #35 (v2.3)
alter table `project_task`
  add column `RELATED_RECORD` char(20) comment '相关业务记录',
  add index IX92_project_task (`RELATED_RECORD`, `PROJECT_ID`);

-- #34 (v2.2)
alter table `revision_history`
  add column `IP_ADDR` varchar(100) comment 'IP地址';

-- #33 (v2.2)
-- ************ Entity [ExtformConfig] DDL ************
create table if not exists `extform_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '所属实体',
  `NAME`               varchar(100) not null comment '名称',
  `PORTAL_CONFIG`      text(32767) comment '表单配置 (JSON Map)',
  `START_TIME`         timestamp null default null comment '开始时间',
  `END_TIME`           timestamp null default null comment '结束时间',
  `BIND_USER`          char(20) comment '数据绑定用户',
  `HOOK_URL`           varchar(300) comment '回调地址',
  `HOOK_SECRET`        varchar(300) comment '回调安全码',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;

-- #32 (v2.1) TaskTag
alter table `project_task_tag`
  add column `CREATED_BY` char(20) not null comment '创建人',
  add column `CREATED_ON` timestamp not null default current_timestamp comment '创建时间';
alter table `project_task_tag_relation`
  add column `CREATED_BY` char(20) not null comment '创建人',
  add column `CREATED_ON` timestamp not null default current_timestamp comment '创建时间';

-- #31 (v2.1)
-- ************ Entity [TransformConfig] DDL ************
create table if not exists `transform_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '源实体',
  `TARGET_ENTITY`      varchar(100) not null comment '目标实体',
  `NAME`               varchar(100) comment '名称',
  `CONFIG`             text(32767) comment '映射配置',
  `IS_DISABLED`        char(1) default 'F' comment '是否禁用',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;

-- #30 Language (v2.0)
-- ************ Entity [Language] DDL ************
create table if not exists `language` (
  `LANG_ID`            char(20) not null,
  `LOCALE`             varchar(10) not null comment 'Locale',
  `NAME`               varchar(100) not null comment '语言名',
  `VALUE`              varchar(300) not null comment '语言值',
  primary key  (`LANG_ID`),
  index IX0_language (`LOCALE`, `NAME`)
)Engine=InnoDB;

-- #29 (v2.0)
-- Add commons fields
alter table `project_config`
  add column `CREATED_BY` char(20) not null comment '创建人' default '001-0000000000000000',
  add column `CREATED_ON` timestamp not null default current_timestamp comment '创建时间',
  add column `MODIFIED_BY` char(20) not null comment '修改人' default '001-0000000000000000',
  add column `MODIFIED_ON` timestamp not null default current_timestamp comment '修改时间';
-- Meta config
alter table `meta_field`
  add column `QUERYABLE` char(1) default 'T';
alter table `meta_entity`
  add column `EXT_CONFIG` varchar(700) comment '更多扩展配置 (JSON Map)';

-- #28
alter table `project_config`
  add column `PRINCIPAL` char(20) comment '负责人';

-- #27 Project/Kanban (v1.11)
-- ************ Entity [ProjectConfig] DDL ************
create table if not exists `project_config` (
  `CONFIG_ID`          char(20) not null,
  `PROJECT_NAME`       varchar(100) not null comment '项目名称',
  `PROJECT_CODE`       varchar(10) not null comment '项目代号',
  `ICON_NAME`          varchar(30) comment '图标ICON',
  `COMMENTS`           varchar(300) comment '备注',
  `MEMBERS`            varchar(420) comment '项目成员($MemberID)',
  `SCOPE`              smallint(6) default '1' comment '可见范围(1=公开 2=成员)',
  `EXTRA_DEFINITION`   text(32767) comment '扩展配置(JSON Map)',
  primary key  (`CONFIG_ID`),
  unique index UIX0_project_config (`PROJECT_CODE`)
)Engine=InnoDB;
-- ************ Entity [ProjectPlanConfig] DDL ************
create table if not exists `project_plan_config` (
  `CONFIG_ID`          char(20) not null,
  `PROJECT_ID`         char(20) not null comment '所属项目',
  `PLAN_NAME`          varchar(100) not null comment '面板名称',
  `COMMENTS`           varchar(300) comment '备注',
  `SEQ`                int(11) default '0' comment '排序(小到大)',
  `FLOW_STATUS`        smallint(6) default '1' comment '工作流状态',
  `FLOW_NEXTS`         varchar(420) comment '可转换到哪个面板',
  primary key  (`CONFIG_ID`),
  index IX0_project_plan_config (`PROJECT_ID`, `SEQ`)
)Engine=InnoDB;
-- ************ Entity [ProjectTask] DDL ************
create table if not exists `project_task` (
  `TASK_ID`            char(20) not null,
  `PROJECT_ID`         char(20) not null comment '所属项目',
  `PROJECT_PLAN_ID`    char(20) not null comment '所属面板',
  `TASK_NUMBER`        bigint(20) not null comment '任务编号',
  `TASK_NAME`          varchar(191) not null comment '任务名称',
  `EXECUTOR`           char(20) comment '执行人',
  `PARTNERS`           varchar(420) default 'ALL' comment '参与者(可选值: $UserID)',
  `PRIORITY`           smallint(6) default '1' comment '优先级(0=较低 1=普通 2=紧急 3=非常紧急)',
  `STATUS`             smallint(6) default '0' comment '状态(0=未完成/未开始)',
  `DEADLINE`           timestamp null default null comment '截至时间',
  `START_TIME`         timestamp null default null comment '开始时间',
  `END_TIME`           timestamp null default null comment '完成时间',
  `DESCRIPTION`        text(32767) comment '详情',
  `ATTACHMENTS`        varchar(700) comment '附件',
  `PARENT_TASK_ID`     char(20) comment '父级任务',
  `SEQ`                int(11) default '0' comment '排序(小到大)',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`TASK_ID`),
  index IX0_project_task (`PROJECT_ID`, `PROJECT_PLAN_ID`, `SEQ`),
  index IX1_project_task (`PROJECT_ID`, `TASK_NUMBER`, `TASK_NAME`, `STATUS`)
)Engine=InnoDB;
-- ************ Entity [ProjectTaskRelation] DDL ************
create table if not exists `project_task_relation` (
  `RELATION_ID`        char(20) not null,
  `TASK_ID`            char(20) not null,
  `RELATION_TASK_ID`   char(20) not null,
  `RELATION_TYPE`      smallint(6) default '0' comment '关系类型(0=相关 1=前置 2=后置)',
  primary key  (`RELATION_ID`),
  index IX0_project_task_relation (`TASK_ID`, `RELATION_TASK_ID`)
)Engine=InnoDB;
-- ************ Entity [ProjectTaskComment] DDL ************
create table if not exists `project_task_comment` (
  `COMMENT_ID`         char(20) not null,
  `TASK_ID`            char(20) not null comment '哪个任务',
  `CONTENT`            text(32767) not null comment '内容',
  `ATTACHMENTS`        varchar(700) comment '附件',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`COMMENT_ID`),
  index IX0_project_task_comment (`TASK_ID`)
)Engine=InnoDB;
-- ************ Entity [ProjectTaskTag] DDL ************
create table if not exists `project_task_tag` (
  `TAG_ID`             char(20) not null,
  `PROJECT_ID`         char(20) not null comment '所属项目',
  `TAG_NAME`           varchar(100) not null comment '标签名',
  `COLOR`              varchar(20) comment '颜色',
  primary key  (`TAG_ID`),
  index IX0_project_task_tag (`PROJECT_ID`)
)Engine=InnoDB;
-- ************ Entity [ProjectTaskTagRelation] DDL ************
create table if not exists `project_task_tag_relation` (
  `RELATION_ID`        char(20) not null,
  `TASK_ID`            char(20) not null,
  `TAG_ID`             char(20) not null,
  primary key  (`RELATION_ID`),
  index IX0_project_task_tag_relation (`TASK_ID`, `TAG_ID`)
)Engine=InnoDB;

-- #26 (v1.11)
alter table `smsend_log`
  add column `TYPE` smallint(6) default '0' comment '1=短信; 2=邮件';

-- #25
alter table `robot_trigger_config`
  add column `WHEN_TIMER` varchar(100) comment '定期执行';

-- #24 SmsendLog
-- ************ Entity [SmsendLog] DDL ************
create table if not exists `smsend_log` (
  `SEND_ID`            char(20) not null,
  `TO`                 varchar(100) not null comment '收件人',
  `CONTENT`            text(32767) not null comment '发送内容',
  `SEND_TIME`          timestamp not null default current_timestamp comment '发送时间',
  `SEND_RESULT`        varchar(191) comment '发送结果(OK:xxx|ERR:xxx)',
  primary key  (`SEND_ID`),
  index IX0_smsend_log (`SEND_TIME`, `SEND_RESULT`)
)Engine=InnoDB;

-- #23 workphone for User
alter table `user`
  add column `WORKPHONE` varchar(100) comment '电话',
  add index IX91_user (`QUICK_CODE`, `FULL_NAME`, `EMAIL`);

-- #22 scheduleTime for Feeds
alter table `feeds`
  add column `SCHEDULE_TIME` timestamp null default null comment '日程时间',
  add index IX91_feeds (`TYPE`, `SCHEDULE_TIME`, `CREATED_BY`);

-- #21 QuickCode for ClassificationData
alter table `classification_data`
  add column `QUICK_CODE` varchar(70) default '';

-- #20 Attachment delete mark
alter table `attachment`
  add column `IS_DELETED` char(1) default 'F' comment '标记删除';

-- #19 Announcement in Feeds
alter table `feeds`
  add column `CONTENT_MORE` text(32767) comment '不同类型的扩展内容, JSON格式KV';

-- #18 Folder scope
alter table `attachment_folder`
  add column `SCOPE` varchar(20) default 'ALL' comment '哪些人可见, 可选值: ALL/SELF/$TeamID',
  add index `IX1_attachment_folder` (`SCOPE`, `CREATED_BY`);

-- #17 Team
-- ************ Entity [Team] DDL ************
create table if not exists `team` (
  `TEAM_ID`            char(20) not null,
  `NAME`               varchar(100) not null comment '团队名称',
  `PRINCIPAL_ID`       char(20) comment '负责人',
  `IS_DISABLED`        char(1) default 'F' comment '是否停用',
  `QUICK_CODE`         varchar(70),
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`TEAM_ID`)
)Engine=InnoDB;
-- ************ Entity [TeamMember] DDL ************
create table if not exists `team_member` (
  `MEMBER_ID`          char(20) not null,
  `TEAM_ID`            char(20) not null,
  `USER_ID`            char(20) not null,
  primary key  (`MEMBER_ID`),
  unique index UIX0_team_member (`TEAM_ID`, `USER_ID`)
)Engine=InnoDB;
insert into `team` (`TEAM_ID`, `NAME`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`, `QUICK_CODE`)
  values
  ('006-9000000000000001', 'RB示例团队', CURRENT_TIMESTAMP, '001-0000000000000000', CURRENT_TIMESTAMP, '001-0000000000000000', 'RBSLTD');
insert into `layout_config` (`CONFIG_ID`, `BELONG_ENTITY`, `CONFIG`, `APPLY_TYPE`, `SHARE_TO`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`)
  values
  ('013-9000000000000004', 'Team', '[{"field":"name","isFull":false},{"field":"isDisabled","isFull":false}]', 'FORM', 'ALL', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001');

-- Principal
alter table `department`
  add column `PRINCIPAL_ID` char(20) comment '负责人';

-- #16 Feeds
-- ************ Entity [Feeds] DDL ************
create table if not exists `feeds` (
  `FEEDS_ID`           char(20) not null,
  `TYPE`               smallint(6) not null default '1' comment '类型',
  `CONTENT`            text(32767) not null comment '内容',
  `IMAGES`             varchar(700) comment '图片',
  `ATTACHMENTS`        varchar(700) comment '附件',
  `RELATED_RECORD`     char(20) comment '相关业务记录',
  `SCOPE`              varchar(20) default 'ALL' comment '哪些人可见, 可选值: ALL/SELF/$TeamID',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`FEEDS_ID`),
  index IX0_feeds (`CREATED_ON`, `SCOPE`, `TYPE`, `CREATED_BY`),
  index IX1_feeds (`RELATED_RECORD`),
  fulltext index FIX2_feeds (`CONTENT`)
)Engine=InnoDB;
-- ************ Entity [FeedsComment] DDL ************
create table if not exists `feeds_comment` (
  `COMMENT_ID`         char(20) not null,
  `FEEDS_ID`           char(20) not null comment '哪个动态',
  `CONTENT`            text(32767) not null comment '内容',
  `IMAGES`             varchar(700) comment '图片',
  `ATTACHMENTS`        varchar(700) comment '附件',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  primary key  (`COMMENT_ID`),
  index IX0_feeds_comment (`FEEDS_ID`)
)Engine=InnoDB;
-- ************ Entity [FeedsLike] DDL ************
create table if not exists `feeds_like` (
  `LIKE_ID`            char(20) not null,
  `SOURCE`             char(20) not null comment '哪个动态/评论',
  `CREATED_BY`         char(20) not null comment '创建人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`LIKE_ID`),
  index IX0_feeds_like (`SOURCE`, `CREATED_BY`)
)Engine=InnoDB;
-- ************ Entity [FeedsMention] DDL ************
create table if not exists `feeds_mention` (
  `MENTION_ID`         char(20) not null,
  `FEEDS_ID`           char(20) not null comment '哪个动态',
  `COMMENT_ID`         char(20) comment '哪个评论',
  `USER`               char(20) not null comment '哪个用户',
  primary key  (`MENTION_ID`),
  index IX0_feeds_mention (`USER`, `FEEDS_ID`, `COMMENT_ID`)
)Engine=InnoDB;

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
  `RECORD_NAME`        varchar(191) not null comment '名称字段值',
  `RECORD_CONTENT`     longtext not null comment '数据',
  `DELETED_BY`         char(20) not null comment '删除人',
  `DELETED_ON`         timestamp not null default current_timestamp comment '删除时间',
  `CHANNEL_WITH`       char(20) comment '删除渠道(空为直接删除，否则为关联删除)',
  primary key  (`RECYCLE_ID`),
  index IX0_recycle_bin (`BELONG_ENTITY`, `RECORD_NAME`, `DELETED_BY`, `DELETED_ON`),
  index IX1_recycle_bin (`RECORD_ID`, `CHANNEL_WITH`)
)Engine=InnoDB;
-- ************ Entity [RevisionHistory] DDL ************
create table if not exists `revision_history` (
  `REVISION_ID`        char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '所属实体',
  `RECORD_ID`          char(20) not null comment '记录ID',
  `REVISION_TYPE`      smallint(6) default '1' comment '变更类型',
  `REVISION_CONTENT`   longtext not null comment '变更数据',
  `REVISION_BY`        char(20) not null comment '操作人',
  `REVISION_ON`        timestamp not null default current_timestamp comment '操作时间',
  `CHANNEL_WITH`       char(20) comment '变更渠道(空为直接，否则为关联)',
  primary key  (`REVISION_ID`),
  index IX0_revision_history (`BELONG_ENTITY`, `REVISION_TYPE`, `REVISION_BY`, `REVISION_ON`),
  index IX1_revision_history (`RECORD_ID`, `CHANNEL_WITH`)
)Engine=InnoDB;

-- #10 Reports (v1.5)
-- ************ Entity [DataReportConfig] DDL ************
create table if not exists `data_report_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null comment '应用实体',
  `NAME`               varchar(100) not null comment '报表名称',
  `TEMPLATE_FILE`      varchar(200) comment '模板文件',
  `TEMPLATE_CONTENT`   text(32767) comment '模板内容',
  `IS_DISABLED`        char(1) default 'F' comment '是否停用',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
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
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  primary key  (`UNIQUE_ID`),
  unique index UIX0_rebuild_api (`APP_ID`)
)Engine=InnoDB;
-- ************ Entity [RebuildApiRequest] DDL ************
create table if not exists `rebuild_api_request` (
  `REQUEST_ID`         char(20) not null,
  `APP_ID`             varchar(20) not null comment 'APPID',
  `REMOTE_IP`          varchar(100) not null comment '来源IP',
  `REQUEST_URL`        varchar(300) not null comment '请求URL',
  `REQUEST_BODY`       text(32767) comment '请求数据',
  `RESPONSE_BODY`      text(32767) not null comment '响应数据',
  `REQUEST_TIME`       timestamp not null default current_timestamp comment '请求时间',
  `RESPONSE_TIME`      timestamp not null default current_timestamp comment '响应时间',
  primary key  (`REQUEST_ID`),
  index IX0_rebuild_api_request (`APP_ID`, `REMOTE_IP`, `REQUEST_TIME`)
)Engine=InnoDB;

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
  `FLOW_DEFINITION`    text(32767) comment '流程定义',
  `IS_DISABLED`        char(1) default 'F' comment '是否停用',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
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
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`STEP_ID`),
  index IX0_robot_approval_step (`RECORD_ID`, `APPROVAL_ID`, `NODE`, `IS_CANCELED`, `IS_WAITING`)
)Engine=InnoDB;

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
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;
-- ************ Entity [RobotTriggerConfig] DDL ************
create table if not exists `robot_trigger_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null,
  `WHEN`               int(11) default '0' comment '动作(累加值)',
  `WHEN_FILTER`        text(32767) comment '附加过滤器',
  `ACTION_TYPE`        varchar(50) not null comment '预定义的触发操作类型',
  `ACTION_CONTENT`     text(32767) comment '预定义的触发操作类型, JSON KV 对',
  `PRIORITY`           int(11) default '1' comment '执行优先级, 越大越高(越先执行)',
  `NAME`               varchar(100) comment '触发器名称',
  `IS_DISABLED`        char(1) default 'F' comment '是否停用',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`CONFIG_ID`)
)Engine=InnoDB;

-- #3 Example User and Role (v1.2)
insert into `user` (`USER_ID`, `LOGIN_NAME`, `PASSWORD`, `FULL_NAME`, `DEPT_ID`, `ROLE_ID`, `IS_DISABLED`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`, `QUICK_CODE`)
  values ('001-9000000000000001', 'rebuild', 'cf44886e54f424ce136dc38e4d9ef5b4b556d06060705262d6fcce02b4322539', 'RB示例用户', '002-9000000000000001', '003-9000000000000001', 'F', CURRENT_TIMESTAMP, '001-0000000000000000', CURRENT_TIMESTAMP, '001-0000000000000000', 'RBSLYH');
insert into `department` (`DEPT_ID`, `NAME`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`, `QUICK_CODE`)
  values ('002-9000000000000001', 'RB示例部门', CURRENT_TIMESTAMP, '001-0000000000000000', CURRENT_TIMESTAMP, '001-0000000000000000', 'RBSLBM');
insert into `role` (`ROLE_ID`, `NAME`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`, `QUICK_CODE`)
  values ('003-9000000000000001', 'RB示例角色', CURRENT_TIMESTAMP, '001-0000000000000000', CURRENT_TIMESTAMP, '001-0000000000000000', 'RBSLJS');

-- #2 for Classification field (v1.1)
-- ************ Entity [Classification] DDL ************
create table if not exists `classification` (
  `DATA_ID`            char(20) not null,
  `NAME`               varchar(100) not null,
  `DESCRIPTION`        varchar(600),
  `IS_DISABLED`        char(1) default 'F',
  `OPEN_LEVEL`         smallint(6) default '0',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  primary key  (`DATA_ID`)
)Engine=InnoDB;
-- ************ Entity [ClassificationData] DDL ************
create table if not exists `classification_data` (
  `ITEM_ID`            char(20) not null,
  `DATA_ID`            char(20) not null,
  `NAME`               varchar(100) not null,
  `FULL_NAME`          varchar(191) not null comment '包括父级名称, 用点号分割',
  `PARENT`             char(20),
  `CODE`               varchar(50),
  `LEVEL`              smallint(6) default '0',
  `IS_HIDE`            char(1) default 'F',
  `CREATED_ON`         timestamp not null default current_timestamp comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `MODIFIED_ON`        timestamp not null default current_timestamp comment '修改时间',
  primary key  (`ITEM_ID`),
  index IX0_classification_data (`DATA_ID`, `PARENT`),
  index IX1_classification_data (`DATA_ID`, `FULL_NAME`)
)Engine=InnoDB;
insert into `classification` (`DATA_ID`, `NAME`, `DESCRIPTION`, `OPEN_LEVEL`, `IS_DISABLED`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`)
  values
  ('018-0000000000000001', '地区', NULL, 2, 'F', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001'),
  ('018-0000000000000002', '行业', NULL, 1, 'F', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001');

-- #1 Add LoginLog table (v1.1)
-- ************ Entity [LoginLog] DDL ************
create table if not exists `login_log` (
  `LOG_ID`             char(20) not null,
  `USER`               char(20) not null comment '登陆用户',
  `IP_ADDR`            varchar(100) comment 'IP地址',
  `USER_AGENT`         varchar(200) comment '客户端',
  `LOGIN_TIME`         timestamp not null default current_timestamp comment '登陆时间',
  `LOGOUT_TIME`        timestamp null default null comment '退出时间',
  primary key  (`LOG_ID`),
  index IX0_login_log (`USER`, `LOGIN_TIME`)
)Engine=InnoDB;
insert into `layout_config` (`CONFIG_ID`, `BELONG_ENTITY`, `CONFIG`, `APPLY_TYPE`, `SHARE_TO`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`)
  values ('013-9000000000000005', 'LoginLog', '[{"field":"user"},{"field":"loginTime"},{"field":"userAgent"},{"field":"ipAddr"},{"field":"logoutTime"}]', 'DATALIST', 'ALL', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001');
