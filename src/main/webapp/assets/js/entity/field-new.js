$(document).ready(function () {
  const entity = $urlp('entity')
  let btn = $('.btn-primary').click(function () {
    let fieldLabel = $val('#fieldLabel'),
      type = $val('#type'),
      comments = $val('#comments'),
      refEntity = $val('#refEntity'),
      refClassification = $val('#refClassification')
    if (!fieldLabel) {
      RbHighbar.create('请输入字段名称')
      return
    }
    if (type === 'REFERENCE' && !refEntity) {
      RbHighbar.create('请选择引用实体')
      return
    }
    if (type === 'CLASSIFICATION' && !refClassification) {
      RbHighbar.create('请选择分类数据')
      return
    }

    let _data = {
      entity: entity,
      label: fieldLabel,
      type: type,
      comments: comments,
      refEntity: refEntity,
      refClassification: refClassification
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
        $.get(rb.baseUrl + '/admin/classification/list', (res) => {
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
    } else {
      $('.J_dt-REFERENCE, .J_dt-CLASSIFICATION').addClass('hide')
    }
  })
})