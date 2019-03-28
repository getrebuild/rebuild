/* eslint-disable no-undef */
$(document).ready(function () {
  let tmplBox = $('.level-boxes>.col-md-3')
  init_levelbox(tmplBox)
  init_levelbox($(tmplBox.clone(false)).appendTo('.level-boxes'), 2)
  init_levelbox($(tmplBox.clone(false)).appendTo('.level-boxes'), 3)
  init_levelbox($(tmplBox.clone(false)).appendTo('.level-boxes'), 4)


})

const LNAME = { 1: '一级分类', 2: '二级分类', 3: '三级分类', 4: '四级分类' }
let init_levelbox = function (box, level) {
  box.attr('data-level', level || 1)
  if (level > 1) {
    box.addClass('off')
    box.find('h5').text(LNAME[level])
    let newFor = 'trunOn' + level
    box.find('.turn-on label').attr('for', newFor)
    box.find('.turn-on input').attr('id', newFor).change(function () {
      let chk = $(this).prop('checked') === true
      for (let i = 2; i <= level; i++) {
        let $box = $('.level-boxes>div[data-level=' + i + ']')
        if (chk) $box.removeClass('off')
        else $box.addClass('off')
      }
    })
  }

  box.find('.dd-list').sortable({
    placeholder: 'dd-placeholder',
    handle: '.dd-handle',
    axis: 'y',
  }).disableSelection()

  box.find('.J_confirm').click(() => {
    let name = box.find('.J_name').val()
    if (name) {
      render_unset([$random(), name], box.find('.dd-list'))
      box.find('.J_name').val('')
    }
    return false
  })
}

// Over sortable.js
render_unset_after = function (item, data) {
  item.off('click')
  $('<div class="dd3-action"><a><i class="zmdi zmdi-edit"></i></a><a><i class="zmdi zmdi-close"></i></a></div>').appendTo(item)
}