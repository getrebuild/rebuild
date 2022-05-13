/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

/* eslint-disable react/jsx-no-undef */

class RbFeeds extends React.Component {
  state = { ...this.props }

  render() {
    const s = $urlp('s', location.hash)
    return (
      <React.Fragment>
        <FeedsPost ref={(c) => (this._post = c)} call={this.search} />
        <FeedsList ref={(c) => (this._list = c)} focusFeed={s} />
      </React.Fragment>
    )
  }

  // 搜索
  search = (filter) => this._list.setState({ pageNo: 1 }, () => this._list.fetchFeeds(filter))
}

class GroupList extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <ul className="list-unstyled">
        {!this.state.list && <li className="nodata">{$L('加载中')}</li>}
        {this.state.list && this.state.list.length === 0 && <li className="nodata">{$L('暂无数据')}</li>}
        {(this.state.list || []).map((item) => {
          return (
            <li key={`item-${item.id}`} data-id={item.id} className={this.state.active === item.id ? 'active' : ''}>
              <a className="text-truncate" onClick={() => this._handleActive(item.id)}>
                {this._showAvatar && <img src={`${rb.baseUrl}/account/user-avatar/${item.id}`} className="avatar" alt="Avatar" />}
                {item.name}
              </a>
              <i className={`zmdi zmdi-star-outline ${item.star && 'star'}`} onClick={() => this._handleStar(item)} title={$L('星标')} />
            </li>
          )
        })}
      </ul>
    )
  }

  loadData(q) {
    $.get(`/feeds/group/group-list?q=${$encode(q || '')}`, (res) => this.setState({ list: res.data || [] }))
  }

  _handleActive(id) {
    if (this.state.active === id) id = null
    this.setState({ active: id }, () => execFilter())
  }

  _handleStar(item) {
    const newList = this.state.list.map((x) => {
      if (x.id === item.id) x.star = !x.star
      return x
    })

    this.setState({ list: newList })
    $.post(`/feeds/group/star-toggle?user=${item.id}`)
  }

  val() {
    return this.state.active
  }
}

class UserList extends GroupList {
  constructor(props) {
    super(props)
    this._showAvatar = true
  }

  loadData(q) {
    $.get(`/feeds/group/user-list?q=${$encode(q || '')}`, (res) => this.setState({ list: res.data || [] }))
  }
}

let rbFeeds
let rbGroupList
let rbUserList

// 构建搜索条件
const execFilter = function () {
  const group = rbGroupList.val()
  const user = rbUserList.val()
  const key = $('.J_search-key').val()
  const date1 = $('.J_date-begin').val()
  const date2 = $('.J_date-end').val()
  const type = ~~$('#collapseFeedsType li.active').data('type')

  let items = []
  if (group) items.push({ field: 'scope', op: 'EQ', value: group })
  if (user) items.push({ field: 'createdBy', op: 'EQ', value: user })
  if (key) items.push({ field: 'content', op: 'LK', value: key })
  if (date1) items.push({ field: 'createdOn', op: 'GE', value: date1 })
  if (date2) items.push({ field: 'createdOn', op: 'LE', value: date2 })
  if (type > 0) items.push({ field: 'type', op: 'EQ', value: type })

  rbFeeds.search({
    entity: 'Feeds',
    equation: 'AND',
    items: items,
  })
}

$(document).ready(function () {
  const gs = $urlp('gs', location.hash)
  if (gs) $('.search-input-gs, .J_search-key').val($decode(gs))

  renderRbcomp(<RbFeeds />, 'rb-feeds', function () {
    rbFeeds = this
  })
  renderRbcomp(<GroupList hasAction={true} />, $('#collapseGroup .aside-tree'), function () {
    rbGroupList = this
  })
  renderRbcomp(<UserList />, $('#collapseUser .aside-tree'), function () {
    rbUserList = this
  })

  let rbGroupListLoaded = false,
    rbUserListLoaded = false
  $('#headingGroup').on('click', () => {
    if (!rbGroupListLoaded) rbGroupList.loadData()
    rbGroupListLoaded = true
  })
  $('#headingUser').on('click', () => {
    if (!rbUserListLoaded) rbUserList.loadData()
    rbUserListLoaded = true
  })
  $('#collapseGroup .search-member>input').on('input', function () {
    const q = $(this).val()
    $setTimeout(() => rbGroupList.loadData(q), 300, 'headingGroup-search')
  })
  $('#collapseUser .search-member>input').on('input', function () {
    const q = $(this).val()
    $setTimeout(() => rbUserList.loadData(q), 300, 'headingUser-search')
  })

  function __clear(el) {
    $setTimeout(
      () => {
        const $clear = $(el).next().find('a')
        if ($(el).val()) $clear.addClass('show')
        else $clear.removeClass('show')
      },
      50,
      'Close-Show'
    )
  }

  $('#collapseSearch .append>a').on('click', function () {
    const $i = $(this).parent().prev().val('')
    __clear($i)
    setTimeout(execFilter, 100)
  })

  $('.J_search-key').on('keydown', function (e) {
    __clear(this)
    if (e.keyCode === 13) execFilter()
  })

  $('.J_date-begin, .J_date-end')
    .datetimepicker({
      format: 'yyyy-mm-dd',
      minView: 2,
      startView: 'month',
      endDate: new Date(),
    })
    .on('changeDate', function () {
      __clear(this)
      execFilter()
    })

  let lastType = 0
  $('#collapseFeedsType li>a').on('click', function () {
    $('#collapseFeedsType li').removeClass('active')
    const $li = $(this).parent()
    if (~~$li.data('type') === lastType) {
      lastType = 0
    } else {
      $li.addClass('active')
      lastType = ~~$li.data('type')
    }
    execFilter()
  })

  execFilter()
})
