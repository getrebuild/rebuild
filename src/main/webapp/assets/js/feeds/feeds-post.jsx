/* eslint-disable react/prop-types */
// ~ 动态发布
// eslint-disable-next-line no-unused-vars
class FeedsPost extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props, type: 1 }
  }

  render() {
    return (<div className="feeds-post">
      <ul className="list-unstyled list-inline mb-1 pl-1">
        <li className="list-inline-item">
          <a onClick={() => this.setState({ type: 1 })} className={`${this.state.type === 1 && 'text-primary'}`}>动态</a>
        </li>
        <li className="list-inline-item">
          <a onClick={() => this.setState({ type: 2 })} className={`${this.state.type === 2 && 'text-primary'}`}>跟进</a>
        </li>
      </ul>
      <div className="arrow_box" style={{ marginLeft: this.state.type === 2 ? 53 : 8 }}></div>
      <div>
        <FeedsRichInput ref={(c) => this._input = c} />
      </div>
      <div className="mt-3">
        <div className="float-right">
          <button className="btn btn-primary" ref={(c) => this._btn = c} onClick={this._post}>发布</button>
        </div>
        <div className="float-right mr-4">
          <div className="btn-group" style={{ border: '0 none' }}>
            <button className="btn btn-scope btn-link" data-toggle="dropdown" ref={(c) => this._scopeBtn = c}><i className="icon up-1 zmdi zmdi-chart-donut" />公开</button>
            <div className="dropdown-menu dropdown-menu-right">
              <a className="dropdown-item" onClick={this._selectScope} data-scope="ALL" title="全部可见"><i className="icon up-1 zmdi zmdi-chart-donut" />公开</a>
              <a className="dropdown-item" onClick={this._selectScope} data-scope="SELF" title="仅自己可见"><i className="icon up-1 zmdi zmdi-lock" />私密</a>
              <a className="dropdown-item" onClick={this._selectScope} data-scope="GROUP" title="群组内可见"><i className="icon up-1 zmdi zmdi-accounts" />群组</a>
            </div>
          </div>
        </div>
        <div className="clearfix"></div>
      </div>
    </div>)
  }

  componentDidMount() {
    $('#rb-feeds').attr('class', '')
  }

  _selectScope = (e) => {
    let target = e.target
    this.setState({ scope: target.dataset.scope }, () => {
      $(this._scopeBtn).html($(target).html())
    })
  }

  _post = () => {
    let data = { content: this._input.val(), type: this.state.type, scope: this.state.scope }
    if (!data.content) return
    data.metadata = { entity: 'Feeds' }

    let btn = $(this._btn).button('loading')
    $.post(`${rb.baseUrl}/feeds/post/publish`, JSON.stringify(data), (res) => {
      btn.button('reset')
      this._input.reset()
      typeof this.props.call === 'function' && this.props.call(res.data)
    })
  }
}

// 复写组件
class UserSelectorExt extends UserSelector {
  constructor(props) {
    super(props)
  }
  componentDidMount() {
    $(this._scroller).perfectScrollbar()
  }
  clickItem(e) {
    let id = e.target.dataset.id
    let name = $(e.target).text()
    this.props.call && this.props.call(id, name)
  }
}

// ~ 输入框
class FeedsRichInput extends React.Component {

  constructor(props) {
    super(props)
    this.state = { ...props }
  }

  render() {
    let es = []
    for (let k in EMOJIS) {
      let item = EMOJIS[k]
      es.push(<a key={`em-${item}`} title={k} onClick={() => this._selectEmoji(k)}><img src={`${rb.baseUrl}/assets/img/emoji/${item}`} /></a>)
    }

    return (<div className={`rich-input ${this.state.focus && 'active'}`}>
      <textarea ref={(c) => this._input = c} placeholder={this.props.placeholder}
        onFocus={() => this.setState({ focus: true })}
        onBlur={() => this.setState({ focus: false })}
        defaultValue={this.props.initValue} />
      <div className="action-btns">
        <ul className="list-unstyled list-inline m-0 p-0">
          <li className="list-inline-item">
            <a onClick={this._toggleEmoji} title="表情"><i className="zmdi zmdi-mood" /></a>
            <span className={`mount ${this.state.showEmoji ? '' : 'hide'}`} ref={(c) => this._emoji = c}>
              {this.state.renderEmoji && <div className="emoji-wrapper">{es}</div>}
            </span>
          </li>
          <li className="list-inline-item">
            <a onClick={this._toggleAtUser} title="@用户"><i className="zmdi at-text">@</i></a>
            <span className={`mount ${this.state.showAtUser ? '' : 'hide'}`} ref={(c) => this._atUser = c}>
              <UserSelectorExt hideDepartment={true} hideRole={true} ref={(c) => this._UserSelector = c} call={this._selectAtUser} />
            </span>
          </li>
          <li className="list-inline-item"><a title="图片"><i className="zmdi zmdi-image-o" /></a></li>
          <li className="list-inline-item"><a title="附件"><i className="zmdi zmdi-attachment-alt zmdi-hc-rotate-45" /></a></li>
        </ul>
      </div>
    </div>)
  }

