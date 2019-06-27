/* eslint-disable no-undef */
$(document).ready(() => {
  let steps = [
    'st=>start: 开始:$node_click',
    'ed=>end: 结束:$node_click',
    'op1=>operation: 操作1',
    'op2=>operation: 操作2',
    'st->op1',
    'st->op2',
    'op1->ed',
    'op2->ed',
  ]
  let diagram = flowchart.parse(steps.join('\n'))
  diagram.drawSVG('diagram', {
    'line-width': 2,
    'line-color': '#bbbbbb',
    'line-length': 80,
    'font-size': 14,
    'font-color': '#666666',
    'text-margin': 30,
    'element-color': '#1890FF',
    'fill': '#E8F7FF',
    'symbols': {
      'start': {
        'element-color': '#FFC069',
        'fill': '#FFF3E9'
      },
      'end': {
        'element-color': '#FFC069',
        'fill': '#FFF3E9'
      }
    },
    'yes-text': 'Y',
    'no-text': 'N'
  })
})

var node_click = function (event, node) {
  console.log(JSON.stringify(node))
}