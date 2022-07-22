/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 字段值设置
// eslint-disable-next-line no-unused-vars
class FieldValueSet extends React.Component {
  render() {
    const field = this.props.field
    if (['ID', 'AVATAR', 'IMAGE', 'FILE', 'BARCODE', 'SERIES', 'SIGN'].includes(field.type)) {
      return <div className="form-control-plaintext text-danger">{$L('暂不支持')}</div>
    }

    if (
      field.type === 'PICKLIST' ||
      field.type === 'STATE' ||
      field.type === 'MULTISELECT' ||
      field.type === 'BOOL' ||
      field.type === 'REFERENCE' ||
      field.type === 'N2NREFERENCE' ||
      field.type === 'CLASSIFICATION'
    ) {
      return (
        <select className="form-control form-control-sm" multiple={field.type === 'MULTISELECT' || field.type === 'N2NREFERENCE'} ref={(c) => (this._$value = c)} key={field.name}>
          {(field.options || []).map((item) => {
            let value = item.id || item.mask
            // for BOOL
            if (item.id === false) value = 'false'
            if (item.id === true) value = 'true'

            return (
              <option key={value} value={value}>
                {item.text}
              </option>
            )
          })}
        </select>
      )
    }

    return <input className="form-control form-control-sm" placeholder={this.props.placeholder} ref={(c) => (this._$value = c)} key={field.name} maxLength="255" />
  }

  componentDidMount() {
    if (!this._$value) return

    const field = this.props.field
    if (this._$value.tagName === 'SELECT') {
      if (field.type === 'REFERENCE' || field.type === 'N2NREFERENCE' || field.type === 'CLASSIFICATION') {
        this.__$select2 = $initReferenceSelect2(this._$value, {
          entity: this.props.entity,
          name: field.name,
          label: field.label,
          searchType: field.type === 'CLASSIFICATION' ? 'classification' : null,
          placeholder: this.props.placeholder || ' ',
        })
      } else {
        this.__$select2 = $(this._$value).select2({
          placeholder: this.props.placeholder || '',
        })
      }
      this.__$select2.val(null).trigger('change')
    } else if (field.type === 'DATE' || field.type === 'DATETIME' || field.type === 'TIME') {
      let dpcfg = {
        format: field.type === 'DATE' ? 'yyyy-mm-dd' : 'yyyy-mm-dd hh:ii:ss',
        minView: field.type === 'DATE' ? 'month' : 0,
      }

      if (field.type === 'TIME') {
        dpcfg = {
          format: 'hh:ii:ss',
          startView: 1,
          minView: 0,
          maxView: 1,
          title: $L('选择时间'),
        }
      }

      this.__$datetimepicker = $(this._$value).datetimepicker(dpcfg)
    }
  }

  componentWillUnmount() {
    if (this.__$select2) {
      this.__$select2.select2('destroy')
      this.__$select2 = null
    }
    if (this.__$datetimepicker) {
      this.__$datetimepicker.datetimepicker('remove')
      this.__$datetimepicker = null
    }
  }

  val() {
    if (!this._$value) return null

    const field = this.props.field
    let value
    if (field.type === 'MULTISELECT') {
      let maskValue = 0
      this.__$select2.val().forEach((mask) => (maskValue += ~~mask))
      value = maskValue
    } else if (this._$value.tagName === 'SELECT') {
      value = this.__$select2.val()
    } else {
      value = $(this._$value).val()
    }

    if (!value) return null
    if (typeof value === 'object' && value.length === 0) return null // 空数组

    // 验证

    if (field.type === 'NUMBER' || field.type === 'DECIMAL') {
      if (isNaN(value)) {
        RbHighbar.create($L('%s 格式不正确', field.label))
        return null
      } else if ($isTrue(field.notNegative) && ~~value < 0) {
        RbHighbar.create($L('%s 不能为负数', field.label))
        return null
      }
    } else if (field.type === 'EMAIL') {
      if (!$regex.isMail(value)) {
        RbHighbar.create($L('%s 格式不正确', field.label))
        return null
      }
    } else if (field.type === 'URL') {
      if (!$regex.isUrl(value)) {
        RbHighbar.create($L('%s 格式不正确', field.label))
        return null
      }
    } else if (field.type === 'PHONE') {
      if (!$regex.isTel(value)) {
        RbHighbar.create($L('%s 格式不正确', field.label))
        return null
      }
    }

    return typeof value === 'object' ? value.join(',') : value
  }

  setValue(val) {
    console.log(val)
  }
}

// 获取字段值文本
FieldValueSet.formatFieldText = function (value, field) {
  if (!value) return null
  if (!field) return value

  if (field.options) {
    if (field.type === 'MULTISELECT') {
      const texts = []
      field.options.forEach((item) => {
        if ((value & item.mask) !== 0) texts.push(item.text)
      })
      return texts.join(', ')
    } else {
      const found = field.options.find((x) => $is(x.id, value))
      return found ? found.text : value.toUpperCase()
    }
  }

  if (field.type === 'REFERENCE' || field.type === 'N2NREFERENCE' || field.type === 'CLASSIFICATION') {
    const result = $.ajax({
      url: `/commons/search/read-labels?ids=${value}`,
      async: false,
    }).responseJSON

    if (result && result.data) {
      if (field.type === 'N2NREFERENCE') {
        const texts = []
        value.split(',').forEach((item) => {
          texts.push(result.data[item])
        })
        value = texts.join(', ')
      } else {
        value = result.data[value]
      }
    } else {
      value = `@${value.toUpperCase()}`
    }
  }

  return value
}
