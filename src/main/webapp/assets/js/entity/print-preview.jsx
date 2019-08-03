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
      <table className="table table-bordered table-sm">
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

  formatValue(item) {
    if (!item || !item.value) return null
    if (typeof item.value === 'object') return item.value[1]
    return item.value
  }
}