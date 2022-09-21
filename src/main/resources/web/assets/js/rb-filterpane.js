/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
// deps: rb-advfilter.js

const REFENTITY_CACHE = window.REFENTITY_CACHE || {}
const IS_N2NREF = window.IS_N2NREF || {}
const BIZZ_ENTITIES = window.BIZZ_ENTITIES || []

// eslint-disable-next-line no-unused-vars
class AdvFilterPane extends React.Component {
  constructor(props) {
    super(props)

    this.state = {}
    this._itemsRef = []
  }

  onRef = (c) => this._itemsRef.push(c)

  componentDidMount() {
    const items = (this.props.fields || []).map((item) => {
      if (item.type === 'REFERENCE' || item.type === 'N2NREFERENCE') {
        REFENTITY_CACHE[`${this.props.entity}.${item.name}`] = item.ref
        if (item.type === 'N2NREFERENCE') IS_N2NREF.push(item.name)

        // NOTE: Use `NameField` field-type
        if (!BIZZ_ENTITIES.includes(item.ref[0])) {
          item.type = item.ref[1]
        }
      }
      return item
    })

    let showxLast
    function show() {
      const ww = $(window).width()
      let showx = 4
      if (ww <= 992) showx = 1
      else if (ww <= 1200) showx = 2
      else if (ww <= 1400) showx = 3

      if (showxLast === showx) return
      showxLast = showx

      console.log(showx)

      $('.quick-filter-pane>.row>.col').each((idx, item) => {
        if (idx < showx) $(item).addClass('show')
        else $(item).removeClass('show')
      })
    }

    this.setState({ items }, () => {
      setTimeout(() => this.clearFilter(), 200)

      show()
      $(window).on('resize', show)
    })
  }

  render() {
    if (!this.state.items) return null

    return (
      <form className="row" onSubmit={(e) => this.searchNow(e)}>
        {this.state.items.map((item, i) => {
          return (
            <div className="col" key={i}>
              <div>
                <label>{item.label}</label>
                <div className="adv-filter">
                  <div className="filter-items">
                    <FilterItemExt onRef={this.onRef} $$$parent={this} fields={[item]} allowClear />
                  </div>
                </div>
              </div>
            </div>
          )
        })}

        <div className="col operating-btn">
          <div>
            <div className="btn-group">
              <button className="btn btn-secondary" type="submit">
                <i className="icon zmdi zmdi-search"></i> {$L('查询')}
              </button>
              <button className="btn btn-secondary dropdown-toggle w-auto" type="button" data-toggle="dropdown">
                <i className="icon zmdi zmdi-chevron-down" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0 mr-2">
                  <input className="custom-control-input" type="radio" name="useEquation" value="OR" defaultChecked />
                  <span className="custom-control-label">{$L('符合任一')}</span>
                </label>
                <label className="custom-control custom-control-sm custom-radio custom-control-inline mb-0 mr-0">
                  <input className="custom-control-input" type="radio" name="useEquation" value="AND" ref={(c) => (this._$useEquationAnd = c)} />
                  <span className="custom-control-label">{$L('符合全部')}</span>
                </label>
              </div>
            </div>
            <a className="ml-3 down-3" onClick={() => this.clearFilter(true)} title={$L('重置')}>
              <i className="icon mdi mdi-replay" />
            </a>
            {(this.props.fields || []).length > 2 && (
              <a className="ml-1 down-3" onClick={() => this.toggleExtended()}>
                {this.state.extended ? <i className="icon mdi mdi-arrow-collapse-vertical" title={$L('收起')} /> : <i className="icon mdi mdi-arrow-expand-vertical" title={$L('展开')} />}
              </a>
            )}
            {rb.isAdminUser && (
              <a
                className="ml-1 down-3 admin-show"
                title={$L('配置查询面板字段')}
                onClick={() => {
                  RbModal.create(
                    `/p/admin/metadata/list-filterpane?entity=${this.props.entity}`,
                    <RF>
                      {$L('配置查询面板字段')}
                      <sup className="rbv" title={$L('增值功能')} />
                    </RF>
                  )
                }}>
                <i className="icon mdi mdi-cog" />
              </a>
            )}
          </div>
        </div>
      </form>
    )
  }

  searchNow(e) {
    $stopEvent(e, true)

    const filters = []
    for (let i = 0; i < this._itemsRef.length; i++) {
      const item = this._itemsRef[i].getFilterJson()
      if (item) filters.push(item)
    }

    const s = {
      entity: this.props.entity,
      equation: this._$useEquationAnd.checked ? 'AND' : 'OR',
      items: filters,
    }

    if (rb.env === 'dev') console.log(JSON.stringify(s))
    typeof this.props.onSearch === 'function' && this.props.onSearch(s)
  }

  clearFilter(searchNow) {
    this._itemsRef.forEach((i) => i.clear())
    searchNow === true && setTimeout(() => this.searchNow(), 200)
  }

  toggleExtended() {
    this.setState({ extended: !this.state.extended }, () => {
      if (this.state.extended) $('.quick-filter-pane').addClass('extended')
      else $('.quick-filter-pane').removeClass('extended')
    })
  }
}

// eslint-disable-next-line no-undef
class FilterItemExt extends FilterItem {
  constructor(props) {
    super(props)
  }

  componentDidMount() {
    super.componentDidMount()

    const $s2op = this.__select2[1]
    setTimeout(() => {
      const type = this.state.type
      if (type === 'DATE' || type === 'DATETIME' || type === 'TIME' || type === 'NUMBER' || type === 'DECIMAL') {
        $s2op.val('EQ').trigger('change')
      }
    }, 200)
  }

  // @e = el or event
  valueCheck(e) {
    const v = e.target ? e.target.value : e.val()
    if (!v) return

    super.valueCheck(e)
    // $el.removeClass('is-invalid')
  }
}
