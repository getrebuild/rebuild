-- Upgrade SQL scripts for rebuild 1.x
-- Each upgraded starts with `-- #VERSION`

-- #2 Add new scripts below

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
