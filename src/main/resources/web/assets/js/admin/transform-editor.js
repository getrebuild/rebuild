/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

$(document).ready(() => {
  let advFilter
  $('#useFilter').click(() => {
    if (advFilter) {
      advFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$L('SetAdvFiletr')} inModal={true} canNoFilters={true} entity={wpc.sourceEntity.entity} filter={advFilter_data} confirm={_saveFilter} />, null, function () {
        advFilter = this
      })
    }
  })

  const config = wpc.config || {}

  let fieldsMapping
  let fieldsMappingDetail
  renderRbcomp(<FieldsMapping source={wpc.sourceEntity} target={wpc.targetEntity} data={config.fieldsMapping} />, 'EMAIN', function () {
    fieldsMapping = this
  })
  if (wpc.sourceDetailEntity) {
    renderRbcomp(<FieldsMapping source={wpc.sourceDetailEntity} target={wpc.targetDetailEntity} data={config.fieldsMappingDetail} />, 'EDETAIL', function () {
      fieldsMappingDetail = this
    })
  }

  const fillbackFields = []
  wpc.sourceEntity.fields.forEach((item) => {
    if (!item.id.includes('.') && item.type === `REFERENCE.${wpc.targetEntity.entity}`) {
      fillbackFields.push(item)
    }
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

  const $btn = $('.J_save').click(function () {
    const fm = fieldsMapping.buildMapping()
    if (!fm) return
    let fmd
    if (fieldsMappingDetail) {
      fmd = fieldsMappingDetail.buildMapping()
      if (!fmd) return
    }

    const config = {
      fieldsMapping: fm,
      fieldsMappingDetail: fmd,
      fillbackField: $('#fillbackField').val(),
      useFilter: advFilter_data,
    }

    const _data = {
      metadata: { entity: 'TransformConfig', id: wpc.configId },
      config: config,
    }

    $btn.button('loading')
    $.post('/app/entity/record-save', JSON.stringify(_data), (res) => {
      if (res.error_code === 0) location.href = '../transforms'
      else RbHighbar.error(res.error_msg)
      $btn.button('reset')
    })
  })

  // Load
  setTimeout(() => {
    _saveFilter(config.useFilter)

    if (config.fillbackField) {
      $('#fillbackField').val(config.fillbackField).trigger('change')
    }
  }, 100)
})

class FieldsMapping extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  componentDidMount() {
    const that = this
    const _data = this.props.data || {}
    $(this._fieldsMapping)
      .find('select')
      .each(function () {
        const $this = $(this)
        const field = $this.data('field')
        const ftc = $this.data('ftc')
        const req = $this.data('req')

        const sourceFields = []
        that.props.source.fields.forEach((item) => {
          if ((item.type === 'ID' && ftc === `REFERENCE.${that.props.source.entity}`)
              || _isCompatible(item, ftc)) sourceFields.push(item)
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
          .on('change', function () {
            if ($this.val()) $this.parents('.row').addClass('active')
            else $this.parents('.row ').removeClass('active')
          })
          .val(_data[field] || null)
          .trigger('change')
      })
  }

  render() {
    const _source = this.props.source
    const _target = this.props.target
    return (
      <div ref={(c) => (this._fieldsMapping = c)}>
        <div className="row">
          <div className="col-7">
            <div className="form-control-plaintext text-bold">{_source.label}</div>
          </div>
          <div className="col-5">
            <div className="form-control-plaintext text-bold">{_target.label}</div>
          </div>
        </div>
        {_target.fields.map((item) => {
          return (
            <div className="row mb-1" key={item.id}>
              <div className="col-7">
                <select className="form-control form-control-sm" data-ftc={item.type} data-req={!item.nullable} data-field={item.id}></select>
              </div>
              <div className="col-5">
                <span className="badge">
                  {!item.nullable && <i title={$L('Required')}></i>}
                  {item.text}
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
          const label = $this.parent().next().find('.badge').text()
          RbHighbar.create($L('PlsSelectSourceField').replace('%s', label))
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

function _isCompatible(field, ftc) {
  if (field.type === ftc) return true
  const allow = FT_COMPATIBLE[ftc] || []
  return allow.includes('*') || allow.includes(field.type)
}

let advFilter_data
function _saveFilter(res) {
  advFilter_data = res
  if (advFilter_data && advFilter_data.items && advFilter_data.items.length > 0) $('#useFilter').text(`${$L('AdvFiletrSeted')} (${advFilter_data.items.length})`)
  else $('#useFilter').text($L('ClickSet'))
}
