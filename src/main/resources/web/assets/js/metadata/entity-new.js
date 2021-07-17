/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

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

  let indexLoaded = false
  $('.J_imports').click(() => {
    if (indexLoaded) return
    renderRbcomp(<MetaschemaList />, 'metaschemas')
    // eslint-disable-next-line react/jsx-no-undef
    // renderRbcomp(<BusinessModelBuilder />, 'metaschemas')
    indexLoaded = true
  })
})

class MetaschemaList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return (
      <div>
        {this.state.indexes ? (
          <div className="rbs-indexes ">
            {this.state.indexes.map((item) => {
              return (
                <div key={'data-' + item.file}>
                  <div className="float-left" style={{ width: '80%' }}>
                    <h5>{item.name}</h5>
                    <div className="text-muted text-ellipsis" title={item.desc}>
                      {item.desc}
                    </div>
                  </div>
                  <div className="float-right">
                    {item.exists ? (
                      <button disabled className="btn btn-sm btn-primary">
                        {$L('已存在')}
                      </button>
                    ) : (
                      <button disabled={this.state.inProgress === true} className="btn btn-sm btn-primary" onClick={() => this.imports(item)}>
                        {$L('导入')}
                      </button>
                    )}
                  </div>
                  <div className="clearfix"></div>
                </div>
              )
            })}
          </div>
        ) : (
          <RbSpinner fully={true} />
        )}
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/rbstore/load-metaschemas', (res) => {
      if (res.error_code === 0) {
        this.setState({ indexes: res.data }, () => {
          parent.RbModal.resize()
        })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  imports(item) {
    const tips = [`<strong>${$L('导入')} [ ${item.name} ]</strong>`]
    if ((item.refs || []).length > 0) {
      const refNames = []
      this.state.indexes.forEach((bar) => {
        if (item.refs.includes(bar.key) && !bar.exists) refNames.push(bar.name)
      })
      if (refNames.length > 0) tips.push($L('导入本实体将同时导入 **%s** 等依赖实体。', `[ ${refNames.join(', ')} ]`))
    }
    tips.push($L('你可在导入后进行适当调整。开始导入吗？'))

    const that = this
    const $mp2 = parent && parent.$mp ? parent.$mp : $mp
    parent.RbAlert.create(tips.join('<br>'), {
      html: true,
      confirm: function () {
        this.hide()
        that.setState({ inProgress: true })

        $mp2.start()
        $.post(`/admin/metadata/imports?key=${item.key}`, (res) => {
          $mp2.end()
          that.setState({ inProgress: false })
          if (res.error_code === 0) {
            RbHighbar.success($L('导入成功'))
            setTimeout(() => (parent.location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`), 1500)
          } else {
            RbHighbar.error(res.error_msg)
          }
        })
      },
    })
  }
}
