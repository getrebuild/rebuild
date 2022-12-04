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
                $('.rebuild-guide').addClass('hide')
              }}>
              {$L('关闭')}
            </a>
          </div>
        </div>

        <CommonGuide title={$L('系统配置')} feat="syscfg" />
      </div>
    )
  }

  componentDidMount() {
    setTimeout(() => this._pcalc(15), 1000)
  }

  _pcalc(value) {
    const option = {
      animation: false,
      series: {
        type: 'pie',
        radius: ['80%', '100%'],
        center: ['50%', '50%'],
        startAngle: 260,
        hoverAnimation: false,
        silent: false,
        label: {
          show: true,
          position: 'center',
          formatter: '{d}%',
          fontSize: 18,
        },
        color: ['#eeeeee', '#34a853'],
        emphais: {
          scale: false,
        },
        labelLine: {
          show: true,
        },
        data: [
          {
            value: 100 - value,
            name: '',
            emphais: {
              label: {
                show: false,
              },
            },
          },
          {
            value: value,
            name: '',
          },
        ],
      },
    }

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
            <li key={item.item} className={`shadow-sm ${item.confirm && 'confirm'}`}>
              <div className="d-flex">
                <span className="w-50">
                  {item.confirm && <i className="icon mdi mdi-check-circle-outline text-bold mr-1 text-success" />}
                  {item.item}
                </span>
                <span className="w-50 text-right">
                  {item.confirm ? (
                    <a href={`${rb.baseUrl}/${item.url}`}>{$L('继续完善')}</a>
                  ) : (
                    <RF>
                      <a href={`${rb.baseUrl}/${item.url}`}>{$L('去设置')}</a>
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

    this.setState({ p: (p * 100) / items.length })
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
  const $wrap = $('<div class="rebuild-guide-screen"></div>').appendTo('.main-content')
  renderRbcomp(<RebuildGuide />, $wrap[0])
})
