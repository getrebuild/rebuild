/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global InitModels */

$(document).ready(function () {
  const $new = $('.btn-primary.new').on('click', function () {
    const entityLabel = $val('#entityLabel'),
      comments = $val('#comments')
    if (!entityLabel) return RbHighbar.create($L('请输入实体名称'))

    const data = {
      label: entityLabel,
      comments: comments,
    }

    if ($val('#isDetail')) {
      data.mainEntity = $val('#mainEntity')
      if (!data.mainEntity) return RbHighbar.create($L('请选择主实体'))
    }

    $new.button('loading')
    $.post(`/admin/entity/entity-new?nameField=${$val('#nameField')}`, JSON.stringify(data), (res) => {
      if (res.error_code === 0) parent.location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`
      else RbHighbar.error(res.error_msg)
    })
  })

  const $copy = $('.btn-primary.copy').on('click', () => {
    const sourceEntity = $val('#copySourceEntity')
    if (!sourceEntity) RbHighbar.create($L('请选择从哪个实体复制'))

    const entityLabel = $val('#newEntityLabel')
    if (!entityLabel) return RbHighbar.create($L('请输入实体名称'))

    const data = {
      sourceEntity: sourceEntity,
      entityName: entityLabel,
      detailEntityName: $val('#newDetailLabel'),
      keepConfig: [],
    }

    $copy.button('loading')
    $.post('/admin/entity/entity-copy', JSON.stringify(data), (res) => {
      if (res.error_code === 0) parent.location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`
      else RbHighbar.error(res.error_msg)
    })
  })

  let entities
  function _loadEntities(call) {
    if (entities) {
      typeof call === 'function' && call(entities)
    } else {
      $.get('/admin/entity/entity-list', (res) => {
        entities = res.data
        typeof call === 'function' && call(entities)
      })
    }
  }

  _loadEntities((e) => {
    e.forEach(function (item) {
      $(`<option value="${item.entityName}" data-detail="${item.detailEntity || ''}">${item.entityLabel}</option>`).appendTo('#copySourceEntity')
    })

    $('#copySourceEntity')
      .on('change', function () {
        const $s = $('#copySourceEntity option:selected')
        if ($s.data('detail')) $('.J_newDetailLabel').removeClass('hide')
        else $('.J_newDetailLabel').addClass('hide')

        parent.RbModal.resize()
      })
      .trigger('change')
  })

  $('#isDetail').on('click', function () {
    $('.J_mainEntity').toggleClass('hide')
    parent.RbModal.resize()

    if ($('#mainEntity option').length === 0) {
      _loadEntities((e) => {
        e.forEach(function (item) {
          if (!item.detailEntity) $(`<option value="${item.entityName}">${item.entityLabel}</option>`).appendTo('#mainEntity')
        })

        if (e.length === 0) $(`<option value="">${$L('无可用实体')}</option>`).appendTo('#mainEntity')
      })
    }
  })

  let _MetaschemaList
  $('.J_imports').on('click', () => {
    if (_MetaschemaList) return
    renderRbcomp(<MetaschemaList />, 'metaschemas', function () {
      _MetaschemaList = this
    })
  })

  $('.nav-tabs a').on('click', () => parent.RbModal.resize())
})

class MetaschemaList extends React.Component {
  render() {
    return (
      <div>
        <InitModels ref={(c) => (this._InitModels = c)} onLoad={() => parent.RbModal.resize()} />
        <div className="dialog-footer">
          <div className="float-right">
            <button className="btn btn-primary" onClick={() => this.imports()} ref={(c) => (this._$btn = c)}>
              {$L('开始导入')}
            </button>
          </div>
          <div className="float-right">
            <p className="protips mt-2 pr-2">{$L('可在导入后根据自身需求做适当调整/修改')}</p>
          </div>
          <div className="clearfix" />
        </div>
      </div>
    )
  }

  componentDidMount() {
    parent.RbModal.resize()
  }

  imports() {
    const s = this._InitModels.getSelected()
    if (s.length < 1) {
      return RbHighbar.create($L('请选择要导入的实体'))
    }

    const $btn = $(this._$btn).button('loading')
    const $mp2 = parent && parent.$mp ? parent.$mp : $mp
    $mp2.start()
    $.post(`/admin/metadata/imports?key=${s.join(',')}`, (res) => {
      $mp2.end()
      $btn.button('reset')

      if (res.error_code === 0) {
        RbHighbar.success($L('导入成功'))
        setTimeout(() => parent.location.reload(), 1500)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}
