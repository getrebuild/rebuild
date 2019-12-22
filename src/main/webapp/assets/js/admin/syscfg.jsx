// 系统参数设置 - 公共部分

$(document).ready(() => {
  $('.card-header-divider>a').click((e) => {
    e.preventDefault()
    editMode()
  })
  $('.edit-footer>.btn-link').click(() => location.reload())
  $('.edit-footer>.btn-primary').click(() => post(__data))
})

let __data = {}
const changeValue = function (e) {
  let name = e.target.name
  __data[name] = e.target.value
}
// 激活编辑模式
const editMode = function () {
  $('.syscfg table td[data-id]').each(function () {
    let $item = $(this)
    let val = $item.text()
    if (val === '未配置') val = ''
    let name = $item.data('id')

    let options = $item.data('options')
    if (options) {
      options = options.split(';')
      let comp = <select name={name} className="form-control form-control-sm" onChange={changeValue}>
        {options.map((item) => {
          let kv = item.split(':')
          return <option value={kv[0]} key={kv[0]}>{kv[1]}</option>
        })}
      </select>
      renderRbcomp(comp, $item)
    } else {
      renderRbcomp(<input defaultValue={val} name={name} className="form-control form-control-sm" onChange={changeValue} />, $item)
    }
  })
  $('.syscfg').addClass('edit')
}

// 提交
const post = function (data) {
  for (let k in data) {
    if (!data[k]) {
      let field = $('td[data-id=' + k + ']').prev().text()
      RbHighbar.create(field + '不能为空')
      return false
    }
  }

  const btn = $('.edit-footer>.btn-primary').button('loading')
  $.post(location.href, JSON.stringify(data), (res) => {
    btn.button('reset')
    if (res.error_code === 0) location.reload()
    else RbHighbar.error(res.error_msg)
  })
}