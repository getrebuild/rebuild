/* eslint-disable react/prop-types */
/* eslint-disable no-undef */
$(document).ready(function () {
  renderRbcomp(<LevelBoxes id={dataId} />, 'boxes')
})

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
    this.notifyToggle(openLevel + 1, true)
  }

  notifyItemActive(level, id) {
    if (level < 3) {
      this.boxes[level + 1].loadItems(id)
      for (let i = level + 2; i <= 3; i++) {
        this.boxes[i].setState({ items: [] })
      }
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
        <div className="float-left"><h5>{LNAME[this.props.level]}级分类</h5></div>
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
            <div className="input-group-append"><button className="btn btn-primary" type="submit">{this.state.itemId ? '保存' : '添加'}</button></div>
          </div>
        </form>
        <ol className="dd-list unset-list mt-3">
          {(this.state.items || []).map((item) => {
            let active = this.state.activeId === item[0]
            return (
              <li className={'dd-item ' + (active && 'active')} key={item[0]} onClick={() => this.clickItem(item[0])}>
                <div className="dd-handle">{item[1]}</div>
                <div className="dd-action">
                  <a><i className="zmdi zmdi-edit" onClick={(e) => this.editItem(item, e)}></i></a>
                  <a><i className="zmdi zmdi-delete" onClick={(e) => this.delItem(item, e)}></i></a>
                </div>
                {active && <span className="zmdi zmdi-caret-right arrow hide"></span>}
              </li>
            )
          })}
        </ol>
      </div>)
  }
  componentDidMount() {
    if (this.props.level === 0) this.loadItems()
  }

  loadItems(p) {
    let url = `${rb.baseUrl}/admin/entityhub/classification/load-data-items?data_id=${dataId}&parent=${p || ''}`
    $.get(url, (res) => {
      this.setState({ items: res.data, activeId: null })
    })
    this.parentId = p
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
    let name = this.state.itemName
    if (!name) return
    if (this.props.level >= 1 && !this.parentId) {
      rb.highbar('请先选择上级分类项')
      return
    }

    let url = `${rb.baseUrl}/admin/entityhub/classification/save-data-item?data_id=${dataId}&name=${name}`
    if (this.state.itemId) url += `&item_id=${this.state.itemId}`
    else url += `&parent=${this.parentId}`
    $.post(url, (res) => {
      if (res.error_code === 0) {
        let items = this.state.items || []
        if (this.state.itemId) {
          items.forEach((i) => {
            if (i[0] === this.state.itemId) i[1] = name
          })
        } else {
          items.push([res.data, name])
        }
        this.setState({ items: items, itemName: null, itemId: null })
      } else rb.hberror(res.error_msg)
    })
  }
  editItem(item, e) {
    e.stopPropagation()
    this.setState({ itemName: item[1], itemId: item[0] })
  }
  delItem(item, e) {
    e.stopPropagation()
    let that = this
    rb.alert('删除后其子级分类也将一并删除。确认删除此分类项？', {
      confirm: function () {
        $.post(`${rb.baseUrl}/app/entity/record-delete?id=${item[0]}`, (res) => {
          this.hide()
          if (res.error_code !== 0) {
            rb.hberror(res.error_msg)
            return
          }
          let items = []
          that.state.items.forEach((i) => {
            if (i[0] !== item[0]) items.push(i)
          })
          that.setState({ items: items })
        })
        return false
      }
    })
  }
}

var saveOpenLevel_last = openLevel
var saveOpenLevel = function () {
  $setTimeout(() => {
    let level = $('.switch-button input:checkbox:checked:last').attr('id') || 't-1'
    level = ~~level.split('-')[1]
    if (saveOpenLevel_last === level) return

    let data = { openLevel: level }
    data.metadata = { entity: 'Classification', id: dataId }
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(data), (res) => {
      if (res.error_code > 0) rb.hberror(res.error_msg)
      saveOpenLevel_last = level
    })
  }, 500, 'saveOpenLevel')
}