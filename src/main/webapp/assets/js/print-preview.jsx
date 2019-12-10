/* eslint-disable react/prop-types */
const wpc = window.__PageConfig
$(document).ready(() => {
  renderRbcomp(<PreviewTable data={wpc.content} />, 'preview-table')
})

class PreviewTable extends React.Component {
  constructor(props) {
    super(props)
  }

  render() {
    let rows = []
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
            let c1 = row[0]
            let c2 = row[1] || {}
            if (row.length === 1) {
              if (c1.field === '$DIVIDER$') {
                return <tr key={'k-' + idx}>
                  <th colSpan="4">{c1.label}</th>
                </tr>
              }

              return <tr key={'k-' + idx}>
                <th>{c1.label}</th>
                <td colSpan="3">{this.formatValue(c1)}</td>
              </tr>
            }

            return <tr key={'k-' + idx}>
              <th>{c1.label}</th>
              <td>{this.formatValue(c1)}</td>
              <th>{c2.label}</th>
              <td >{this.formatValue(c2)}</td>
            </tr>
          })}
        </tbody>
      </table>
    )
  }

  componentDidMount = () => $('.font-italic.hide').removeClass('hide')

  formatValue(item) {
    if (!item || !item.value) return null

    if (item.type === 'FILE') {
      return (<ul className="list-unstyled m-0">
        {JSON.parse(item.value).map((x) => {
          return <li key={`file-${x}`}>{$fileCutName(x)}</li>
        })}
      </ul>)
    } else if (item.type === 'IMAGE') {
      return (<ul className="list-inline m-0">
        {JSON.parse(item.value).map((x) => {
          return <li className="list-inline-item" key={`image-${x}`}><img src={`${rb.baseUrl}/filex/img/${x}?imageView2/2/w/100/interlace/1/q/100`} /></li>
        })}
      </ul>)
    } else if (item.type === 'NTEXT') {
      return <React.Fragment>
        {item.value.split('\n').map((line, idx) => {
          return <p key={'kl-' + idx}>{line}</p>
        })}
      </React.Fragment>
    } else if (item.type === 'BOOL') {
      return { 'T': '是', 'F': '否' }[item.value]
    } else if (item.type === 'MULTISELECT') {
      // eslint-disable-next-line no-undef
      return __findMultiTexts(item.options, item.value).join(', ')
    } else if (item.type === 'PICKLIST' || item.type === 'STATE') {
      // eslint-disable-next-line no-undef
      return __findOptionText(item.options, item.value)
    } else if (typeof item.value === 'object') {
      let text = item.value.text
      if (!text && item.value.id) text = `@${item.value.id.toUpperCase()}`
      return text
    } else {
      return item.value
    }
  }
}