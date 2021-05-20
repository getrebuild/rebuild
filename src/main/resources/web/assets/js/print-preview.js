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
  constructor(props) {
    super(props)
  }

  render() {
    const rows = []
    for (let i = 0; i < this.props.data.elements.length; i++) {
      let c = this.props.data.elements[i]
      let cNext = this.props.data.elements[i + 1]
      if (c.isFull || c.field === '$DIVIDER$' || (cNext && cNext.field === '$DIVIDER$')) {
        rows.push([c])
      } else {
        rows.push([c, cNext])
        i++
      }
    }

    return (
      <table className="table table-bordered table-sm table-fixed">
        <tbody>
          {rows.map((row, idx) => {
            const c1 = row[0]
            const c2 = row[1] || {}
            if (row.length === 1) {
              if (c1.field === '$DIVIDER$') {
                return (
                  <tr key={'k-' + idx}>
                    <th colSpan="4" className="divider">
                      {c1.label}
                    </th>
                  </tr>
                )
              }

              return (
                <tr key={'k-' + idx}>
                  <th>{c1.label}</th>
                  <td colSpan="3">{this.formatValue(c1)}</td>
                </tr>
              )
            }

            return (
              <tr key={'k-' + idx}>
                <th>{c1.label}</th>
                <td>{this.formatValue(c1)}</td>
                <th>{c2.label}</th>
                <td>{this.formatValue(c2)}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    )
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
            return <li key={x.id}>{__formatRefText(x)}</li>
          })}
        </ul>
      )
    } else if (typeof item.value === 'object') {
      return __formatRefText(item.value)
    } else {
      return item.value
    }
  }
}

const __formatRefText = function (value) {
  const text = value.text
  if (!text && value.id) return `@${value.id.toUpperCase()}`
  else return text
}
