/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  renderRbcomp(<FuncList />, 'funclist')
})

const _FUNCTIPS = {
  'ENT': $L('含分类管理'),
}

class FuncList extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    return (
      <div ref={(c) => (this._$funclist = c)}>
        <RbAlertBox message={$L('选择**非超级管理员**所能使用的管理中心功能')} type="info" />
        {this.state.funclist &&
          this.state.funclist.map((item) => {
            const isSYS = item[0] === 'SYS'
            return (
              <label key={item[0]} className="custom-control custom-control-sm custom-checkbox custom-control-inline">
                <input className="custom-control-input" type="checkbox" value={item[0]} defaultChecked={item[2] || isSYS} disabled={isSYS} />
                <span className="custom-control-label">
                  {item[1]}
                  {_FUNCTIPS[item[0]] && <i className="zmdi zmdi-help zicon" title={_FUNCTIPS[item[0]]} />}
                </span>
              </label>
            )
          })}
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/settings/navs', (res) => {
      this.setState({ funclist: res.data }, () => {
        $(this._$funclist).find('.zicon').tooltip({})
        parent && parent.RbModal.resize()
      })
    })

    const $btn = $('.J_save').on('click', () => {
      let navs = []
      $(this._$funclist)
        .find('input:checked')
        .each(function () {
          navs.push(this.value)
        })

      $btn.button('loading')
      $.post(`/admin/settings/navs?conf=${navs.join(',')}`, () => {
        parent.location.reload()
      })
    })
  }
}
