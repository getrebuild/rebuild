/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 字段类型
// eslint-disable-next-line no-unused-vars
const FIELD_TYPES = {
  'TEXT': [$L('文本'), 'mdi-form-textbox'],
  'NTEXT': [$L('多行文本'), 'mdi-form-textarea'],
  'PHONE': [$L('电话'), 'mdi-phone-classic'],
  'EMAIL': [$L('邮箱'), 'mdi-email'],
  'URL': [$L('链接'), 'mdi-link'],
  'NUMBER': [$L('整数'), 'mdi-numeric'],
  'DECIMAL': [$L('小数'), 'mdi-numeric'],
  'SERIES': [$L('自动编号'), 'mdi-alphabetical'],
  'DATE': [$L('日期'), 'mdi-calendar-multiselect-outline'],
  'DATETIME': [$L('日期时间'), 'mdi-calendar-multiselect-outline'],
  'TIME': [$L('时间'), 'mdi-clock-time-seven-outline'],
  'PICKLIST': [$L('下拉列表'), 'mdi-form-select'],
  'CLASSIFICATION': [$L('分类'), 'mdi-form-dropdown'],
  'MULTISELECT': [$L('多选'), 'mdi-format-list-checks'],
  'TAG': [$L('标签'), 'mdi-tag-outline', true],
  'REFERENCE': [$L('引用'), 'mdi-feature-search-outline'],
  'N2NREFERENCE': [$L('多引用'), 'mdi-text-box-search-outline'],
  'FILE': [$L('附件'), 'mdi-attachment'],
  'IMAGE': [$L('图片'), 'mdi-image-outline'],
  'AVATAR': [$L('头像'), 'mdi-account-box-outline'],
  'BARCODE': [$L('二维码'), 'mdi-qrcode'],
  'LOCATION': [$L('位置'), 'mdi-map-marker'],
  'SIGN': [$L('签名'), 'mdi-file-sign'],
  'BOOL': [$L('布尔'), 'mdi-toggle-switch-off-outline'],
  'STATE': [$L('状态'), 'mdi-language-java', true],
  'ANYREFERENCE': [$L('任意引用'), 'mdi-language-java', true],
}
