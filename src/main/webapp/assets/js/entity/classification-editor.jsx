/* eslint-disable react/prop-types */
/* eslint-disable no-undef */
const wpc = window.__PageConfig
$(document).ready(function () {
  renderRbcomp(<LevelBoxes id={wpc.id} />, 'boxes')
  $('.J_imports').click(() => { renderRbcomp(<DlgImports id={wpc.id} />) })
  window.resize_handler()
})
window.resize_handler = function () {
  $('#boxes .rb-scroller').css('max-height', $(window).height() - 312)
}

class LevelBoxes extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.boxes = []
  }
  render() {
    return (<div className="row level-boxes">
      <LevelBox level={0} $$$parent={this} ref={(c) => this.boxes[0] = c} />
      <LevelBox level={1} $$$parent={this} ref={(c) => this.boxes[1] = c} />
      <LevelBox level={2} $$$parent={this} ref={(c) => this.boxes[2] = c} />
      <LevelBox level={3} $$$parent={this} ref={(c) => this.boxes[3] = c} />
    </div>)
  }
  componentDidMount() {
    this.notifyToggle(wpc.openLevel + 1, true)
    $('#boxes').removeClass('rb-loading-active')
    window.resize_handler()
    $('#boxes .rb-scroller').perfectScrollbar()
  }

  notifyItemActive(level, id) {
    if (level < 3) {
      this.boxes[level + 1].loadItems(id)
      for (let i = level + 2; i <= 3; i++) {
        this.boxes[i].clear(true)
      }
    }
  }
  notifyItemClean(level) {
    for (let i = level + 1; i <= 3; i++) {
      this.boxes[i].clear(true)
    }
  }
  notifyToggle(level, c) {
    let e = { target: { checked: c } }
    if (c === true) {
      for (let i = 1; i < level; i++) {
        this.boxes[i].turnToggle(e, true)
      }
    } else {
      for (let i = level + 1; i <= 3; i++) {
        this.boxes[i].turnToggle(e, true)
      }
    }
  }
}

