/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */
// 表单附加操作，可在其他页面独立引入

// ~~ 分类数据选择

class ClassificationSelector extends React.Component {
  constructor(props) {
    super(props)

    this._select = []
    this._select2 = []
    this.state = { openLevel: props.openLevel || 0, datas: [] }
  }

  render() {
    return (
      <div className="modal selector" ref={(c) => (this._dlg = c)}>
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('选择%s', this.props.label)}</h5>
              <div>
                <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                  {(this.state.datas[0] || []).map((item) => {
                    return (
                      <option key={'item-' + item[0]} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
              {this.state.openLevel >= 1 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[1] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 2 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[2] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 3 && (
                <div>
                  <select ref={(c) => this._select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[3] || []).map((item) => {
                      return (
                        <option key={'item-' + item[0]} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              <div>
                <button className="btn btn-primary btn-outline w-100" onClick={() => this.confirm()}>
                  <i className="icon zmdi zmdi-check" /> {$L('确定')}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $m = this.show()
    if (this.props.keepModalOpen) {
      $m.on('hidden.bs.modal', () => {
        $(document.body).addClass('modal-open') // keep scroll
      })
    }

    const that = this
    $(this._select).each(function (idx) {
      const s = $(this)
        .select2({
          placeholder: $L('选择 %d 级分类', idx + 1),
          allowClear: false,
        })
        .on('change', () => {
          const p = $(s).val()
          if (p) {
            if (s.__level < that.state.openLevel) {
              that._loadData(s.__level + 1, p) // Load next-level
            }
          }
        })
      s.__level = idx
      that._select2.push(s)
    })
    this._loadData(0)
  }

  _loadData(level, p) {
    $.get(`/commons/metadata/classification?entity=${this.props.entity}&field=${this.props.field}&parent=${p || ''}`, (res) => {
      const s = this.state.datas
      s[level] = res.data
      this.setState({ datas: s }, () => this._select2[level].trigger('change'))
    })
  }

  confirm() {
    const last = this._select2[this.state.openLevel]
    const v = last.val()
    if (!v) {
      RbHighbar.create($L('请选择 %s', this.props.label))
    } else {
      const text = []
      $(this._select2).each(function () {
        text.push(this.select2('data')[0].text)
      })

      typeof this.props.onSelect === 'function' && typeof this.props.onSelect({ id: v, text: text.join('.') })
      this.hide()
    }
  }

  show() {
    return $(this._dlg).modal({ show: true, keyboard: true })
  }

  hide(dispose) {
    $(this._dlg).modal('hide')
    if (dispose === true) $unmount($(this._dlg).parent())
  }
}

// ~~ 引用字段搜索

window.referenceSearch__call = function (selected) {}
window.referenceSearch__dlg

// see `reference-search.html`
class ReferenceSearcher extends RbModal {
  constructor(props) {
    super(props)
  }

  render() {
    return this.state.destroy === true ? null : (
      <div className="modal" ref={(c) => (this._rbmodal = c)}>
        <div className="modal-dialog modal-xl">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">{this.props.title || $L('查询')}</h3>
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body iframe" style={{ borderTop: '1px solid #dee2e6' }}>
              <iframe src={this.props.url} frameBorder="0" style={{ minHeight: 360 }} />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    window.referenceSearch__dlg = this
  }

  destroy() {
    this.setState({ destroy: true })
    window.referenceSearch__dlg = null
  }
}

// ~~ 删除确认

class DeleteConfirm extends RbAlert {
  constructor(props) {
    super(props)
    this.state = { enableCascades: false }
  }

  render() {
    let message = this.props.message
    if (!message) message = this.props.ids ? $L('确认删除选中的 %d 条记录？', this.props.ids.length) : $L('确认删除当前记录吗？')

    return (
      <div className="modal rbalert" ref={(c) => (this._dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="text-center ml-6 mr-6">
                <div className="text-danger">
                  <span className="modal-main-icon zmdi zmdi-alert-triangle" />
                </div>
                <div className="mt-3 text-bold">{message}</div>
                {!this.props.entity ? null : (
                  <div className="mt-2">
                    <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">
                      <input className="custom-control-input" type="checkbox" checked={this.state.enableCascade === true} onChange={() => this.enableCascade()} />
                      <span className="custom-control-label"> {$L('同时删除关联记录')}</span>
                    </label>
                    <div className={this.state.enableCascade ? '' : 'hide'}>
                      <select className="form-control form-control-sm" ref={(c) => (this._cascades = c)} multiple>
                        {(this.state.cascadesEntity || []).map((item) => {
                          return (
                            <option key={`opt-${item[0]}`} value={item[0]}>
                              {item[1]}
                            </option>
                          )
                        })}
                      </select>
                    </div>
                  </div>
                )}
                <div className="mt-4 mb-3" ref={(c) => (this._btns = c)}>
                  <button className="btn btn-space btn-secondary" type="button" onClick={() => this.hide()}>
                    {$L('取消')}
                  </button>
                  <button className="btn btn-space btn-danger" type="button" onClick={() => this.handleDelete()}>
                    {$L('删除')}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  enableCascade() {
    this.setState({ enableCascade: !this.state.enableCascade })
    if (!this.state.cascadesEntity) {
      $.get(`/commons/metadata/references?entity=${this.props.entity}&permission=D`, (res) => {
        this.setState({ cascadesEntity: res.data }, () => {
          this.__select2 = $(this._cascades)
            .select2({
              placeholder: $L('选择关联实体 (可选)'),
              width: '88%',
            })
            .val(null)
            .trigger('change')
        })
      })
    }
  }

  handleDelete() {
    let ids = this.props.ids || this.props.id
    if (!ids || ids.length === 0) return
    if (typeof ids === 'object') ids = ids.join(',')
    const cascades = this.__select2 ? this.__select2.val().join(',') : ''

    const $btns = $(this._btns).find('.btn').button('loading')
    $.post(`/app/entity/record-delete?id=${ids}&cascades=${cascades}`, (res) => {
      if (res.error_code === 0) {
        if (res.data.deleted === res.data.requests) RbHighbar.success($L('删除成功'))
        else if (res.data.deleted === 0) RbHighbar.error($L('无法删除选中记录'))
        else RbHighbar.success($L('成功删除 %d 条记录', res.data.deleted))

        this.hide()
        typeof this.props.deleteAfter === 'function' && this.props.deleteAfter()
      } else {
        RbHighbar.error(res.error_msg)
        $btns.button('reset')
      }
    })
  }
}

// ~~ 百度地图

// https://mapopen-pub-jsapi.bj.bcebos.com/jsapi/reference/jsapi_webgl_1_0.html#a1b0
class BaiduMap extends React.Component {
  constructor(props) {
    super(props)
    this._mapid = `map-${$random()}`
  }

  render() {
    return <div className="map-container" id={this._mapid} />
  }

  componentDidMount() {
    const that = this
    $useMap(() => {
      const _BMapGL = window.BMapGL
      const map = new _BMapGL.Map(that._mapid)
      map.addControl(new _BMapGL.ZoomControl())
      map.addControl(new _BMapGL.ScaleControl())
      // 滚动缩放
      if (that.props.disableScrollWheelZoom !== true) map.enableScrollWheelZoom()

      that._map = map

      // 初始位置
      if (that.props.lnglat && that.props.lnglat.lng && that.props.lnglat.lat) {
        that.center(that.props.lnglat)
      } else {
        const geo = new window.BMapGL.Geolocation()
        geo.enableSDKLocation()
        geo.getCurrentPosition(function (e) {
          if (this.getStatus() === window.BMAP_STATUS_SUCCESS) {
            map.centerAndZoom(e.point, 14)
          } else {
            map.centerAndZoom('北京市', 14)
            console.log('Geolocation failed :', this.getStatus())
          }
        })
      }

      if (that.props.canPin) {
        const geoc = new _BMapGL.Geocoder()
        let lastMarker = null

        // 点选
        map.addEventListener('click', function (e) {
          if (lastMarker) map.removeOverlay(lastMarker)

          const latlng = e.latlng
          lastMarker = new _BMapGL.Marker(new _BMapGL.Point(latlng.lng, latlng.lat))
          map.addOverlay(lastMarker)

          geoc.getLocation(latlng, (r) => {
            const v = {
              lng: latlng.lng,
              lat: latlng.lat,
              text: r.address,
            }
            typeof that.props.onPin === 'function' && that.props.onPin(v)
          })
        })

        // 搜索
        that._mapLocalSearch = new _BMapGL.LocalSearch(map, {
          renderOptions: { map: map },
          onSearchComplete: function () {},
        })
      }
    })
  }

  componentWillUnmount() {
    this._map && this._map.destroy()
    this._map = null
  }

  center(lnglat) {
    if (!lnglat.lng || !lnglat.lat) return

    const _BMapGL = window.BMapGL
    const map = this._map

    const point = new _BMapGL.Point(lnglat.lng, lnglat.lat)
    if (map.isLoaded()) {
      map.clearOverlays()
      // map.panTo(point)
      map.flyTo(point, 14)
    } else {
      setTimeout(() => map.centerAndZoom(point, 14), 200)
    }

    map.addOverlay(
      new _BMapGL.Marker(point, {
        title: lnglat.text || lnglat.address || '',
      })
    )
  }

  search(s) {
    this._mapLocalSearch.search(s)
  }
}

class BaiduMapModal extends RbModal {
  constructor(props) {
    super(props)
  }

  render() {
    const ss = this.state.suggestion || []

    return this.state.destroy === true ? null : (
      <div className="modal" ref={(c) => (this._rbmodal = c)}>
        <div className="modal-dialog modal-xl">
          <div className="modal-content">
            <div className="modal-header">
              <h3 className="modal-title">{this.props.title || $L('位置')}</h3>
              <button className="close" type="button" onClick={() => this.hide()} title={$L('关闭')}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body p-0">
              {this.props.canPin && (
                <div className="map-pin">
                  <div className="row">
                    <div className="col-6">
                      <div className="dropdown">
                        <div className="input-group w-100">
                          <input
                            type="text"
                            ref={(c) => (this._$searchValue = c)}
                            className="form-control form-control-sm dropdown-toggle"
                            placeholder={$L('查找位置')}
                            defaultValue={this.props.lnglat ? this.props.lnglat.text || '' : ''}
                            onKeyDown={(e) => {
                              if (e.which === 38 || e.which === 40) this._updown(e.which)
                              else if (e.which === 13) this._search()
                              else this._suggest()
                            }}
                            onFocus={() => this._suggest()}
                          />
                          <div className="input-group-append">
                            <button className="btn btn-secondary" type="button" onClick={() => this._search()}>
                              <i className="icon zmdi zmdi-search" />
                            </button>
                          </div>
                        </div>
                        <div className={`dropdown-menu map-suggestion ${ss.length > 0 && 'show'}`}>
                          {ss.map((item) => {
                            return (
                              <a
                                key={$random()}
                                className="dropdown-item"
                                title={item.address}
                                onClick={(e) => {
                                  $stopEvent(e, true)
                                  $(this._$searchValue).val(item.address)
                                  this._BaiduMap.center(item.location)
                                  this.setState({ suggestion: [] })
                                }}>
                                {item.address}
                              </a>
                            )
                          })}
                        </div>
                      </div>
                    </div>
                    <div className="col-6 text-right">
                      <button className="btn btn-primary btn-outline" type="button" onClick={() => this._onConfirm()}>
                        <i className="icon zmdi zmdi-check" /> {$L('确定')}
                      </button>
                    </div>
                  </div>
                </div>
              )}
              <div style={{ height: 500 }}>
                <BaiduMap
                  ref={(c) => (this._BaiduMap = c)}
                  lnglat={this.props.lnglat}
                  canPin={this.props.canPin}
                  onPin={(latlng) => {
                    if (this._$searchValue) {
                      this._latlngValue = latlng
                      $(this._$searchValue).val(latlng.text)
                    }
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    $(this._rbmodal).on('click', (e) => {
      if (e.target && e.target.tagName === 'INPUT') return
      setTimeout(() => {
        this.setState({ suggestion: [] })
      }, 100)
    })
  }

  // show(lnglat) {
  //   $(this._$modal).modal('show')
  //   if (lnglat) {
  //     setTimeout(() => this._BaiduMap.center(lnglat), 100)
  //   }
  // }

  destroy() {
    this.setState({ destroy: true })
  }

  _search() {
    this._BaiduMap.search($val(this._$searchValue))
    this.setState({ suggestion: [] })
  }

  _suggest() {
    if (this._sugTimer) {
      clearTimeout(this._sugTimer)
      this._sugTimer = null
    }

    let q = $(this._$searchValue).val()
    q = $.trim(q)

    this._sugTimer = setTimeout(() => {
      if (!q || q.length < 3) {
        this.setState({ suggestion: [] })
        return
      }

      this._sugCached = this._sugCached || {}
      if (this._sugCached[q]) {
        this.setState({ suggestion: this._sugCached[q] })
        return
      }

      $.get(`/commons/map/suggest?q=${$encode(q)}`, (res) => {
        const result = res.data ? res.data.result || [] : []
        const ss = []
        result.forEach((item) => {
          item.address && ss.push({ address: item.address.replaceAll('-', ''), location: item.location })
        })
        this.setState({ suggestion: ss })
        this._sugCached[q] = ss
      })
    }, 600)
  }

  _updown(key) {
    // TODO
  }

  _onConfirm() {
    if (!this._latlngValue) {
      RbHighbar.create($L('请选取位置'))
      return
    }

    const val = { ...this._latlngValue, text: $val(this._$searchValue) }
    typeof this.props.onConfirm === 'function' && this.props.onConfirm(val)
    this.hide()
  }

  // ~~ Usage
  /**
   * @param {object} lnglat
   */
  static view(lnglat) {
    if (BaiduMapModal._ViewModal) {
      BaiduMapModal._ViewModal.show()
      if (lnglat) BaiduMapModal._ViewModal._BaiduMap.center(lnglat)
    } else {
      renderRbcomp(<BaiduMapModal lnglat={lnglat} />, null, function () {
        BaiduMapModal._ViewModal = this
      })
    }
  }
}

// ~~ 签名板

const SignPad_PenColors = {
  black: 'rgb(0, 0, 0)',
  blue: 'rgb(26, 97, 204)',
  red: 'rgb(202, 51, 51)',
}

class SignPad extends React.Component {
  constructor(props) {
    super(props)

    this._defaultPenColor = $storage.get('SignPad_PenColor') || 'black'
    this.state = { ...props, penColor: this._defaultPenColor }
  }

  render() {
    return (
      <div className="modal sign-pad" ref={(c) => (this._$dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <h5 className="mt-0 text-bold text-uppercase">{$L('签名区')}</h5>
              <button className="close" type="button" onClick={this.hide}>
                <i className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body pt-1">
              <div className="sign-pad-canvas">
                <div className="pen-colors">
                  {Object.keys(SignPad_PenColors).map((item) => {
                    return <a className={`color-${item} && ${this.state.penColor === item && 'active'}`} onClick={() => this._selectPenColor(item)} key={item} />
                  })}
                </div>
                <canvas ref={(c) => (this._$canvas = c)} />
              </div>
              <div className="sign-pad-footer mt-2">
                <button type="button" className="btn btn-secondary btn-space" onClick={() => this._SignaturePad.clear()}>
                  {$L('擦除')}
                </button>
                <button
                  type="button"
                  className="btn btn-primary btn-outline btn-space mr-0"
                  onClick={() => {
                    const data = this._SignaturePad.isEmpty() ? null : this._SignaturePad.toDataURL()
                    typeof this.props.onConfirm === 'function' && this.props.onConfirm(data)
                    this.hide()
                  }}>
                  {$L('确定')}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $root = $(this._$dlg).on('hidden.bs.modal', () => {
      $keepModalOpen()
      if (this.props.disposeOnHide === true) {
        $root.modal('dispose')
        $unmount($root.parent())
      }
    })

    const that = this
    function initSign() {
      that._$canvas.width = Math.min(540, $(window).width() - 40)
      that._$canvas.height = 180
      that._SignaturePad = new window.SignaturePad(that._$canvas, {
        backgroundColor: 'rgba(255, 255, 255, 0)',
        penColor: SignPad_PenColors[that._defaultPenColor],
      })
      that.show()
    }

    if (!window.SignaturePad) {
      $.ajax({
        url: '/assets/lib/widget/signature_pad.umd.min.js',
        dataType: 'script',
        cache: true,
        success: initSign,
      })
    } else {
      initSign()
    }
  }

  componentWillUnmount() {
    if (this._SignaturePad) this._SignaturePad.off()
  }

  _selectPenColor(c) {
    this.setState({ penColor: c }, () => {
      $storage.set('SignPad_PenColor', c)
      this._SignaturePad.clear()
      this._SignaturePad.penColor = SignPad_PenColors[c]
    })
  }

  hide = () => $(this._$dlg).modal('hide')
  show = (clear) => {
    if (clear && this._SignaturePad) this._SignaturePad.clear()
    $(this._$dlg).modal('show')
  }
}

// ~~ 重复记录查看

class RepeatedViewer extends RbModalHandler {
  render() {
    // 第一行为字段名
    const data = this.props.data

    return (
      <RbModal ref={(c) => (this._dlg = c)} title={$L('存在重复记录')} disposeOnHide={true} colored="warning">
        <table className="table table-hover dialog-table">
          <thead>
            <tr>
              <th width="32" />
              {data[0].map((item, idx) => {
                if (idx === 0) return null
                return <th key={`field-${idx}`}>{item}</th>
              })}
              <th width="55" />
            </tr>
          </thead>
          <tbody>
            {data.map((item, idx) => {
              if (idx === 0) return null
              else return this.renderRow(item, idx)
            })}
          </tbody>
        </table>
      </RbModal>
    )
  }

  renderRow(item, idx) {
    return (
      <tr key={`row-${idx}`}>
        <td className="text-right pl-0">{idx}</td>
        {item.map((o, i) => {
          if (i === 0) return null
          return <td key={`col-${idx}-${i}`}>{o || <span className="text-muted">{$L('无')}</span>}</td>
        })}
        <td className="actions">
          <button type="button" className="btn btn-light btn-sm w-auto" onClick={() => this.openView(item[0])} title={$L('查看详情')}>
            <i className="zmdi zmdi-open-in-new fs-16 down-2" />
          </button>
        </td>
      </tr>
    )
  }

  openView(id) {
    if (window.RbViewModal) window.RbViewModal.create({ id: id, entity: this.props.entity })
    else window.open(`${rb.baseUrl}/app/list-and-view?id=${id}`)
  }
}
