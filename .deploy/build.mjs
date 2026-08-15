/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

import { build, transformSync } from 'esbuild'
import * as acorn from 'acorn'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import crypto from 'crypto'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const WEB_ROOT = path.resolve(__dirname, '../src/main/resources/web')
const RBV_ROOT = path.resolve(__dirname, '../@rbv/main/resources/web')
const OUT_ROOT = path.resolve(__dirname, '../target/classes/web')

const ESBUILD_JS_OPTIONS = {
  jsx: 'transform',
  minifySyntax: true,
  minifyWhitespace: true,
  minifyIdentifiers: false,
  target: 'es2015',
}

// Rewrite top-level let/const -> var, class X -> var X = class
// so that global bindings are accessible via window.X
function rewriteTopLevelLetConst(code) {
  const ast = acorn.parse(code, { ecmaVersion: 'latest', sourceType: 'script' })
  const edits = []
  for (const node of ast.body) {
    if (node.type === 'VariableDeclaration' && node.kind !== 'var') {
      edits.push([node.start, node.start + node.kind.length, 'var'])
    } else if (node.type === 'ClassDeclaration') {
      // class X extends Y { ... } -> var X = class extends Y { ... }
      edits.push([node.start, node.start + 5, 'var'])
      edits.push([node.id.end, node.id.end, ' = class'])
    }
  }
  // Apply from end to start to preserve positions
  edits.sort((a, b) => b[0] - a[0])
  for (const [s, e, text] of edits) {
    code = code.slice(0, s) + text + code.slice(e)
  }
  return code
}

// collect unique lib references across all html (for summary, no per-file spam)
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

async function compileJs(m) {
  const srcDir = path.join(m, 'assets/js')
  const files = walkSync(srcDir, '.js')
  if (!files.length) return
  const result = await build({
    entryPoints: files,
    outdir: path.join(OUT_ROOT, 'assets/js'),
    outbase: srcDir,
    loader: { '.js': 'jsx' },
    ...ESBUILD_JS_OPTIONS,
    bundle: false,
    write: false,
    logLevel: 'warning',
  })
  for (const f of result.outputFiles) {
    fs.mkdirSync(path.dirname(f.path), { recursive: true })
    fs.writeFileSync(f.path, rewriteTopLevelLetConst(f.text))
  }
  return files.length
}

async function compileCss(m) {
  const srcDir = path.join(m, 'assets/css')
  const files = walkSync(srcDir, '.css')
  if (!files.length) return
  await build({
    entryPoints: files,
    outdir: path.join(OUT_ROOT, 'assets/css'),
    outbase: srcDir,
    minify: true,
    bundle: false,
    write: true,
    logLevel: 'warning',
  })
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
    content = content.replace(/<script type="text\/babel">([\s\S]*?)<\/script>/gim, (match, code) => {
      if (code.trim().length === 0) return '<!-- No script -->'
      const r = transformSync(code, { loader: 'jsx', ...ESBUILD_JS_OPTIONS })
      return '<script>\n' + rewriteTopLevelLetConst(r.code) + '\n</script>'
    })

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
      const r = transformSync(p, { loader: 'css', minify: true })
      return '<style>\n' + r.code + '\n</style>'
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

async function main() {
  const t0 = Date.now()
  console.log('[rebuild-compiler] starting')

  const s = {}
  await Promise.all([
    compileJs(WEB_ROOT).then((n) => (s.jsWeb = n || 0)),
    compileCss(WEB_ROOT).then((n) => (s.cssWeb = n || 0)),
    compileJs(RBV_ROOT).then((n) => (s.jsRbv = n || 0)),
    compileCss(RBV_ROOT).then((n) => (s.cssRbv = n || 0)),
  ])
  s.htmlWeb = compileHtml(WEB_ROOT)
  s.htmlRbv = compileHtml(RBV_ROOT)

  const row = (k, w, r) => `  ${k.padEnd(5)}: ${w + r} files (WEB ${w}, RBV ${r})`
  console.log(row('js', s.jsWeb, s.jsRbv))
  console.log(row('css', s.cssWeb, s.cssRbv))
  console.log(row('html', s.htmlWeb, s.htmlRbv))
  if (_libsUsed.size) console.log(`  libs : ${_libsUsed.size} unique referenced`)
  console.log(`done in ${((Date.now() - t0) / 1000).toFixed(2)}s\n`)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
