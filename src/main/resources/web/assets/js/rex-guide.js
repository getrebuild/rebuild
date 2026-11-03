/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef */
/* global RbHeroModal */

// ~~ AI 助手引导弹窗（未配置引导管理员配置、已配置引导用户体验）
/* global RbHeroModal */
// ~~ AI 助手引导弹窗

const __injectRexGuideStyle = function () {
  if (document.getElementById('rex-guide-style')) return
  const css = `
.rex-guide-hint { color: #808080; flex: 1; line-height: 1.5; }
.rex-guide-hint kbd { background: #f2f4f8; border: 1px solid #dde2ea; border-bottom-width: 2px; border-radius: 4px; padding: 1px 6px; font-family: inherit; color: #000; }
.rex-guide-btn { border-radius: 20px; padding: 8px 22px; font: inherit; cursor: pointer; border: none; margin-left: 10px; background: linear-gradient(120deg, #4b6bff, #8b4bff); color: #fff; }
.rex-guide-btn:hover { filter: brightness(1.08); }
.rex-guide-btn.plain { background: #fff; border: 1px solid #d5dbe5; color: #404040; }
.rex-guide-btn.plain:hover { filter: none; background: #f7f8fa; }
.rex-guide-steps { display: flex; gap: 8px; }
.rex-guide-step { flex: 1; background: #fafaff; border: 1px solid #ececf5; border-radius: 8px; padding: 12px 10px; line-height: 1.6; text-align: center; }
.rex-guide-num { display: inline-flex; width: 20px; height: 20px; border-radius: 50%; background: linear-gradient(120deg, #4b6bff, #8b4bff); color: #fff; align-items: center; justify-content: center; font-size: 11px; margin-bottom: 7px; }
.rex-guide-st { display: block; font-weight: 600; margin-bottom: 2px; }
.rex-guide-sd { color: #808080; }
.rex-guide-qtitle { color: #808080; margin-bottom: 10px; padding: 0 2px; }
.rex-guide-gap { margin-top: 14px; }
.rex-guide-feat { display: flex; align-items: flex-start; gap: 8px; color: #404040; padding: 5px 2px; }
.rex-guide-feat .rex-guide-qicon { margin-top: 1px; }
.rex-guide-note { margin-top: 12px; background: #fff8e6; border: 1px solid #f5e3b3; color: #8a6d1a; border-radius: 8px; padding: 10px 12px; line-height: 1.6; }
.rex-guide-q { display: flex; align-items: center; gap: 10px; border: 1px solid #ececf5; background: #fafaff; border-radius: 8px; padding: 10px 14px; margin-bottom: 8px; cursor: pointer; transition: all 0.15s; color: #404040; }
.rex-guide-q:last-child { margin-bottom: 0; }
.rex-guide-q:hover { border-color: #7b4bff; background: #f6f3ff; }
.rex-guide-qicon { color: #7b4bff; }
.rex-guide-go { margin-left: auto; color: #ccc; }
.rex-guide-q:hover .rex-guide-go { color: #7b4bff; }
`
  $('<style id="rex-guide-style"></style>').text(css).appendTo(document.head)
}

