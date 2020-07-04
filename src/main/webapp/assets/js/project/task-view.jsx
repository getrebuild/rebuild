/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// 任务视图
// eslint-disable-next-line no-unused-vars
class TaskViewer extends React.Component {
  state = { ...this.props }

  render() {
    return (
      <div className="modal rbview task-view" ref={(c) => this._dlg = c}>
        <div className="modal-dialog">
          <div className="modal-content">
            <div className="modal-body rb-loading">
              {this.props.id}
            </div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    const $dlg = $(this._dlg)
    $dlg.on('shown.bs.modal', () => {
      $dlg.find('.modal-content').css('margin-right', 0)
      location.hash = '#!/View/ProjectTask/' + this.props.id
    }).on('hidden.bs.modal', () => {
      $dlg.find('.modal-content').css('margin-right', -1000)
      location.hash = '#!/View/'
    })

    $dlg.modal({
      show: true
    })
  }
}
