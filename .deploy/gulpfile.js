/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const { src, dest, series, parallel } = require('gulp')
const babel = require('gulp-babel')
const babelCore = require('@babel/core')
const cleanCSS = require('gulp-clean-css')
const cleanCSS2 = require('clean-css')

const fs = require('fs')
const debug = require('gulp-debug')
const revHash = require('rev-hash')
const replace = require('gulp-replace')
const filter = require('gulp-filter')

const BABEL_OPTIONS = {
  presets: ['@babel/preset-env', '@babel/preset-react'],
  plugins: ['@babel/plugin-proposal-class-properties'],
  minified: true,
}

const WEB_ROOT = '../src/main/resources/web'
const RBV_ROOT = '../@rbv/main/resources/web'
const OUT_ROOT = '../target/classes/web'

function compileJs(m) {
  return (
    src(`${m || WEB_ROOT}/assets/js/**/*.js`)
      .pipe(babel(BABEL_OPTIONS))
      .pipe(
        debug({
          title: 'Compiled .js : ',
        })
      )
      .pipe(dest(`${OUT_ROOT}/assets/js`))
  )
}

function compileCss(m) {
  return src(`${m || WEB_ROOT}/assets/css/**/*.css`)
    .pipe(cleanCSS())
    .pipe(
      debug({
        title: 'Compiled .css : ',
      })
    )
    .pipe(dest(`${OUT_ROOT}/assets/css`))
}

const _assetsHexCached = {}

function _useAssetsHex(file) {
  let hex = _assetsHexCached[file]
  if (!hex) {
    try {
      hex = revHash(fs.readFileSync(`${WEB_ROOT}${file}`))
    } catch (err) {
      try {
        hex = revHash(fs.readFileSync(`${RBV_ROOT}${file}`))
      } catch (err1) {
        if (file.includes('frontjs-sdk.js')) console.log('No `@rbv` exists :', file)
        else console.log('Cannot #revHash :', file, err1)

        // Use date
        const d = new Date()
        hex = [d.getFullYear(), d.getMonth() + 1, d.getDate()].join('')
      }
    }

    _assetsHexCached[file] = hex
  }
  return hex
}

function compileHtml(m) {
  return src(`${m || WEB_ROOT}/**/*.html`)
    .pipe(filter((file) => !(/node_modules/.test(file.path) || /lib/.test(file.path))))
    .pipe(
      replace(/<script type="text\/babel">([\s\S]*)<\/script>/gim, (m, p) => {
        if (p.trim().length === 0) return '<!-- No script -->'
        const jsmin = babelCore.transformSync(p, BABEL_OPTIONS).code
        return '<script>\n' + jsmin + '\n</script>'
      })
    )
    .pipe(replace(/ type="text\/babel"/gi, ''))
    .pipe(
      replace(/<script th:src="@\{(.*)\}"><\/script>/gi, (m, p) => {
        let file = p
        if (file.includes('/lib/') || file.includes('/use-')) {
          console.log('Using lib :', file)
          if (file.includes('/babel')) return '<!-- No Babel -->'
          if (file.includes('.development.js')) file = file.replace('.development.js', '.production.min.js')
          return '<script th:src="@{' + file + '}"></script>'
        } else {
          file += '?v=' + _useAssetsHex(file.split('?')[0])
          return '<script th:src="@{' + file + '}"></script>'
        }
      })
    )
    .pipe(
      replace(/<style type="text\/css">([\s\S]*)<\/style>/gim, (m, p) => {
        if (p.trim().length === 0) return '<!-- No style -->'
        const cssmin = new cleanCSS2({}).minify(p).styles
        return '<style type="text/css">\n' + cssmin + '\n</style>'
      })
    )
    .pipe(
      replace(/<link rel="stylesheet" type="text\/css" th:href="@\{(.*)\}" \/>/gi, (m, p) => {
        let file = p
        if (file.includes('/lib/') || file.includes('use-')) {
          console.log('Using lib :', file)
          return '<link rel="stylesheet" type="text/css" th:href="@{' + file + '}" />'
        } else {
          file += '?v=' + _useAssetsHex(file.split('?')[0])
          return '<link rel="stylesheet" type="text/css" th:href="@{' + file + '}" />'
        }
      })
    )
    .pipe(
      debug({
        title: 'Compiled .html : ',
      })
    )
    .pipe(dest(OUT_ROOT))
}

function maven(cb) {
  const pomfile = `${__dirname}/../pom.xml`
  console.log('Using pom.xml :', pomfile)

  const mvn = require('child_process').spawnSync(process.platform === 'win32' ? 'mvn.cmd' : 'mvn', ['clean', 'package', '-f', pomfile], {
    stdio: 'inherit',
  })

  if (mvn.status !== 0) {
    process.stderr.write(mvn.stderr)
    process.exit(mvn.status)
  }
  cb()
}

exports.default = series(
  parallel(
    () => compileJs(WEB_ROOT),
    () => compileCss(WEB_ROOT),
    () => compileJs(RBV_ROOT),
    () => compileCss(RBV_ROOT)
  ),
  () => compileHtml(WEB_ROOT),
  () => compileHtml(RBV_ROOT)
)

exports.mvn = maven
