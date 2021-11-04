[![Codacy](https://api.codacy.com/project/badge/Grade/599a0a3e46f84e6bbc29e8fbe4632860)](https://www.codacy.com/app/getrebuild/rebuild)
[![codecov](https://codecov.io/gh/getrebuild/rebuild/branch/master/graph/badge.svg)](https://codecov.io/gh/getrebuild/rebuild)
[![Package](https://github.com/getrebuild/rebuild/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/getrebuild/rebuild/actions/workflows/maven-publish.yml)
[![Build Status](https://travis-ci.com/getrebuild/rebuild.svg?branch=master)](https://travis-ci.com/getrebuild/rebuild)
[![License GPLv3](https://img.shields.io/github/license/getrebuild/rebuild.svg)](LICENSE)
[![License COMMERCIAL](https://img.shields.io/badge/license-COMMERCIAL-orange.svg)](COMMERCIAL)

## 项目特色

REBUILD 更侧重于业务需求实现，而非基础的技术框架或项目启动模板。通过 REBUILD 可以真正实现零代码快速搭建业务系统，无需编程、无需编译代码，甚至无需了解技术。

「开放式设计」是 REBUILD 的重要设计理念，得益于开发团队超过十年的成熟 B 端产品经验，我们实现了对企业日常各类需求的可配置化管理，无需开发即可使用，有效避免过度依赖高成本研发投入。

更多详情介绍 [https://getrebuild.com/](https://getrebuild.com/)

> **福利：欢迎加入 REBUILD QQ 交流群 819865721**

## V2.6 新特性

本次更新为你带来众多功能增强与优化。

1. [新增] 项目任务卡片支持配置显示字段
2. [新增] 数据列表多种显示模式
3. [新增] 数据列表导出可配置 Excel 模板
4. [新增] 动态中可通过输入 @ 拉起用户提及组件
5. [新增] 移动端钉钉/企业微信支持分享记录给他人
6. [优化] 多项安全性更新
7...

更多新特性请移步 [CHANGLOG](https://getrebuild.com/docs/dev/changelog?v=2.6)

## 在线体验

[https://nightly.getrebuild.com/](https://nightly.getrebuild.com/)

> 管理员用户名密码均为 `admin`，普通用户名密码均为 `rebuild`

## 使用

开始使用 REBUILD 非常简单，你不需要搭建复杂的运行环境，甚至不需要安装 JDK（`standalone` 安装包自带）。零依赖快速部署，就是那么简单！

#### 1. 使用已发布版本

_生产环境强烈推荐使用此方式 !!!_

进入 [releases](https://github.com/getrebuild/rebuild/releases) 页面选择适合的安装包下载。从 2.0 版本开始，我们同时提供 `boot` 与 `standalone` 两种安装包。`boot` 为 Spring Boot 的 `jar` 独立运行模式，`standalone` 为外置 Tomcat 运行模式，两种模式在功能上并无区别。

更多信息请参考 [安装文档](https://getrebuild.com/docs/admin/install)

#### 2. 通过源码编译

_注意 !!! 生产环境请使用 `master` 分支（即默认分支），其他分支为开发分支，功能存在不确定性_

```
# 拉取
git clone --depth=1 https://github.com/getrebuild/rebuild.git

# 编译
mvn package

# 运行
java -jar target/rebuild.jar
```

打开浏览器输入 [http://127.0.0.1:18080/](http://127.0.0.1:18080/) 开始体验。

## 开发

RB 从 2.0 版本开始支持 `jar` 与 `war` 两种打包/运行模式，两种模式在开发与使用上并无区别。默认情况下使用 Spring Boot 的独立 `jar` 模式，启动类为 [BootApplication](https://github.com/getrebuild/rebuild/blob/master/src/main/java/com/rebuild/core/BootApplication.java) 。

如你希望使用外部 Tomcat（或其他 Java Web 容器） 即 `war` 方式，请将 `pom.xml` 文件中注释为 `UNCOMMENT USE TOMCAT` 的下一行取消注释。

#### 启动参数

RB 中有几个非常重要地启动参数需要了解，无论是开发还是运行都非常重要。

| 参数       | 说明                                                 | 默认值        |
| ---------- | ---------------------------------------------------- | ------------- |
| `-Drbdev`  | 开发模式下请设为 `true`，会启用众多开发特性          | `false`       |
| `-Drbpass` | 用于解密加密的配置参数，加密参数使用 `AES(xxx)` 包裹 | `REBUILD2018` |

更多信息请参考 [开发人员文档](https://getrebuild.com/docs/dev/)

## 版权 License

REBUILD 使用开源 [GPL-3.0](LICENSE) 和 [商业授权](COMMERCIAL) 双重许可授权。

REBUILD uses both open source ([GPL-3.0](LICENSE)) and [commercial](COMMERCIAL) dual-licensing authorizations.

## 购买商业版

从 2.0 版本开始，RB 将推出商业版增值功能计划。如果 REBUILD 对贵公司业务有帮助，可以考虑 [购买商业授权](https://getrebuild.com/#pricing-plans) 以支持 RB 的日常运营及发展。除了增值功能，还可以得到更好的技术支持服务。非常感谢！
