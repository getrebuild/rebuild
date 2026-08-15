/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

import { createRequire } from 'module'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import crypto from 'crypto'

const require = createRequire(import.meta.url)
const { transformSync } = require('@babel/core')
const CleanCSS = require('clean-css')

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const WEB_ROOT = path.resolve(__dirname, '../src/main/resources/web')
const RBV_ROOT = path.resolve(__dirname, '../@rbv/main/resources/web')
const OUT_ROOT = path.resolve(__dirname, '../target/classes/web')

// https://babeljs.io/docs/en/options#primary-options
const BABEL_OPTIONS = {
  presets: ['@babel/preset-env', '@babel/preset-react'],
  plugins: ['@babel/plugin-proposal-class-properties'],
  minified: true,
  generatorOpts: {
    comments: false,
  },
}

// collect unique lib references across all html (for summary)
const _libsUsed = new Set()

function walkSync(dir, ext, out = []) {
  if (!fs.existsSync(dir)) return out
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name)
    const stat = fs.statSync(p)
    if (stat.isDirectory()) walkSync(p, ext, out)
    else if (name.endsWith(ext)) out.push(p)
  }
  return out
}

// equivalent of `rev-hash` (content sha1, first 8 chars)
function revHash(buf) {
  return crypto.createHash('sha1').update(buf).digest('hex').slice(0, 8)
}

function compileJs(m) {
  const srcDir = path.join(m, 'assets/js')
  const files = walkSync(srcDir, '.js')
  if (!files.length) return 0
  for (const f of files) {
    const code = fs.readFileSync(f, 'utf8')
    const r = transformSync(code, { ...BABEL_OPTIONS, filename: f })
    const rel = path.relative(srcDir, f)
    const dest = path.join(OUT_ROOT, 'assets/js', rel)
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    fs.writeFileSync(dest, r.code)
  }
  return files.length
}

function compileCss(m) {
  const srcDir = path.join(m, 'assets/css')
  const files = walkSync(srcDir, '.css')
  if (!files.length) return 0
  for (const f of files) {
    const code = fs.readFileSync(f, 'utf8')
    const r = new CleanCSS({
      level: { 1: { specialComments: 0 } },
    }).minify(code)
    const rel = path.relative(srcDir, f)
    const dest = path.join(OUT_ROOT, 'assets/css', rel)
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    fs.writeFileSync(dest, r.styles)
  }
  return files.length
}

const _assetsHexCached = {}

function useAssetsHex(file) {
  if (_assetsHexCached[file]) return _assetsHexCached[file]
  let hex
  try {
    hex = revHash(fs.readFileSync(path.join(WEB_ROOT, file)))
  } catch (err) {
    try {
      hex = revHash(fs.readFileSync(path.join(RBV_ROOT, file)))
    } catch (errAgain) {
      console.warn(`[warn] rev-hash fallback for ${file}: ${errAgain.message}`)
      const d = new Date()
      hex = [d.getFullYear(), d.getMonth() + 1, d.getDate()].join('')
    }
  }
  _assetsHexCached[file] = hex
  return hex
}

function compileHtml(m) {
  const files = walkSync(m, '.html').filter((p) => {
    const n = p.replace(/\\/g, '/')
    return !n.includes('/node_modules/') && !n.includes('/lib/')
  })
  let count = 0
  for (const f of files) {
    let content = fs.readFileSync(f, 'utf8')

    // 1. inline `<script type="text/babel">...</script>` -> compiled
    content = content.replace(
      /<script type="text\/babel">([\s\S]*?)<\/script>/gim,
      (match, code) => {
        if (code.trim().length === 0) return '<!-- No script -->'
        const r = transformSync(code, BABEL_OPTIONS)
        return '<script>\n' + r.code + '\n</script>'
      },
    )

    // 2. remove `type="text/babel"` attr (external refs)
    content = content.replace(/ type="text\/babel"/gi, '')

    // 3. `<script th:src="@{...}">` -> lib switch / version hash
    content = content.replace(/<script th:src="@\{(.*)\}"><\/script>/gi, (match, p) => {
      let file = p
      if (file.includes('/lib/') || file.includes('/use-')) {
        _libsUsed.add(file)
        if (file.includes('/babel')) return '<!-- No Babel -->'
        if (file.includes('.development.js')) file = file.replace('.development.js', '.production.min.js')
        return '<script th:src="@{' + file + '}"></script>'
      } else {
        file += '?v=' + useAssetsHex(file.split('?')[0])
        return '<script th:src="@{' + file + '}"></script>'
      }
    })

    // 4. inline `<style>...</style>` -> minified
    content = content.replace(/<style>([\s\S]*?)<\/style>/gim, (match, p) => {
      if (p.trim().length === 0) return '<!-- No style -->'
      const r = new CleanCSS({}).minify(p)
      return '<style>\n' + r.styles + '\n</style>'
    })

    // 5. `<link ... th:href="@{...}" />` -> version hash (lib kept as-is)
    content = content.replace(/<link rel="stylesheet" type="text\/css" th:href="@\{(.*)\}" \/>/gi, (match, p) => {
      let file = p
      if (file.includes('/lib/') || file.includes('use-')) {
        _libsUsed.add(file)
        return '<link rel="stylesheet" type="text/css" th:href="@{' + file + '}" />'
      } else {
        file += '?v=' + useAssetsHex(file.split('?')[0])
        return '<link rel="stylesheet" type="text/css" th:href="@{' + file + '}" />'
      }
    })

    const rel = path.relative(m, f)
    const dest = path.join(OUT_ROOT, rel)
    fs.mkdirSync(path.dirname(dest), { recursive: true })
    fs.writeFileSync(dest, content)
    count++
  }
  return count
}

function main() {
  const t0 = Date.now()
  console.log('[rebuild-compiler] starting')

  const s = {}
  s.jsWeb = compileJs(WEB_ROOT)
  s.cssWeb = compileCss(WEB_ROOT)
  s.jsRbv = compileJs(RBV_ROOT)
  s.cssRbv = compileCss(RBV_ROOT)
  s.htmlWeb = compileHtml(WEB_ROOT)
  s.htmlRbv = compileHtml(RBV_ROOT)

  const row = (k, w, r) => `  ${k.padEnd(5)}: ${w + r} files (WEB ${w}, RBV ${r})`
  console.log(row('js', s.jsWeb, s.jsRbv))
  console.log(row('css', s.cssWeb, s.cssRbv))
  console.log(row('html', s.htmlWeb, s.htmlRbv))
  if (_libsUsed.size) console.log(`  libs : ${_libsUsed.size} unique referenced`)
  console.log(`done in ${((Date.now() - t0) / 1000).toFixed(2)}s\n`)
}

try {
  main()
} catch (err) {
  console.error(err)
  process.exit(1)
}
