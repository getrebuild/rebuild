[![Codacy](https://api.codacy.com/project/badge/Grade/599a0a3e46f84e6bbc29e8fbe4632860)](https://www.codacy.com/app/getrebuild/rebuild)
[![codecov](https://codecov.io/gh/getrebuild/rebuild/branch/master/graph/badge.svg)](https://codecov.io/gh/getrebuild/rebuild)
[![Package](https://github.com/getrebuild/rebuild/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/getrebuild?tab=packages&repo_name=rebuild)
[![Build Status](https://travis-ci.com/getrebuild/rebuild.svg?branch=master)](https://travis-ci.com/getrebuild/rebuild)
[![License GPLv3](https://img.shields.io/github/license/getrebuild/rebuild.svg)](https://getrebuild.com/legal/service-terms)
[![License 商业授权](https://img.shields.io/badge/license-%E5%95%86%E4%B8%9A%E6%8E%88%E6%9D%83-red.svg)](https://getrebuild.com/legal/service-terms)

## 项目特色

**_REBUILD 通过创新的业务流程引擎帮助你快速搭建各类企业管理系统，全图形化配置无需了解技术。_**

REBUILD 侧重于业务需求实现，而非基础技术框架或项目启动模板，通过 REBUILD 可以真正实现零代码快速搭建！无需编程、无需编译代码，甚至无需了解任何技术。

更多详情介绍 [https://getrebuild.com/](https://getrebuild.com/)

> **福利：加入 REBUILD QQ 交流群 819865721(满) 1013051587 GET 使用技能**

## V3.4 新特性

本次更新为你带来众多功能增强与优化。

1. [新增] 触发器执行日志
2. [新增] 表单回填支持启用后端回填
3. [新增] 发送通知触发器支持发送钉钉/企业微信（机器人）群消息
4. [新增] 字段聚合、分组聚合触发器支持多引用字段“连接”模式
5. [新增] 标签类字段支持自定义颜色
6. [新增] 报表模板导出为 HTML 网页
7. [新增] 日期条件支持去年/明年、上季度/下季度、上月/下月、上周/下周
8. [新增] 字段“允许重复”选项支持配置检查数据范围、逻辑条件
9. ...

更多新特性请参见 [更新日志](https://getrebuild.com/docs/dev/changelog)

## 在线体验

[https://nightly.getrebuild.com/](https://nightly.getrebuild.com/)

> 默认超级管理员用户名密码为 `admin` `admin`

## 使用

开始使用 REBUILD 非常简单，不需要配置复杂的运行环境，零依赖快速部署，超简单！

#### 1. 使用已发布版本

_生产环境强烈推荐使用此方式 !!!_

首先 [下载](https://getrebuild.com/download) 安装包，我们同时提供 `standalone` 与 `boot` 两种安装包。`standalone` 为集成安装包（推荐），`boot` 为 SpringBoot 的 `jar` 包，两种安装包在功能上没有区别。

下载后解压（集成安装包），通过 `start-rebuild.bat` 或 `start-rebuild.sh` 启动， 打开浏览器输入 [http://localhost:18080/](http://localhost:18080/) 开始体验。

更多详情请参见 [安装文档](https://getrebuild.com/docs/admin/install)

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

REBUILD 从 2.0 版本开始支持 `jar` 与 `war` 两种打包/运行模式，两种模式在开发与使用上没有区别。默认情况下使用 SpringBoot `jar` 模式，启动类为 [BootApplication](https://github.com/getrebuild/rebuild/blob/master/src/main/java/com/rebuild/core/BootApplication.java) 。

如你希望使用外部 Tomcat（或其他 Java Web 容器） 即 `war` 方式，请将 `pom.xml` 文件中注释为 `UNCOMMENT USE TOMCAT` 的下一行取消注释。

#### 开发环境

REBUILD 对于开发环境的要求非常简单，由于使用 Java 开发，因此可以运行在几乎所有操作系统上。请按如下清单准备：

- JDK 1.8+（兼容 OpenJDK）
- MySQL 5.6+
- Redis 3.2+（非必须，默认使用内置的 Ehcache 缓存）
- Tomcat 8.0+（非必须，默认使用 SpringBooot 内置 Tomcat）
- Apache Maven 3.3+
- IDEA 或 Eclipse (for JEE)

更多详情请参见 [开发人员文档](https://getrebuild.com/docs/dev/)

## 授权 License

REBUILD 使用 GPL-3.0 开源许可和商业授权双重授权协议，使用将被视为你自愿承诺接受 [协议](https://getrebuild.com/legal/service-terms) 之所有条款。

REBUILD uses the GPL-3.0 open source license and commercial license dual license agreement. Use will be deemed your voluntary commitment to accept all terms of the [Agreement](https://getrebuild.com/legal/service-terms).

## 购买商业授权

从 2.0 版本开始，REBUILD 将推出 [增值功能](https://getrebuild.com/docs/rbv-features) 计划。如果 REBUILD 对贵公司业务有帮助，请考虑 [购买商业授权](https://getrebuild.com/#pricing-plans) 以支持 REBUILD 可持续发展。除了永久享有全部最新功能以外，还可以得到更专业的技术支持服务。非常感谢！
