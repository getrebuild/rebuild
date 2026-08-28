/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global RichContent, $enableScrollTop */

const SharedAibotChat = function ({ messages }) {
  return messages.map((m, idx) => {
    if (m.role === 'user') {
      return (
        <div className="chat-message" key={idx}>
          <div className="msg-user">
            <div className="msg-content">{m.content || ''}</div>
            {m.attach && m.attach.length > 0 && (
              <div className="msg-attach">
                {m.attach.map((name, i) => (
                  <a key={i}>{name}</a>
                ))}
              </div>
            )}
          </div>
        </div>
      )
    }

    return (
      <div className="chat-message" key={idx}>
        <div className="msg-ai">
          <div className="avatar">
            <img src={`${rb.baseUrl}/assets/img/icon-256x256.png`} alt="AI" />
          </div>
          <div className="msg-content">
            <RichContent content={m.content || ''} />
          </div>
        </div>
      </div>
    )
  })
}

$(document).ready(() => {
  const messages = (window.__PageConfig && window.__PageConfig.messages) || []
  renderRbcomp(<SharedAibotChat messages={messages} />, $('.J_content')[0], function () {
    $('.rb-loading-active').remove()
    $('.J_content, page-footer').removeClass('hide')
    $enableScrollTop()
  })
})
