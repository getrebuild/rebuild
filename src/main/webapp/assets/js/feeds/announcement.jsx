/* eslint-disable react/prop-types */

const EMOJIS = { '赞': 'fs_zan.png', '握手': 'fs_woshou.png', '耶': 'fs_ye.png', '抱拳': 'fs_baoquan.png', 'OK': 'fs_ok.png', '拍手': 'fs_paishou.png', '拜托': 'fs_baituo.png', '差评': 'fs_chaping.png', '微笑': 'fs_weixiao.png', '撇嘴': 'fs_piezui.png', '花痴': 'fs_huachi.png', '发呆': 'fs_fadai.png', '得意': 'fs_deyi.png', '大哭': 'fs_daku.png', '害羞': 'fs_haixiu.png', '闭嘴': 'fs_bizui.png', '睡着': 'fs_shuizhao.png', '敬礼': 'fs_jingli.png', '崇拜': 'fs_chongbai.png', '抱抱': 'fs_baobao.png', '忍住不哭': 'fs_renzhubuku.png', '尴尬': 'fs_ganga.png', '发怒': 'fs_fanu.png', '调皮': 'fs_tiaopi.png', '开心': 'fs_kaixin.png', '惊讶': 'fs_jingya.png', '呵呵': 'fs_hehe.png', '思考': 'fs_sikao.png', '哭笑不得': 'fs_kuxiaobude.png', '抓狂': 'fs_zhuakuang.png', '呕吐': 'fs_outu.png', '偷笑': 'fs_touxiao.png', '笑哭了': 'fs_xiaokule.png', '白眼': 'fs_baiyan.png', '傲慢': 'fs_aoman.png', '饥饿': 'fs_jie.png', '困': 'fs_kun.png', '吓': 'fs_xia.png', '流汗': 'fs_liuhan.png', '憨笑': 'fs_hanxiao.png', '悠闲': 'fs_youxian.png', '奋斗': 'fs_fendou.png', '咒骂': 'fs_zhouma.png', '疑问': 'fs_yiwen.png', '嘘': 'fs_xu.png', '晕': 'fs_yun.png', '惊恐': 'fs_jingkong.png', '衰': 'fs_shuai.png', '骷髅': 'fs_kulou.png', '敲打': 'fs_qiaoda.png', '再见': 'fs_zaijian.png', '无语': 'fs_wuyu.png', '抠鼻': 'fs_koubi.png', '鼓掌': 'fs_guzhang.png', '糗大了': 'fs_qiudale.png', '猥琐的笑': 'fs_weisuodexiao.png', '哼': 'fs_heng.png', '不爽': 'fs_bushuang.png', '打哈欠': 'fs_dahaqian.png', '鄙视': 'fs_bishi.png', '委屈': 'fs_weiqu.png', '安慰': 'fs_anwei.png', '坏笑': 'fs_huaixiao.png', '亲亲': 'fs_qinqin.png', '冷汗': 'fs_lenghan.png', '可怜': 'fs_kelian.png', '生病': 'fs_shengbing.png', '愉快': 'fs_yukuai.png', '幸灾乐祸': 'fs_xingzailehuo.png', '大便': 'fs_dabian.png', '干杯': 'fs_ganbei.png', '钱': 'fs_qian.png' }

// eslint-disable-next-line no-unused-vars
const converEmoji = function (text) {
  const es = text.match(/\[(.+?)\]/g)
  if (!es) return text
  es.forEach((e) => {
    let img = EMOJIS[e.substr(1, e.length - 2)]
    if (img) {
      img = `<img class="emoji" src="${rb.baseUrl}/assets/img/emoji/${img}"/>`
      text = text.replace(e, img)
    }
  })
  return text.replace(/\n/g, '<br />')
}

// 公告
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
              <span className="float-left text-muted fs-12">由 {this.props.publishBy} 发布于 {this.props.publishOn}</span>
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
      $(this).find('p>a').click((e) => e.stopPropagation())
    })
  })
}

$(document).ready(() => $showAnnouncement())