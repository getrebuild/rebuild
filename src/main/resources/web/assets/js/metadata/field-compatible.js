/*!
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
// 字段类型必须对应
const FT_TYPE2TYPE = ['FILE', 'IMAGE', 'AVATAR', 'SIGN']

/**
 * 字段兼容判断
 * see backend `Easyield#convertCompatibleValue`
 *
 * @param s 源字段
 * @param t 目标字段
 * @returns {boolean}
 */
// eslint-disable-next-line no-unused-vars
function $fieldIsCompatible(s, t) {
  if (!s || !t) {
    console.log('Bad fileds of source or target :', s, t)
    return false
  }

  // 必须对应
  if (FT_TYPE2TYPE.includes(s.type) && s.type !== t.type) return false

  // 判断附加参数
  if ((t.type === 'REFERENCE' || t.type === 'N2NREFERENCE' || t.type === 'ID') && t.ref && s.ref && t.ref[0] === s.ref[0]) return true
  if (t.type === 'CLASSIFICATION' && t.classification && t.classification === s.classification) return true
  if (t.type === 'STATE' && t.stateClass && t.stateClass === s.stateClass) return true
  if (t.type === 'ANYREFERENCE' && (s.type === 'REFERENCE' || s.type === 'N2NREFERENCE' || s.type === 'ID')) return true

  if (t.type === s.type) return true
  const allow = FT_COMPATIBLE[t.type] || []
  return allow.includes('*') || allow.includes(s.type)
}
