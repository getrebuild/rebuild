/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 字段类型兼容

const FT_COMPATIBLE = {
  NUMBER: ['DECIMAL'],
  DECIMAL: ['NUMBER'],
  DATE: ['DATETIME'],
  DATETIME: ['DATE'],
  TEXT: ['*'],
  NTEXT: ['*'],
}
/**
 * 字段兼容判断
 * see backend `FieldValueCompatibleConversion.java`
 *
 * @param s 源字段
 * @param t 目标字段
 * @returns {boolean}
 */
// eslint-disable-next-line no-unused-vars
function $fieldIsCompatible(s, t) {
  // 必须对应
  if (s.type === 'FILE' && t.type !== 'FILE') return false
  if (s.type === 'IMAGE' && t.type !== 'IMAGE') return false
  if (s.type === 'AVATAR' && t.type !== 'AVATAR') return false

  // 判断附加参数
  if (t.type === 'REFERENCE' || t.type === 'N2NREFERENCE' || t.type === 'ID') {
    return t.ref && s.ref && t.ref[0] === s.ref[0]
  }
  if (t.type === 'CLASSIFICATION') {
    return t.classification && t.classification === s.classification
  }
  if (t.type === 'STATE') {
    return t.stateClass && t.stateClass === s.stateClass
  }

  if (t.type === s.type) return true
  const allow = FT_COMPATIBLE[t.type] || []
  return allow.includes('*') || allow.includes(s.type)
}
