/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global filesList */

// 附件
const __DEFAULT_ALL = 1

// ~ 实体树
class EntityTree extends React.Component {
  state = { activeItem: 1, ...this.props }

  render() {
    return (
      <div className="aside-tree p-0">
        <ul className="list-unstyled">
          {(this.state.list || []).map((item) => {
            return (
              <li key={`entity-${item.id}`} className={this.state.activeItem === item.id ? 'active' : ''}>
                <a onClick={() => this._clickItem(item)} href={`#!/Entity/${item.id}`}>
                  {item.text}
                </a>
              </li>
            )
          })}
        </ul>
      </div>
    )
  }

  _clickItem(item) {
    this.setState({ activeItem: item.id }, () => {
      this.props.call && this.props.call(item)
    })
  }

  componentDidMount = () => this.loadData()

  loadData() {
    $.get('/files/tree-entity', (res) => {
      let _list = res.data || []
      _list.unshift({ id: __DEFAULT_ALL, text: $L('全部') })
      this.setState({ list: _list })
    })
  }
}

// eslint-disable-next-line no-undef
class FilesList4Atts extends FilesList {
  state = { ...this.props }

  renderExtras(item) {
    return item.relatedRecord ? (
      <span>
        <a title={$L('点击查看记录')} onClick={(e) => $stopEvent(e)} href={`${rb.baseUrl}/app/list-and-view?id=${item.relatedRecord[0]}`}>
          {item.relatedRecord[1]}
        </a>
      </span>
    ) : null
  }
}

$(document).ready(() => {
  const clickNav = function (item) {
    filesList && filesList.loadData(item.id)
    $('.file-path .active').text(item.text)
  }
  renderRbcomp(<EntityTree call={clickNav} />, 'navTree')

  renderRbcomp(<FilesList4Atts />, $('.file-viewport'), function () {
    // eslint-disable-next-line no-global-assign
    filesList = this
  })
})
