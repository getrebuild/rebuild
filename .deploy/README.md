## About

用于发布到生产环境时编译 js(x)/css 并自动在 jsp 中添加版本号。

## How use

首先安装 `gulp` 及依赖包

```
npm install -g gulp gulp-cli
npm install
```

然后执行编译

```
gulp
```


编译成功后的文件会输出到 `build` 目录，复制此目录下编译好的文件覆盖到生产环境即可。