/* eslint-disable no-console */
/* eslint-disable react/prop-types */
$(document).ready(() => {
  renderRbcomp(<RbFlowCanvas nodeId="RBFLOW" />, 'rbflow')
  $(document.body).click(function () {
    console.log('body removeClass')
    // $(this).removeClass('open-right-sidebar')
  })

  window.resize_handler()
})
window.resize_handler = function () {
  $('#rbflow').css('min-height', $(window).height() - 222)
}

// 节点类型
const NTs = {
  'start': ['start', '发起人', '所有人'],
  'approver': ['approver', '审批人', '发起人自选'],
  'cc': ['cc', '抄送人', '发起人自选']
}
// 添加节点按钮
const AddNodeButton = function (props) {
  let c = function () { showDlgAddNode(props.addNodeCall) }
  return (<div className="add-node-btn-box"><div className="add-node-btn"><button type="button" onClick={c} title={props.title}><i className="zmdi zmdi-plus" /></button></div></div>)
}

// 节点规范
class NodeSpec extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  addNodeQuick = (type) => {
    this.props.$$$parent.addNode(type, this.props.nodeId)
  }
  removeNodeQuick = () => {
    this.props.$$$parent.removeNode(this.props.nodeId)
  }
  openConfig = (e) => {
    e.stopPropagation()
    e.nativeEvent.stopImmediatePropagation()

    let that = this
    let call = function (d) {
      that.setState({ data: d })
    }
    renderRbcomp(<NodeConfig type={this.nodeType} call={call} data={this.state.data} />, 'config-side', () => {
      $(document.body).addClass('open-right-sidebar')
    })
  }
  serialize() {
  }
}
// 画布规范
class CanvasSpec extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, nodes: props.nodes || [] }
  }
  renderNodes() {
    let nodes = (this.state.nodes || []).map((item) => {
      let props = { ...item, key: 'k-' + item.nodeId, $$$parent: this }
      if (item.type === 'condition') return <ConditionNode {...props} />
      else return <Node {...props} />
    })
    return nodes
  }
  addNode = (type, depsNodeId, call) => {
    let n = { type: type, nodeId: $random(type === 'condition' ? 'COND' : 'NODE') }
    let nodes = []
    if (depsNodeId) {
      if (depsNodeId === 'ROOT' || depsNodeId === 'COND') nodes.push(n)
      this.state.nodes.forEach((item) => {
        nodes.push(item)
        if (depsNodeId === item.nodeId) nodes.push(n)
      })
    } else {
      nodes = this.state.nodes || []
      nodes.push(n)
    }
    this.setState({ nodes: nodes }, () => {
      typeof call === 'function' && call()
      hideDlgAddNode()
    })
  }
  removeNode = (nodeId) => {
    let nodes = []
    this.state.nodes.forEach((item) => {
      if (nodeId !== item.nodeId) nodes.push(item)
    })
    this.setState({ nodes: nodes })
  }
}
// 画布:节点 1:N

// 一般节点
class Node extends NodeSpec {
  constructor(props) {
    super(props)
    this.nodeType = props.type || 'approver'
  }
  render() {
    let nt = NTs[this.nodeType]
    return (<div className="node-wrap">
      <div className={'node-wrap-box ' + nt[0] + '-node animated fadeIn'}>
        <div className="title">
          <span>{nt[1]}</span>
          {this.props.nodeId !== 'ROOT' && <i className="zmdi zmdi-close aclose" title="移除" onClick={this.removeNodeQuick} />}
        </div>
        <div className="content" onClick={this.openConfig}>
          <div className="text">{nt[2]}</div>
          <i className="zmdi zmdi-chevron-right arrow"></i>
        </div>
      </div>
      <AddNodeButton addNodeCall={this.addNodeQuick} />
    </div>)
  }
}

// 条件节点
class ConditionNode extends NodeSpec {
  constructor(props) {
    super(props)
    this.state.columns = props.columns || [{ index: 1, nodeId: $random('COND') }, { index: 2, nodeId: $random('COND') }]
    this.columnIndex = this.state.columns.length + 1
  }
  render() {
    let colLen = this.state.columns.length - 1
    return (colLen > 0 && <div className="branch-wrap">
      <div className="branch-box-wrap">
        <div className="branch-box">
          <button className="add-branch" onClick={this.addColumn}>添加分支</button>
          {this.state.columns.map((item, idx) => {
            return <ConditionCanvas key={this.props.nodeId + '-col' + idx} isFirst={idx === 0} isLast={idx === colLen} $$$parent={this} {...item} />
          })}
        </div>
        <AddNodeButton addNodeCall={this.addNodeQuick} />
      </div>
    </div>)
  }
  addColumn = () => {
    let columns = this.state.columns
    columns.push({ index: this.columnIndex++, nodeId: $random('COND') })
    this.setState({ columns: columns })
  }
  removeColumn = (nodeId) => {
    let columns = []
    this.state.columns.forEach((item) => {
      if (nodeId !== item.nodeId) columns.push(item)
    })
    this.setState({ columns: columns })
  }
}

