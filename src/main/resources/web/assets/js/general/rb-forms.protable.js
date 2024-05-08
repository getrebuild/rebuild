/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global detectElement, TYPE_DIVIDER */
/* eslint-disable no-unused-vars */

// ~~ 表格型表单

const COLUMN_MIN_WIDTH = 30
const COLUMN_MAX_WIDTH = 500
const COLUMN_DEF_WIDTH = 178 // 48
const COLUMN_WIDTH_PLUS = ['REFERENCE', 'N2NREFERENCE', 'CLASSIFICATION']

class ProTable extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
    this._isReadonly = props.$$$main.props.readonly
  }

  render() {
    if (this.state.hasError) {
      // $('.detail-form-table .btn-group .btn').attr('disabled', true)
      return <RbAlertBox message={this.state.hasError} />
    }

    // 等待初始化
    if (!this.state.formFields) return null

    const formFields = this.state.formFields
    const details = this.state.details || [] // 编辑时有

    // fixed 模式大概 5 个字段
    const ww = $(window).width()
    const fw = ww > 1064 ? 994 : ww - 70
    const fixed = COLUMN_DEF_WIDTH * formFields.length + (38 + 48) > fw

    return (
      <div className={`protable rb-scroller ${fixed && 'column-fixed-pin'}`} ref={(c) => (this._$scroller = c)}>
        <table className={`table table-sm ${!fixed && 'table-fixed'}`}>
          <thead>
            <tr>
              <th className="col-index action">
                <a
                  title={$L('全屏')}
                  onClick={() => {
                    const $d = $(this._$scroller).parents('.detail-form-table').toggleClass('fullscreen')
                    const $modal = $(this._$scroller).parents('.rbmodal')
                    $modal.find('.modal-dialog').toggleClass('fullscreen')
                    // height
                    const wh = $d.hasClass('fullscreen') ? $(window).height() - 165 : 'auto'
                    $d.height(wh)
                    $modal.find('.modal-body').height(wh)
                  }}>
                  <i className="mdi mdi-arrow-expand hide" />
                </a>
              </th>
              {formFields.map((item) => {
                if (item.field === TYPE_DIVIDER) return null

                let colStyle2 = { minWidth: COLUMN_DEF_WIDTH }
                if (fixed) {
                  // v35
                  if (item.colspan) colStyle2.minWidth = (COLUMN_DEF_WIDTH / 2) * ~~item.colspan
                  if (COLUMN_WIDTH_PLUS.includes(item.type)) colStyle2.minWidth += 38 // btn
                  if (colStyle2.minWidth > COLUMN_DEF_WIDTH * 2) colStyle2.minWidth = COLUMN_DEF_WIDTH * 2
                }
                // v37 LAB
                if (item.width) {
                  colStyle2.width = item.width
                  colStyle2.minWidth = 'auto'
                }

                return (
                  <th key={item.field} data-field={item.field} style={colStyle2} className={item.nullable ? '' : 'required'}>
                    {item.label}
                    {item.tip && <i className="tipping zmdi zmdi-info-outline" title={item.tip} />}
                    <i className="dividing hide" />
                  </th>
                )
              })}
              <td className={`col-action ${this._initModel.detailsCopiable && 'has-copy-btn'} ${fixed && 'column-fixed'}`} />
            </tr>
          </thead>
          <tbody>
            {(this.state.inlineForms || []).map((FORM, idx) => {
              const key = FORM.key
              return (
                <tr key={`inline-${key}`}>
                  <th className="col-index">{details.length + idx + 1}</th>
                  {FORM}

                  <td className={`col-action ${fixed && 'column-fixed'}`}>
                    {this._initModel.detailsCopiable && (
                      <button className="btn btn-light" title={$L('复制')} onClick={() => this.copyLine(key)} disabled={this._isReadonly}>
                        <i className="icon zmdi zmdi-copy fs-14" />
                      </button>
                    )}
                    <button className="btn btn-light" title={$L('移除')} onClick={() => this.removeLine(key)} disabled={this._isReadonly}>
                      <i className="icon zmdi zmdi-close fs-16 text-bold" />
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>

        {(this.state.inlineForms || []).length === 0 && <div className="text-center text-muted mt-6">{$L('请添加明细')}</div>}
      </div>
    )
  }

  componentDidMount() {
    const entity = this.props.entity
    const initialValue = {
      '$MAINID$': this.props.mainid || '$MAINID$',
    }

    $.post(`/app/${entity.entity}/form-model?id=`, JSON.stringify(initialValue), (res) => {
      // 包含错误
      if (res.error_code > 0 || !!res.data.error) {
        const error = (res.data || {}).error || res.error_msg
        this.setState({ hasError: error })
        return
      }

      this._initModel = res.data // 新建用
      this.setState({ formFields: res.data.elements }, () => {
        $(this._$scroller).perfectScrollbar({
          suppressScrollY: true,
        })
        $(this._$scroller).find('thead .tipping').tooltip()
      })

      // 正常编辑
      if (this.props.mainid) {
        $.get(`/app/${entity.entity}/detail-models?mainid=${this.props.mainid}`, (res) => {
          if (res.error_code === 0) this.setLines(res.data)
          else RbHighbar.error($L('明细加载失败，请稍后重试'))
        })
      }
      // 记录转换
      else if (this.props.previewid) {
        $.get(`/app/${entity.entity}/detail-models?previewid=${this.props.previewid}`, (res) => {
          if (res.error_code === 0) this.setLines(res.data)
          else RbHighbar.error($L('明细加载失败，请稍后重试'))
        })
      }

      this._dividing37()
    })
  }

  _dividing37() {
    const $scroller = $(this._$scroller)
    const that = this
    $scroller.find('th .dividing').draggable({
      containment: $scroller,
      axis: 'x',
      helper: 'clone',
      stop: function (e, ui) {
        const field = $(e.target).parents('th').data('field')
        let left = ui.position.left - -10
        if (left < COLUMN_MIN_WIDTH) left = COLUMN_MIN_WIDTH
        else if (left > COLUMN_MAX_WIDTH) left = COLUMN_MAX_WIDTH

        const fields = that.state.formFields
        for (let i = 0; i < fields.length; i++) {
          if (fields[i].field === field) {
            fields[i].width = left
            break
          }
        }

        that.setState({ formFields: fields }, () => $scroller.perfectScrollbar('update'))
      },
    })
  }

  addNew(specFieldValues) {
    const model = $clone(this._initModel)
    if (specFieldValues) {
      model.elements.forEach((item) => {
        if (specFieldValues[item.field]) item.value = specFieldValues[item.field]
      })
    }
    this._addLine(model)
  }

  _addLine(model) {
    // 明细未配置或出错
    if (!model) {
      if (this.state.hasError) RbHighbar.create(this.state.hasError)
      return
    }

    const entityName = this.props.entity.entity
    const lineKey = `${entityName}-${model.id ? model.id : $random()}`
    const ref = React.createRef()
    const FORM = (
      <InlineForm entity={entityName} id={model.id} rawModel={model} $$$parent={this} $$$main={this.props.$$$main} key={lineKey} ref={ref}>
        {model.elements.map((item) => {
          return detectElement({ ...item, colspan: 4 })
        })}
      </InlineForm>
    )

    const forms = this.state.inlineForms || []
    forms.push(FORM)
    this.setState({ inlineForms: forms }, () => {
      const refs = this._inlineFormsRefs || []
      refs.push(ref)
      this._inlineFormsRefs = refs
      this._onLineUpdated(lineKey)
    })
  }

  copyLine(lineKey) {
    const f = this.getLineForm(lineKey)
    const data = f ? f.getFormData() : null
    if (!data) return

    // New
    delete data.metadata.id

    const mainid = this.props.$$$main.props.id || '000-0000000000000000'
    $.post(`/app/entity/extras/formdata-rebuild?mainid=${mainid}`, JSON.stringify(data), (res) => {
      if (res.error_code === 0) this._addLine(res.data)
      else RbHighbar.error(res.error_msg)
    })
  }

  removeLine(lineKey) {
    if (!this.state.inlineForms) return
    const forms = this.state.inlineForms.filter((x) => {
      if (x.key === lineKey && x.props.id) {
        const d = this._deletes || []
        d.push(x.props.id)
        this._deletes = d
      }
      return x.key !== lineKey
    })
    this.setState({ inlineForms: forms }, () => this._onLineUpdated(lineKey))
  }

  setLines(models = []) {
    models.forEach((item, idx) => {
      setTimeout(() => this._addLine(item), idx * 20)
    })
  }

  isEmpty() {
    return !this.state.inlineForms || this.state.inlineForms.length === 0
  }

  clear() {
    this.state.inlineForms &&
      this.state.inlineForms.forEach((c) => {
        if (c.props.id) {
          const d = this._deletes || []
          d.push(c.props.id)
          this._deletes = d
        }
      })
    this.setState({ inlineForms: [] }, () => this._onLineUpdated())
  }

  // 新增/删除行时触发
  _onLineUpdated(lineKey) {
    if (this.props.$$$main && this.props.$$$main._onProTableLineUpdated) {
      this.props.$$$main._onProTableLineUpdated(lineKey, this)
    }
  }

  /**
   * 清空某字段值
   * @param {string} field
   */
  setFieldNull(field) {
    this.state.inlineForms &&
      this.state.inlineForms.forEach((c) => {
        const fieldComp = c.ref.current.refs[`fieldcomp-${field}`]
        fieldComp && fieldComp.setValue(null)
      })
  }

  /**
   * 获取指定 InlineForm
   * @param {string} lineKey
   * @returns
   */
  getLineForm(lineKey) {
    if (!this.state.inlineForms) return null
    const f = this.state.inlineForms.find((c) => c.key === lineKey)
    return f ? f.ref.current || null : null
  }

  /**
   * 构建数据
   * @param {boolean} retAll 是否返回所有数据
   * @returns
   */
  buildFormData(retAll) {
    const datas = []
    let error = null

    this._inlineFormsRefs &&
      this._inlineFormsRefs.forEach((item) => {
        if (!item.current) return
        const d = item.current.buildFormData(retAll)

        if (!d || typeof d === 'string') {
          if (!error) error = d
        } else if (Object.keys(d).length > 0) {
          datas.push(d)
        }
      })

    if (error) {
      RbHighbar.create(error)
      return null
    }

    // 删除
    if (this._deletes) {
      this._deletes.forEach((item) => {
        const d = {
          metadata: {
            entity: this.props.entity.entity,
            id: item,
            delete: true,
          },
        }
        datas.push(d)
      })
    }

    return datas
  }

  // --

  /**
   * 导入明细
   * @param {*} transid
   * @param {*} form
   * @param {*} cb
   * @returns
   */
  static detailImports(transid, form, cb) {
    const formdata = form.getFormData()
    const mainid = form.props.id || null

    $.post(`/app/entity/extras/detail-imports?transid=${transid}&mainid=${mainid}`, JSON.stringify(formdata), (res) => {
      if (res.error_code === 0) {
        if ((res.data || []).length === 0) RbHighbar.create($L('没有可导入的明细记录'))
        else typeof cb === 'function' && cb(res.data)
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

class InlineForm extends RbForm {
  constructor(props) {
    super(props)
    this._InlineForm = true
  }

  render() {
    return (
      <RF>
        {this.props.children.map((fieldComp) => {
          if (fieldComp.props.field === TYPE_DIVIDER) return null
          const refid = `fieldcomp-${fieldComp.props.field}`
          return (
            <td key={`td-${refid}`} ref={(c) => (this._$ref = c)}>
              {React.cloneElement(fieldComp, { $$$parent: this, ref: refid })}
            </td>
          )
        })}
      </RF>
    )
  }

  buildFormData(retAll) {
    const $idx = $(this._$ref).parent().find('th.col-index').removeAttr('title')

    const data = {}
    if (retAll) {
      this.props.rawModel.elements.forEach((item) => {
        let val = item.value
        if (val) {
          val = typeof val === 'object' ? val.id || val : val
          data[item.field] = val || null
        }
      })
    }

    let error = null
    for (let k in this.__FormData) {
      const err = this.__FormData[k].error
      if (err) {
        error = err
        $idx.attr('title', err)
        break
      } else {
        data[k] = this.__FormData[k].value
      }
    }

    if (error) return error

    // 是否修改
    if (Object.keys(data).length > 0) {
      data.metadata = {
        entity: this.state.entity,
        id: this.state.id || null,
      }
    }
    return data
  }

  getFormData() {
    const data = {}
    this.props.rawModel.elements.forEach((item) => {
      let val = item.value
      if (val) {
        val = typeof val === 'object' ? val.id || val : val
        data[item.field] = val || null
      }
    })
    // updated
    for (let k in this.__FormData) {
      const err = this.__FormData[k].error
      if (err) data[k] = null
      else data[k] = this.__FormData[k].value
    }

    data.metadata = {
      entity: this.state.entity,
      id: this.state.id || null,
    }
    return data
  }
}

// Excel 粘贴数据
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
            <i className="mdi mdi-microsoft-excel" style={{ fontSize: 32 }} />
          </div>
          {tips}
        </div>
      )
    }

    return (
      <div className="rsheetb-table" ref={(c) => (this._$table = c)}>
        <div className="head-action">
          <span className="float-left">
            <h5 className="text-bold m-0 mt-3">{$L('选择列字段')}</h5>
          </span>
          <span className="float-right">
            <button className="btn btn-primary" onClick={() => this._handleConfirm()} ref={(c) => (this._$btn = c)}>
              {$L('确定')}
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
    function _init() {
      $(document).on('paste.csv-data', (e) => {
        let data
        try {
          // https://docs.sheetjs.com/docs/demos/local/clipboard/
          // https://docs.sheetjs.com/docs/api/utilities/html
          const c = e.originalEvent.clipboardData.getData('text/html')
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
          that.setState({ data: data, hasError: null }, () => that._tableAfter())
        } else {
          RbHighbar.createl('未识别到有效数据')
        }
      })
    }

    if (window.XLSX) _init()
    else $getScript('/assets/lib/charts/xlsx.full.min.js', setTimeout(_init, 200))
  }

  _tableAfter() {
    if (!this._$table) return

    const $table = $(this._$table).find('table').addClass('table table-sm')

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

    const dataWithFields = []
    $(this._$table)
      .find('tbody tr')
      .each(function () {
        const L = {}
        $(this)
          .find('td')
          .each(function (idx) {
            const name = fields[idx]
            if (name) L[name] = $(this).text() || null
          })
        dataWithFields.push(L)
      })

    const $btn = $(this._$btn).button('loading')
    $.post(`/app/entity/extras/csvdata-rebuild?entity=${this.props.entity}&mainid=${this.props.mainid}`, JSON.stringify(dataWithFields), (res) => {
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
      <RbModal title={$L('从 Excel 添加')} width="1000" className="modal-rsheetb" disposeOnHide ref={(c) => (this._dlg = c)}>
        <ExcelClipboardData
          {...this.props}
          onConfirm={(data) => {
            this.props.onConfirm && this.props.onConfirm(data)
            this.hide()
          }}
        />
      </RbModal>
    )
  }
}
