/* eslint-disable indent */
/* global process, __dirname */
const gulp = require('gulp')
const gulpSequence = require('gulp-sequence')
const babel = require('gulp-babel')
const uglify = require('gulp-uglify')
const cleancss = require('gulp-clean-css')
const replace = require('gulp-replace')
const debug = require('gulp-debug')
const filter = require('gulp-filter')
const BabelCore = require('@babel/core')
const fs = require('fs')
const revHash = require('rev-hash')
const CleanCSS2 = require('clean-css')

// JS 压缩 ES6>ES5
gulp.task('xjs', () => {
    return gulp.src('../target/rebuild/assets/js/**/*.js?(x)')
        .pipe(gulp.dest('./temp/es6'))
        .pipe(babel({
            presets: ['@babel/env', '@babel/react']
        }))
        .pipe(gulp.dest('./temp/es5'))
        .pipe(uglify())
        .pipe(debug({ title: 'Compressed js(x) file : ' }))
        .pipe(gulp.dest('./build/assets/js'))
})

// CSS 压缩
gulp.task('xcss', () => {
    return gulp.src('../target/rebuild/assets/css/**/*.css')
        .pipe(cleancss())
        .pipe(debug({ title: 'Compressed css file : ' }))
        .pipe(gulp.dest('./build/assets/css'))
})

// 去除 babel 标记并为 JS/CSS 文件添加版本号
// JSP 文件内的 JS/CSS 压缩
const __ASSETS_HEX = {}
const __fileHex = (file) => {
    let hex = __ASSETS_HEX[file]
    if (!hex) {
        try {
            hex = revHash(fs.readFileSync(file.replace('${baseUrl}', './build')))
        } catch (err) {
            hex = revHash(fs.readFileSync(file.replace('${pageContext.request.contextPath}', './build')))
        }
        __ASSETS_HEX[file] = hex
    }
    return hex
}

gulp.task('xjsp', () => {
    return gulp.src('../target/rebuild/**/*.jsp')
        .pipe(debug({ title: 'Compressing jsp file : ' }))
        .pipe(replace(/<script type="text\/babel">([\s\S]*)<\/script>/igm, (m, p) => {
            if (p.trim().length === 0) return '<!-- No script -->'
            const min = BabelCore.transformSync(p, {
                presets: ['@babel/env', '@babel/react'],
                minified: true
            }).code
            return '<script>' + min + '</script>'
        }))
        .pipe(replace(/ type="text\/babel"/ig, '')) // remove type="text/babel"
        .pipe(replace(/<script src="(.*)"><\/script>/ig, (m, p) => {
            let file = p
            if (file.includes('/lib/')) {
                if (file.includes('babel')) return '<!-- No Babel -->'
                if (file.includes('.development.js')) file = file.replace('.development.js', '.production.min.js')
                return '<script src="' + file + '"></script>'
            } else if (file.includes('/language/')) {
                console.warn('Ignore file : ' + file)
                return '<script src="' + file + '"></script>'
            } else {
                file = file.replace('.jsx', '.js').split('?')[0]
                console.log(p + ' >> ' + file)
                file += '?v=' + __fileHex(file)
                return '<script src="' + file + '"></script>'
            }
        }))
        .pipe(replace(/<style type="text\/css">([\s\S]*)<\/style>/igm, (m, p) => {
            if (p.trim().length === 0) return '<!-- No style -->'
            const min = new CleanCSS2({}).minify(p).styles
            return '<style type="text/css">' + min + '</style>'
        }))
        .pipe(replace(/<link rel="stylesheet" type="text\/css" href="(.*)">/ig, (m, p) => {
            let file = p
            if (file.includes('/lib/')) return '<link rel="stylesheet" type="text/css" href="' + file + '">'
            file = file.split('?')[0]
            file += '?v=' + __fileHex(file)
            console.log(p + ' >> ' + file)
            return '<link rel="stylesheet" type="text/css" href="' + file + '">'
        }))
        .pipe(gulp.dest('./build'))
})

// MVN 编译&打包
gulp.task('maven', (cb) => {
    const pomfile = `${__dirname}/../pom.xml`
    console.log('Using pom.xml : ' + pomfile)
    const mvn = require('child_process').spawnSync(
        process.platform === 'win32' ? 'mvn.cmd' : 'mvn',
        ['clean', 'package', '-f', pomfile],
        { stdio: 'inherit' })

    if (mvn.status !== 0) {
        process.stderr.write(mvn.stderr)
        process.exit(mvn.status)
    }
    typeof cb === 'function' && cb()
})

// Group tasks

const DEPLOY_HOME = '/data/rebuild47070/webapps/ROOT'
gulp.task('cp2server', () => {
    gulp.src('./build/**')
        .pipe(gulp.dest(DEPLOY_HOME))
})

const PACKAGE_HOME = 'D:/MAVEN2017/rebuild/for-production/rebuild-standalone/REBUILD'
gulp.task('cp2release', () => {
    gulp.src('../target/rebuild/**')
        .pipe(filter((file) => {
            const pro = /\.jsx/.test(file.path) || /\.development\./.test(file.path) || /babel\./.test(file.path)
                || /rebel\.xml/.test(file.path)
            pro && console.log('Filtered file : ' + file.path)
            return !pro
        }))
        .pipe(gulp.dest(PACKAGE_HOME))
        .on('end', () => {
            gulp.src('./build/**')
                .pipe(gulp.dest(PACKAGE_HOME))
        })
})

gulp.task('default', gulpSequence(['xjs', 'xcss'], 'xjsp'))
gulp.task('d', gulpSequence(['xjs', 'xcss'], 'xjsp', 'cp2server'))
gulp.task('p', gulpSequence('maven', ['xjs', 'xcss'], 'xjsp', 'cp2release'))