// 条件列画布
class ConditionCanvas extends CanvasSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div className="col-box">
      {this.state.isFirst && <div className="top-left-cover-line"></div>}
      {this.state.isFirst && <div className="bottom-left-cover-line"></div>}
      <div className="condition-node">
        <div className="condition-node-box animated fadeIn">
          <div className="auto-judge">
            <div className="title-wrapper">
              <span className="editable-title float-left">分支条件{this.props.index}</span>
              <span className="priority-title float-right">默认优先级</span>
              <i className="zmdi zmdi-close aclose" title="移除" onClick={() => this.props.$$$parent.removeColumn(this.props.nodeId)} />
            </div>
            <div className="content">
              请设置条件
            </div>
          </div>
          <AddNodeButton addNodeCall={this.addNode} />
        </div>
      </div>
      {this.renderNodes()}
      {this.state.isLast && <div className="top-right-cover-line"></div>}
      {this.state.isLast && <div className="bottom-right-cover-line"></div>}
    </div>)
  }
  componentWillReceiveProps(props) {
    this.setState({ ...props, nodes: this.state.nodes })
  }
  addNode(type, depsNodeId) {
    super.addNode(type, depsNodeId || 'COND')
  }
}

// 大画布
class RbFlowCanvas extends CanvasSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div className="box-scale">
      <Node type="start" $$$parent={this} nodeId="ROOT" />
      {this.renderNodes()}
      <div className="end-node">
        <div className="end-node-circle"></div>
        <div className="end-node-text">流程结束</div>
      </div>
    </div>)
  }
  componentDidMount() {
    this.addNode('approver', null, () => {
      this.addNode('cc')
    })
    $('.box-scale').draggable({ cursor: 'move', axis: 'x', scroll: false })
  }
}

// ~ 添加节点
class DlgAddNode extends React.Component {
  constructor(props) {
    super(props)
    this.state = { call: props.call || function (t) { alert(t) } }
  }
  render() {
    return (
      <div className="modal add-node" tabIndex="-1" ref={(c) => this._dlg = c}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              <div className="row">
                <div className="col-4">
                  <a className="approver" onClick={() => this.state.call('approver')}>
                    <div><i className="zmdi zmdi-account"></i></div>
                    <p>审批人</p>
                  </a>
                </div>
                <div className="col-4">
                  <a className="cc" onClick={() => this.state.call('cc')}>
                    <div><i className="zmdi zmdi-mail-send"></i></div>
                    <p>抄送人</p>
                  </a>
                </div>
                <div className="col-4">
                  <a className="condition" onClick={() => this.state.call('condition')}>
                    <div><i className="zmdi zmdi-usb zmdi-hc-rotate-180"></i></div>
                    <p>条件分支</p>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }
  componentDidMount() {
    this.show()
  }
  hide() {
    $(this._dlg).modal('hide')
  }
  show(call) {
    $(this._dlg).modal({ show: true, keyboard: true })
    if (call) this.setState({ call: call })
  }
}
let __DlgAddNode
const showDlgAddNode = function (call) {
  if (__DlgAddNode) __DlgAddNode.show(call)
  else __DlgAddNode = renderRbcomp(<DlgAddNode call={call} />)
}
const hideDlgAddNode = function () {
  if (__DlgAddNode) __DlgAddNode.hide()
}

// 节点选项编辑
class NodeConfig extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div>
      <div className="header"><h5>发起人</h5></div>
      <form>
        <div className="form-group  mb-0">
          <label>谁可以发起这个审批</label>
          <label className="custom-control custom-control-sm custom-radio">
            <input className="custom-control-input" type="radio" name={'radio-' + this.props.type} value="ALL" checked={true} /><span className="custom-control-label">所有人</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio">
            <input className="custom-control-input" type="radio" name={'radio-' + this.props.type} value="SPEC" /><span className="custom-control-label">指定人员</span>
          </label>
        </div>
        <div className="form-group">
          <UserSelector />
        </div>
      </form>
    </div >)
  }
  componentDidMount() {
  }
}