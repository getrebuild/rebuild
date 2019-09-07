$(document).ready(function () {
  const entity = $urlp('entity')
  let btn = $('.btn-primary').click(function () {
    let fieldLabel = $val('#fieldLabel'),
      type = $val('#type'),
      comments = $val('#comments'),
      refEntity = $val('#refEntity'),
      refClassification = $val('#refClassification'),
      stateClass = $val('#stateClass') || 'com.rebuild.server.helper.state.HowtoState'
    if (!fieldLabel) {
      RbHighbar.create('请输入字段名称')
      return
    }
    if (type === 'REFERENCE' && !refEntity) {
      RbHighbar.create('请选择引用实体')
      return
    } else if (type === 'CLASSIFICATION' && !refClassification) {
      RbHighbar.create('请选择分类数据')
      return
    } else if (type === 'STATE' && !stateClass) {
      RbHighbar.create('请填写状态类')
      return
    }

    let _data = {
      entity: entity,
      label: fieldLabel,
      type: type,
      comments: comments,
      refEntity: refEntity,
      refClassification: refClassification,
      stateClass: stateClass
    }
    btn.button('loading')
    $.post(rb.baseUrl + '/admin/entity/field-new', JSON.stringify(_data), function (res) {
      btn.button('reset')
      if (res.error_code === 0) parent.location.href = rb.baseUrl + '/admin/entity/' + entity + '/field/' + res.data
      else RbHighbar.error(res.error_msg)
    })
  })

  let referenceLoaded = false
  let classificationLoaded = false
  $('#type').change(function () {
    parent.RbModal.resize()
    let dt = $(this).val()
    $('.J_dt-' + dt).removeClass('hide')

    if (dt === 'REFERENCE') {
      if (referenceLoaded === false) {
        referenceLoaded = true
        $.get(rb.baseUrl + '/admin/entity/entity-list', (res) => {
          $(res.data).each(function () {
            $('<option value="' + this.entityName + '">' + this.entityLabel + '</option>').appendTo('#refEntity')
          })
          if (res.data.length === 0) $('<option value="">无可用实体</option>').appendTo('#refEntity')
        })
      }
    } else if (dt === 'CLASSIFICATION') {
      if (classificationLoaded === false) {
        classificationLoaded = true
        $.get(rb.baseUrl + '/admin/entityhub/classification/list', (res) => {
          let hasData = false
          $(res.data).each(function () {
            if (!this[2]) {
              $('<option value="' + this[0] + '">' + this[1] + '</option>').appendTo('#refClassification')
              hasData = true
            }
          })
          if (!hasData) $('<option value="">无可用分类数据</option>').appendTo('#refClassification')
        })
      }
    } else if (dt === 'STATE') {
      // Nothing
    } else {
      $('.J_dt-REFERENCE, .J_dt-CLASSIFICATION, .J_dt-STATE').addClass('hide')
    }
  })
})