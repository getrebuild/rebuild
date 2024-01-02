/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~ 公告展示
class AnnouncementModal extends React.Component {
  state = { ...this.props }

  render() {
    const props = this.props
    const state = this.state
    const contentHtml = $converEmoji(props.content.replace(/\n/g, '<br/>'))

    return (
      <div className="modal" tabIndex={state.tabIndex || -1} ref={(c) => (this._dlg = c)}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={this.hide}>
                <i className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="text-break announcement-contents" dangerouslySetInnerHTML={{ __html: contentHtml }} />
              <div>
                <span className="float-left text-muted">{$L('由 %s 发布于 %s', props.publishBy, props.publishOn)}</span>
                <span className="float-right">
                  <a href={`${rb.baseUrl}/app/redirect?id=${props.id}`}>{$L('前往动态查看')}</a>
                </span>
                {state.readState && (
                  <span className="float-right mr-2">
                    {state.readState === 1 && (
                      <a className="text-primary" onClick={() => this._makeRead()}>
                        <i className="icon zmdi zmdi-check text-bold" /> {$L('标记已读')}
                      </a>
                    )}
                    {state.readState !== 1 && (
                      <a className="text-muted">
                        <i className="icon zmdi zmdi-check text-bold" /> {$L('已读')}
                      </a>
                    )}
                    <span className="text-muted text-bold ml-2">·</span>
                  </span>
                )}
                <span className="clearfix" />
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const root = $(this._dlg)
      .modal({ show: true, keyboard: true })
      .on('hidden.bs.modal', () => {
        root.modal('dispose')
        $unmount(root.parent())
      })
  }

  hide = () => $(this._dlg).modal('hide')

  _makeRead() {
    if (!rb.currentUser) {
      location.href = `${rb.baseUrl}/app/redirect?id=${this.props.id}`
      return
    }

    $.post(`/commons/announcements/make-read?id=${this.props.id}`, (res) => {
      if (res.error_code === 0) {
        this.setState({ readState: 2 })
      } else {
        RbHighbar.error(res.error_msg)
      }
    })
  }
}

const $showAnnouncement = function () {
  const $aw = $('.announcement-wrapper')
  if ($aw.length === 0 || $aw.find('div').length > 0) return

  $.get('/commons/announcements', (res) => {
    if (res.error_code !== 0 || !res.data || res.data.length === 0) return

    const as = res.data.map((item, idx) => {
      return (
        <div className="bg-warning" key={idx} title={$L('查看详情')} onClick={() => renderRbcomp(<AnnouncementModal {...item} />)}>
          <i className="icon zmdi zmdi-notifications-active" />
          <p dangerouslySetInnerHTML={{ __html: item.content }} />
        </div>
      )
    })
    renderRbcomp(<React.Fragment>{as}</React.Fragment>, $aw, function () {
      $(this)
        .find('p>a[href]')
        .click((e) => e.stopPropagation())
    })
  })
}

$(document).ready(() => $showAnnouncement())
