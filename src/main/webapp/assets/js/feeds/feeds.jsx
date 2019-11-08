/* eslint-disable react/prop-types */
/* eslint-disable react/jsx-no-undef */

class RbFeeds extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return <React.Fragment>
      <FeedsPost ref={(c) => this._post = c} call={this.search} />
      <FeedsList ref={(c) => this._list = c} />
    </React.Fragment>
  }
  search = (filter) => this._list.fetchFeeds(filter)
}

// ~ 群组
class FeedsGroup extends RbFormHandler {

  constructor(props) {
    super(props)
  }

  render() {
    return (<RbModal title={`${this.props.id ? '修改' : '添加'}群组`} ref={(c) => this._dlg = c} disposeOnHide={true}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">群组名称</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" name="name" value={this.state.name || ''} onChange={this.handleChange} />
          </div>
        </div>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">成员</label>
          <div className="col-sm-7">
            <UserSelector ref={(c) => this._userSelector = c} selected={this.state.selectedUsers} />
            <div className="form-text">发布到群组内的动态仅成员可见</div>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3">
            <button className="btn btn-primary" type="button" onClick={() => this._post()}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }

  componentDidMount() {
    if (this.props.members) {
      $.post(`${rb.baseUrl}/commons/search/user-selector`, JSON.stringify(this.props.members), (res) => {
        if (res.data.length > 0) this.setState({ selectedUsers: res.data })
      })
    }
  }

  _post() {
    let data = { name: this.state.name, members: this._userSelector.val() }
    if (!data.name || !data.members || data.members.length === 0) return
    data.metadata = { entity: 'FeedsGroup', id: this.props.id || null }

    $(this._btn).button('loading')
    $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(data), (res) => {
      this.hide()
      rbGroupList.loadData()
      typeof this.props.call === 'function' && this.props.call(res.data)
    })
  }
}

class GroupList extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (<ul className="list-unstyled">
      {!this.state.list && <li className="nodata">加载中</li>}
      {(this.state.list && this.state.list.length === 0) && <li className="nodata">暂无群组</li>}
      {(this.state.list || []).map((item) => {
        return <li key={'item-' + item.id} data-id={item.id} className={this.state.active === item.id ? 'active' : ''}>
          <a className="text-truncate" onClick={() => this._handleActive(item.id)}>{item.name}</a>
          {(rb.isAdminUser && this.props.hasAction) && <div className="action">
            <a className="J_edit" onClick={() => this._handleEdit(item)}><i className="zmdi zmdi-edit"></i></a>
            <a className="J_del" onClick={() => this._handleDelete(item.id)}><i className="zmdi zmdi-delete"></i></a>
          </div>}
        </li>
      })}
    </ul>
    )
  }

  loadData() {
    $.get(`${rb.baseUrl}/feeds/group/group-list?all=true`, (res) => this.setState({ list: res.data || [] }))
  }

  _handleActive(id) {
    if (this.state.active === id) id = null
    this.setState({ active: id }, () => execFilter())
  }

  _handleEdit(item) {
    renderRbcomp(<FeedsGroup id={item.id} name={item.name} members={item.members} call={() => this.loadData()} />)
  }

  _handleDelete(id) {
    RbAlert.create('如果此群组已被使用则不允许被删除。确认删除？', {
      type: 'danger',
      confirmText: '删除',
      confirm: function () {
        this.disabled(true)
        $.post(`${rb.baseUrl}/app/entity/record-delete?id=${id}`, (res) => {
          if (res.error_code > 0) RbHighbar.error(res.error_msg)
          this.hide()
          this.loadData()
        })
      }
    })
  }

  val() {
    return this.state.active
  }
}

class UserList extends GroupList {

  constructor(props) {
    super(props)
  }

  loadData() {
    $.get(`${rb.baseUrl}/feeds/group/user-list`, (res) => {
      this.setState({ list: res.data || [] })
    })
  }
}

let rbFeeds
let rbGroupList
let rbUserList

// 构建搜索条件
const execFilter = function () {
  let group = rbGroupList.val()
  let user = rbUserList.val()
  let key = $('.J_search-key').val()
  let date1 = $('.J_date-begin').val()
  let date2 = $('.J_date-end').val()
  let type = ~~$('#collapseFeedsType li.active').data('type')

  let items = []
  if (group) items.push({ field: 'scope', op: 'EQ', value: group })
  if (user) items.push({ field: 'createdBy', op: 'EQ', value: user })
  if (key) items.push({ field: 'content', op: 'LK', value: key })
  if (date1) items.push({ field: 'createdOn', op: 'GE', value: date1 })
  if (date2) items.push({ field: 'createdOn', op: 'LE', value: date2 })
  if (type > 0) items.push({ field: 'type', op: 'EQ', value: type })

  rbFeeds.search({ entity: 'Feeds', equation: 'AND', items: items })
}

$(document).ready(function () {
  renderRbcomp(<RbFeeds />, 'rb-feeds', function () { rbFeeds = this })

  renderRbcomp(<GroupList hasAction={true} />, $('#collapseGroup .dept-tree'), function () { rbGroupList = this })
  renderRbcomp(<UserList />, $('#collapseUser .dept-tree'), function () { rbUserList = this })

  let rbGroupListLoaded = false,
    rbUserListLoaded = false
  $('#headingGroup').click(() => {
    if (!rbGroupListLoaded) rbGroupList.loadData()
    rbGroupListLoaded = true
  })
  $('#headingUser').click(() => {
    if (!rbUserListLoaded) rbUserList.loadData()
    rbUserListLoaded = true
  })

  function __clear(el) {
    $setTimeout(() => {
      let $clear = $(el).next().find('a')
      if ($(el).val()) $clear.addClass('show')
      else $clear.removeClass('show')
    }, 50, 'Close-Show')
  }
  $('#collapseSearch .append>a').click(function () {
    let $i = $(this).parent().prev().val('')
    __clear($i)
    setTimeout(execFilter, 100)
  })

  if (rb.isAdminUser) $('.add-group').click(() => renderRbcomp(<FeedsGroup />))
  $('.J_search-key').keydown(function (e) {
    __clear(this)
    if (e.keyCode === 13) execFilter()
  })
  let dpcfg = {
    navIcons: { rightIcon: 'zmdi zmdi-chevron-right', leftIcon: 'zmdi zmdi-chevron-left' },
    format: 'yyyy-mm-dd',
    minView: 2,
    startView: 'month',
    weekStart: 1,
    autoclose: true,
    language: 'zh',
    todayHighlight: true,
    showMeridian: false,
    endDate: new Date()
  }
  $('.J_date-begin, .J_date-end').datetimepicker(dpcfg).on('changeDate', function () {
    __clear(this)
    execFilter()
  })

  let lastType = 0
  $('#collapseFeedsType li>a').click(function () {
    $('#collapseFeedsType li').removeClass('active')
    let $li = $(this).parent()
    if (~~$li.data('type') === lastType) {
      lastType = 0
    } else {
      $li.addClass('active')
      lastType = ~~$li.data('type')
    }
    execFilter()
  })
})

