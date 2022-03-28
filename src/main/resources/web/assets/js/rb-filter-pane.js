/*
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
    $.get(`/commons/metadata/fields?entity=${this.props.entity}`, (res) => {
      const items = res.data.map((item) => {
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
    })

    // const items = [
    //   { name: 'UnitPrice', label: '单价', type: 'DECIMAL' },
    //   { name: 'UnitPrice', label: '单价', type: 'DECIMAL' },
    //   { name: 'UnitPrice', label: '单价', type: 'DECIMAL' },
    //   { name: 'UnitPrice', label: '单价', type: 'DECIMAL' },
    //   { name: 'UnitPrice', label: '单价', type: 'DECIMAL' },
    //   { name: 'UnitPrice', label: '单价', type: 'DECIMAL' },
    // ]
    // this.setState({ items })
  }

  render() {
    if (!this.state.items) return null

    const col = $('#react-list').width() > 1200 ? 3 : 4
    return (
      <div className="row" onKeyPress={(e) => this.searchByKey(e)}>
        {this.state.items.map((item, i) => {
          return (
            <div className={`col col-${col}`} key={i}>
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

        <div className={`col col-${col}`}>
          <div>
            <label>&nbsp;</label>
            <button className="btn btn-primary btn-outline" type="button" onClick={() => this.searchNow()}>
              <i className="icon zmdi zmdi-search"></i> {$L('查询')}
            </button>
          </div>
        </div>
      </div>
    )
  }

  searchByKey(e) {
    e.which === 13 && this.searchNow()
  }

  searchNow() {
    const filters = []
    for (let i = 0; i < this._itemsRef.length; i++) {
      const item = this._itemsRef[i].getFilterJson()
      if (item) filters.push(item)
    }

    const adv = {
      entity: this.props.entity,
      items: filters,
    }

    if (rb.env === 'dev') console.log(JSON.stringify(adv))
  }
}

// eslint-disable-next-line no-undef
class FilterItemExt extends FilterItem {
  constructor(props) {
    super(props)
  }

  // @e = el or event
  valueCheck(e) {
    const v = e.target ? e.target.value : e.val()
    if (!v) return
    else super.valueCheck(e)
    // $el.removeClass('is-invalid')
  }
}
