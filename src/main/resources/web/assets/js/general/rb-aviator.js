/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// AviatorScript 语法高亮与格式化（CodeMirror 5）
// 字段变量 {fieldName} / {^fieldName} / {fieldName$$$$SUM} / {NOW} 作为独立 token

;(function (root) {
  'use strict'

  const MODE_NAME = 'aviator'
  const MIME = 'text/x-aviator'
  const INDENT_UNIT = 2

  const KEYWORDS = words('let if else elsif for in while do fn lambda return break continue try catch finally throw new use static end')
  const ATOMS = words('nil true false')
  const NAMESPACES = words('StringUtils seq math string date json sysdate')
  const BUILTIN_FUNCS = words('print println p type count str eval tuple map now date rand long double bigint decimal abs max min is_def is_a is_def_var load require assert')

  let customFuncs = {}

  function words(s) {
    const m = {}
    ;(s || '').split(/\s+/).forEach((w) => {
      if (w) m[w] = 1
    })
    return m
  }

  function isFunc(name) {
    return customFuncs[name] === 1 || BUILTIN_FUNCS[name] === 1
  }

  function setFunctions(list) {
    customFuncs = {}
    ;(list || []).forEach((name) => {
      if (name) customFuncs[String(name)] = 1
    })
  }

  // ~~ 词法规则
  const R_FIELD = /^\{\^?[0-9a-zA-Z._$]{3,}\}/
  const R_STRING2 = /^"(?:[^"\\\n]|\\[^\n])*"?/
  const R_STRING1 = /^'(?:[^'\\\n]|\\[^\n])*'?/
  const R_NUMBER = /^(?:0[xX][0-9a-fA-F]+|\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)[lL]?\b/
  const R_IDENT = /^[A-Za-z_$][\w$]*/
  const R_OPERATOR = /^(?:==|!=|>=|<=|&&|\|\||=~|!~|->|\+\+|--|\+=|-=|\*=|\/=|%=|<<|>>|[+\-*/%=!<>?:&|^~])/
  const R_PUNCT = /^[()[\]{},;.]/

  // ~~ CodeMirror mode
  function createMode() {
    return {
      startState: function () {
        return { blockComment: false, braces: 0, afterDot: false }
      },
      copyState: function (s) {
        return { blockComment: s.blockComment, braces: s.braces, afterDot: s.afterDot }
      },
      token: function (stream, state) {
        if (state.blockComment) {
          if (stream.skipTo('*/')) {
            stream.match('*/')
            state.blockComment = false
          } else {
            stream.skipToEnd()
          }
          return 'comment'
        }

        if (stream.eatSpace()) return null

        // 注释
        if (stream.match(/^##/)) {
          stream.skipToEnd()
          state.afterDot = false
          return 'comment'
        }
        if (stream.match(/^\/\*/)) {
          state.blockComment = true
          state.afterDot = false
          return 'comment'
        }

        // 字段变量优先于花括号
        if (stream.match(R_FIELD)) {
          state.afterDot = false
          return 'aviator-var'
        }

        if (stream.match(R_STRING2) || stream.match(R_STRING1)) {
          state.afterDot = false
          return 'string'
        }

        if (stream.match(R_NUMBER)) {
          state.afterDot = false
          return 'number'
        }

        if (stream.match(R_IDENT)) {
          const word = stream.current()
          let type
          if (state.afterDot) type = 'property'
          else if (KEYWORDS[word]) type = 'keyword'
          else if (ATOMS[word]) type = 'atom'
          else if (stream.match(/^\s*\(/, false)) type = isFunc(word) ? 'builtin' : 'def'
          else if (NAMESPACES[word]) type = 'type'
          else type = 'variable'

          state.afterDot = false
          return type
        }

        if (stream.match(R_OPERATOR)) {
          state.afterDot = false
          return 'operator'
        }

        if (stream.match(R_PUNCT)) {
          const ch = stream.current()
          if (ch === '{') state.braces++
          else if (ch === '}') state.braces = Math.max(0, state.braces - 1)
          state.afterDot = ch === '.'
          return 'bracket'
        }

        stream.next()
        state.afterDot = false
        return null
      },
      indent: function (state, textAfter) {
        let n = state.braces * INDENT_UNIT
        if (/^\s*[}\])]/.test(textAfter)) n -= INDENT_UNIT
        return Math.max(0, n)
      },
      lineComment: '##',
      electricInput: /^\s*[{}]$/,
    }
  }

  // ~~ 格式化
  function matchAt(src, i, re) {
    const m = re.exec(src.slice(i))
    return m ? m[0] : null
  }

  function scanString(src, i) {
    const quote = src.charAt(i)
    let j = i + 1
    while (j < src.length) {
      const ch = src.charAt(j)
      if (ch === '\\') j += 2
      else if (ch === quote) return j + 1
      else if (ch === '\n') return -1
      else j++
    }
    return -1
  }

  function isUnaryOp(prev, op) {
    if (op === '!' || op === '~') return true
    if (op !== '-' && op !== '+') return false
    if (!prev) return true
    if (prev.t === 'op') return true
    if (prev.t === 'keyword') return true
    if (prev.t === 'punct' && '([,;'.indexOf(prev.v) >= 0) return true
    return false
  }

  function lex(src) {
    const tokens = []
    let i = 0

    while (i < src.length) {
      const ch = src.charAt(i)

      if (ch === '\r') {
        i++
        continue
      }
      if (ch === '\n') {
        tokens.push({ t: 'nl', v: '\n' })
        i++
        continue
      }
      if (ch === ' ' || ch === '\t') {
        i++
        continue
      }

      if (ch === '#' && src.charAt(i + 1) === '#') {
        let end = src.indexOf('\n', i)
        if (end < 0) end = src.length
        tokens.push({ t: 'comment', v: src.slice(i, end) })
        i = end
        continue
      }

      if (ch === '/' && src.charAt(i + 1) === '*') {
        const end = src.indexOf('*/', i + 2)
        if (end < 0) throw new Error('Unterminated block comment')
        tokens.push({ t: 'comment', v: src.slice(i, end + 2) })
        i = end + 2
        continue
      }

      let m = matchAt(src, i, R_FIELD)
      if (m) {
        tokens.push({ t: 'field', v: m })
        i += m.length
        continue
      }

      if (ch === '"' || ch === "'") {
        const end = scanString(src, i)
        if (end < 0) throw new Error('Unterminated string')
        tokens.push({ t: 'string', v: src.slice(i, end) })
        i = end
        continue
      }

      m = matchAt(src, i, R_NUMBER)
      if (m) {
        tokens.push({ t: 'number', v: m })
        i += m.length
        continue
      }

      m = matchAt(src, i, R_IDENT)
      if (m) {
        let j = i + m.length
        while (j < src.length && (src.charAt(j) === ' ' || src.charAt(j) === '\t')) j++
        let type = 'ident'
        if (KEYWORDS[m]) type = 'keyword'
        else if (ATOMS[m]) type = 'atom'
        else if (src.charAt(j) === '(') type = 'call'
        tokens.push({ t: type, v: m })
        i += m.length
        continue
      }

      m = matchAt(src, i, R_OPERATOR)
      if (m) {
        const tok = { t: 'op', v: m }
        if (m === '-' || m === '+' || m === '!' || m === '~') tok.unary = isUnaryOp(tokens[tokens.length - 1], m)
        tokens.push(tok)
        i += m.length
        continue
      }

      tokens.push({ t: 'punct', v: ch })
      i++
    }

    return tokens
  }

  const TIGHT_BEFORE = { ',': 1, ';': 1, ')': 1, ']': 1, '}': 1, '.': 1 }
  const TIGHT_AFTER = { '(': 1, '[': 1, '.': 1 }

  function isPunct(tok, v) {
    return tok && tok.t === 'punct' && tok.v === v
  }

  function isValue(tok) {
    return tok && ['ident', 'call', 'field', 'number', 'string', 'atom'].indexOf(tok.t) >= 0
  }

  function needSpace(prev, cur) {
    if (!prev) return false

    const p = prev.v
    const c = cur.v

    if (cur.t === 'punct' && TIGHT_BEFORE[c]) {
      if (c === '}') return !isPunct(prev, '{')
      return false
    }
    if (prev.t === 'punct' && TIGHT_AFTER[p]) return false

    // ++/-- 贴紧
    if ((cur.t === 'op' && (c === '++' || c === '--')) || (prev.t === 'op' && (p === '++' || p === '--'))) return false

    // 一元运算符后不空格
    if (prev.t === 'op' && prev.unary) return false

    // 调用与索引
    if (c === '(' && (isValue(prev) || isPunct(prev, ')') || isPunct(prev, ']'))) return false
    if (c === '[' && (isValue(prev) || isPunct(prev, ')') || isPunct(prev, ']'))) return false

    // 空块 {}
    if (isPunct(prev, '{') && cur.t === 'punct' && c === '}') return false

    // 块花括号
    if (cur.t === 'punct' && c === '{') return true
    if (prev.t === 'punct' && p === '{') return true

    return true
  }

  function renderTokens(tokens) {
    let out = ''

    for (let k = 0; k < tokens.length; k++) {
      const tok = tokens[k]

      if (tok.t === 'comment') {
        out += out ? ' ' + tok.v : tok.v
        continue
      }

      if (needSpace(tokens[k - 1], tok)) out += ' '
      out += tok.v
    }

    return out.replace(/[ \t]+$/, '')
  }

  function countBraces(tokens) {
    let n = 0
    tokens.forEach((tok) => {
      if (tok.t !== 'punct') return
      if (tok.v === '{') n++
      else if (tok.v === '}') n--
    })
    return n
  }

  function format(code) {
    if (typeof code !== 'string' || !code.trim()) return code

    try {
      let src = code.trim()
      const hasPrefix = src.indexOf('{{{{') === 0
      const hasSuffix = src.slice(-4) === '}}}}'
      if (hasPrefix !== hasSuffix) return code

      let wrapped = false
      if (hasPrefix) {
        wrapped = true
        src = src.slice(4, -4)
      }

      const tokens = lex(src)
      const lines = []
      let line = []

      tokens.forEach((tok) => {
        if (tok.t === 'nl') {
          lines.push(line)
          line = []
        } else if (tok.t === 'comment' && tok.v.indexOf('\n') >= 0) {
          line.push(tok)
          lines.push(line)
          line = []
        } else {
          line.push(tok)
        }
      })
      if (line.length) lines.push(line)

      const output = []
      let depth = 0

      lines.forEach((lineTokens) => {
        if (!lineTokens.length) {
          if (output.length && output[output.length - 1] !== '') output.push('')
          return
        }

        let lineDepth = depth
        if (isPunct(lineTokens[0], '}')) lineDepth = Math.max(0, depth - 1)

        const text = renderTokens(lineTokens)
        output.push(text ? '  '.repeat(lineDepth) + text : '')
        depth = Math.max(0, depth + countBraces(lineTokens))
      })

      while (output.length && output[output.length - 1] === '') output.pop()

      let formatted = output.join('\n')
      if (wrapped) formatted = '{{{{' + formatted + '}}}}'
      return formatted
    } catch (err) {
      if (root.rb && root.rb.env === 'dev') console.warn('Cannot format aviator code : ' + err)
      return code
    }
  }

  // ~~ 注册
  if (root.CodeMirror && root.CodeMirror.defineMode) {
    root.CodeMirror.defineMode(MODE_NAME, createMode)
    root.CodeMirror.defineMIME(MIME, MODE_NAME)
  }

  root.AviatorCode = {
    MODE: MIME,
    setFunctions: setFunctions,
    getFunctions: function () {
      return Object.keys(customFuncs)
    },
    format: format,
  }
  root.$formatAviator = format
})(typeof window !== 'undefined' ? window : this)