  componentDidMount() {
    $(document.body).click((e) => {
      if (e.target && $(e.target).parents('li.list-inline-item').length > 0) return
      this.setState({ showEmoji: false, showAtUser: false })
    })
    // eslint-disable-next-line no-undef
    autosize(this._input)
  }

  _toggleEmoji = () => {
    this.setState({ renderEmoji: true, showEmoji: !this.state.showEmoji }, () => {
      if (this.state.showEmoji) this.setState({ showAtUser: false })
    })
  }
  _toggleAtUser = () => {
    this.setState({ showAtUser: !this.state.showAtUser }, () => {
      if (this.state.showAtUser) {
        this.setState({ showEmoji: false })
        this._UserSelector.openDropdown()
      }
    })
  }
  _selectEmoji(emoji) {
    $(this._input).insertAtCursor(`[${emoji}]`)
    this.setState({ showEmoji: false })
  }
  _selectAtUser = (id, name) => {
    $(this._input).insertAtCursor(`@${name} `)
    this.setState({ showAtUser: false })
  }

  val() {
    return $(this._input).val()
  }

  focus() {
    $(this._input).selectRange(9999, 9999)  // Move to last
  }

  reset() {
    $(this._input).val('')
  }
}

const EMOJIS = { '赞': 'fs_zan.png', '握手': 'fs_woshou.png', '耶': 'fs_ye.png', '抱拳': 'fs_baoquan.png', 'OK': 'fs_ok.png', '拍手': 'fs_paishou.png', '拜托': 'fs_baituo.png', '差评': 'fs_chaping.png', '微笑': 'fs_weixiao.png', '撇嘴': 'fs_piezui.png', '花痴': 'fs_huachi.png', '发呆': 'fs_fadai.png', '得意': 'fs_deyi.png', '大哭': 'fs_daku.png', '害羞': 'fs_haixiu.png', '闭嘴': 'fs_bizui.png', '睡着': 'fs_shuizhao.png', '敬礼': 'fs_jingli.png', '崇拜': 'fs_chongbai.png', '抱抱': 'fs_baobao.png', '忍住不哭': 'fs_renzhubuku.png', '尴尬': 'fs_ganga.png', '发怒': 'fs_fanu.png', '调皮': 'fs_tiaopi.png', '开心': 'fs_kaixin.png', '惊讶': 'fs_jingya.png', '呵呵': 'fs_hehe.png', '思考': 'fs_sikao.png', '哭笑不得': 'fs_kuxiaobude.png', '抓狂': 'fs_zhuakuang.png', '呕吐': 'fs_outu.png', '偷笑': 'fs_touxiao.png', '笑哭了': 'fs_xiaokule.png', '白眼': 'fs_baiyan.png', '傲慢': 'fs_aoman.png', '饥饿': 'fs_jie.png', '困': 'fs_kun.png', '吓': 'fs_xia.png', '流汗': 'fs_liuhan.png', '憨笑': 'fs_hanxiao.png', '悠闲': 'fs_youxian.png', '奋斗': 'fs_fendou.png', '咒骂': 'fs_zhouma.png', '疑问': 'fs_yiwen.png', '嘘': 'fs_xu.png', '晕': 'fs_yun.png', '惊恐': 'fs_jingkong.png', '衰': 'fs_shuai.png', '骷髅': 'fs_kulou.png', '敲打': 'fs_qiaoda.png', '再见': 'fs_zaijian.png', '无语': 'fs_wuyu.png', '抠鼻': 'fs_koubi.png', '鼓掌': 'fs_guzhang.png', '糗大了': 'fs_qiudale.png', '猥琐的笑': 'fs_weisuodexiao.png', '哼': 'fs_heng.png', '不爽': 'fs_bushuang.png', '打哈欠': 'fs_dahaqian.png', '鄙视': 'fs_bishi.png', '委屈': 'fs_weiqu.png', '安慰': 'fs_anwei.png', '坏笑': 'fs_huaixiao.png', '亲亲': 'fs_qinqin.png', '冷汗': 'fs_lenghan.png', '可怜': 'fs_kelian.png', '生病': 'fs_shengbing.png', '愉快': 'fs_yukuai.png', '幸灾乐祸': 'fs_xingzailehuo.png', '大便': 'fs_dabian.png', '干杯': 'fs_ganbei.png', '钱': 'fs_qian.png' }
// eslint-disable-next-line no-unused-vars
const converEmoji = function (text) {
  let es = text.match(/\[(.+?)\]/g)
  if (!es) return text
  es.forEach((e) => {
    let img = EMOJIS[e.substr(1, e.length - 2)]
    if (img) {
      img = `<img class="emoji" src="${rb.baseUrl}/assets/img/emoji/${img}"/>`
      text = text.replace(e, img)
    }
  })
  return text.replace(/\n/g, '<br/>')
}