/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
// 打印视图

const wpc = window.__PageConfig
$(document).ready(() => {
  renderRbcomp(<PreviewTable data={wpc.content} />, 'preview-table')

  setDefaultStyle('fontSize')
  setDefaultStyle('fontFamily')
  setDefaultStyle('fontBold')
})

function setDefaultStyle(id) {
  const $el = $(`#${id}`).on('change', function () {
    const v = $(this).val()
    $storage.set(id, v)

    if (id === 'fontSize') {
      $('.preview-content').css('font-size', (14 * ~~v) / 10)
    }
    if (id === 'fontFamily') {
      $('.preview-content').css('font-family', v === '-' ? '' : v)
    }
    if (id === 'fontBold') {
      $('.preview-content').attr('data-bold', v)
    }
  })

  const d = $storage.get(id)
  if (d) $el.val(d).trigger('change')
}

class PreviewTable extends React.Component {
  render() {
    const rows = [] // [[]]
    let currentRow = []
    let currentSpan = 0

    this.props.data.elements.forEach((c) => {
      let colspan = 6
      if (c.isFull || c.colspan === 4 || c.field === '$DIVIDER$') colspan = 12
      else if (c.colspan === 3) colspan = 9
      else if (c.colspan === 2) colspan = 6
      else if (c.colspan === 1) colspan = 3
      else if (c.colspan === 9) colspan = 4
      else if (c.colspan === 8) colspan = 8
      // correct
      c.colspan = colspan

      if (currentSpan + colspan > 12) {
        rows.push(currentRow)
        currentRow = [c]
        currentSpan = colspan
      } else if (currentSpan + colspan === 12) {
        currentRow.push(c)
        rows.push(currentRow)
        currentRow = []
        currentSpan = 0
      } else {
        currentRow.push(c)
        currentSpan += colspan
      }
    })
    // last
    if (currentRow.length > 0) rows.push(currentRow)

    return (
      <table className="table table-bordered table-sm table-fixed">
        <tbody>
          <tr className="hide">
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((i) => {
              return <td data-col={i} key={i} />
            })}
          </tr>
          {rows.map((row, idx) => {
            if (row[0].field === '$REFFORM$') return null

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
        <th colSpan="12" className="divider">
          {c1.label}
        </th>
      )
      return cells
    }

    cells.push(<th>{c1.label}</th>)
    cells.push(<td colSpan={c2 ? c1.colspan - 1 : 11}>{this.formatValue(c1)}</td>)
    if (!c2) return cells

    cells.push(<th>{c2.label}</th>)
    cells.push(<td colSpan={c3 ? c2.colspan - 1 : 11 - c1.colspan}>{this.formatValue(c2)}</td>)
    if (!c3) return cells

    cells.push(<th>{c3.label}</th>)
    cells.push(<td colSpan={c4 ? c3.colspan - 1 : 11 - c1.colspan - c2.colspan}>{this.formatValue(c3)}</td>)
    if (!c4) return cells

    cells.push(<th>{c4.label}</th>)
    cells.push(<td colSpan={2}>{this.formatValue(c4)}</td>)
    return cells
  }

  componentDidMount() {
    $('.preview-content.hide').removeClass('hide')
    if (~~$urlp('mode') === 1) setTimeout(() => window.print(), 100)
  }

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
        <ul className="m-0 p-0 pl-4">
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
          <RF>
            {item.value.split('\n').map((line, idx) => {
              return <p key={'kl-' + idx}>{line}</p>
            })}
          </RF>
        )
      }
    } else if (item.type === 'BOOL') {
      return item.value === 'T' || item.value === true ? $L('是') : $L('否')
    } else if (item.type === 'MULTISELECT') {
      return (
        <ul className="m-0 p-0 pl-4">
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
        <ul className="m-0 p-0 pl-4">
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
    } else if (item.type === 'TAG') {
      return (
        <ul className="m-0 p-0 pl-4">
          {(item.value || []).map((x) => {
            return <li key={x}>{x}</li>
          })}
        </ul>
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
