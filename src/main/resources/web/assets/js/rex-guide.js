/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-undef */
/* global RbHeroModal */

// ~~ AI 助手引导弹窗（未配置引导管理员配置、已配置引导用户体验）

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

  const mark = function () {
    if (configured) $storage.set('RexGuideEnd-' + rb.currentUser, 'yes')
    else if (isAdmin) $storage.set('RexGuideCfgCount-' + rb.currentUser, ~~$storage.get('RexGuideCfgCount-' + rb.currentUser) + 1)
    else $storage.set('RexGuideUserEnd-' + rb.currentUser, 'yes')
  }

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
    onClose: mark, // 任何途径关闭（含 Esc）均触发
  }

  const feats = (
    <div>
      <div className="rex-guide-feat">
        <i className="mdi mdi-shimmer rex-guide-qicon" />
        解答系统使用和业务知识问题
      </div>
      <div className="rex-guide-feat">
        <i className="mdi mdi-comment-text-outline rex-guide-qicon" />
        用一句话创建业务记录
      </div>
      <div className="rex-guide-feat">
        <i className="mdi mdi-database-search-outline rex-guide-qicon" />
        快速查找并汇总你需要的数据
      </div>
    </div>
  )

  if (configured) {
    const questions = ['系统里都有哪些业务实体？', '帮我创建一条客户记录', '如何配置一个自动化触发器？']
    RbHeroModal.show({
      ...heroProps,
      icon: <i className="mdi mdi-shimmer" />,
      title: `认识一下 ${name}`,
      subtitle: 'AI 模型已配置完成，随时为你答疑解惑、协助处理数据',
      footer: (
        <React.Fragment>
          <span className="rex-guide-hint">
            快捷键 <kbd>Shift + /</kbd> 随时唤起
          </span>
          <button type="button" className="rex-guide-btn" onClick={() => openAiBot()}>
            {`打开 ${name}`}
          </button>
        </React.Fragment>
      ),
      children: (
        <div>
          <div className="rex-guide-qtitle">试试这些</div>
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
      title: `配置 ${name}`,
      subtitle: '只需三步接入 AI 模型，约一分钟即可完成',
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
              mark() // 跳转可能中断关闭动画，先落去重标记
              hide()
              location.href = rb.baseUrl + '/admin/integration/aibot'
            }}>
            去配置
          </button>
        </React.Fragment>
      ),
      children: (
        <div>
          <div className="rex-guide-qtitle">配置完成后，你和团队可以</div>
          {feats}
          <div className="rex-guide-qtitle rex-guide-gap">配置步骤</div>
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
      subtitle: '系统内置的智能助手，可以帮助你完成日常工作',
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
          <div className="rex-guide-note">系统尚未接入 AI 模型，请转告管理员在管理中心完成配置</div>
        </div>
      ),
    })
  }
}

// 调试用：强制指定显示场景，0=正常逻辑，1=未配置·管理员，2=未配置·普通用户，3=已配置·体验
// 控制台改值后执行 __checkRexGuide() 即可重现，强制模式跳过去重和 AI 入口检查
window.__RexGuideScene = window.__RexGuideScene || 0

window.__checkRexGuide = function () {
  if (!rb.currentUser) return

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
      // 无 AI 入口（如商业版未授权）的用户不引导
      if (!$('.aibot-show')[0]) return
      if ($storage.get('RexGuideEnd-' + rb.currentUser)) return
    } else if (isAdmin) {
      // 管理员最多提醒 3 次，无视说明暂不打算配置
      if (~~$storage.get('RexGuideCfgCount-' + rb.currentUser) >= 3) return
    } else {
      // 普通用户无法行动，告知一次即可
      if ($storage.get('RexGuideUserEnd-' + rb.currentUser)) return
    }

    const show = function () {
      setTimeout(() => __showRexGuide(configured, isAdmin), 1000)
    }
    // 等待新手向导结束
    if (window.tourStarted) $(document).one('rbTourComplete', show)
    else show()
  })
}

// 自触发。本文件为 text/babel 脚本，晚于 DOM ready 执行，
// 延时以覆盖 startTour 的启动延迟（最大 1000ms，见 dashboard.js 等调用点）
setTimeout(function () {
  window.__checkRexGuide()
}, 1500)
