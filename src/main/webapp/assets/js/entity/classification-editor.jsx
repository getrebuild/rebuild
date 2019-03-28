$(document).ready(function () {
  let boxTmpl = $('.level-box>.col-md-3')
  init_levelbox(boxTmpl)
})

var init_levelbox = function (box) {
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
}