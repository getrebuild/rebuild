/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

$(document).ready(() => {
  renderRbcomp(
    <AiBot
      chatid={$urlp('chatid', location.hash)}
      onChatidChanged={(id) => {
        location.hash = 'chatid=' + (id || '')
      }}
    />,
    'chat-wrapper',
    () => {
      $addResizeHandler(() => {
        $('#chat-wrapper .modal-content').height($(window).height() - 40)
      })()
    }
  )
})
