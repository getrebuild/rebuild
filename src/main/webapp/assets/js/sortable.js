$(document).ready(function(){
  $('.J_config').sortable({
    placeholder: 'dd-placeholder',
    handle: '.dd3-handle',
    axis: 'y',
  }).disableSelection()
})

const render_unset = function(data) {
  let item = $('<li class="dd-item" data-key="' + data[0] + '"><div class="dd-handle">' + data[1] + '</div></li>').appendTo('.unset-list')
  item.click(function() {
    render_item(data)
    item.remove()
  })
  render_unset_after(item, data)
  return item
}
let render_unset_after = function(item, data) {
  // 更多实现
}

const render_item = function(data) {
  let item = $('<li class="dd-item dd3-item" data-key="' + data[0] + '"><div class="dd-handle dd3-handle"></div><div class="dd3-content">' + data[1] + '</div></li>').appendTo('.J_config')
  let del = $('<div class="dd3-action"><a href="javascript:;">[移除]</a></div>').appendTo(item)
  del.find('a').click(function() {
    data[1] = item.find('.dd3-content').text()
    render_unset(data)
    item.remove()
  })
  render_item_after(item, data)
  return item
}
let render_item_after = function(item, data) {
  // 更多实现
}