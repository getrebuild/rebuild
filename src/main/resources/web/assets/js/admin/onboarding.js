/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global InitModels */

class Onboarding extends React.Component {
  state = { busy: false, mode: 'modules', count: 0 }

  render() {
    return (
      <div className="onboarding-content">
        <h3>{$L('选择一种方式开始')}</h3>
        <InitModels ref={(c) => (this._$models = c)} onboarding onLoad={this._sync} onChange={this._sync} />
        <div className="onboarding-actions">
          {this.state.mode !== 'ai' && (
            <button className="btn btn-primary btn-lg fs-14" type="button" disabled={this.state.busy || this.state.count === 0} onClick={this._submit}>
              {$L('确定')}
            </button>
          )}
          <span className="onboarding-count">{this._summary()}</span>
          <a className="onboarding-skip" href={`${rb.baseUrl}/dashboard/home`}>
            {$L('下次提醒我')}
          </a>
        </div>
      </div>
    )
  }

  _summary() {
    if (this.state.mode === 'ai') return $L('接入模型后就能用')
    if (this.state.mode === 'package') return this.state.count > 0 ? $L('已选择应用资源包') : $L('请选择应用资源包')
    return $L('已选 %s 个', this.state.count)
  }

  _sync = () => {
    const models = this._$models
    if (!models) return

    const mode = models.getMode()
    const count = mode === 'package' ? (models.getPackage() ? 1 : 0) : mode === 'modules' ? models.getSelected().length : 0
    this.setState({ mode, count })
  }

  _submit = () => {
    const models = this._$models
    if (!models || this.state.busy) return

    if (models.getMode() === 'package') {
      this._post('/onboarding/restore-package', JSON.stringify({ package: models.getPackage() }), $L('导入成功'), 1500)
      return
    }

    this._post(`/onboarding/imports?key=${$encode(models.getSelected().join(','))}`, null, $L('导入成功'), 1500)
  }

  _post(url, data, okText, delay) {
    this.setState({ busy: true })
    $mp.start()
    $.post(url, data, (res) => {
      $mp.end()
      if (res.error_code === 0) {
        RbHighbar.success(okText)
        setTimeout(() => (location.href = rb.baseUrl + '/dashboard/home'), delay)
      } else {
        this.setState({ busy: false })
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

$(document).ready(() => {
  renderRbcomp(<Onboarding />, $('#onboardingRoot')[0])
})
