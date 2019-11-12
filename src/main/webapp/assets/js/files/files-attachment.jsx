// 附件

let filesList

$(document).ready(() => {
  let clickNav = function (item) {
    filesList && filesList.loadFiles(item.id)
    $('.file-path .active').text(item.text)
  }

  // eslint-disable-next-line react/jsx-no-undef
  renderRbcomp(<NavTree call={clickNav} dataUrl={`${rb.baseUrl}/files/list-entity`} />, 'navTree')
  // eslint-disable-next-line react/jsx-no-undef
  renderRbcomp(<FilesList />, $('.file-viewport'), function () { filesList = this })
})