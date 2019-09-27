const gulp = require('gulp')
const gulpSequence = require('gulp-sequence')
const babel = require('gulp-babel')
const uglify = require('gulp-uglify')
const cleancss = require('gulp-clean-css')
const rename = require('gulp-rename')
const replace = require('gulp-replace')
const debug = require('gulp-debug')
const BabelCore = require('@babel/core')
const fs = require('fs')
const revHash = require('rev-hash')
const del = require('del')
const CleanCSS2 = require('clean-css')

// ES6 转码并压缩
gulp.task('xjs', () => {
    return gulp.src('../src/main/webapp/assets/js/**/*.js?(x)')
        .pipe(gulp.dest('./_temp/es6'))
        .pipe(babel({
            presets: ["@babel/env", "@babel/react"]
        }))
        .pipe(gulp.dest('./_temp/es5'))
        .pipe(uglify())
        .pipe(debug({ title: 'compress js file : ' }))
        //.pipe(rename({ extname: '.min.js' }))
        .pipe(gulp.dest('./build/assets/js'))
})

// CSS 压缩
gulp.task('xcss', () => {
    return gulp.src('../src/main/webapp/assets/css/**/*.css')
        .pipe(cleancss())
        .pipe(debug({ title: 'compress css file : ' }))
        //.pipe(rename({ extname: '.min.css' }))
        .pipe(gulp.dest('./build/assets/css'))
})

// JSP 文件内的 ES6 转码并压缩
// 去除 babel 标记并为 JS/CSS 添加版本号
const ASSETS_HEX = {}
const fileHex = (file) => {
    let hex = ASSETS_HEX[file]
    if (!!!hex) {
        try {
            hex = revHash(fs.readFileSync(file.replace('${baseUrl}', './build')))
        } catch (err) {
            hex = revHash(fs.readFileSync(file.replace('${pageContext.request.contextPath}', './build')))
        }
        ASSETS_HEX[file] = hex
    }
    return hex
}

gulp.task('xjsp', () => {
    return gulp.src('../src/main/webapp/**/*.jsp')
        .pipe(debug({ title: 'compress jsp file : ' }))
        .pipe(replace(/<script type="text\/babel">([\s\S]*)<\/script>/igm, (m, p, o, s) => {
            if (p.trim().length == 0) return '<!-- No script -->'
            let min = BabelCore.transformSync(p, {
                presets: ["@babel/env", "@babel/react"], minified: true
            }).code
            return '<script>' + min + '</script>'
        }))
        .pipe(replace(/ type="text\/babel"/ig, '')) // remove type="text/babel"
        .pipe(replace(/<script src="(.*)"><\/script>/ig, (m, p, o, s) => {
            let file = p
            if (file.includes('/lib/')) {
                if (file.includes('babel')) return '<!-- No Babel -->'
                if (file.includes('.development.js')) file = file.replace('.development.js', '.production.min.js')
                return '<script src="' + file + '"></script>'
            }
            file = file.replace('.jsx', '.js').split('?')[0]
            file += '?v=' + fileHex(file)
            console.log(p + ' >> ' + file)
            return '<script src="' + file + '"></script>'
        }))
        .pipe(replace(/<style type="text\/css">([\s\S]*)<\/style>/igm, (m, p, o, s) => {
            if (p.trim().length == 0) return '<!-- No style -->'
            let min = new CleanCSS2({}).minify(p).styles
            return '<style type="text/css">' + min + '</style>'
        }))
        .pipe(replace(/<link rel="stylesheet" type="text\/css" href="(.*)">/ig, (m, p, o, s) => {
            let file = p
            if (file.includes('/lib/')) return '<link rel="stylesheet" type="text/css" href="' + file + '">'
            file = file.split('?')[0]
            file += '?v=' + fileHex(file)
            console.log(p + ' >> ' + file)
            return '<link rel="stylesheet" type="text/css" href="' + file + '">'
        }))
        .pipe(gulp.dest('./build'))
})


gulp.task('clear', () => {
    del(['./_temp', './build'])
})

gulp.task('cp', () => {
    gulp.src('./build/**')
        .pipe(gulp.dest('/data/rebuild47070/webapps/ROOT'))
})
gulp.task('cp2', () => {
    gulp.src('./build/**')
        .pipe(gulp.dest('../target/rebuild'))
})

gulp.task('default', gulpSequence(['xjs', 'xcss'], 'xjsp'))

gulp.task('all', gulpSequence(['xjs', 'xcss'], 'xjsp', 'cp'))
gulp.task('all2', gulpSequence(['xjs', 'xcss'], 'xjsp', 'cp2'))

