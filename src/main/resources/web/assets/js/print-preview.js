/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
// 打印视图

const wpc = window.__PageConfig
$(document).ready(() => {
  renderRbcomp(<PreviewTable data={wpc.content} />, 'preview-table')
})

class PreviewTable extends React.Component {
  render() {
    const rows = [] // [[]]
    let crtRow = []
    let crtSpan = 0

    this.props.data.elements.forEach((c) => {
      let colspan = c.colspan || 2
      if (c.isFull || c.colspan === 4 || c.field === '$DIVIDER$') colspan = 4
      // set
      c.colspan = colspan

      if (crtSpan + colspan > 4) {
        rows.push(crtRow)
        crtRow = [c]
        crtSpan = colspan
      } else if (crtSpan + colspan === 4) {
        crtRow.push(c)
        rows.push(crtRow)
        crtRow = []
        crtSpan = 0
      } else {
        crtRow.push(c)
        crtSpan += colspan
      }
    })
    // last
    if (crtRow.length > 0) rows.push(crtRow)

    return (
      <table className="table table-bordered table-sm table-fixed">
        <tbody>
          <tr className="hide">
            <td />
            <td />
            <td />
            <td />
            <td />
            <td />
            <td />
            <td />
          </tr>
          {rows.map((row, idx) => {
            const k = `row-${idx}-`
            return <tr key={k}>{this._renderRow(row).map((c, idx2) => React.cloneElement(c, { key: `${k}${idx2}` }))}</tr>
          })}
        </tbody>
      </table>
    )
  }

  _renderRow(row) {
    const c1 = row[0]
    const c2 = row[1]
    const c3 = row[2]
    const c4 = row[3]
    const cells = []

    if (c1.field === '$DIVIDER$') {
      cells.push(
        <th colSpan="8" className="divider">
          {c1.label}
        </th>
      )
      return cells
    }

    function _colSpan(n) {
      if (n === 4) return 7
      else if (n === 3) return 5
      else if (n === 2) return 3
      else return 1
    }

    cells.push(<th>{c1.label}</th>)
    let colSpan = _colSpan(c2 ? c1.colspan : 4)
    cells.push(<td colSpan={colSpan}>{this.formatValue(c1)}</td>)
    if (!c2) return cells

    cells.push(<th>{c2.label}</th>)
    colSpan = _colSpan(c3 ? c2.colspan : 4 - c1.colspan)
    cells.push(<td colSpan={colSpan}>{this.formatValue(c2)}</td>)
    if (!c3) return cells

    cells.push(<th>{c3.label}</th>)
    colSpan = _colSpan(c4 ? c3.colspan : 4 - c1.colspan - c2.colspan)
    cells.push(<td colSpan={colSpan}>{this.formatValue(c3)}</td>)
    if (!c4) return cells

    cells.push(<th>{c4.label}</th>)
    colSpan = 1
    cells.push(<td colSpan={colSpan}>{this.formatValue(c4)}</td>)
  }

  componentDidMount = () => $('.font-italic.hide').removeClass('hide')

  formatValue(item) {
    if (item && item.type === 'AVATAR' && !item.value) {
      return (
        <div className="img-field avatar">
          <span className="img-thumbnail img-upload">
            <img src={`${rb.baseUrl}/assets/img/avatar.png`} alt="Avatar" />
          </span>
        </div>
      )
    }

    if (!item || !item.value) return null

    if (item.type === 'FILE') {
      return (
        <ul className="m-0 p-0 pl-3">
          {item.value.map((x) => {
            return <li key={x}>{$fileCutName(x)}</li>
          })}
        </ul>
      )
    } else if (item.type === 'IMAGE') {
      return (
        <ul className="list-inline m-0">
          {item.value.map((x) => {
            return (
              <li className="list-inline-item" key={x}>
                <img src={`${rb.baseUrl}/filex/img/${x}?imageView2/2/w/100/interlace/1/q/100`} alt="IMG" />
              </li>
            )
          })}
        </ul>
      )
    } else if (item.type === 'AVATAR') {
      return (
        <div className="img-field avatar">
          <span className="img-thumbnail img-upload">
            <img src={`${rb.baseUrl}/filex/img/${item.value}?imageView2/2/w/100/interlace/1/q/100`} alt="Avatar" />
          </span>
        </div>
      )
    } else if (item.type === 'NTEXT') {
      if (item.useMdedit) {
        // eslint-disable-next-line no-undef
        const md2html = SimpleMDE.prototype.markdown(item.value)
        return <div className="mdedit-content" dangerouslySetInnerHTML={{ __html: md2html }} />
      } else {
        return (
          <React.Fragment>
            {item.value.split('\n').map((line, idx) => {
              return <p key={'kl-' + idx}>{line}</p>
            })}
          </React.Fragment>
        )
      }
    } else if (item.type === 'BOOL') {
      return item.value ? $L('是') : $L('否')
    } else if (item.type === 'MULTISELECT') {
      return (
        <ul className="m-0 p-0 pl-3">
          {(item.value.text || []).map((x) => {
            return <li key={x}>{x}</li>
          })}
        </ul>
      )
    } else if (item.type === 'PICKLIST' || item.type === 'STATE') {
      // eslint-disable-next-line no-undef
      return __findOptionText(item.options, item.value)
    } else if (item.type === 'BARCODE') {
      return (
        <div className="img-field barcode">
          <span className="img-thumbnail">
            <img src={`${rb.baseUrl}/commons/barcode/render${item.barcodeType === 'BARCODE' ? '' : '-qr'}?t=${$encode(item.value)}`} alt={item.value} />
          </span>
        </div>
      )
    } else if (item.type === 'N2NREFERENCE') {
      return (
        <ul className="m-0 p-0 pl-3">
          {item.value.map((x) => {
            return <li key={x.id}>{this._findMixValue(x)}</li>
          })}
        </ul>
      )
    } else if (item.type === 'SIGN') {
      return (
        <div className="img-field sign">
          <span className="img-thumbnail img-upload">
            <img src={item.value} alt="SIGN" />
          </span>
        </div>
      )
    } else if (typeof item.value === 'object') {
      return this._findMixValue(item.value)
    } else {
      return item.value
    }
  }

  _findMixValue(value) {
    const text = value.text
    if (!text && value.id) return `@${value.id.toUpperCase()}`
    else return text
  }
}
