[![Codacy](https://api.codacy.com/project/badge/Grade/599a0a3e46f84e6bbc29e8fbe4632860)](https://www.codacy.com/app/getrebuild/rebuild)
[![codecov](https://codecov.io/gh/getrebuild/rebuild/branch/master/graph/badge.svg)](https://codecov.io/gh/getrebuild/rebuild)
[![Package](https://github.com/getrebuild/rebuild/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/getrebuild/rebuild/actions/workflows/maven-publish.yml)
[![Build Status](https://travis-ci.com/getrebuild/rebuild.svg?branch=master)](https://travis-ci.com/getrebuild/rebuild)
[![License GPLv3](https://img.shields.io/github/license/getrebuild/rebuild.svg)](LICENSE)
[![License COMMERCIAL](https://img.shields.io/badge/license-COMMERCIAL-orange.svg)](COMMERCIAL)

## 项目特色

REBUILD 通过领先的业务流程引擎为你快速搭建企业管理系统。REBUILD 侧重于业务需求实现，而非基础的技术框架或项目启动模板，通过 REBUILD 可以真正实现零代码快速搭建，无需编程、无需编译代码，甚至无需了解技术。

更多详情介绍 [https://getrebuild.com/](https://getrebuild.com/)

> **福利：加入 REBUILD QQ 交流群 819865721 GET 使用技能**

## V3.0 新特性

本次更新为你带来众多功能增强与优化。

1. [新增] 自动审批新增“提交模式”
2. [新增] 记录转换新增“预览模式”、同时支持为转换字段设置固定值
3. [新增] 审批流程支持驳回到指定步骤
4. [新增] 时间类型字段
5. [新增] 父级级联（明细实体）字段可选择主实体字段
6. [优化] 免费版用户可自定义 LOGO、系统名称
7. [优化] 视图相关项支持设置过滤条件（如仅显示审批通过的记录）
8. ...

更多新特性请参见 [更新日志](https://getrebuild.com/docs/dev/changelog)

## 在线体验

[https://nightly.getrebuild.com/](https://nightly.getrebuild.com/)

> 默认的管理员用户名密码均为 `admin`，普通用户名密码均为 `rebuild`

## 使用

开始使用 REBUILD 非常简单，不需要搭建复杂的运行环境，零依赖快速部署，超简单！

#### 1. 使用已发布版本

_生产环境强烈推荐使用此方式 !!!_

首先 [下载](https://getrebuild.com/download) 安装包，我们同时提供 `standalone` 与 `boot` 两种安装包。`standalone` 为集成安装包（推荐），`boot` 为 SpringBoot 的 `jar` 包，两种安装包在功能上没有区别。

下载后解压（集成安装包），通过 `start-rebuild.bat` 或 `start-rebuild.sh` 启动， 打开浏览器输入 [http://localhost:18080/](http://localhost:18080/) 开始体验。

更多详情请参见 [帮助文档](https://getrebuild.com/docs/)

#### 2. 通过源码编译

_注意 !!! 生产环境请使用 `master` 分支（默认分支），其他分支为开发分支，功能存在不确定性！_

```
# 拉取
git clone --depth=1 https://github.com/getrebuild/rebuild.git

# 编译
mvn package

# 运行
java -jar target/rebuild.jar
```

打开浏览器输入 [http://localhost:18080/](http://localhost:18080/) 开始体验。

## 开发

REBUILD 从 2.0 版本开始支持 `jar` 与 `war` 两种打包/运行模式，两种模式在开发与使用上并无区别。默认情况下使用 SpringBoot `jar` 模式，启动类为 [BootApplication](https://github.com/getrebuild/rebuild/blob/master/src/main/java/com/rebuild/core/BootApplication.java) 。

如你希望使用外部 Tomcat（或其他 Java Web 容器） 即 `war` 方式，请将 `pom.xml` 文件中注释为 `UNCOMMENT USE TOMCAT` 的下一行取消注释。

#### 开发环境

REBUILD 对于开发环境的要求很简单，由于使用 Java 开发，因此可以运行在几乎所有操作系统上。请按如下清单准备：

- JDK 1.8+（兼容 OpenJDK）
- MySQL 5.6+
- Redis 3.2+（非必须，可使用内建的 Ehcache 缓存）
- Tomcat 8.0+（非必须，从 V2.0 开始默认使用 SpringBooot 内置 Tomcat）
- Apache Maven 3.3+
- IDEA 或 Eclipse (for JEE)

更多详情请参见 [开发人员文档](https://getrebuild.com/docs/dev/)

## 授权 License

REBUILD 使用 GPL-3.0 开源许可和商业授权双重授权协议，使用将被视为你自愿承诺接受 [协议](https://getrebuild.com/legal/service-terms) 之所有条款。

REBUILD uses the GPL-3.0 open source license and commercial license dual license agreement. Use will be deemed your voluntary commitment to accept all terms of the [Agreement](https://getrebuild.com/legal/service-terms).

## 购买商业版

从 2.0 版本开始，REBUILD 将推出商业版增值功能计划。如果 REBUILD 对贵公司业务有帮助，请考虑 [购买商业授权](https://getrebuild.com/#pricing-plans) 支持 REBUILD 可持续发展。除了增值功能以外，还可以得到更专业的技术支持服务。非常感谢！
