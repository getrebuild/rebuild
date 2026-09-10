/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global DEFAULT_MDE_TOOLBAR, EasyMDE */

let _lastContent = null
let _EasyMDE

// 图片加载/mermaid 渲染会改变预览高度
function _resyncPreviewScroll() {
  const cm = _EasyMDE.codemirror
  const preview = cm.getWrapperElement().nextSibling
  const info = cm.getScrollInfo()
  const edRange = info.height - info.clientHeight
  const pvRange = preview.scrollHeight - preview.clientHeight
  if (edRange <= 0 || pvRange <= 0) return
  preview.scrollTop = pvRange * (info.top / edRange)
}

$(document).ready(() => {
  $createUploader(document.getElementById('_fieldValue__upload'), null, (res) => {
    let text = `![](${rb.baseUrl}/filex/img/${res.key})`
    const pos = _EasyMDE.codemirror.getCursor()
    _EasyMDE.codemirror.setSelection(pos, pos)
    _EasyMDE.codemirror.replaceSelection(text)
  })

  let toolbar = DEFAULT_MDE_TOOLBAR({
    _fieldValue__upload: document.getElementById('_fieldValue__upload'),
  })
  toolbar = toolbar.filter((i) => {
    return !(i.name === 'preview' || i.name === 'fullscreen')
  })
  toolbar.push({
    name: 'side-by-side',
    action: EasyMDE.toggleSideBySide,
    className: 'mdi mdi-view-agenda-outline mdi-rotate-90',
    title: $L('分屏预览'),
  })
  toolbar.push({
    name: 'save',
    action: (e) => {
      const data = {
        id: $('#viewer').data('fileid'),
        content: _EasyMDE.value(),
      }

      const $save = $('.editor-toolbar button.save').attr('disabled', true)
      if (_lastContent === data.content) {
        setTimeout(() => $save.attr('disabled', false), 500)
        return
      }

      $.post('/commons/md-editor-save', JSON.stringify(data), (res) => {
        setTimeout(() => $save.attr('disabled', false), 500)
        if (res.error_code === 0) {
          _lastContent = data.content
          // RbHighbar.success($L('保存成功'))
        } else {
          RbHighbar.error(res.error_msg)
        }
      })
    },
    className: 'mdi mdi-content-save',
    title: $L('保存'),
  })

  _EasyMDE = new EasyMDE({
    element: document.getElementById('editor'),
    status: false,
    spellChecker: false,
    toolbar: toolbar,
    previewClass: 'markdown-body',
    autoDownloadFontAwesome: false,
    previewRender: (plainText, preview) => {
      setTimeout(() => $renderMermaid($(preview)), 200)
      return marked.parse(plainText)
    },
  })
  _EasyMDE.toggleFullScreen()
  _EasyMDE.toggleSideBySide()
  _lastContent = _EasyMDE.value()

  const _preview = _EasyMDE.codemirror.getWrapperElement().nextSibling
  _preview.addEventListener('load', _resyncPreviewScroll, true)
  new MutationObserver(_resyncPreviewScroll).observe(_preview, { childList: true, subtree: true })

  // 阻止F11
  document.addEventListener(
    'keydown',
    function (e) {
      if (e.key === 'F11' || e.keyCode === 122) {
        e.preventDefault()
        e.stopPropagation()
        return false
      }
    },
    true,
  )
})

window.onbeforeunload = function () {
  if (_EasyMDE.value() === _lastContent) return undefined
  return 'SHOW-UNSAVED-CHANGES'
}
