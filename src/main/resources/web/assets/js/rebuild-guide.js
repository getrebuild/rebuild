/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

class RebuildGuide extends React.Component {
  render() {
    return (
      <div className="rebuild-guide">
        <div className="top d-flex mb-2">
          <div className="w-75">
            <div className="float-left mr-3">
              <div id="guide-progress"></div>
            </div>
            <h3 className="m-0 mb-1 mt-1">{$L('初始化向导')}</h3>
            <p className="text-muted">{$L('跟随初始化向导，帮助你快速完成系统搭建')}</p>
          </div>
          <div className="w-25 text-right">
            <a
              href="###"
              onClick={(e) => {
                $stopEvent(e, true)
                $('.rebuild-guide-body').removeClass('rebuild-guide-body')
              }}>
              {$L('关闭')}
            </a>
          </div>
        </div>

        <CommonGuide title={$L('系统通用配置')} feat="syscfg" index="1" pcalc={() => this.pcalc()} />
        <CommonGuide title={$L('业务实体配置')} feat="entitymrg" index="2" pcalc={() => this.pcalc()} />
        <CommonGuide title={$L('用户配置')} feat="usermrg" index="3" pcalc={() => this.pcalc()} />
        <CommonGuide title={$L('更多功能')} feat="others" index="4" pcalc={() => this.pcalc()} />
        <div className="clearfix"></div>
      </div>
    )
  }

  componentDidMount() {}

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
        radius: ['85%', '100%'],
        center: ['50%', '50%'],
        startAngle: 280,
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
    renderEChart(option, 'guide-progress')
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
      </div>
    )
  }

  renderItems() {
    const items = this.state.items || []
    return (
      <ul className="list-unstyled">
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
                      <a
                        href="###"
                        className="confirm"
                        title={$L('我已完成')}
                        onClick={(e) => {
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
      this.setState({ items: res.data }, () => this._pcalc())
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
    const that = this
    RbAlert.create($L('已完成此项？'), {
      onConfirm: function () {
        this.hide()
        $.post(`/common/guide/confirm?url=${$encode(url)}`)

        const items = that.state.items
        items.forEach((item) => {
          if (item.url === url) item.confirm = true
        })
        that.setState({ items }, () => that._pcalc())
      },
    })
  }
}

$(document).ready(() => {
  $(document.body).addClass('rebuild-guide-body')
  const $mc = $('.main-content')
  const $wrap = $('<div class="rebuild-guide-screen shadow"></div>').appendTo($mc)
  renderRbcomp(<RebuildGuide />, $wrap[0])

  $addResizeHandler(() => {
    const wh = $(window).height()
    $mc.css('height', wh - 88)
  })()
})
