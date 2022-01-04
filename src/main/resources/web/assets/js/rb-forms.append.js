/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
// 表单附加操作，可在其他页面独立引入

// 分类数据选择
// eslint-disable-next-line no-unused-vars
class ClassificationSelector extends React.Component {
  constructor(props) {
    super(props)

    this._select = []
    this._select2 = []
    this.state = { openLevel: props.openLevel || 0, datas: [] }
  }

  render() {
    return (
      <div className="modal selector" ref={(c) => (this._dlg = c)} tabIndex="-1">
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

// eslint-disable-next-line no-unused-vars
window.referenceSearch__call = function (selected) {}
// eslint-disable-next-line no-unused-vars
window.referenceSearch__dlg

// ~~ 引用字段搜索
// see `reference-search.html`
// eslint-disable-next-line no-unused-vars
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
              <iframe src={this.props.url} frameBorder="0" style={{ minHeight: 368 }} />
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    // eslint-disable-next-line no-unused-vars
    window.referenceSearch__dlg = this
  }

  destroy() {
    this.setState({ destroy: true })
  }
}

// 删除确认
// eslint-disable-next-line no-unused-vars
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

// 百度地图
// https://mapopen-pub-jsapi.bj.bcebos.com/jsapi/reference/jsapi_webgl_1_0.html#a1b0
class BaiduMap extends React.Component {
  render() {
    return <div className="map-container" ref={(c) => (this._$container = c)} />
  }

  componentDidMount() {
    const that = this
    $useMap(() => {
      const _BMapGL = window.BMapGL
      const map = new _BMapGL.Map(that._$container)
      map.addControl(new _BMapGL.ZoomControl())
      map.addControl(new _BMapGL.ScaleControl())
      // 滚动缩放
      if (that.props.enableScrollWheelZoom) map.enableScrollWheelZoom()

      that._map = map

      // 初始位置
      if (that.props.lnglat && that.props.lnglat.lng && that.props.lnglat.lat) {
        that.center(that.props.lnglat)
      } else {
        // map.centerAndZoom('北京市', 14)
        const geo = new window.BMapGL.Geolocation()
        geo.enableSDKLocation()
        geo.getCurrentPosition(function (e) {
          if (this.getStatus() === window.BMAP_STATUS_SUCCESS) {
            map.panTo(e.point)
          } else {
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
      map.flyTo(point)
    }

    map.addOverlay(
      new _BMapGL.Marker(point, {
        title: lnglat.text || lnglat.address || '',
      })
    )

    if (!map.isLoaded()) {
      map.centerAndZoom(point, 14)
    }
  }

  search(s) {
    this._mapLocalSearch.search(s)
  }
}

// eslint-disable-next-line no-unused-vars
class BaiduMapModal extends RbModal {
  constructor(props) {
    super(props)
  }

  render() {
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
                      <div className="input-group w-100">
                        <input
                          type="text"
                          ref={(c) => (this._searchValue = c)}
                          className="form-control form-control-sm"
                          placeholder={$L('查找位置')}
                          onKeyDown={(e) => {
                            e.which === 13 && this._search()
                          }}
                          defaultValue={this.props.lnglat ? this.props.lnglat.text || '' : ''}
                        />
                        <div className="input-group-append">
                          <button className="btn btn-secondary" type="button" onClick={() => this._search()}>
                            <i className="icon zmdi zmdi-search" />
                          </button>
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
                    if (this._searchValue) {
                      this._latlngValue = latlng
                      $(this._searchValue).val(latlng.text)
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
    this._BaiduMap.search($val(this._searchValue))
  }

  _onConfirm() {
    if (!this._latlngValue) {
      RbHighbar.create($L('请选取位置'))
      return
    }

    const val = { ...this._latlngValue, text: $val(this._searchValue) }
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

// 签名板
// eslint-disable-next-line no-unused-vars
class SignPad extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="modal sign-pad" ref={(c) => (this._$dlg = c)} tabIndex="-1">
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <h5 className="mt-0 text-bold">{$L('签名区')}</h5>
              <button className="close" type="button" onClick={this.hide}>
                <i className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body pt-1">
              <div className="sign-pad-canvas">
                <canvas ref={(c) => (this._$canvas = c)} />
              </div>
              <div className="sign-pad-footer mt-2">
                <button type="button" className="btn btn-secondary btn-space" onClick={() => this._SignaturePad.clear()}>
                  {$L('清除')}
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
    const that = this
    function initSign() {
      that._$canvas.width = 540
      that._$canvas.height = 180
      that._SignaturePad = new window.SignaturePad(that._$canvas, {
        backgroundColor: 'rgba(255, 255, 255, 0)',
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
    console.log('SignPad componentWillUnmount')
    if (this._SignaturePad) this._SignaturePad.off()
  }

  hide = () => $(this._$dlg).modal('hide')
  show = (clear) => {
    if (clear && this._SignaturePad) this._SignaturePad.clear()
    $(this._$dlg).modal('show')
  }
}
