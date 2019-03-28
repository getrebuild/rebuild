-- Database upgrade scripts for rebuild 1.x
-- Each upgraded starts with `-- #VERSION`

-- #2 for Classification field
create table if not exists `classification` (
  `MODIFIED_ON`        timestamp not null default '0000-00-00 00:00:00' comment '修改时间',
  `DATA_ID`            char(20) not null,
  `CREATED_BY`         char(20) not null comment '创建人',
  `NAME`               varchar(100) not null,
  `DESCRIPTION`        varchar(600),
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `IS_DISABLED`        char(1) default 'F',
  `CREATED_ON`         timestamp not null default '0000-00-00 00:00:00' comment '创建时间',
  primary key  (`DATA_ID`)
)Engine=InnoDB;
create table if not exists `classification_data` (
  `ITEM_ID`            char(20) not null,
  `PARENT`             char(20),
  `MODIFIED_ON`        timestamp not null default '0000-00-00 00:00:00' comment '修改时间',
  `CODE`               varchar(50),
  `DATA_ID`            char(20) not null,
  `CREATED_BY`         char(20) not null comment '创建人',
  `NAME`               varchar(100) not null,
  `FULL_NAME`          varchar(200) not null comment '父级fullName+fullName',
  `MODIFIED_BY`        char(20) not null comment '修改人',
  `CREATED_ON`         timestamp not null default '0000-00-00 00:00:00' comment '创建时间',
  primary key  (`ITEM_ID`)
)Engine=InnoDB;
alter table `classification_data`
  add index `IX1_classification_data` (`DATA_ID`, `PARENT`, `NAME`, `FULL_NAME`);

-- #1 Add LoginLog table (v1.1)
create table if not exists `login_log` (
  `LOGOUT_TIME`        timestamp null default '0000-00-00 00:00:00' comment '退出时间',
  `LOGIN_TIME`         timestamp not null default '0000-00-00 00:00:00' comment '登陆时间',
  `LOG_ID`             char(20) not null,
  `USER_AGENT`         varchar(100) comment '客户端',
  `USER`               char(20) not null comment '登陆用户',
  `IP_ADDR`            varchar(100) comment 'IP地址',
  primary key  (`LOG_ID`)
)Engine=InnoDB;
alter table `login_log`
  add index `IX1_login_log` (`USER`, `LOGIN_TIME`);
INSERT INTO `layout_config` (`CONFIG_ID`, `BELONG_ENTITY`, `APPLY_TYPE`, `CONFIG`, `SHARE_TO`, `CREATED_ON`, `CREATED_BY`, `MODIFIED_ON`, `MODIFIED_BY`) VALUES ('013-016933622dc30002', 'LoginLog', 'DATALIST', '[{"field":"user"},{"field":"loginTime"},{"field":"userAgent"},{"field":"ipAddr"},{"field":"logoutTime"}]', 'ALL', '2019-02-28 17:14:26', '001-0000000000000001', '2019-02-28 17:37:36', '001-0000000000000001');