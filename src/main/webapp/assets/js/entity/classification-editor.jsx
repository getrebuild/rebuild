/* eslint-disable no-undef */
$(document).ready(function () {
  // let tmplBox = $('.level-boxes>.col-md-3').eq(0)
  // init_levelbox(tmplBox)
  // init_levelbox($(tmplBox.clone(false)).appendTo('.level-boxes'), 2)
  // init_levelbox($(tmplBox.clone(false)).appendTo('.level-boxes'), 3)
  // init_levelbox($(tmplBox.clone(false)).appendTo('.level-boxes'), 4)

  // load_items(null, tmplBox)
  renderRbcomp(<LevelBoxes id={rb.classificationId} />, 'boxes')
})

class LevelBoxes extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    let L4 = <LevelBox level={4} />
    let L3 = <LevelBox level={3} next={L4} />
    let L2 = <LevelBox level={2} next={L3} />
    let L1 = <LevelBox level={1} next={L2} />
    return (<div className="row level-boxes">
      {L1}
      {L2}
      {L3}
      {L4}
    </div>)
  }
}

const LNAME = { 1: '一', 2: '二', 3: '三', 4: '四' }
class LevelBox extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
    this.trunOn = this.trunOn.bind(this)
  }
  render() {
    let trunId = 'trunOn' + this.props.level
    return (
      <div className="col-md-3">
        <div className="float-left"><h5>{LNAME[this.props.level]}级分类</h5></div>
        {this.props.level <= 1 ? null :
          <div className="float-right">
            <div className="switch-button switch-button-xs">
              <input type="checkbox" id={trunId} onChange={this.trunOn} />
              <span><label htmlFor={trunId} title="启用/禁用"></label></span>
            </div>
          </div>}
        <div className="clearfix"></div>
        <form className="mt-1">
          <div className="input-group input-group-sm">
            <input className="form-control J_name" type="text" maxlength="50" placeholder="名称" />
            <div className="input-group-append"><button class="btn btn-primary J_save" type="submit">添加</button></div>
          </div>
        </form>
        <ol className="dd-list unset-list mt-3">
          {(this.state.items || []).each(() => {
            <li className="dd-item">
            </li>
          })}
        </ol>
      </div>)
  }
  componentDidMount() {
  }

  trunOn(e) {
  }
}



let init_levelbox = function (box, level) {
  box.attr('data-level', level || 1)
  if (level > 1) {
    box.addClass('off')
    box.find('h5').text(LNAME[level])
    let newFor = 'trunOn' + level
    box.find('.turn-on label').attr('for', newFor)
    box.find('.turn-on input').attr('id', newFor).change(function () {
      let chk = $(this).prop('checked') === true
      for (let i = 2; i <= level; i++) {
        let $box = $('.level-boxes>div[data-level=' + i + ']')
        if (chk) $box.removeClass('off')
        else $box.addClass('off')
      }
    })
  }

  box.find('.dd-list').sortable({
    placeholder: 'dd-placeholder',
    handle: '.dd-handle',
    axis: 'y',
  }).disableSelection()

  box.find('.J_save').click(() => {
    let name = box.find('.J_name').val()
    if (name) {
      let url = rb.baseUrl + '/admin/entityhub/classification/save-data-item?data_id=' + rb.classificationId
      url += '&name=' + $encode(name)
      $.post(url, (res) => {
        if (res.error_code == 0) {
          render_unset([res.data, name], box.find('.dd-list'))
          box.find('.J_name').val('')
        } else rb.hberror(res.error_msg)
      })
    }
    return false
  })
}

let load_items = function (parent, destBox) {
  let url = rb.baseUrl + '/admin/entityhub/classification/load-data-items?data_id=' + rb.classificationId
  if (parent) url += '&parent=' + parent
  $.get(url, (res) => {
    $(res.data).each(function () {
      render_unset([this[0], this[1]], destBox.find('.dd-list'))
    })
  })
}

// Over sortable.js
render_unset_after = function (item, data) {
  let $box = $(item).parents('.col-md-3')
  item.off('click').click(function () {
    let $this = $(this)
    if ($this.hasClass('active')) {
      $this.removeClass('active')
    } else {
      $box.find('li').removeClass('active')
      $this.addClass('active')
    }
  })

  let acts = $('<div class="dd-action"><a><i class="zmdi zmdi-edit"></i></a><a><i class="zmdi zmdi-delete"></i></a></div>').appendTo(item)
  acts.find('a').eq(0).click(() => {
    let $form = $box.find('form')
    $form.find('button').text('保存')
    $form.find('input').val(data[1]).focus()
  })
  acts.find('a').eq(1).click(() => {

  })
}