const LNAME = ['一', '二', '三', '四']
class LevelBox extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, turnOn: props.level === 0 }
  }
  render() {
    let forId = 'turnOn-' + this.props.level
    return (
      <div className={'col-md-3 ' + (this.state.turnOn === true ? '' : 'off')}>
        <div className="float-left"><h5 className="text-bold">{LNAME[this.props.level]}级分类</h5></div>
        {this.props.level < 1 ? null :
          <div className="float-right">
            <div className="switch-button switch-button-xs">
              <input type="checkbox" id={forId} onChange={this.turnToggle} checked={this.state.turnOn} />
              <span><label htmlFor={forId} title="启用/禁用"></label></span>
            </div>
          </div>}
        <div className="clearfix"></div>
        <form className="mt-1" onSubmit={this.saveItem}>
          <div className="input-group input-group-sm">
            <input className="form-control" type="text" maxLength="50" placeholder="名称" value={this.state.itemName || ''} data-id="itemName" onChange={this.changeVal} />
            {(this.state.itemId && this.state.itemHide) && (<label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
              <input className="custom-control-input" type="checkbox" data-id="itemUnhide" onChange={this.changeVal} />
              <span className="custom-control-label">启用</span>
            </label>)}
            <div className="input-group-append"><button className="btn btn-primary" type="submit" disabled={this.state.inSave === true}>{this.state.itemId ? '保存' : '添加'}</button></div>
          </div>
        </form>
        <div className="rb-scroller mt-3">
          <ol className="dd-list unset-list">
            {(this.state.items || []).map((item) => {
              let active = this.state.activeId === item[0]
              return (
                <li className={'dd-item' + (active ? ' active' : '')} key={item[0]} onClick={() => this.clickItem(item[0])}>
                  <div className={'dd-handle' + (item[3] ? ' text-disabled' : '')}>{item[1]}{item[3] && <small />}</div>
                  <div className="dd-action">
                    <a><i className="zmdi zmdi-edit" title="编辑" onClick={(e) => this.editItem(item, e)}></i></a>
                    <a><i className="zmdi zmdi-delete" title="删除" onClick={(e) => this.delItem(item, e)}></i></a>
                  </div>
                  {active && <span className="zmdi zmdi-caret-right arrow hide"></span>}
                </li>
              )
            })}
          </ol>
        </div>
      </div>)
  }
  componentDidMount() {
    if (this.props.level === 0) this.loadItems()
  }

  loadItems(p) {
    this.parentId = p
    let url = `${rb.baseUrl}/admin/entityhub/classification/load-data-items?data_id=${wpc.id}&parent=${p || ''}`
    $.get(url, (res) => {
      this.clear()
      this.setState({ items: res.data, activeId: null })
    })
  }
  clickItem(id) {
    this.setState({ activeId: id })
    this.props.$$$parent.notifyItemActive(this.props.level, id)
  }

  turnToggle = (e, stop) => {
    let c = e.target.checked
    this.setState({ turnOn: c })
    if (stop !== true) {
      this.props.$$$parent.notifyToggle(this.props.level, c)
    }
    saveOpenLevel()
  }
  changeVal = (e) => {
    let s = {}
    s[e.target.dataset.id] = e.target.value
    this.setState(s)
  }
  saveItem = (e) => {
    e.preventDefault()
    let name = $.trim(this.state.itemName)
    if (!name) return
    if (this.props.level >= 1 && !this.parentId) {
      RbHighbar.create('请先选择上级分类项')
      return
    }

    let hasRepeat = false
    let that = this
    $(this.state.items).each(function () {
      if (this[1] === name && this[0] !== that.state.itemId) {
        hasRepeat = true
        return false
      }
    })
    if (hasRepeat) {
      RbHighbar.create('存在同名分类项')
      return
    }

    let url = `${rb.baseUrl}/admin/entityhub/classification/save-data-item?data_id=${wpc.id}&name=${name}`
    if (this.state.itemId) url += `&item_id=${this.state.itemId}`
    else url += `&parent=${this.parentId}&level=${this.props.level}`
    let isUnhide = null
    if (this.state.itemHide && this.state.itemUnhide) {
      url += '&hide=false'
      isUnhide = true
    }
    this.setState({ inSave: true })
    $.post(url, (res) => {
      if (res.error_code === 0) {
        let items = this.state.items || []
        if (this.state.itemId) {
          items.forEach((i) => {
            if (i[0] === this.state.itemId) {
              i[1] = name
              if (isUnhide === true) i[3] = false
            }
          })
        } else {
          items.insert(0, [res.data, name, null, false])
        }
        this.setState({ items: items, itemName: null, itemId: null, inSave: false })
      } else RbHighbar.error(res.error_msg)
    })
  }
  editItem(item, e) {
    e.stopPropagation()
    this.setState({ itemName: item[1], itemId: item[0], itemHide: item[3] })
  }
  delItem(item, e) {
    e.stopPropagation()
    let that = this
    let alertMsg = '删除后其子分类也将被一并删除。如果此分类项已被使用，使用了这些分类项的字段也将无法显示。确认删除吗？'
    let alertExt = {
      type: 'danger',
      confirm: function () {
        this.disabled()
        $.post(`${rb.baseUrl}/admin/entityhub/classification/delete-data-item?item_id=${item[0]}`, (res) => {
          this.hide()
          if (res.error_code !== 0) {
            RbHighbar.error(res.error_msg)
            return
          }
          RbHighbar.success('分类项已删除')
          let ns = []
          that.state.items.forEach((i) => {
            if (i[0] !== item[0]) ns.push(i)
          })
          that.setState({ items: ns }, () => {
            that.props.$$$parent.notifyItemClean(that.props.level)
          })
        })
        return false
      }
    }

    if (item[3] !== true) {
      alertMsg = '删除后其子分类也将被一并删除。如果此分类项已被使用，建议你禁用。否则已使用了这些分类项的字段将无法显示。'
      alertExt.confirmText = '确认删除'
      alertExt.cancelText = '禁用'
      alertExt.cancel = function () {
        let url = `${rb.baseUrl}/admin/entityhub/classification/save-data-item?item_id=${item[0]}&hide=true`
        this.disabled()
        $.post(url, (res) => {
          this.hide()
          if (res.error_code !== 0) {
            RbHighbar.error(res.error_msg)
            return
          }
          RbHighbar.success('分类项已禁用')
          let ns = []
          $(that.state.items || []).each(function () {
            if (this[0] === item[0]) this[3] = true
            ns.push(this)
          })
          that.setState({ items: ns })
        })
      }
    }
    RbAlert.create(alertMsg, alertExt)
  }

  clear(isAll) {
    if (isAll === true) this.parentId = null
    this.setState({ items: [], itemId: null, itemName: null, activeId: null })
  }
}

