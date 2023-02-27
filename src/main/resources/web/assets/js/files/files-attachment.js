/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global filesList */

// 附件
const __DEFAULT_ALL = 1

const EntityTree = {
  load: function () {
    $.get('/files/tree-entity', (res) => {
      const data = [{ id: __DEFAULT_ALL, text: $L('全部') }, ...res.data]

      renderRbcomp(
        <AsideTree
          data={data}
          hideCollapse
          activeItem={__DEFAULT_ALL}
          onItemClick={(item) => {
            filesList && filesList.loadData(item.id)
            $('.file-path .active').text(item.text)
            location.hash = `!/Entity/${item.id}`
          }}
        />,
        'navTree'
      )
    })
  },
}

// eslint-disable-next-line no-undef
class FilesList4Atts extends FilesList {
  state = { ...this.props }

  renderExtras(item) {
    return item.relatedRecord ? (
      <span>
        <a title={$L('查看记录')} onClick={(e) => $stopEvent(e)} href={`${rb.baseUrl}/app/redirect?id=${item.relatedRecord[0]}`}>
          {item.relatedRecord[1]}
        </a>
      </span>
    ) : null
  }
}

$(document).ready(() => {
  EntityTree.load()

  renderRbcomp(<FilesList4Atts />, $('.file-viewport'), function () {
    // eslint-disable-next-line no-global-assign
    filesList = this
  })
})
