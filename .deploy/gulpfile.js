const {
  src,
  dest,
  series,
  parallel
} = require('gulp')
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

const WEB_ROOT = '../src/main/resources/web'
const OUT_ROOT = '../target/classes/web'

function compileJs(cb) {
  return src(`${WEB_ROOT}/assets/js/**/*.js`)
    .pipe(
      babel(BABEL_OPTIONS)
    )
    .pipe(
      debug({
        title: 'Compiled : '
      })
    )
    .pipe(
      dest(`${OUT_ROOT}/assets/js`)
    )
}

function compileCss(cb) {
  return src(`${WEB_ROOT}/assets/css/**/*.css`)
    .pipe(
      cleanCSS()
    )
    .pipe(
      debug({
        title: 'Compiled : '
      })
    )
    .pipe(
      dest(`${OUT_ROOT}/assets/css`)
    )
}

const _assetsHexCached = {}

function _assetsHex(file) {
  let hex = _assetsHexCached[file]
  if (!hex) {
    try {
      hex = revHash(fs.readFileSync(`${WEB_ROOT}${file}`))
    } catch (err) {
      console.log('Hash of file error : ' + file, err)
      // Use date
      const d = new Date()
      hex = [d.getFullYear(), d.getMonth() + 1, d.getDate()].join('')
    }
    _assetsHexCached[file] = hex
  }
  return hex
}

function compileHtml(cb) {
  return src(`${WEB_ROOT}/**/*.html`)
    .pipe(filter(file => !/node_modules/.test(file.path)))
    .pipe(
      replace(/<script type="text\/babel">([\s\S]*)<\/script>/igm, (m, p) => {
        if (p.trim().length === 0) return '<!-- No script -->'
        const min = babelCore.transformSync(p, BABEL_OPTIONS).code
        return '<script>\n' + min + '\n</script>'
      })
    )
    .pipe(
      replace(/ type="text\/babel"/ig, '')
    )
    .pipe(
      replace(/<script th:src="@\{(.*)\}"><\/script>/ig, (m, p) => {
        let file = p
        if (file.includes('/lib/') || file.includes('/language/')) {
          if (file.includes('babel')) return '<!-- No Babel -->'
          if (file.includes('.development.js')) file = file.replace('.development.js', '.production.min.js')
          return '<script th:src="@{' + file + '}"></script>'
        } else {
          file += '?v=' + _assetsHex(file.split('?')[0])
          return '<script th:src="@{' + file + '}"></script>'
        }
      })
    )
    .pipe(
      replace(/<style type="text\/css">([\s\S]*)<\/style>/igm, (m, p) => {
        if (p.trim().length === 0) return '<!-- No style -->'
        const min = new cleanCSS2({}).minify(p).styles
        return '<style type="text/css">\n' + min + '\n</style>'
      })
    )
    .pipe(
      replace(/<link rel="stylesheet" type="text\/css" th:href="@\{(.*)\}" \/>/ig, (m, p) => {
        let file = p
        if (file.includes('/lib/')) {
          return '<link rel="stylesheet" type="text/css" th:href="@{' + file + '}" />'
        } else {
          file += '?v=' + _assetsHex(file.split('?')[0])
          return '<link rel="stylesheet" type="text/css" th:href="@{' + file + '}" />'
        }
      })
    )
    .pipe(
      debug({
        title: 'Compiled : '
      })
    )
    .pipe(
      dest(OUT_ROOT)
    )
}

function maven(cb) {
  const pomfile = `${__dirname}/../pom.xml`
  console.log('Using pom.xml : ' + pomfile)

  const mvn = require('child_process').spawnSync(
    process.platform === 'win32' ? 'mvn.cmd' : 'mvn',
    ['clean', 'package', '-f', pomfile], {
    stdio: 'inherit'
  })

  if (mvn.status !== 0) {
    process.stderr.write(mvn.stderr)
    process.exit(mvn.status)
  }
  cb()
}

// const RELEASE_HOME = 'D:/GitHub/for-production/rebuild-standalone/REBUILD'

// function release(cb) {
//   return src('../target/rebuild/**')
//     .pipe(
//       filter((file) => {
//         const m = /\.jsx/.test(file.path) || /\.development\./.test(file.path) || /babel\./.test(file.path) ||
//           /rebel\.xml/.test(file.path)
//         m && console.log('Filtered : ' + file.path)
//         return !m
//       })
//     )
//     .pipe(
//       dest(RELEASE_HOME)
//     )
//     .on('end', () => {
//       src('build/**')
//         .pipe(
//           dest(RELEASE_HOME)
//         )
//     })
// }

exports.default = series(parallel(compileJs, compileCss), compileHtml)
exports.mvn = maven
// exports.p = series(maven, parallel(compileJs, compileCss), compileHtml, release)