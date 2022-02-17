/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 显示样式
// eslint-disable-next-line no-unused-vars
class ShowStyles extends React.Component {
  render() {
    return (
      <div className="modal rbalert" ref={(c) => (this._$dlg = c)} tabIndex="-1">
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="form">
                <div className="form-group row">
                  <label className="col-sm-3 col-form-label text-sm-right">{$L('别名')}</label>
                  <div className="col-sm-7">
                    <input className="form-control form-control-sm" placeholder={$L('默认')} defaultValue={this.props.label || ''} maxLength="50" ref={(c) => (this._$label = c)} />
                  </div>
                </div>
                <div className="form-group row footer">
                  <div className="col-sm-7 offset-sm-3">
                    <button className="btn btn-primary btn-space" type="button" onClick={() => this.saveProps()}>
                      {$L('确定')}
                    </button>
                    <a className="btn btn-link btn-space" onClick={() => this.hide()}>
                      {$L('取消')}
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    $(this._$dlg).modal({ show: true, keyboard: true })
  }

  show() {
    $(this._$dlg).modal('show')
  }

  hide() {
    $(this._$dlg).modal('hide')
  }

  saveProps() {
    const data = {
      label: $(this._$label).val() || '',
    }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(data)
    this.hide()
  }
}
