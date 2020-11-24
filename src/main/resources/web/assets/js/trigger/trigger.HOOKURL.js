/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 回调 Url
// eslint-disable-next-line no-undef
class ContentHookUrl extends ActionContentSpec {
  state = { ...this.props }

  render() {
    return (
      <div className="hook-url">
        <form className="simple">
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('HookURL')}</label>
            <div className="col-12 col-lg-8">
              <input type="input" className="form-control form-control-sm" placeholder="https://example.com/postreceive" ref={(c) => this._hookUrl = c} />
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right">{$L('HookSecret')}</label>
            <div className="col-12 col-lg-8">
              <input type="input" className="form-control form-control-sm" placeholder={$L('Optional')} ref={(c) => this._hookSecret = c} />
            </div>
          </div>
          <div className="form-group row pt-1">
            <label className="col-12 col-lg-3 col-form-label text-lg-right"/>
            <div className="col-12 col-lg-8">
              <button type="button" className="btn btn-primary btn-outline">{$L('SendTest')}</button>
            </div>
          </div>
        </form>
      </div>
    )
  }

  componentDidMount() {
  }

  buildContent() {
    const _data = {
      hookUrl: $(this._hookUrl).val(),
      hookSecret: $(this._hookSecret).val(),
    }
    if (!_data.hookUrl) {
      RbHighbar.create($L('PlsInputSone,HookUrl'))
      return false
    }

    return _data
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentHookUrl {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
