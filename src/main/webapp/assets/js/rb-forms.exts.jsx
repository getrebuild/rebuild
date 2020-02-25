/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~扩展字段类型

class RbFormAvatar extends RbFormElement {
  constructor(props) {
    super(props)
  }

  renderElement() {
    let aUrl = rb.baseUrl + (this.state.value ? `/filex/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return <div className="img-field avatar">
      <span title={this.props.readonly ? null : '选择头像'}>
        {!this.props.readonly &&
          <input ref={(c) => this._fieldValue__input = c} type="file" className="inputfile" id={`${this.props.field}-input`} accept="image/png,image/jpeg,image/gif" />
        }
        <label htmlFor={`${this.props.field}-input`} className="img-thumbnail img-upload"><img src={aUrl} alt="头像" /></label>
      </span>
    </div>
  }

  renderViewElement() {
    let aUrl = rb.baseUrl + (this.state.value ? `/filex/img/${this.state.value}?imageView2/2/w/100/interlace/1/q/100` : '/assets/img/avatar.png')
    return <div className="img-field avatar"><a className="img-thumbnail img-upload"><img src={aUrl} /></a></div>
  }

  onEditModeChanged(destroy) {
    if (destroy) {
      // NOOP
    } else {
      let mp
      $createUploader(this._fieldValue__input, (res) => {
        if (!mp) mp = new Mprogress({ template: 2, start: true })
        mp.set(res.percent / 100)  // 0.x
      }, (res) => {
        mp.end()
        this.handleChange({ target: { value: res.key } }, true)
      })
    }
  }
}

// eslint-disable-next-line no-unused-vars
var detectElementExt = function (item) {
  if (item.type === 'AVATAR') {
    return <RbFormAvatar {...item} />
  }
  return null
}

// 列表渲染

if (window.CellRenders) {
  CellRenders.addRender('AVATAR', function (v, s, k) {
    let imgUrl = rb.baseUrl + '/filex/img/' + v + '?imageView2/2/w/100/interlace/1/q/100'
    return <td key={k} className="user-avatar"><img src={imgUrl} alt="Avatar" /></td>
  })
}