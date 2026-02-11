/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

class ContactList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <div className="contact-list">
        {(this.state.data || []).map((item) => {
          return (
            <span key={item.id}>
              <div className="card">
                <div className="card-body">
                  <div className="img float-left">
                    <a className="user-show">
                      <div className="avatar">
                        <img src={`${rb.baseUrl}/account/user-avatar/${item.id}`} />
                      </div>
                    </a>
                  </div>
                  <div className="info">
                    <table>
                      <tbody>
                        <tr>
                          <td colSpan="2">
                            <strong>{item.fullName}</strong>
                            <p className="m-0 text-muted" style={{ marginTop: -2 }}>
                              {item.deptName}
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td>{$L('邮箱')}</td>
                          <td>
                            {item.email ? (
                              <a href={`mailto:${item.email}`} className="link" title={item.email}>
                                {item.email}
                              </a>
                            ) : (
                              <span className="text-muted">{$L('无')}</span>
                            )}
                          </td>
                        </tr>
                        <tr>
                          <td>{$L('电话')}</td>
                          <td>
                            {item.workphone ? (
                              <a href={`tel:${item.workphone}`} className="link" title={item.workphone}>
                                {item.workphone}
                              </a>
                            ) : (
                              <span className="text-muted">{$L('无')}</span>
                            )}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </span>
          )
        })}

        {this._page === 1 && this.state.data && this.state.data.length === 0 && (
          <div className="list-nodata">
            <i className="icon zmdi zmdi-account-box-phone" />
            <p>{$L('暂无数据')}</p>
          </div>
        )}
      </div>
    )
  }

  componentDidMount = () => this.search()

  _fetch() {
    const url = `/contacts/list-users?isall=false&page=${this._page}&dept=${this._dept || 'ALL'}&q=${encodeURIComponent(this._q || '')}&sort=${this._sort || ''}`
    $.get(url, (res) => {
      const current = res.data || []
      let data = this._page === 1 ? [] : this.state.data
      data = [].concat(data, current)
      this.setState({ data: data, currentSize: current.length, currentActive: [] })
    })
  }

  search(dept) {
    this._page = 1
    this._q = currentSearch
    this._sort = currentSort
    this._dept = dept || this._dept
    this._fetch(1)
  }
}

let currentSearch
let currentSort
let _ContactList

$(document).ready(() => {
  $('.side-toggle').on('click', () => {
    const $el = $('.rb-aside').toggleClass('rb-aside-collapsed')
    $.cookie('rb.asideCollapsed', $el.hasClass('rb-aside-collapsed'), { expires: 180 })
  })

  const $content = $('.page-aside .tab-content')
  $addResizeHandler(() => {
    $content.height($(window).height() - 135)
    $content.perfectScrollbar('update')
  })()

  const gs = $urlp('gs', location.hash)
  if (gs) {
    currentSearch = $decode(gs)
    $('.input-search input').val(currentSearch)
  }
  // 搜索
  const $btn = $('.input-search .input-group-btn .btn').on('click', () => {
    currentSearch = $('.input-search input').val()
    _ContactList && _ContactList.search()
  })
  const $input = $('.input-search input').on('keydown', (e) => (e.which === 13 ? $btn.trigger('click') : true))
  $('.input-search .btn-input-clear').on('click', () => {
    $input.val('')
    $btn.trigger('click')
  })

  'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('').forEach((i) => {
    $(`<a>${i}</a>`).appendTo('.az-search')
  })
  $('.az-search>a').click(function () {
    $input.val($(this).text() + '*')
    $btn.trigger('click')
  })
  // 排序
  $('.J_sort .dropdown-menu>a').on('click', function () {
    $('.J_sort button>span').text($(this).text())
    currentSort = $(this).data('sort')
    $btn.trigger('click')
  })

  loadDeptTree()

  renderRbcomp(<ContactList />, $('.contact-viewport'), function () {
    _ContactList = this
  })
})

var _AsideTree
var loadDeptTree = function () {
  $.get('/contacts/list-depts', function (res) {
    if (_AsideTree) {
      ReactDOM.unmountComponentAtNode(document.getElementById('navTree'))
    }

    const activeItem = _AsideTree ? _AsideTree.state.activeItem || 'ALL' : 'ALL'
    const data = [{ id: 'ALL', name: $L('全部部门') }, ...res.data]
    renderRbcomp(
      <AsideTree
        data={data}
        activeItem={activeItem}
        onItemClick={(item) => {
          _ContactList.search(item.id)

          const paths = []
          _findPaths($('#navTree li.active'), paths)

          const $ol = $('.dept-path ol').empty()
          paths.forEach((item) => {
            const $li = $('<li class="breadcrumb-item"></li>').appendTo($ol)
            $li.text(item[0])
          })
          location.hash = `!/Department/${item.id}`
        }}
      />,
      'navTree',
      function () {
        _AsideTree = this
      }
    )
  })
}

function _findPaths(active, into) {
  const $a = active.find('>a')
  into.unshift([$a.text(), $a.data('id')])
  const $li = active.parent('ul').prev('li')
  if ($li.length > 0) _findPaths($li, into)
}
