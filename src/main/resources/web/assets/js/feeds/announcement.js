/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~ 公告展示
class AnnouncementModal extends React.Component {
  state = { ...this.props }

  render() {
    const contentHtml = $converEmoji(this.props.content.replace(/\n/g, '<br/>'))
    return (
      <div className="modal" tabIndex={this.state.tabIndex || -1} ref={(c) => (this._dlg = c)}>
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
                <span className="float-left text-muted">{$L('由 %s 发布于 %s', this.props.publishBy, this.props.publishOn)}</span>
                <span className="float-right">
                  <a href={`${rb.baseUrl}/app/redirect?id=${this.props.id}`}>{$L('前往动态查看')}</a>
                </span>
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
