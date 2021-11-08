/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global InitModels */

$(document).ready(function () {
  const $btn = $('.btn-primary').click(function () {
    const entityLabel = $val('#entityLabel'),
      comments = $val('#comments')
    if (!entityLabel) {
      RbHighbar.create($L('请输入实体名称'))
      return
    }

    const data = {
      label: entityLabel,
      comments: comments,
    }
    if ($val('#isDetail')) {
      data.mainEntity = $val('#mainEntity')
      if (!data.mainEntity) {
        RbHighbar.create($L('请选择主实体'))
        return
      }
    }

    $btn.button('loading')
    $.post(`/admin/entity/entity-new?nameField=${$val('#nameField')}`, JSON.stringify(data), function (res) {
      if (res.error_code === 0) parent.location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`
      else RbHighbar.error(res.error_msg)
    })
  })

  let entityLoaded = false
  $('#isDetail').click(function () {
    $('.J_mainEntity').toggleClass('hide')
    parent.RbModal.resize()
    if (entityLoaded === false) {
      entityLoaded = true
      $.get('/admin/entity/entity-list', function (res) {
        $(res.data).each(function () {
          if (!this.detailEntity) $(`<option value="${this.entityName}">${this.entityLabel}</option>`).appendTo('#mainEntity')
        })
      })
    }
  })

  $('.nav-tabs a').click(() => parent.RbModal.resize())

  let _MetaschemaList
  $('.J_imports').click(() => {
    if (_MetaschemaList) return
    renderRbcomp(<MetaschemaList />, 'metaschemas', function () {
      _MetaschemaList = this
    })
  })
})

class MetaschemaList extends React.Component {
  render() {
    return (
      <div>
        <InitModels ref={(c) => (this._InitModels = c)} onLoad={() => parent.RbModal.resize()} />
        <div className="dialog-footer">
          <button className="btn btn-primary" onClick={() => this.imports()} ref={(c) => (this._$btn = c)}>
            {$L('开始导入')}
          </button>
          <p className="protips mt-2">{$L('可在导入后根据自身需求做适当调整/修改')}</p>
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
