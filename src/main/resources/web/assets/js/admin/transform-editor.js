/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig

$(document).ready(() => {
  let advFilter
  $('#useFilter').on('click', () => {
    if (advFilter) {
      advFilter.show()
    } else {
      renderRbcomp(<AdvFilter title={$L('附加过滤条件')} inModal={true} canNoFilters={true} entity={wpc.sourceEntity.entity} filter={advFilter_data} confirm={_saveFilter} />, null, function () {
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
    if (!item.name.includes('.') && item.type === 'REFERENCE' && item.ref[0] === wpc.targetEntity.entity) {
      fillbackFields.push({ id: item.name, text: item.label })
    }
  })

  $('#fillbackField')
    .select2({
      placeholder: `${$L('(可选)')}`,
      data: fillbackFields,
      allowClear: true,
      language: {
        noResults: () => $L('无可用字段'),
      },
    })
    .val(null)
    .trigger('change')

  const $btn = $('.J_save').on('click', function () {
    const fm = fieldsMapping.buildMapping()
    if (!fm) {
      RbHighbar.create($L('请至少添加 1 个字段映射'))
      return
    }

    const tips = []

    const fmd = fieldsMappingDetail ? fieldsMappingDetail.buildMapping() : null
    if (fieldsMappingDetail && !fmd) tips.push($L('明细实体未配置字段映射，因此明细记录不会转换'))

    function _save() {
      const config = {
        fieldsMapping: fm,
        fieldsMappingDetail: fmd,
        fillbackField: $('#fillbackField').val(),
        transformMode: $('#transformMode').prop('checked') ? 2 : 1,
        useFilter: advFilter_data,
      }

      const _data = {
        config: config,
        metadata: {
          entity: 'TransformConfig',
          id: wpc.configId,
        },
      }

      $btn.button('loading')
      $.post('/app/entity/common-save', JSON.stringify(_data), (res) => {
        if (res.error_code === 0) location.href = '../transforms'
        else RbHighbar.error(res.error_msg)
        $btn.button('reset')
      })
    }

    let unset = 0
    for (let k in fm) {
      if (fm[k] === null) unset++
    }
    for (let k in fmd || {}) {
      if (fmd[k] === null) unset++
    }

    if (unset > 0) tips.push($L('部分必填字段未映射，可能导致转换失败'))

    if (tips.length > 0) {
      RbAlert.create(
        <React.Fragment>
          <strong>{$L('配置存在以下问题，请确认是否继续保存？')}</strong>
          <div className="mt-1">{tips.join(' / ')}</div>
        </React.Fragment>,
        {
          onConfirm: function () {
            this.disabled(true)
            _save()
          },
        }
      )
    } else {
      _save()
    }
  })

  // Load
  setTimeout(() => {
    _saveFilter(config.useFilter)

    if (config.fillbackField) {
      $('#fillbackField').val(config.fillbackField).trigger('change')
    }
    if (config.transformMode === 2) {
      $('#transformMode').attr('checked', true)
    }
  }, 100)
})

class FieldsMapping extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  componentDidMount() {
    const mapping = this.props.data || {}
    const that = this

    $(this._$fieldsMapping)
      .find('select')
      .each(function () {
        const $this = $(this)

        const fieldName = $this.data('field')
        const targetField = that.props.target.fields.find((x) => fieldName === x.name)

        const sourceFields = []
        that.props.source.fields.forEach((item) => {
          if ($fieldIsCompatible(item, targetField)) {
            sourceFields.push({ id: item.name, text: item.label })
          }
        })

        $this
          .select2({
            placeholder: $L('选择源字段'),
            allowClear: true,
            data: sourceFields,
            language: {
              noResults: () => $L('无可用字段'),
            },
          })
          .on('change', function () {
            if ($this.val()) $this.parents('.row').addClass('active')
            else $this.parents('.row ').removeClass('active')
          })
          .val(mapping[fieldName] || null)
          .trigger('change')
      })
  }

  render() {
    const _source = this.props.source
    const _target = this.props.target

    if (!_target.fields || _target.fields.length === 0) {
      return <RbAlertBox message={$L('无可用字段')} />
    }

    return (
      <div ref={(c) => (this._$fieldsMapping = c)}>
        <div className="row mb-0">
          <div className="col-7">
            <div className="form-control-plaintext text-bold">{_source.label}</div>
          </div>
          <div className="col-5">
            <div className="form-control-plaintext text-bold">{_target.label}</div>
          </div>
        </div>
        {_target.fields.map((item) => {
          return (
            <div className="row" key={item.name}>
              <div className="col-7">
                <select className="form-control form-control-sm" data-field={item.name} data-req={!item.nullable} />
              </div>
              <div className="col-5">
                <span className={`badge ${item.nullable ? '' : 'req'}`}>{item.label}</span>
              </div>
            </div>
          )
        })}
      </div>
    )
  }

  buildMapping() {
    const mapping = {}
    let hasMapping = false
    $(this._$fieldsMapping)
      .find('select')
      .each(function () {
        const $this = $(this)
        const req = $this.data('req')
        const field = $this.val()

        if (req && !field) {
          mapping[$this.data('field')] = null // tips
        }
        if (field) {
          mapping[$this.data('field')] = field
          hasMapping = true
        }
      })

    return hasMapping ? mapping : null
  }
}

let advFilter_data
function _saveFilter(res) {
  advFilter_data = res
  if (advFilter_data && advFilter_data.items && advFilter_data.items.length > 0) {
    $('#useFilter').text(`${$L('已设置条件')} (${advFilter_data.items.length})`)
  } else {
    $('#useFilter').text($L('点击设置'))
  }
}
