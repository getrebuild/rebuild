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

    this.setState({ items })
  }

  render() {
    if (!this.state.items) return null

    return (
      <form className="row" onSubmit={(e) => this.searchNow(e)}>
        {this.state.items.map((item, i) => {
          return (
            <div className="col col-6 col-lg-4 col-xl-3" key={i}>
              <div>
                <label>{item.label}</label>
                <div className="adv-filter">
                  <div className="filter-items">
                    <FilterItemExt onRef={this.onRef} $$$parent={this} fields={[item]} />
                  </div>
                </div>
              </div>
            </div>
          )
        })}

        <div className="col col-6 col-lg-4 col-xl-3 operating-btn">
          <div>
            <button className="btn btn-primary btn-outline" type="submit">
              <i className="icon zmdi zmdi-search"></i> {$L('查询')}
            </button>
            {(this.props.fields || []).length > 3 && (
              <a className="ml-3 down-1" onClick={() => this.toggleExtended()}>
                {this.state.extended && (
                  <RF>
                    <i className="icon zmdi zmdi-chevron-up"></i> {$L('收缩')}
                  </RF>
                )}
                {!this.state.extended && (
                  <RF>
                    <i className="icon zmdi zmdi-chevron-down"></i> {$L('展开')}
                  </RF>
                )}
              </a>
            )}
            {rb.isAdminUser && (
              <a
                className="ml-2 down-2 admin-show"
                title={$L('配置查询字段')}
                onClick={() => {
                  RbModal.create(
                    `/p/admin/metadata/list-filterpane?entity=${this.props.entity}`,
                    <RF>
                      {$L('配置查询字段')}
                      <sup className="rbv" title={$L('增值功能')} />
                    </RF>
                  )
                }}>
                <i className="icon zmdi zmdi-settings" />
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
      items: filters,
    }

    if (rb.env === 'dev') console.log(JSON.stringify(s))
    typeof this.props.onSearch === 'function' && this.props.onSearch(s)
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
      if (this.state.type === 'DATE' || this.state.type === 'DATETIME' || this.state.type === 'TIME') {
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
