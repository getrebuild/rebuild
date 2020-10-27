/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

let fieldsMapping

$(document).ready(() => {
  $.get(`/commons/metadata/fields?entity=${wpc.sourceEntity}&deep=2`, (res) => {
    const sourceFields = res.data

    const fillbackFields = []
    sourceFields.forEach((item) => {
      if (!item.name.includes('.') && item.type === 'REFERENCE' && item.ref[0] === wpc.targetEntity) {
        fillbackFields.push({
          id: item.name,
          text: item.label,
        })
      }
    })

    $.get(`/commons/metadata/fields?entity=${wpc.targetEntity}`, (res) => {
      sourceFields.splice(0, 0, { name: wpc.sourcePrimary, label: 'ID', type: 'REFERENCE', ref: [wpc.sourceEntity, 0] })
      renderRbcomp(<FieldsMapping sourceFields={sourceFields} targetFields={res.data} />, 'fieldsMapping', function () {
        fieldsMapping = this
      })
    })

    $('#fillbackField')
      .select2({
        placeholder: `(${$L('Optional')})`,
        data: fillbackFields,
        allowClear: true,
        language: {
          noResults: () => $L('NoUsesField'),
        },
      })
      .val(null)
      .trigger('change')
  })

  $('.J_save').click(function () {
    console.log(fieldsMapping.buildMapping())
  })
})

class FieldsMapping extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  componentDidMount() {
    const that = this
    $(this._fieldsMapping)
      .find('select')
      .each(function () {
        const $this = $(this)
        const ftc = $this.data('ftc')
        const req = $this.data('req')

        const sourceFields = []
        that.props.sourceFields.forEach((item) => {
          if (_isCompatible(item, ftc)) {
            sourceFields.push({
              id: item.name,
              text: item.label,
            })
          }
        })

        $this
          .select2({
            placeholder: $L('SelectSome,SourceField') + (req ? '' : ` (${$L('Optional')})`),
            allowClear: true,
            data: sourceFields,
            language: {
              noResults: () => $L('NoUsesField'),
            },
          })
          .val(null)
          .trigger('change')
      })
  }

  render() {
    return (
      <div ref={(c) => (this._fieldsMapping = c)}>
        {this.props.targetFields.map((item) => {
          if (!item.creatable) return null // 不可创建

          return (
            <div className="row mb-1" key={item.name}>
              <div className="col-7">
                <select className="form-control form-control-sm" data-ftc={_getFullType(item)} data-req={!item.nullable} data-field={item.name} data-title={item.label}></select>
              </div>
              <div className="col-5">
                <span className="badge">
                  {!item.nullable && <i title={$L('Required')}></i>}
                  {item.label}
                </span>
              </div>
            </div>
          )
        })}
      </div>
    )
  }

  buildMapping() {
    let mapping = {}
    $(this._fieldsMapping)
      .find('select')
      .each(function () {
        const $this = $(this)
        const req = $this.data('req')
        const val = $this.val()

        if (req && !val) {
          RbHighbar.create($L('PlsSelectSourceField').replace('%s', $this.data('title')))
          mapping = null
          return false
        }

        if (val) {
          mapping[$this.data('field')] = val
        }
      })
    return mapping
  }
}

// 字段类型兼容
const FT_COMPATIBLE = {
  NUMBER: ['DECIMAL'],
  DECIMAL: ['NUMBER'],
  DATE: ['DATETIME'],
  DATETIME: ['DATE'],
  TEXT: ['*'],
  NTEXT: ['*'],
}

function _getFullType(field) {
  if (field.ref) return `${field.type}-${field.ref[0]}`
  else if (field.stateClass) return `${field.type}-${field.stateClass}`
  else return field.type
}

function _isCompatible(field, ftc) {
  const ftc2 = _getFullType(field)
  if (ftc2 === ftc) return true

  const cc = FT_COMPATIBLE[ftc] || []
  return cc.includes('*') || cc.includes(ftc2)
}
