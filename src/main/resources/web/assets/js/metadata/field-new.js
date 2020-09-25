/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(function () {
  const entity = $urlp('entity')

  const $btn = $('.btn-primary').click(function () {
    const fieldLabel = $val('#fieldLabel'),
      type = $val('#type'),
      comments = $val('#comments'),
      refEntity = $val('#refEntity'),
      refClassification = $val('#refClassification'),
      stateClass = $val('#stateClass') || 'com.rebuild.server.helper.state.HowtoState'
    if (!fieldLabel) {
      RbHighbar.create($lang('PlsInputSome,FieldName'))
      return
    }

    if (type === 'REFERENCE' && !refEntity) {
      RbHighbar.create('PlsSelectSome,RefEntity')
      return
    } else if (type === 'CLASSIFICATION' && !refClassification) {
      RbHighbar.create('PlsSelectSome,Classification')
      return
    } else if (type === 'STATE' && !stateClass) {
      RbHighbar.create('PlsInputSome,StateClass')
      return
    }

    const data = {
      entity: entity,
      label: fieldLabel,
      type: type,
      comments: comments,
      refEntity: refEntity,
      refClassification: refClassification,
      stateClass: stateClass,
    }

    $btn.button('loading')
    $.post('/admin/entity/field-new', JSON.stringify(data), function (res) {
      $btn.button('reset')
      if (res.error_code === 0) {
        if ($val('#saveAndNew')) {
          RbHighbar.success($lang('SomeAdded,Field'))
          $('#fieldLabel, #comments').val('')
          $('#type').val('TEXT').trigger('change')
          $('#fieldLabel').focus()
          // @see `field-new.html`
          parent && parent.loadFields && parent.loadFields()
          // @see `form-design.js`
          parent && parent.add2Layout && parent.add2Layout($val('#add2Layout'), res.data)
        } else {
          parent.location.href = `${rb.baseUrl}/admin/entity/${entity}/field/${res.data}`
        }
      } else RbHighbar.error(res.error_msg)
    })
  })

  let referenceLoaded = false
  let classificationLoaded = false
  $('#type').change(function () {
    parent.RbModal.resize()

    $('.J_dt-REFERENCE, .J_dt-CLASSIFICATION, .J_dt-STATE').addClass('hide')
    const dt = $(this).val()
    $('.J_dt-' + dt).removeClass('hide')

    if (dt === 'REFERENCE') {
      if (referenceLoaded === false) {
        referenceLoaded = true
        $.get('/admin/entity/entity-list?detail=true', (res) => {
          $(res.data).each(function () {
            $(`<option value="${this.entityName}">${this.entityLabel}${this.mainEntity ? ' (' + $lang('DetailEntity') + ')' : ''}</option>`).appendTo('#refEntity')
          })
          if (res.data.length === 0) $(`<option value="">${$lang('NoAnySome,Entity')}</option>`).appendTo('#refEntity')
        })
      }
    } else if (dt === 'CLASSIFICATION') {
      if (classificationLoaded === false) {
        classificationLoaded = true
        $.get('/admin/metadata/classification/list', (res) => {
          let hasData = false
          $(res.data).each(function () {
            if (!this[2]) {
              $(`<option value="${this[0]}">${this[1]}</option>`).appendTo('#refClassification')
              hasData = true
            }
          })
          if (!hasData) $(`<option value="">${$lang('NoAnySome,Classification')}</option>`).appendTo('#refClassification')
        })
      }
    } else if (dt === 'STATE') {
      // NOOP
    }
  })

  if ($urlp('ref') === 'form-design') {
    $('#add2Layout').parent().removeClass('hide')
    $('#saveAndNew').attr('checked', true).parent().addClass('hide')
  }
})
