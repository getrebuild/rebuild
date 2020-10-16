[![Codacy](https://api.codacy.com/project/badge/Grade/599a0a3e46f84e6bbc29e8fbe4632860)](https://www.codacy.com/app/getrebuild/rebuild)
[![codecov](https://codecov.io/gh/getrebuild/rebuild/branch/master/graph/badge.svg)](https://codecov.io/gh/getrebuild/rebuild)
[![Build Status](https://travis-ci.org/getrebuild/rebuild.svg?branch=master)](https://travis-ci.org/getrebuild/rebuild)
[![Crowdin](https://badges.crowdin.net/rebuild/localized.svg)](https://crowdin.com/project/rebuild)
[![License GPLv3](https://img.shields.io/github/license/getrebuild/rebuild.svg)](https://getrebuild.com/license/LICENSE.txt)
[![License COMMERCIAL](https://img.shields.io/badge/license-COMMERCIAL-orange.svg)](https://getrebuild.com/license/COMMERCIAL.txt)


## REBUILD 2.0 新特性

RB 2.0 为您带来重大更新。

1. 使用 SpringBoot (v2.3) 进行全面重构
2. 使用 SpringBoot 御用 Thymeleaf 替代传统 JSP 页面
3. 完整的多语言支持
4. 用户多角色支持
5. ...

更多新特性请移步 [CHANGLOG](https://getrebuild.com/docs/dev/changelog)


## 在线体验

[https://nightly.getrebuild.com/](https://nightly.getrebuild.com/)

> 管理员用户名密码均为 `admin`，普通用户名密码均为 `rebuild`


## 使用

#### 1. 源码编译

> 注意：使用以下步骤编译 `jar` 需要先将 `pom.xml` 文件中注释为 `USE BOOT` 的下一行删除或注释，因为目前默认打包 `war`

```
# 拉取
git clone --depth=1 https://github.com/getrebuild/rebuild.git

# 编译
mvn package

# 运行
java -jar target/rebuild.jar
```

打开浏览器输入 [http://localhost:18080/](http://localhost:18080/) 开始体验。

#### 已发布版本

从 2.0 版本开始，我们将同时提供 `boot` 与 `standalone` 两种安装包。`boot` 为 Spring Boot 的 `jar` 独立运行模式，`standalone` 为外置 Tomcat 运行模式。进入 [releases](https://github.com/getrebuild/rebuild/releases) 页面选择适合你的安装包下载使用。

更多信息请参考 [安装文档](https://getrebuild.com/docs/admin/install)


## 开发

RB 2.0 支持 `jar` 与 `war` 两种打包/运行模式，两种模式在实际使用上并无太大区别。

默认情况下 RB 使用 `war` 模式，也就是需要部署到 Tomcat 或其他 Java Web 容器中运行。在实际开发时，你可以选择任一种模式。

如采用 `jar` 方式，请将 `pom.xml` 文件中注释为 `USE BOOT` 的下一行删除或注释（也可以启用 IDEA 的 `Include dependencies with "Provided" scope` 选项），然后运行 [BootApplication](src/main/java/com/rebuild/core/BootApplication.java) Spring Boot 入口类。

#### 启动参数

RB 中有几个非常重要的启动参数需要了解，无论是开发还是运行都非常重要。

| 参数 | 说明 | 默认值 |
| ---- | ---- | ---- |
| `-Drbdev` | 开发模式下请设为 `true`，会启用众多开发特性 | `false` |
| `-Drbpass` | 用于解密加密的配置参数，加密参数使用 `AES(xxx)` 包裹 | `REBUILD2018` |
| `-DDataDirectory` | 数据目录，RB 启动时需要读取此目录下的配置文件 | `~/.rebuild/` （~ 表示用户目录） |

更多信息请参考 [开发人员文档](https://getrebuild.com/docs/dev/)

## 版权 License

REBUILD 使用开源 [GPL-3.0](LICENSE) 和 [商业授权](COMMERCIAL) 双重许可授权。

REBUILD uses both open source ([GPL-3.0](LICENSE)) and [commercial](COMMERCIAL) dual-licensing authorizations.


## 购买商业版

从 2.0 版本开始，RB 将推出商业版增值功能计划。如果 RB 对贵司业务有帮助，可以考虑 [购买商业授权](https://getrebuild.com/#pricing-plans) 以支持 RB 的发展与日常运营。除了增值功能，还可以得到更好的技术支持服务。非常感谢！
