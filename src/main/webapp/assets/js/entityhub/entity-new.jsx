/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  const sbtn = $('.btn-primary').click(function () {
    const entityLabel = $val('#entityLabel'),
      comments = $val('#comments')
    if (!entityLabel) {
      RbHighbar.create('请输入实体名称')
      return
    }

    const _data = { label: entityLabel, comments: comments }
    // eslint-disable-next-line eqeqeq
    if ($val('#isSlave') == true) {
      _data.masterEntity = $val('#masterEntity')
      if (!_data.masterEntity) {
        RbHighbar.create('请选择选择主实体')
        return
      }
    }

    sbtn.button('loading')
    $.post(rb.baseUrl + '/admin/entity/entity-new?nameField=' + $val('#nameField'), JSON.stringify(_data), function (res) {
      if (res.error_code === 0) parent.location.href = rb.baseUrl + '/admin/entity/' + res.data + '/base'
      else RbHighbar.error(res.error_msg)
      sbtn.button('reset')
    })
  })

  let entitiesLoaded = false
  $('#isSlave').click(function () {
    $('.J_masterEntity').toggleClass('hide')
    parent.RbModal.resize()
    if (entitiesLoaded === false) {
      entitiesLoaded = true
      $.get(rb.baseUrl + '/commons/metadata/entities', function (res) {
        $(res.data).each(function () {
          $('<option value="' + this.name + '">' + this.label + '</option>').appendTo('#masterEntity')
        })
      })
    }
  })

  $('.nav-tabs a').click(() => { parent.RbModal.resize() })

  let indexLoaded = false
  $('.J_imports').click(() => {
    if (indexLoaded) return
    renderRbcomp(<MetaschemaList />, 'metaschemas')
    indexLoaded = true
  })
})

class MetaschemaList extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }
  render() {
    return <div>
      {this.state.indexes ? <div className="rbs-indexes ">{this.state.indexes.map((item) => {
        return (<div key={'data-' + item.file}>
          <div className="float-left">
            <h5>{item.name}</h5>
            <div className="text-muted">
              数据来源 <a target="_blank" className="link" rel="noopener noreferrer" href={item.source}>{item.author || item.source}</a>
              {item.updated && (' · ' + item.updated)}
            </div>
          </div>
          <div className="float-right pt-1">
            <button disabled={this.state.inProgress === true} className="btn btn-sm btn-primary" data-file={item.file} data-name={item.name} onClick={this.imports}>导入</button>
          </div>
          <div className="clearfix"></div>
        </div>)
      })}</div>
        : <RbSpinner fully={true} />}
    </div>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/rbstore/load-index?type=metaschemas`, (res) => {
      if (res.error_code === 0) this.setState({ indexes: res.data }, () => { parent.RbModal.resize() })
      else RbHighbar.error(res.error_msg)
    })
  }

  imports = (e) => {
    const file = e.currentTarget.dataset.file
    const name = e.currentTarget.dataset.name
    const that = this
    const $mp2 = (parent && parent.$mp) ? parent.$mp : $mp
    parent.RbAlert.create(`<strong>导入 [ ${name} ]</strong><br>你可在导入后进行适当调整。开始导入吗？`, {
      html: true,
      confirm: function () {
        this.hide()
        that.setState({ inProgress: true })

        $mp2.start()
        $.post(`${rb.baseUrl}/admin/metaschema/imports?file=${$encode(file)}`, (res) => {
          $mp2.end()
          if (res.error_code === 0) {
            RbHighbar.success('导入完成')
            setTimeout(() => { parent.location.href = `../../entity/${res.data}/base` }, 1500)
          } else RbHighbar.error(res.error_msg)
        })
      }
    })
  }
}