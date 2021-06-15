/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-unused-vars
class AsideTree extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, expandItems: [] }
  }

  render() {
    return <div className="aside-2tree">{this.renderTree(this.props.data || [])}</div>
  }

  renderTree(items, item) {
    return (
      <ul className={`list-unstyled m-0 ${item && !this.state.expandItems.contains(item.id) ? 'hide' : ''}`}>
        {items.map((item) => {
          let $children = null
          if (item.children && item.children.length > 0) {
            $children = this.renderTree(item.children, item)
          }
          const $item = this.renderItem(item, $children !== null)
          return (
            <React.Fragment key={item.id}>
              {$item}
              {$children}
            </React.Fragment>
          )
        })}
      </ul>
    )
  }

  renderItem(item, hasChild) {
    return (
      <li className={this.state.activeItem === item.id ? 'active' : ''}>
        <span
          className={`collapse-icon ${!hasChild && 'no-child'}`}
          onClick={() => {
            if (hasChild) {
              const expandItemsNew = this.state.expandItems
              expandItemsNew.toggle(item.id)
              this.setState({ expandItems: expandItemsNew })
            }
          }}>
          <i className={`zmdi ${this.state.expandItems.contains(item.id) ? 'zmdi-chevron-down' : 'zmdi-chevron-right'} `} />
        </span>
        <a
          data-id={item.id}
          className={`text-ellipsis ${item.disabled && 'text-disabled'}`}
          onClick={(e) => {
            this.setState({ activeItem: item.id })
            typeof this.props.onItemClick === 'function' && this.props.onItemClick(item, e)
          }}>
          {item.text || item.name}
          {item.private === true && <i className="icon zmdi zmdi-lock" title={$L('私有')} />}
        </a>
        {typeof this.props.extrasAction === 'function' && this.props.extrasAction(item)}
      </li>
    )
  }

  refresh(data) {
    this.setState({ data: data })
  }

  static findAllChildIds(item) {
    function _find(x1, into) {
      into.push(x1.id)
      if (x1.children && x1.children.length > 0) {
        x1.children.forEach((x2) => _find(x2, into))
      }
    }

    const s = []
    _find(item, s)
    return s
  }
}
