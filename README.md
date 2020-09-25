## REBUILD 2.0 新特性

RB 2.0 为您带来重大更新。

1. 使用 SpringBoot (v2.3) 进行全面重构
2. 使用 SpringBoot 御用 Thymeleaf 替代传统 JSP 页面
3. 完整的多语言支持
4. 用户多角色支持
5. ...

更多新特性请移步 [CHANGLOG](https://getrebuild.com/docs/dev/changelog)

## 使用

### 自行编译

```
# 拉取
git clone --depth=1 https://github.com/getrebuild/rebuild.git

# 编译
mvn package

# 运行
java -jar target/rebuild.jar
```

打开浏览器输入 [http://localhost:18080/](http://localhost:18080/) 开始体验。

### 已发布版本

从 2.0 版本开始，我们将同时提供 `boot` 与 `standalone` 两种安装包。`boot` 为 Spring Boot 的 `jar` 独立运行模式，`standalone` 为外置 Tomcat 运行模式。

进入 [releases](https://github.com/getrebuild/rebuild/releases) 页面选择最新的安装包下载使用。


## 开发

RB 2.0 支持 `jar` 与 `war` 两种打包/运行模式，两种模式在实际使用上并无区别。

默认情况下 RB 使用 `war` 模式，也就是需要部署到 Tomcat 或其他 Java Web 容器中运行。在实际开发时，你可以选择任一种模式，如采用 `jar` 方式，请直接运行 [BootApplication](src/main/java/com/rebuild/core/BootApplication.java) Spring Boot 入口类。

### 启动参数

RB 中有几个非常重要的启动参数需要了解，无论是开发还是运行都非常重要。

| 参数 | 说明 | 默认值 |
| ---- | ---- | ---- |
| `-Drbdev` | 开发模式下请设为 `true`，会启用众多开发特性 | `false` |
| `-Drbpass` | 用于解密加密的配置参数，加密参数使用 `AES(xxx)` 包裹 | `REBUILD2018` |
| `-DDataDirectory` | 数据目录，RB 启动时需要读取此目录下的配置文件 | `~/.rebuild/` （~ 表示用户目录） |



## 版权 License

REBUILD 使用开源 [GPL-3.0](LICENSE) 和 [商业授权](COMMERCIAL) 双重许可授权。

REBUILD uses both open source ([GPL-3.0](LICENSE)) and [commercial](COMMERCIAL) dual-licensing authorizations.
