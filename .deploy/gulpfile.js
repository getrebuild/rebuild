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
  minified: true
}

function compileJs(cb) {
  return src('../target/rebuild/assets/js/**/*.js?(x)')
    .pipe(
      babel(BABEL_OPTIONS)
    )
    .pipe(
      debug({ title: 'Compiled : ' })
    )
    .pipe(
      dest('build/assets/js')
    )
}

function compileCss(cb) {
  return src('../target/rebuild/assets/css/**/*.css')
    .pipe(
      cleanCSS()
    )
    .pipe(
      debug({ title: 'Compiled : ' })
    )
    .pipe(
      dest('build/assets/css')
    )
}

const _assetsHexCached = {}
function _assetsHex(file) {
  let hex = _assetsHexCached[file]
  if (!hex) {
    try {
      hex = revHash(fs.readFileSync(file.replace('${baseUrl}', 'build')))
    } catch (err) {
      hex = revHash(fs.readFileSync(file.replace('${pageContext.request.contextPath}', 'build')))
    }
    _assetsHexCached[file] = hex
  }
  return hex
}

function compileJsp(cb) {
  return src('../target/rebuild/**/*.jsp')
    .pipe(
      replace(/<script type="text\/babel">([\s\S]*)<\/script>/igm, (match, p) => {
        if (p.trim().length === 0) return '<!-- No script -->'
        const min = babelCore.transformSync(p, BABEL_OPTIONS).code
        return '<script>\n' + min + '\n</script>'
      })
    )
    .pipe(
      replace(/ type="text\/babel"/ig, '')
    )
    .pipe(
      replace(/<script src="(.*)"><\/script>/ig, (m, p) => {
        let file = p
        if (file.includes('/lib/') || file.includes('/language/')) {
          if (file.includes('babel')) return '<!-- No Babel -->'
          if (file.includes('.development.js')) file = file.replace('.development.js', '.production.min.js')
          return '<script src="' + file + '"></script>'
        } else {
          file = file.replace('.jsx', '.js').split('?')[0]
          file += '?v=' + _assetsHex(file)
          return '<script src="' + file + '"></script>'
        }
      })
    )
    .pipe(
      replace(/<style type="text\/css">([\s\S]*)<\/style>/igm, (match, p) => {
        if (p.trim().length === 0) return '<!-- No style -->'
        const min = new cleanCSS2({}).minify(p).styles
        return '<style type="text/css">\n' + min + '\n</style>'
      })
    )
    .pipe(
      replace(/<link rel="stylesheet" type="text\/css" href="(.*)">/ig, (match, p) => {
        let file = p
        if (file.includes('/lib/')) {
          return '<link rel="stylesheet" type="text/css" href="' + file + '">'
        } else {
          file += '?v=' + _assetsHex(file.split('?')[0])
          return '<link rel="stylesheet" type="text/css" href="' + file + '">'
        }
      })
    )
    .pipe(
      debug({ title: 'Compiled : ' })
    )
    .pipe(
      dest('build')
    )
}

function maven(cb) {
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
  cb()
}

const RELEASE_HOME = 'D:/MAVEN2017/rebuild/for-production/rebuild-standalone/REBUILD'
function release(cb) {
  return src('../target/rebuild/**')
    .pipe(
      filter((file) => {
        const m = /\.jsx/.test(file.path) || /\.development\./.test(file.path) || /babel\./.test(file.path)
          || /rebel\.xml/.test(file.path)
        m && console.log('Filtered : ' + file.path)
        return !m
      })
    )
    .pipe(
      dest(RELEASE_HOME)
    )
    .on('end', () => {
      src('build/**')
        .pipe(
          dest(RELEASE_HOME)
        )
    })
}

exports.default = series(maven, parallel(compileJs, compileCss), compileJsp)
exports.mvn = maven
exports.p = series(maven, parallel(compileJs, compileCss), compileJsp, release)