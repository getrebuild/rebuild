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

    this._$select = []
    this._$select2 = []
    this._codes = []
    this.state = { openLevel: props.openLevel || 0, datas: [] }
  }

  render() {
    return (
      <div className="modal selector" ref={(c) => (this._dlg = c)}>
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (Esc)`}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <h5 className="mt-0 text-bold">{$L('选择%s', this.props.label)}</h5>
              <div>
                <select ref={(c) => this._$select.push(c)} className="form-control form-control-sm">
                  {(this.state.datas[0] || []).map((item) => {
                    return (
                      <option key={`item0-${item[0]}`} value={item[0]}>
                        {item[1]}
                      </option>
                    )
                  })}
                </select>
              </div>
              {this.state.openLevel >= 1 && (
                <div>
                  <select ref={(c) => this._$select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[1] || []).map((item) => {
                      return (
                        <option key={`item1-${item[0]}`} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 2 && (
                <div>
                  <select ref={(c) => this._$select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[2] || []).map((item) => {
                      return (
                        <option key={`item2-${item[0]}`} value={item[0]}>
                          {item[1]}
                        </option>
                      )
                    })}
                  </select>
                </div>
              )}
              {this.state.openLevel >= 3 && (
                <div>
                  <select ref={(c) => this._$select.push(c)} className="form-control form-control-sm">
                    {(this.state.datas[3] || []).map((item) => {
                      return (
                        <option key={`item3-${item[0]}`} value={item[0]}>
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
    const $root = this.show()
    $root.on('hidden.bs.modal', () => {
      this.props.keepModalOpen && $keepModalOpen()
      if (this.props.disposeOnHide === true) {
        $root.modal('dispose')
        $unmount($root.parent())
      }
    })

    const that = this
    $(this._$select).each(function (idx) {
      const s = $(this)
        .select2({
          placeholder: $L('选择 %d 级分类', idx + 1),
          allowClear: false,
          templateResult: function (res) {
            const $span = $('<span class="code-append"></span>').attr('title', res.text).text(res.text)
            that._codes[res.id] && $(`<em>${that._codes[res.id]}</em>`).appendTo($span)
            return $span
          },
        })
        .on('change', () => {
          const p = $(s).val()
          if (p && s.__level < that.state.openLevel) {
            that._loadData(s.__level + 1, p) // Load next-level
          }
        })

      s.__level = idx
      that._$select2.push(s)
    })

    // init
    this._loadData(0)
  }

  _loadData(level, p) {
    $.get(`/commons/metadata/classification?entity=${this.props.entity}&field=${this.props.field}&parent=${p || ''}`, (res) => {
      res.data &&
        res.data.forEach((item) => {
          this._codes[item[0]] = item[2]
        })

      const s = this.state.datas
      s[level] = res.data
      this.setState({ datas: s }, () => this._$select2[level].trigger('change'))
    })
  }

  confirm() {
    const last = this._$select2[this.state.openLevel]
    const v = last.val()
    if (!v) {
      RbHighbar.create($L('请选择%s', this.props.label))
    } else {
      const text = []
      $(this._$select2).each(function () {
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
window.referenceSearch__form // fix:4.1.2 哪个表单打开的
// see `reference-search.html`
class ReferenceSearcher extends RbModal {
  renderContent() {
    return this.state.destroy === true ? null : super.renderContent()
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
              <button className="close" type="button" onClick={() => this.hide()} title={`${$L('关闭')} (Esc)`}>
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
                      <span className="custom-control-label"> {$L('同时删除相关记录')}</span>
                    </label>
                    <div className={this.state.enableCascade ? '' : 'hide'}>
                      <select className="form-control form-control-sm" ref={(c) => (this._$cascades = c)} multiple>
                        {(this.state.cascadesEntity || []).map((item) => {
                          if ($isSysMask(item[1])) return null
                          return (
                            <option key={item[0]} value={item[0]}>
                              {item[1]}
                            </option>
                          )
                        })}
                      </select>
                    </div>
                  </div>
                )}
                <div className="mt-4 mb-3" ref={(c) => (this._btns = c)}>
                  <button disabled={this.state.disable} className="btn btn-space btn-secondary" type="button" onClick={() => this.hide()}>
                    {$L('取消')}
                  </button>
                  <button disabled={this.state.disable} className="btn btn-space btn-danger" type="button" onClick={() => this.handleDelete()} ref={(c) => (this._$dbtn = c)}>
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
          this.__select2 = $(this._$cascades)
            .select2({
              placeholder: $L('选择'),
              width: '88%',
              language: {
                noResults: () => $L('无'),
              },
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

    let _timer1 = setTimeout(() => $(this._$dbtn).text($L('请稍后')), 6000)
    let _timer2 = setTimeout(() => $(this._$dbtn).text($L('仍在继续')), 15000)

    this.disabled(true, true)
    $.post(`/app/entity/record-delete?id=${ids}&cascades=${cascades}`, (res) => {
      clearTimeout(_timer1)
      clearTimeout(_timer2)

      if (res.error_code === 0) {
        if (res.data.deleted >= res.data.requests) RbHighbar.success($L('删除成功'))
        else if (res.data.deleted === 0) RbHighbar.error($L('无法删除记录'))
        else RbHighbar.success($L('成功删除 %d 条记录', res.data.deleted))

        this.hide(true)
        typeof this.props.deleteAfter === 'function' && this.props.deleteAfter(res.data.deleted)
      } else {
        $(this._$dbtn).text($L('删除'))
        RbHighbar.error(res.error_msg)
        this.disabled()
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
      if (that.props.disableScrollWheelZoom !== true) {
        map.addControl(new _BMapGL.LocationControl())
        map.enableScrollWheelZoom()
      }

      that._map = map

      // 初始位置
      const _lnglat = that.props.lnglat
      if (_lnglat) {
        if (_lnglat.lng && _lnglat.lat) {
          that.center(_lnglat)
        } else if (_lnglat.text) {
          const geoc = new _BMapGL.Geocoder()
          geoc.getPoint(_lnglat.text, function (point) {
            that.center(point)
          })
        }
      } else {
        const geol = new _BMapGL.Geolocation()
        geol.enableSDKLocation()
        geol.getCurrentPosition(function (e) {
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
  renderContent() {
    return this.state.destroy ? null : (
      <RF>
        {this.props.canPin && (
          <div className="map-pin">
            <div className="row">
              <div className="col-6">
                <div className="dropdown">
                  <div className="input-group w-100">
                    <input
                      type="text"
                      ref={(c) => (this._$searchValue = c)}
                      className="form-control form-control-sm"
                      placeholder={$L('查找位置')}
                      defaultValue={this.props.lnglat ? this.props.lnglat.text || '' : ''}
                      autoComplete="off"
                    />
                    <div className="input-group-append">
                      <button className="btn btn-secondary" type="button" onClick={() => this._search()}>
                        <i className="icon zmdi zmdi-search" />
                      </button>
                    </div>
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
      </RF>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    // https://bootstrap-autocomplete.readthedocs.io/en/latest/
    const that = this
    function _autoComplete() {
      $(that._$searchValue)
        .autoComplete({
          bootstrapVersion: '4',
          noResultsText: '',
          minLength: 2,
          resolverSettings: {
            url: '/commons/map/suggest',
          },
          events: {
            searchPost: function (res) {
              const result = res.data ? res.data.result || [] : []
              const _data = []
              result.forEach((item) => {
                item.address && _data.push({ text: item.address.replaceAll('-', ''), id: item.location })
              })
              return _data
            },
          },
        })
        .on('autocomplete.select', (e, item) => {
          $stopEvent(e, true)
          $(that._$searchValue).val(item.text)
          that._BaiduMap.center(item.id)

          that._latlngValue = {
            lat: item.id.lat,
            lng: item.id.lng,
            text: item.text,
          }
        })
    }
    if (jQuery.prototype.autoComplete) {
      _autoComplete()
    } else {
      $getScript('/assets/lib/bootstrap-autocomplete.min.js?v=2.3.7', () => _autoComplete())
    }
  }

  _search() {
    this._BaiduMap.search($val(this._$searchValue))
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

  destroy() {
    this.setState({ destroy: true })
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
      renderRbcomp(<BaiduMapModal lnglat={lnglat} title={$L('查看位置')} useWhite />, function () {
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
              <button className="close" type="button" onClick={this.hide} title={`${$L('关闭')} (Esc)`}>
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
    $(this._$dlg).modal({
      show: true,
      keyboard: true,
    })
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
        <td className="text-right pl-0 text-muted">{idx}.</td>
        {item.map((o, i) => {
          if (i === 0) return null
          return <td key={`col-${idx}-${i}`}>{o || <span className="text-muted">{$L('无')}</span>}</td>
        })}
        <td className="actions">
          <a className="btn btn-light btn-sm w-auto" style={{ lineHeight: '28px' }} title={$L('打开')} href={`${rb.baseUrl}/app/redirect?id=${item[0]}&type=newtab`} target="_blank">
            <i className="zmdi zmdi-open-in-new fs-16 down-2" />
          </a>
        </td>
      </tr>
    )
  }
}

// -- LiteForm

if (!window.RbForm) window.RbForm = function () {}
// eslint-disable-next-line no-unused-vars
class LiteForm extends RbForm {
  renderDetailForms() {
    return null
  }

  renderFormAction() {
    return null
  }

  componentDidMount() {
    super.componentDidMount()
    // TODO init...
  }

  buildFormData(retAll) {
    const s = retAll ? this.getFormData() : {}
    const data = this.__FormData || {}
    for (let k in data) {
      const error = data[k].error
      if (error) {
        RbHighbar.create(error)
        return false
      }
      s[k] = data[k].value
    }
    s.metadata = { id: this.props.id || '' }
    return s
  }
}

class LiteFormModal extends RbModalHandler {
  constructor(props) {
    super(props)
    this._ids = this.props.ids || []
  }

  render() {
    const props = this.props
    const entity = props.entityMeta
    const title = props.title || (props.id ? $L('编辑%s', entity.entityLabel) : $L('新建%s', entity.entityLabel))
    const fake = {
      state: { id: props.id },
    }

    return (
      <RbModal title={title} ref={(c) => (this._dlg = c)} disposeOnHide>
        <div className="liteform-wrap">
          <LiteForm entity={entity.entity} id={props.id} rawModel={{}} $$$parent={fake} ref={(c) => (this._LiteForm = c)}>
            {props.elements.map((item) => {
              // eslint-disable-next-line no-undef
              return detectElement(item, entity.entity)
            })}
          </LiteForm>

          <div className="footer" ref={(c) => (this._$formAction = c)}>
            {this._ids.length > 1 && <RbAlertBox message={WrapHtml($L('本次保存将修改 **%d** 条记录', this.props.ids.length))} type="info" className="mt-0 mb-2" />}

            <button className="btn btn-primary" type="button" disabled={this.state.readonly === true} onClick={() => this._handleSave()}>
              {props.confirmText || $L('保存')}
            </button>
            <a className="btn btn-link" onClick={this.hide}>
              {$L('取消')}
            </a>
          </div>
        </div>
      </RbModal>
    )
  }

  componentDidMount() {
    // super.componentDidMount()

    let ro = true
    this.props.elements.forEach((item) => {
      if (item.readonly !== true) ro = false
    })
    if (ro) this.setState({ readonly: ro })
  }

  _handleSave(weakMode) {
    const data = this._LiteForm.buildFormData(this._ids.length > 1)
    if (data === false) return

    const props = this.props
    const data2 = {
      ...data,
      metadata: {
        entity: props.entityMeta.entity,
        id: props.id || null,
      },
    }

    if (typeof this.props.onHandleSave === 'function') {
      const s = this.props.onHandleSave(data2, this)
      if (s === false) return
    }

    this.disabled(true)
    let url = `/app/entity/liteform/record-save?weakMode=${weakMode || 0}`
    if (this._ids.length > 1) url += '&ids=' + this.props.ids.join(',')
    $.post(url, JSON.stringify(data2), (res) => {
      this.disabled()
      if (res.error_code === 0) {
        RbHighbar.success($L('保存成功'))

        // 刷新列表
        const rlp = window.RbListPage || parent.RbListPage
        if (rlp) rlp.reload(data.id)
        // 刷新视图
        if (window.RbViewPage) window.RbViewPage.reload()
        // 关闭
        this.hide()
      } else if (res.error_code === 497) {
        // 弱校验
        const that = this
        const msg_id = res.error_msg.split('$$$$')
        RbAlert.create(msg_id[0], {
          onConfirm: function () {
            this.hide()
            that._handleSave(msg_id[1])
          },
        })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }

  disabled(d) {
    if (!this._$formAction) return
    if (d === true) $(this._$formAction).find('.btn').button('loading')
    else $(this._$formAction).find('.btn').button('reset')
  }

  // -- Usage

  /**
   * @param {*} entityOrId
   * @param {*} fields
   * @param {*} title
   * @param {*} onHandleSave
   */
  static create(entityOrId, fields, title, onHandleSave) {
    // 无实体表单模式
    if (entityOrId === false) {
      const fakeModel = {
        entityMeta: {
          entity: '__liteform__',
          entityLabel: '__liteform__',
        },
        elements: [],
      }
      // 修订
      fields.forEach((item) => {
        let field = { nullable: true, type: 'TEXT', colspan: 4 }
        if (typeof F === 'string') {
          field = { ...field, field: item, label: item }
        } else {
          field = { ...field, ...item }
        }
        fakeModel.elements.push(field)
      })

      renderRbcomp(
        <LiteFormModal
          title={title || $L('无标题')}
          onHandleSave={(data, formObj) => {
            if (typeof onHandleSave === 'function') {
              const s = onHandleSave(data, formObj)
              // 默认关闭
              if (s !== false) formObj.hide()
            } else {
              console.log(data)
              formObj.hide()
            }
            return false
          }}
          confirmText={$L('确定')}
          {...fakeModel}
        />
      )
      return
    }

    // v4.1 这里支持修改引用字段（仅单个字段时）
    if (Array.isArray(fields) && fields.length === 1 && typeof fields[0] === 'string' && fields[0].includes('.')) {
      $.get(`/commons/frontjs/reffield-editable?id=${entityOrId}&reffield=${fields[0]}`, (res) => {
        if (res.error_code === 0) {
          LiteFormModal.create(res.data.id, [res.data.field], title, onHandleSave)
        } else {
          RbHighbar.create(res.error_msg)
        }
      })
      return
    }

    const isMultiId = Array.isArray(entityOrId)
    const post = {
      id: isMultiId ? entityOrId[0] : entityOrId,
      fields: fields,
    }

    $.post('/app/entity/liteform/form-model', JSON.stringify(post), (res) => {
      if (res.error_code === 0) {
        renderRbcomp(<LiteFormModal title={title} onHandleSave={onHandleSave} {...res.data} ids={isMultiId ? entityOrId : null} />)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

/**
 * 表单区域
 */
class LiteFormArea extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    const _data = this.state.data
    return _data ? (
      <RF>
        {this.props.divider ? (
          <div className="form-line">
            <fieldset>
              <legend>{this.props.divider}</legend>
            </fieldset>
          </div>
        ) : (
          <div className="col-12"></div>
        )}

        {_data.map((item, idx) => {
          return (
            <div className="col-sm-6 form-group pt-0 pb-1" key={idx}>
              <label className="col-form-label">{item.label}</label>
              <div className="col-form-control">
                <div className="form-control-plaintext">{this._text(item.value)}</div>
              </div>
            </div>
          )
        })}
      </RF>
    ) : null
  }

  componentDidMount() {
    const $$$parent = this.props.$$$parent

    // 视图
    if ($$$parent.__ViewData) {
      this.initOnView($$$parent.__ViewData)
    } else {
      // 新建
      if ($$$parent.isNew) {
        this.initOnFormNew()
      }
      // 编辑
      else {
        const data = {}
        $$$parent.props.rawModel.elements.forEach((item) => (data[item.field] = item.value))
        this.initOnFormEdit(data)
      }
    }
  }

  initOnFormEdit(data) {
    this.initOnView(data)
    this.initOnFormNew()
  }

  initOnFormNew() {
    const $$$parent = this.props.$$$parent
    $$$parent.onFieldValueChange((s) => {
      if (s.name === this.props.triggerField) {
        if (s.value) this._fetch(s.value)
        else this.setState({ data: null })
      }
    })
  }

  initOnView(data) {
    const val = data[this.props.triggerField]
    if (val) this._fetch(typeof val === 'object' ? val.id : val)
  }

  _fetch(id) {
    const post = {
      id: id,
      fields: this.props.showFields || this.props.fields,
    }

    $.post('/app/entity/liteform/form-model', JSON.stringify(post), (res) => {
      if (res.data && res.data.elements) {
        this.setState({ data: res.data.elements })
      } else {
        this.setState({ data: null })
      }
    })
  }

  _text(v) {
    if ($empty(v)) {
      return <span className="text-muted">{$L('无')}</span>
    }

    if (typeof v === 'object') {
      v = v.text
      if (typeof v === 'object') v = v.join(' / ')
      return v
    }

    if (v === 'F') return $L('否')
    else if (v === 'T') return $L('是')
    else return v
  }
}

const EasyFilterEval = {
  evalAndEffect: function (formObject) {
    // LiteForm or Others
    if (!formObject.props.rawModel.layoutId) return

    const key = formObject.props.rawModel.layoutId + (formObject.props.id || '')
    this.__timers = this.__timers || {}
    if (this.__timers[key]) {
      clearTimeout(this.__timers[key])
      this.__timers[key] = null
    }
    this.__timers[key] = setTimeout(() => this._evalAndEffect(formObject), 200)
  },

  _evalAndEffect: function (formObject) {
    const _this = formObject
    $.post(`/app/entity/extras/easyfilter-eval?layout=${_this.props.rawModel.layoutId}&id=${_this.props.id || ''}`, JSON.stringify(_this.getFormData()), (res) => {
      const attrs = res.data || []
      const attrsLast = _this.__lastEasyFilterEval || []
      _this.__lastEasyFilterEval = attrs // 挂载到表单对象

      attrs.forEach((a) => {
        const fieldComp = _this.getFieldComp(a.field)
        if (fieldComp) {
          const aLast = this._getLastAttr(a.field, attrsLast)
          if (a.hidden === true || a.hidden === false) {
            if (aLast.hidden !== a.hidden) fieldComp.setHidden(a.hidden)
          }
          if (a.required === true || a.required === false) {
            if (aLast.required !== a.required) fieldComp.setNullable(!a.required)
          }
          if (a.readonly === true || a.readonly === false) {
            if (aLast.readonly !== a.readonly) fieldComp.setReadonly(a.readonly)
          }
        }
      })
    })
  },

  _getLastAttr(field, attrsLast) {
    for (let i = 0; i < attrsLast.length; i++) {
      if (attrsLast[i].field === field) return attrsLast[i]
    }
    return {}
  },
}

// ~~ Excel 粘贴数据
class ExcelClipboardData extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    if (this.state.hasError) {
      return <div className="must-center text-danger">{this.state.hasError}</div>
    }

    if (!this.state.data) {
      let tips = $L('复制 Excel 单元格 Ctrl + V 粘贴')
      if ($.browser.mac) tips = tips.replace('Ctrl', 'Command')
      return (
        <div className="must-center text-muted">
          <div className="mb-2">
            <i className="mdi mdi-microsoft-excel" style={{ fontSize: 48 }} />
          </div>
          {tips}
        </div>
      )
    }

    return (
      <div className="rsheetb-table" ref={(c) => (this._$table = c)}>
        <div className="head-action">
          <span className="float-left">
            <h5 className="text-bold fs-14 m-0" style={{ paddingTop: 11 }}>
              {$L('请选择列字段')}
            </h5>
          </span>
          <span className="float-right">
            <button className="btn btn-primary" onClick={() => this._handleConfirm()} ref={(c) => (this._$btn = c)}>
              {this.props.confirmText || $L('确定')}
            </button>
          </span>
          <div className="clearfix" />
        </div>
        {WrapHtml(this.state.data)}
      </div>
    )
  }

  componentDidMount() {
    const that = this
    function _FN() {
      $(document).on('paste.csv-data', (e) => {
        let data
        try {
          // https://docs.sheetjs.com/docs/demos/local/clipboard/
          // https://docs.sheetjs.com/docs/api/utilities/html
          const c = e.originalEvent.clipboardData.getData('text/html')
          if (!c) return

          $stopEvent(e, true)
          const wb = window.XLSX.read(c, { type: 'string' })
          const ws = wb.Sheets[wb.SheetNames[0]]
          data = window.XLSX.utils.sheet_to_html(ws, { id: 'rsheetb', header: '', footer: '', editable: true })

          // No content
          if (data && !$(data).text()) data = null
        } catch (err) {
          console.log('Cannot read csv-data from clipboardData', err)
        }

        if (data) {
          if (rb.env === 'dev') console.log(data)
          $(that._$table).find('table thead').remove()
          that.setState({ data: data, hasError: null }, () => that._tableAfter())
        } else {
          RbHighbar.createl('未识别到有效数据')
        }
      })
    }

    if (window.XLSX) _FN()
    else $getScript('/assets/lib/charts/xlsx.full.min.js', setTimeout(_FN, 200))
  }

  _tableAfter() {
    const $table = $(this._$table).find('table').addClass('table table-sm table-bordered')
    const fields = this.props.fields
    if (fields) {
      const len = $table.find('tbody>tr:eq(0)').find('td').length
      const $tr = $('<thead><tr></tr></thead>').appendTo($table).find('tr')
      for (let i = 0; i < len; i++) {
        const $th = $('<th><select></select></th>').appendTo($tr)
        fields.forEach((item) => {
          $(`<option value="${item.field}">${item.label}</option>`).appendTo($th.find('select'))
        })
        $th
          .find('select')
          .select2({
            placeholder: $L('无'),
          })
          .val(fields[i] ? fields[i].field : null)
          .trigger('change')
      }
    }
  }

  _handleConfirm() {
    const fields = []
    $(this._$table)
      .find('thead select')
      .each(function () {
        let fm = $(this).val() || null
        if (fm && fields.includes(fm)) fm = null
        fields.push(fm)
      })

    let noAnyFields = true
    for (let i = 0; i < fields.length; i++) {
      if (fields[i]) {
        noAnyFields = false
        break
      }
    }
    if (noAnyFields) return RbHighbar.createl('请至少选择一个列字段')

    const that = this
    const dataWithFields = []
    $(this._$table)
      .find('tbody tr')
      .each(function () {
        const L = {}
        $(this)
          .find('td')
          .each(function (idx) {
            const name = fields[idx]
            if (name) {
              let val = $(this).text()
              if ((that.props.fields[idx] || {}).type === 'NTEXT') {
                val = $(this).find('>span').html()
                if (val) val = val.replace(/<br>/gi, '\n')
              }
              L[name] = val || null
            }
          })
        dataWithFields.push(L)
      })

    const $btn = $(this._$btn).button('loading')
    const url = `/app/entity/extras/csvdata-rebuild?entity=${this.props.entity}&mainid=${this.props.mainid || ''}&layoutId=${this.props.layoutId || ''}`
    $.post(url, JSON.stringify(dataWithFields), (res) => {
      if (res.error_code === 0) {
        this.props.onConfirm && this.props.onConfirm(res.data)
      } else {
        RbHighbar.error(res.error_msg)
      }
      $btn.button('reset')
    })
  }

  componentWillUnmount() {
    $(document).off('paste.csv-data')
  }
}

class ExcelClipboardDataModal extends RbModalHandler {
  render() {
    return (
      <RbModal title={this.props.title || $L('从 Excel 添加')} width="1000" className="modal-rsheetb" disposeOnHide maximize ref={(c) => (this._dlg = c)}>
        <ExcelClipboardData
          {...this.props}
          onConfirm={(data) => {
            if (typeof this.props.onConfirm === 'function') {
              let res = this.props.onConfirm(data)
              if (res === false);
              else this.hide()
            } else {
              this.hide()
            }
          }}
        />
      </RbModal>
    )
  }
}

// LAB 从 Excel 添加数据
class ExcelClipboardDataModalWithForm extends React.Component {
  state = {}
  render() {
    return this.state.formProps ? <ExcelClipboardDataModal {...this.state.formProps} confirmText={$L('保存')} ref={(c) => (this._ExcelClipboardDataModal = c)} /> : null
  }

  componentDidMount() {
    $.post(`/app/${this.props.entity}/form-model?layout=${this.props.specLayout || ''}`, (res) => {
      // 包含错误
      if (res.error_code > 0 || !!res.data.error) {
        const error = (res.data || {}).error || res.error_msg
        RbHighbar.error(error)
        return
      }

      const formProps = {
        ...this.props,
        entity: res.data.entity,
        layoutId: res.data.layoutId,
        fields: [],
        onConfirm: (data) => this.handleConfirm(data),
      }

      if (this.props.fields && this.props.fields.length) {
        formProps.fields = this.props.fields
      } else {
        res.data.elements.forEach((item) => {
          if (item.readonly || !item.type) return
          formProps.fields.push({
            field: item.field,
            label: item.label,
            type: item.type,
          })
        })
      }

      this.setState({ formProps })
    })
  }

  handleConfirm(data) {
    function _FN() {
      const formsData = []
      data.forEach((item) => {
        const formData = {
          metadata: { entity: this.props.entity },
        }
        item.elements.forEach((d) => {
          if (d.value) {
            if (typeof d.value === 'object') formData[d.field] = d.value.id
            else formData[d.field] = d.value
          }
        })
        formsData.push(formData)
      })

      let len = formsData.length
      let success = 0
      formsData.forEach((formData) => {
        $.post('/app/entity/record-save', JSON.stringify(formData), (res) => {
          len--
          if (res.error_code === 0) success++
          if (len <= 0) {
            RbHighbar.success($L('成功保存 %d 条记录', success))
            this._ExcelClipboardDataModal.hide()
            const rlp = window.RbListPage || parent.RbListPage
            if (rlp) rlp.reload(data.id)
          }
        })
      })
    }

    RbAlert.create($L('是否确认保存？'), { onConfirm: () => _FN() })
    return false
  }
}
