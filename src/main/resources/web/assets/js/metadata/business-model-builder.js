/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// eslint-disable-next-line no-unused-vars
class BusinessModelBuilder extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    let contents
    if (this.state.indexes) {
      contents = []
      for (let key in this.state.indexes) {
        contents.push(<h4 key={`cat-${key}`}>{key}</h4>)
        this.state.indexes[key].map((item) => {
          contents.push(this._renderModel(item))
        })
      }
    }

    return (
      <div>
        {this.state.error && <RbAlertBox message={this.state.error} type="danger" />}
        {contents ? <div className="rbs-indexes container-fluid">{contents}</div> : <RbSpinner fully={true} />}
      </div>
    )
  }

  _renderModel(item) {
    return (
      <div className="row" key={'schema-' + item.key}>
        <div className="col-10">
          <h5>{item.name}</h5>
          <div className="text-muted">{item.desc}</div>
        </div>
        <div className="col-2 text-right">
          {item.exists ? (
            <button disabled className="btn btn-sm btn-primary">
              {$L('Exists')}
            </button>
          ) : (
            <button disabled={this.state.inProgress === true} className="btn btn-sm btn-primary" onClick={() => this.imports(item)}>
              {$L('Import')}
            </button>
          )}
        </div>
      </div>
    )
  }

  componentDidMount() {
    $.get('/admin/rbstore/load-business-model', (res) => {
      if (res.error_code === 0) {
        this.setState({ indexes: res.data }, () => {
          parent.RbModal.resize()
        })
      } else {
        this.setState({ error: res.error_msg })
      }
    })
  }

  imports(item) {
    $.post(`/admin/rbstore/business-model/imports?key=${item.key}`, (res) => {
      if (res.error_code === 0) {
        RbHighbar.success($L('导入成功'))
        setTimeout(() => (parent.location.href = `${rb.baseUrl}/admin/entity/${res.data}/base`), 1500)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}
