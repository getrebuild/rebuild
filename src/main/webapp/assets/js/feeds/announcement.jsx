/*
Copyright (c) REBUILD <https://getrebuild.com/> and its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

var EMOJIS = { '赞': 'rb_zan.png', '握手': 'rb_woshou.png', '耶': 'rb_ye.png', '抱拳': 'rb_baoquan.png', 'OK': 'rb_ok.png', '拍手': 'rb_paishou.png', '拜托': 'rb_baituo.png', '差评': 'rb_chaping.png', '微笑': 'rb_weixiao.png', '撇嘴': 'rb_piezui.png', '花痴': 'rb_huachi.png', '发呆': 'rb_fadai.png', '得意': 'rb_deyi.png', '大哭': 'rb_daku.png', '害羞': 'rb_haixiu.png', '闭嘴': 'rb_bizui.png', '睡着': 'rb_shuizhao.png', '敬礼': 'rb_jingli.png', '崇拜': 'rb_chongbai.png', '抱抱': 'rb_baobao.png', '忍住不哭': 'rb_renzhubuku.png', '尴尬': 'rb_ganga.png', '发怒': 'rb_fanu.png', '调皮': 'rb_tiaopi.png', '开心': 'rb_kaixin.png', '惊讶': 'rb_jingya.png', '呵呵': 'rb_hehe.png', '思考': 'rb_sikao.png', '哭笑不得': 'rb_kuxiaobude.png', '抓狂': 'rb_zhuakuang.png', '呕吐': 'rb_outu.png', '偷笑': 'rb_touxiao.png', '笑哭了': 'rb_xiaokule.png', '白眼': 'rb_baiyan.png', '傲慢': 'rb_aoman.png', '饥饿': 'rb_jie.png', '困': 'rb_kun.png', '吓': 'rb_xia.png', '流汗': 'rb_liuhan.png', '憨笑': 'rb_hanxiao.png', '悠闲': 'rb_youxian.png', '奋斗': 'rb_fendou.png', '咒骂': 'rb_zhouma.png', '疑问': 'rb_yiwen.png', '嘘': 'rb_xu.png', '晕': 'rb_yun.png', '惊恐': 'rb_jingkong.png', '衰': 'rb_shuai.png', '骷髅': 'rb_kulou.png', '敲打': 'rb_qiaoda.png', '再见': 'rb_zaijian.png', '无语': 'rb_wuyu.png', '抠鼻': 'rb_koubi.png', '鼓掌': 'rb_guzhang.png', '糗大了': 'rb_qiudale.png', '猥琐的笑': 'rb_weisuodexiao.png', '哼': 'rb_heng.png', '不爽': 'rb_bushuang.png', '打哈欠': 'rb_dahaqian.png', '鄙视': 'rb_bishi.png', '委屈': 'rb_weiqu.png', '安慰': 'rb_anwei.png', '坏笑': 'rb_huaixiao.png', '亲亲': 'rb_qinqin.png', '冷汗': 'rb_lenghan.png', '可怜': 'rb_kelian.png', '生病': 'rb_shengbing.png', '愉快': 'rb_yukuai.png', '幸灾乐祸': 'rb_xingzailehuo.png', '大便': 'rb_dabian.png', '干杯': 'rb_ganbei.png', '钱': 'rb_qian.png' }
// eslint-disable-next-line no-unused-vars
var converEmoji = function (text) {
  const es = text.match(/\[(.+?)\]/g)
  if (!es) return text
  es.forEach((e) => {
    const key = e.substr(1, e.length - 2)
    if (EMOJIS[key]) {
      const img = `<img class="emoji" src="${rb.baseUrl}/assets/img/emoji/${EMOJIS[key]}" alt="${key}" />`
      text = text.replace(e, img)
    }
  })
  return text.replace(/\n/g, '<br />')
}

// ~ 公告展示
class AnnouncementModal extends React.Component {
  state = { ...this.props }

  render() {
    const contentHtml = converEmoji(this.props.content.replace(/\n/g, '<br>'))
    return <div className="modal" tabIndex={this.state.tabIndex || -1} ref={(c) => this._dlg = c}>
      <div className="modal-dialog modal-dialog-centered">
        <div className="modal-content">
          <div className="modal-header pb-0">
            <button className="close" type="button" onClick={this.hide}><i className="zmdi zmdi-close" /></button>
          </div>
          <div className="modal-body">
            <div className="text-break announcement-contents" dangerouslySetInnerHTML={{ __html: contentHtml }} />
            <div>
              <span className="float-left text-muted">由 {this.props.publishBy} 发布于 {this.props.publishOn}</span>
              <span className="float-right"><a href={`${rb.baseUrl}/app/list-and-view?id=${this.props.id}`}>前往动态查看</a></span>
              <span className="clearfi"></span>
            </div>
          </div>
        </div>
      </div>
    </div >
  }

  componentDidMount() {
    const root = $(this._dlg).modal({ show: true, keyboard: true }).on('hidden.bs.modal', () => {
      root.modal('dispose')
      $unmount(root.parent())
    })
  }

  hide = () => $(this._dlg).modal('hide')
}

var $showAnnouncement = function () {
  $.get(`${rb.baseUrl}/commons/announcements`, (res) => {
    if (res.error_code !== 0 || !res.data || res.data.length === 0) return
    const as = res.data.map((item, idx) => {
      return <div className="bg-warning" key={'a-' + idx} title="查看详情" onClick={() => renderRbcomp(<AnnouncementModal {...item} />)}>
        <i className="icon zmdi zmdi-notifications-active" />
        <p dangerouslySetInnerHTML={{ __html: item.content }}></p>
      </div>
    })
    renderRbcomp(<React.Fragment>{as}</React.Fragment>, $('.announcement-wrapper'), function () {
      $(this).find('p>a[href]').click((e) => e.stopPropagation())
    })
  })
}

$(document).ready(() => $showAnnouncement())