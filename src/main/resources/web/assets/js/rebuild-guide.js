/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

class RebuildGuide extends React.Component {
  render() {
    return (
      <div className="rebuild-guide" ref={(c) => (this._$guide = c)}>
        <div className="top d-flex mb-2">
          <div className="w-75">
            <div className="float-left mr-3">
              <div id="rebuild-guide-progress2"></div>
            </div>
            <h3 className="m-0 mb-1 mt-1">{$L('初始化向导')}</h3>
            <p>{$L('跟随初始化向导，帮助你快速完成系统搭建')}</p>
          </div>
          <div className="w-25 text-right">
            <input
              type="checkbox"
              className="mr-1 down-1"
              onClick={(e) => {
                const s = $val(e.target)
                $.cookie('GuideShowNaver', s, { expires: null })
                $.post(`/common/guide/show-naver?s=${s}`, () => {})
              }}
            />
            <span className="mr-2 text-muted">{$L('下次登录不再显示')}</span>
            <a
              href="###"
              onClick={(e) => {
                $stopEvent(e, true)
                $.cookie('GuideShowNaverTime', true, { expires: null })
                $('.rebuild-guide-body').removeClass('rebuild-guide-body')
              }}>
              {$L('关闭')}
            </a>
          </div>
        </div>

        <CommonGuide title={$L('系统通用配置')} tips={$L('配置系统名称、LOGO，或根据需要配置短信和邮件服务等')} feat="syscfg" index="1" pcalc={() => this.pcalc()} />
        <CommonGuide title={$L('业务实体配置')} tips={$L('业务实体是系统的基础与核心，所有业务功能均由此展开')} feat="entitymrg" index="2" pcalc={() => this.pcalc()} />
        <CommonGuide title={$L('系统用户与权限')} tips={$L('添加登录用户并为他们分配角色，角色规定了他们对业务数据的的访问权限')} feat="usermrg" index="3" pcalc={() => this.pcalc()} />
        <CommonGuide title={$L('更多功能')} tips={$L('REBUILD 拥有众多强大的功能，你可以持续探索')} feat="others" index="4" pcalc={() => this.pcalc()} />
        <div className="clearfix"></div>
      </div>
    )
  }

  componentDidMount() {
    if ($isTrue($.cookie('GuideShowNaver'))) {
      $(this._$guide).find('input[type="checkbox"]').attr('checked', true)
    }
  }

  pcalc() {
    $setTimeout(() => this._pcalc(), 200, 'rebuild-guide-p')
  }

  _pcalc() {
    const t1 = $('.rebuild-guide .guide .items li').length
    const t2 = $('.rebuild-guide .guide .items li.confirm').length
    const p = ~~((t2 * 100) / t1)

    let option = {
      series: {
        type: 'pie',
        radius: ['80%', '100%'],
        center: ['50%', '50%'],
        hoverAnimation: false,
        silent: false,
        label: {
          show: true,
          position: 'center',
          formatter: '{d}%',
          fontSize: 18,
        },
        color: ['#eeeeee', '#34a853'],
        emphasis: {
          scale: false,
        },
        labelLine: {
          show: true,
        },
        data: [
          {
            value: 100 - p,
            name: '',
            emphasis: {
              label: {
                show: false,
              },
            },
          },
          {
            value: p,
            name: '',
          },
        ],
      },
    }

    // eslint-disable-next-line no-undef
    option = { ...ECHART_BASE, ...option }
    // eslint-disable-next-line no-undef
    renderEChart(option, 'rebuild-guide-progress1')
    // eslint-disable-next-line no-undef
    renderEChart(option, 'rebuild-guide-progress2')
  }
}

class CommonGuide extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return (
      <div className="guide">
        <div className="header">
          <h5>
            <strong>{this.props.index || 1}</strong>
            {this.props.title}
            <small>{this.state.p}%</small>
          </h5>
        </div>
        <div className="items">{this.renderItems()}</div>
        <p className="tips">{this.props.tips}</p>
      </div>
    )
  }

  renderItems() {
    const items = this.state.items || []
    return (
      <ul className="list-unstyled" ref={(c) => (this._$guideItems = c)}>
        {items.map((item) => {
          return (
            <li key={item.item} className={`shadow-sm1 ${item.confirm && 'confirm'}`}>
              <div className="d-flex">
                <span className="w-50">
                  {item.confirm && <i className="icon mdi mdi-check-circle-outline mr-1 text-success" />}
                  {item.item}
                </span>
                <span className="w-50 text-right">
                  {item.confirm ? (
                    <a href={`${rb.baseUrl}/${item.url}`}>
                      {item.num === -1 ? $L('继续使用') : $L('继续完善')}
                      {item.num > 0 && ` (${item.num})`}
                    </a>
                  ) : (
                    <RF>
                      <a href={`${rb.baseUrl}/${item.url}`}>
                        {item.num === -1 ? $L('去使用') : $L('去配置')}
                        {item.num > 0 && ` (${item.num})`}
                      </a>
                      <em>|</em>
                      <a
                        href="###"
                        className="confirm"
                        title={$L('我已完成')}
                        onClick={(e) => {
                          $(e.currentTarget).tooltip('dispose')
                          $stopEvent(e, true)
                          this.handleConfirm(item.url)
                        }}>
                        <i className="icon mdi mdi-check text-bold" />
                      </a>
                    </RF>
                  )}
                </span>
              </div>
            </li>
          )
        })}
      </ul>
    )
  }

  componentDidMount() {
    $.get(`/common/guide/${this.props.feat}`, (res) => {
      this.setState({ items: res.data }, () => {
        this._pcalc()
        $(this._$guideItems).find('a.confirm').tooltip({})
      })
    })
  }

  _pcalc() {
    const items = this.state.items || []
    let p = 0
    items.forEach((item) => {
      if (item.confirm) p++
    })
    this.setState({ p: ~~((p * 100) / items.length) }, () => this.props.pcalc())
  }

  handleConfirm(url) {
    $.post(`/common/guide/confirm?url=${$encode(url)}`, () => {
      const items = this.state.items
      items.forEach((item) => {
        if (item.url === url) item.confirm = true
      })
      this.setState({ items }, () => this._pcalc())
    })
  }
}

$(document).ready(() => {
  const $mc = $('.main-content')
  $(`<div class="rebuild-guide-progress" title="${$L('打开初始化向导')}"><div id="rebuild-guide-progress1"></div></div>`).appendTo($mc)

  const $wrap = $('<div class="rebuild-guide-screen shadow"></div>').appendTo($mc)
  renderRbcomp(<RebuildGuide />, $wrap[0])

  $addResizeHandler(() => {
    const wh = $(window).height()
    $mc.css('height', wh - 80)
  })()

  const $c = $('.rebuild-guide-progress').on('click', () => {
    $(document.body).addClass('rebuild-guide-body')
    $.removeCookie('GuideShowNaverTime')
  })

  if (!$.cookie('GuideShowNaverTime')) $c.trigger('click')
})
