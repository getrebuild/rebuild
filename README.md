[![Codacy](https://api.codacy.com/project/badge/Grade/599a0a3e46f84e6bbc29e8fbe4632860)](https://www.codacy.com/app/getrebuild/rebuild)
[![codecov](https://codecov.io/gh/getrebuild/rebuild/branch/master/graph/badge.svg)](https://codecov.io/gh/getrebuild/rebuild)
[![Build Status](https://travis-ci.com/getrebuild/rebuild.svg?branch=master)](https://travis-ci.com/getrebuild/rebuild)
[![License GPLv3](https://img.shields.io/github/license/getrebuild/rebuild.svg)](https://getrebuild.com/legal/service-terms)
[![License 商业授权](https://img.shields.io/badge/license-%E5%95%86%E4%B8%9A%E6%8E%88%E6%9D%83-red.svg)](https://getrebuild.com/legal/service-terms)
[![Docker Image Version](https://img.shields.io/docker/v/getrebuild/rebuild?label=Docker%20Image)](https://www.getrebuild.com/learn/install-use-docker)

## 项目简介

**_相较于同类产品，REBUILD 更侧重于业务需求实现，而非基础的技术框架或项目启动模板。_**

「开放式设计」是 REBUILD 的重要设计理念！得益于团队成熟的企业管理系统经验，我们实现了对企业日常各类需求的可配置化管理，全图形化设计零代码搭建，所见即所得。

> **福利：加入 REBUILD VIP 用户 QQ 交流群 744487038 1013051587 GET 使用技能**

### 为什么需要 REBUILD

相较于传统软件系统，REBUILD 提供了绝佳的灵活性与可控性，可以完全按照企业需求进行量身打造。同时，当系统投入使用一段时间后会遇到业务变化或需求变更，通过 REBUILD 提供的高度可配置化能力，可快速完成需求变更而无需额外投入。

### REBUILD 适合哪类用户

REBUILD 适合需要灵活搭建业务系统的企业 IT 团队、缺乏专职开发资源的中小企业、特定业务领域的管理者、从传统系统迁移或升级的企业，或是开发者或技术合作伙伴。

#### 企业 IT 团队

- 场景：快速搭建 CRM/MES/WMS 等业务系统
- 优势：零代码配置业务实体、权限、流程
- 用户：IT 管理员、实施顾问

#### 中小企业

- 场景：缺乏开发资源与预算
- 优势：开源免费版满足基础需求，商业版提供更多高阶功能

#### 业务管理者（典型应用）

- 销售（客户/商机管理）
- 供应链（库存/出入库）
- 生产（工单/进度监控）

#### 系统升级企业

- 场景：替换老旧 OA/Excel 管理
- 能力：数据迁移、多实体关联、审计追踪

#### 开发者

- 扩展：Java/SpringBoot 二次开发，提供 OpenAPI 集成外部系统
- 部署：支持 Docker 私有云或本地部署

更多详情介绍 [https://getrebuild.com/learn/declaration](https://getrebuild.com/learn/declaration)

## V4.2 新特性

本次更新为你带来众多功能增强与优化。

1. [新增] 主实体明细实体布局绑定
2. [新增] 字段/实体支持拼音搜索
3. [新增] 外部表单支持手机/邮箱验证码
4. [新增] 审批支持批量提交
5. [新增] Office 文件支持在线协同编辑
6. [新增] 多个 FrontJS 函数
7. [新增] 图表支持多彩背景色
8. [优化] 20+ 细节/BUG/安全性更新
9. ...

更多更新详情请参见 [更新日志](https://getrebuild.com/docs/dev/changelog?v=4.1)

## 在线体验

[https://nightly.getrebuild.com/](https://nightly.getrebuild.com/)

> 默认超级管理员用户名密码为 `admin` `admin`

## 使用

开始使用 REBUILD 非常简单，无需配置复杂的运行环境，零依赖快速部署！

### 1. 使用已发布版本

_生产环境强烈推荐使用此方式 !!!_

首先 [下载](https://getrebuild.com/download) 安装包，我们同时提供 `standalone` 与 `boot` 两种安装包。`standalone` 为集成安装包（推荐），`boot` 为 SpringBoot 的 `jar` 包，两种安装包在功能上没有区别。

下载后解压（集成安装包），通过 `start-rebuild.bat` 或 `start-rebuild.sh` 启动，然后打开浏览器输入 [http://localhost:18080/](http://localhost:18080/) 开始体验。

或者您也可以 [使用 Docker 安装](https://getrebuild.com/learn/install-use-docker)。更多详情请参见 [安装文档](https://getrebuild.com/learn/install)

### 2. 通过源码编译

_注意 !!! 生产环境请使用 `master` 分支（默认分支），其他分支为开发分支，功能存在不确定性！_

```
# 拉取
git clone --depth=1 https://github.com/getrebuild/rebuild.git

# 编译
mvn package

# 运行
java -jar target/rebuild.jar
```

运行后打开浏览器输入 [http://localhost:18080/](http://localhost:18080/) 开始体验。

## 开发

REBUILD 从 2.0 版本开始支持 `jar` 与 `war` 两种打包/运行模式，两种模式在开发与使用上没有区别。默认情况下使用 SpringBoot `jar` 模式，启动类为 [BootApplication](https://github.com/getrebuild/rebuild/blob/master/src/main/java/com/rebuild/core/BootApplication.java) 。

如你希望使用外部 Tomcat（或其他 Java Web 容器） 即 `war` 方式，请将 `pom.xml` 文件中注释为 `UNCOMMENT USE TOMCAT` 的下一行取消注释。

### 开发环境

REBUILD 对于开发环境的要求非常简单，由于使用 Java 开发，因此可以运行在几乎所有操作系统上。请按如下清单准备：

- JDK 1.8+（兼容 OpenJDK）
- MySQL 5.6+
- Apache Maven 3.6+（非必须，IDE 自带）
- Redis 3.2+（非必须，默认使用内置 Ehcache 缓存）
- Tomcat 8.0+（非必须，默认使用内置 Tomcat）
- IDEA 或 Eclipse (for JEE)

更多详情请参见 [开发人员文档](https://getrebuild.com/docs/dev/)

## 授权 License

REBUILD 使用 GPL-3.0 开源许可和商业授权双重授权协议，使用将被视为你自愿承诺接受 [用户服务协议](https://getrebuild.com/legal/service-terms) 之所有条款。

REBUILD uses the GPL-3.0 open source license and commercial license dual license agreement. Use will be deemed your voluntary commitment to accept all terms of the [Agreement](https://getrebuild.com/legal/service-terms).

## 购买商业授权

从 2.0 版本开始，REBUILD 将推出 [增值功能](https://getrebuild.com/docs/rbv-features) 计划。如果 REBUILD 对贵公司业务有帮助，请考虑 [购买商业授权](https://getrebuild.com/#pricing-plans) 以支持 REBUILD 可持续发展。除了可享有全部功能以外，还可以得到更专业的技术支持服务。非常感谢！
