/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* global EntityNew2 */

$(document).ready(() => {
  const NO_TAG = '__'
  $.get('/admin/entity/entity-list?bizz=true', function (res) {
    $('#entityList').empty()

    const groups = { [NO_TAG]: [] }
    $(res.data).each(function () {
      const tags = this.tags || NO_TAG
      tags.split(',').forEach((tag) => {
        let g = groups[tag]
        if (!g) {
          g = []
          groups[tag] = g
        }
        g.push(this)
      })
    })

    $(groups[NO_TAG]).each(function () {
      if (this.builtin) render_entity(this)
    })
    $(groups[NO_TAG]).each(function () {
      if (!this.builtin) render_entity(this)
    })

    let _EntityNew2
    const $new = render_entity({
      icon: 'plus',
      entityLabel: $L('添加实体'),
    })
    $new
      .addClass('new-entity')
      .find('a.card')
      .attr('href', 'javascript:;')
      .on('click', function () {
        if (_EntityNew2) {
          _EntityNew2.show()
        } else {
          renderRbcomp18(
            <EntityNew2
              ref={function (o) {
                _EntityNew2 = o
              }}
            />
          )
        }
      })
      .find('.more-action')
      .remove()

    delete groups[NO_TAG]

    const keys = Object.keys(groups).sort()
    keys.forEach((tag) => {
      $(`<div class="tag"><h3><i class="icon mdi mdi-widgets-outline mr-2"></i>${tag}</h3></div>`).appendTo('#entityList')
      $(groups[tag]).each(function () {
        render_entity(this)
      })
    })
  })
})

const render_entity = function (item) {
  const $t = $($('#entity-tmpl').html()).appendTo('#entityList')
  $t.find('a.card').attr({
    href: `entity/${item.entityName}/base`,
    title: item.comments || null,
  })
  $t.find('.icon:eq(0)').addClass(`zmdi-${item.icon}`)
  $t.find('span').text(item.entityLabel)
  if (item.comments) $t.find('p').text(item.comments)
  else $t.find('p').html('&nbsp;')

  if (item.builtin) $(`<i class="badge badge-pill badge-secondary font-weight-light">${$L('内置')}</i>`).appendTo($t.find('a.card .badge-wrap'))
  if (item.hadApproval) $(`<i class="badge badge-pill badge-secondary font-weight-light">${$L('审批')}</i>`).appendTo($t.find('a.card .badge-wrap'))
  if (item.detailEntity) $(`<i class="badge badge-pill badge-secondary font-weight-light">${$L('明细')}</i>`).appendTo($t.find('a.card .badge-wrap'))
  return $t
}

$(document).bind('keydown', 'ctrl+f', (e) => {
  $stopEvent(e, true)
  renderRbcomp(<EntitySearcher className="modal-dialog-centered-unset" />)
})

// v4.0 搜索
class EntitySearcher extends RbAlert {
  renderContent() {
    return (
      <RF>
        <div>
          <input
            type="text"
            className="form-control fs-14"
            placeholder={$L('搜索实体、字段')}
            autoComplete="off"
            onChange={(e) => this.searchMeta(e.target.value)}
            ref={(c) => (this._$searchValue = c)}
          />
        </div>
        <div className="search-results">
          <ul className="list-unstyled m-0">
            {this.state.results &&
              this.state.results.map((item, idx) => {
                return (
                  <li key={idx}>
                    <a href={item.entity ? `./entity/${item.entity}/field/${item.name}` : `./entity/${item.name}/base`} target="_blank">
                      <span>{WrapHtml(item.label.replace(this.__q, `<b>${this.__q}</b>`))}</span>
                      <div className="float-right">{item.entity ? <span className="badge badge-light">{$L('字段')}</span> : <span className="badge badge-info">{$L('实体')}</span>}</div>
                    </a>
                  </li>
                )
              })}
          </ul>
        </div>
      </RF>
    )
  }

  componentDidMount() {
    super.componentDidMount()
    this._$searchValue.focus()
  }

  searchMeta(q) {
    if ($empty(q)) {
      this.setState({ results: [] })
      return
    }

    $setTimeout(
      () => {
        $.get(`/admin/entities/search?q=${$encode(q)}`, (res) => {
          this.__q = q
          this.setState({ results: res.data || [] })
        })
      },
      400,
      '__searchMeta'
    )
  }
}