var saveOpenLevel_last = wpc.openLevel
var saveOpenLevel = function () {
  $setTimeout(() => {
    let level = $('.switch-button input:checkbox:checked:last').attr('id') || 't-0'
    level = ~~level.split('-')[1]
    if (saveOpenLevel_last === level) return

    let data = { openLevel: level }
    data.metadata = { entity: 'Classification', id: wpc.id }
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(data), (res) => {
      if (res.error_code > 0) RbHighbar.error(res.error_msg)
      else {
        saveOpenLevel_last = level
        RbHighbar.success('已启用' + LNAME[level] + '级分类')
      }
    })
  }, 500, 'saveOpenLevel')
}

class DlgImports extends RbModalHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return <RbModal title="导入分类数据" ref={(c) => this._dlg = c}>
      {this.state.indexes ? <div className="rbs-indexes">{this.state.indexes.map((item) => {
        return (<div key={'data-' + item.file}>
          <div className="float-left">
            <h5>{item.name}</h5>
            <div className="text-muted">
              数据来源 <a target="_blank" rel="noopener noreferrer" href={item.source}>{item.author || item.source}</a>
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
    </RbModal>
  }
  componentDidMount() {
    $.get(`${rb.baseUrl}/admin/rbstore/load-index?type=classifications`, (res) => {
      if (res.error_code === 0) this.setState({ indexes: res.data })
      else RbHighbar.error(res.error_msg)
    })
  }

  imports = (e) => {
    let file = e.currentTarget.dataset.file
    let name = e.currentTarget.dataset.name
    let url = `${rb.baseUrl}/admin/entityhub/classification/imports/start?dest=${this.props.id}&file=${$encode(file)}`
    let that = this
    RbAlert.create(`<strong>${name}</strong><br>请注意，导入将导致现有数据被清空。<br>如当前分类数据已被使用则不建议导入。确认导入吗？`, {
      html: true,
      confirm: function () {
        this.hide()
        that.setState({ inProgress: true })
        that.__mpro = new Mprogress({ template: 2, start: true, parent: '.rbmodal .modal-body' })
        $.post(url, (res) => {
          if (res.error_code === 0) that.__checkState(res.data)
          else RbHighbar.error(res.error_msg || '导入失败')
        })
      }
    })
  }

  __checkState(taskid) {
    $.get(`${rb.baseUrl}/commons/task/state?taskid=${taskid}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.hasError) {
          this.__mpro.end()
          RbHighbar.error(res.data.hasError)
          return
        }

        let cp = res.data.completed
        if (cp >= 1) {
          RbHighbar.success('导入完成')
          this.__mpro.end()
          setTimeout(() => { location.reload() }, 1500)
        } else {
          this.__mpro.set(cp)
          setTimeout(() => { this.__checkState(taskid) }, 1000)
        }
      }
    })
  }
}