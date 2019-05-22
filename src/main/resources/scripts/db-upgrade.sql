-- Database upgrade scripts for rebuild 1.x
-- Each upgraded starts with `-- #VERSION`

-- #4 for AutoFillin feature
-- ************ Entity [AutoFillinConfig] DDL ************
create table if not exists `auto_fillin_config` (
  `CONFIG_ID`          char(20) not null,
  `BELONG_ENTITY`      varchar(100) not null,
  `BELONG_FIELD`       varchar(100) not null,
  `SOURCE_FIELD`       varchar(100) not null comment '引用实体的字段',
  `TARGET_FIELD`       varchar(100) not null comment '当前实体的字段',
  `EXT_CONFIG`         varchar(700) comment '更多扩展配置, JSON格式KV',
  `CREATED_ON`         timestamp not null default '0000-00-00 00:00:00' comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_ON`        timestamp not null default '0000-00-00 00:00:00' comment '修改时间',
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
  `OPEN_LEVEL`         smallint(6) default '0',
  `IS_DISABLED`        char(1) default 'F',
  `DESCRIPTION`        varchar(600),
  `CREATED_ON`         timestamp not null default '0000-00-00 00:00:00' comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_ON`        timestamp not null default '0000-00-00 00:00:00' comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`DATA_ID`)
)Engine=InnoDB;
-- ************ Entity [ClassificationData] DDL ************
create table if not exists `classification_data` (
  `ITEM_ID`            char(20) not null,
  `DATA_ID`            char(20) not null,
  `CODE`               varchar(50),
  `NAME`               varchar(100) not null,
  `FULL_NAME`          varchar(300) not null comment '包括父级名称, 用 . 分割',
  `PARENT`             char(20),
  `CREATED_ON`         timestamp not null default '0000-00-00 00:00:00' comment '创建时间',
  `CREATED_BY`         char(20) not null comment '创建人',
  `MODIFIED_ON`        timestamp not null default '0000-00-00 00:00:00' comment '修改时间',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  primary key  (`ITEM_ID`)
)Engine=InnoDB;
alter table `classification_data`
  add index `IX1_classification_data` (`DATA_ID`, `PARENT`, `NAME`),
  add index `IX2_classification_data` (`DATA_ID`, `FULL_NAME`);
INSERT INTO `classification` (`DATA_ID`, `NAME`, `DESCRIPTION`, `OPEN_LEVEL`, `IS_DISABLED`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`) 
  VALUES
  ('018-0000000000000001', '地区', NULL, 2, 'F', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001'),
  ('018-0000000000000002', '行业', NULL, 1, 'F', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001');
  
-- #1 Add LoginLog table (v1.1)
create table if not exists `login_log` (
  `LOG_ID`             char(20) not null,
  `USER`               char(20) not null comment '登陆用户',
  `USER_AGENT`         varchar(100) comment '客户端',
  `IP_ADDR`            varchar(100) comment 'IP地址',
  `LOGOUT_TIME`        timestamp null default '0000-00-00 00:00:00' comment '退出时间',
  `LOGIN_TIME`         timestamp not null default '0000-00-00 00:00:00' comment '登陆时间',
  primary key  (`LOG_ID`)
)Engine=InnoDB;
alter table `login_log`
  add index `IX1_login_log` (`USER`, `LOGIN_TIME`);
INSERT INTO `layout_config` (`CONFIG_ID`, `BELONG_ENTITY`, `CONFIG`, `APPLY_TYPE`, `SHARE_TO`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`)
  VALUES (CONCAT('013-',SUBSTRING(MD5(RAND()),1,16)), 'LoginLog', '[{"field":"user"},{"field":"loginTime"},{"field":"userAgent"},{"field":"ipAddr"},{"field":"logoutTime"}]', 'DATALIST', 'ALL', CURRENT_TIMESTAMP, '001-0000000000000001', CURRENT_TIMESTAMP, '001-0000000000000001');