const __showRexGuide = function (configured, isAdmin) {
  __injectRexGuideStyle()

  let name = rb._aibotName || 'Rex AI 助手'

  let marked = false
  const mark = function () {
    if (marked) return
    marked = true

    if (configured) $storage.set('RexGuideEnd-' + rb.currentUser, 'yes')
    else if (isAdmin) {
      $storage.set('RexGuideCfgCount-' + rb.currentUser, ~~$storage.get('RexGuideCfgCount-' + rb.currentUser) + 1)
      sessionStorage.setItem('RexGuideCfgSess-' + rb.currentUser, 'yes')
    } else $storage.set('RexGuideUserEnd-' + rb.currentUser, 'yes')
  }

  // 展示即计数，避免未关闭就离开页面导致重复弹；强制调试模式不计数
  if (!~~window.__RexGuideScene) mark()

  let modal
  const hide = function () {
    modal && modal.hide()
  }

  const openAiBot = function (question) {
    hide()
    const props = { draggable: true }
    if (question) props.preset = { content: question, autoSend: true }
    window.AiBot && window.AiBot.init(props, false)
  }

  const heroProps = {
    ref: (c) => (modal = c),
  }

  const feats = (
    <div>
      <div className="rex-guide-feat">
        <i className="mdi mdi-shimmer rex-guide-qicon" />
        系统和业务问题，问了就有答案
      </div>
      <div className="rex-guide-feat">
        <i className="mdi mdi-comment-text-outline rex-guide-qicon" />
        一句话建好业务记录，不用逐个填表单
      </div>
      <div className="rex-guide-feat">
        <i className="mdi mdi-database-search-outline rex-guide-qicon" />
        要找要汇总的数据，说一句就行
      </div>
    </div>
  )

  if (configured) {
    const questions = isAdmin ? ['系统里都有哪些业务实体', '如何配置一个自动化触发器', '帮我创建一条客户记录'] : ['帮我创建一条客户记录', '怎么快速找到我需要的数据', '有哪些待我审批的记录']
    RbHeroModal.show({
      ...heroProps,
      icon: <i className="mdi mdi-shimmer" />,
      title: `${name} 已就绪`,
      subtitle: '问一句试试，它比你更熟悉这套系统',
      footer: (
        <React.Fragment>
          <span className="rex-guide-hint">
            以后按 <kbd>Shift + /</kbd> 随时叫它
          </span>
          <button type="button" className="rex-guide-btn" onClick={() => openAiBot()}>
            {`打开 ${name}`}
          </button>
        </React.Fragment>
      ),
      children: (
        <div>
          <div className="rex-guide-qtitle">先从这几个问题开始</div>
          {questions.map((q) => (
            <div key={q} className="rex-guide-q" onClick={() => openAiBot(q)}>
              <i className="mdi mdi-shimmer rex-guide-qicon" />
              <span>{q}</span>
              <i className="zmdi zmdi-chevron-right rex-guide-go" />
            </div>
          ))}
        </div>
      ),
    })
  } else if (isAdmin) {
    RbHeroModal.show({
      ...heroProps,
      icon: <i className="mdi mdi-shimmer" />,
      title: '只差一步，接入 AI 模型',
      subtitle: `约一分钟完成配置，${name} 就能开始为你和团队干活`,
      footer: (
        <React.Fragment>
          <span className="rex-guide-hint">配置入口 管理中心 - Rex AI 助手</span>
          <button type="button" className="rex-guide-btn plain" onClick={hide}>
            稍后再说
          </button>
          <button
            type="button"
            className="rex-guide-btn"
            onClick={() => {
              hide()
              location.href = rb.baseUrl + '/admin/integration/aibot'
            }}>
            立即接入
          </button>
        </React.Fragment>
      ),
      children: (
        <div>
          <div className="rex-guide-qtitle">接好模型，它能做这些</div>
          {feats}
          <div className="rex-guide-qtitle rex-guide-gap">三步接入</div>
          <div className="rex-guide-steps">
            <div className="rex-guide-step">
              <span className="rex-guide-num">1</span>
              <span className="rex-guide-st">填写 API 地址</span>
              <span className="rex-guide-sd">模型服务的接口地址</span>
            </div>
            <div className="rex-guide-step">
              <span className="rex-guide-num">2</span>
              <span className="rex-guide-st">填写 API Key</span>
              <span className="rex-guide-sd">用于调用模型服务</span>
            </div>
            <div className="rex-guide-step">
              <span className="rex-guide-num">3</span>
              <span className="rex-guide-st">选择默认模型</span>
              <span className="rex-guide-sd">可从模型列表中选择</span>
            </div>
          </div>
        </div>
      ),
    })
  } else {
    RbHeroModal.show({
      ...heroProps,
      icon: <i className="mdi mdi-shimmer" />,
      title: `认识一下 ${name}`,
      subtitle: '能答疑、能帮你建记录找数据的智能助手',
      footer: (
        <React.Fragment>
          <span className="rex-guide-hint">
            配置完成后，快捷键 <kbd>Shift+/</kbd> 可随时唤起
          </span>
          <button type="button" className="rex-guide-btn" onClick={hide}>
            知道了
          </button>
        </React.Fragment>
      ),
      children: (
        <div>
          <div className="rex-guide-qtitle">它可以帮你</div>
          {feats}
          <div className="rex-guide-note">它还差 AI 模型没接入，提醒管理员到 管理中心 - Rex AI 助手 配置一下就能用</div>
        </div>
      ),
    })
  }
}

window.__RexGuideScene = window.__RexGuideScene || 0
window.__checkRexGuide = function () {
  if (window.top !== window.self) return
  if (!rb.currentUser) return
  if (!~~window.__RexGuideScene && $storage.get('RexGuideEnd-' + rb.currentUser)) return

  $.get('/aibot/check-configured', function (res) {
    let configured = !!res.data
    let isAdmin = !!rb.isAdminUser

    const scene = ~~window.__RexGuideScene
    if (scene === 1) {
      configured = false
      isAdmin = true
    } else if (scene === 2) {
      configured = false
      isAdmin = false
    } else if (scene === 3) {
      configured = true
    } else if (configured) {
      if (!$('.aibot-show')[0]) return
      if ($storage.get('RexGuideEnd-' + rb.currentUser)) return
    } else if (isAdmin) {
      if (~~$storage.get('RexGuideCfgCount-' + rb.currentUser) >= 3) return
      if (sessionStorage.getItem('RexGuideCfgSess-' + rb.currentUser)) return
    } else {
      if ($storage.get('RexGuideUserEnd-' + rb.currentUser)) return
    }

    if (window.tourStarted) return
    __showRexGuide(configured, isAdmin)
  })
}

setTimeout(function () {
  window.__checkRexGuide()
}, 3000)
