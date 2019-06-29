/* eslint-disable react/prop-types */
$(document).ready(() => {
  renderRbcomp(<RbFlow />, 'rbflow')
})

const NTs = {
  'start': ['start', '发起人', '所有人'],
  'approver': ['approver', '审批人', '发起人自选'],
  'cc': ['cc', '抄送人', '发起人自选']
}

const AddNodeButton = function (props) {
  props = props || { addNode: function () { alert('添加节点') } }
  return (<div className="add-node-btn-box"><div className="add-node-btn"><button type="button" onClick={props.addNode}><i className="zmdi zmdi-plus" /></button></div></div>)
}

// ~ 节点
class Node extends React.Component {
  constructor(props) {
    super(props)
    this.nodeType = NTs[props.type || 'approver']
  }
  render() {
    return (<div className="node-wrap">
      <div className={'node-wrap-box ' + this.nodeType[0] + '-node'}>
        <div>
          <div className="title"><span>{this.nodeType[1]}</span></div>
          <div className="content">
            <div className="text">{this.nodeType[2]}</div>
            <i className="zmdi zmdi-chevron-right arrow"></i>
          </div>
        </div>
      </div>
      <AddNodeButton addNode={showDlgAddNode} />
    </div>)
  }
}

// 条件分支节点
class ConditionNode extends React.Component {
  constructor(props) {
    super(props)
    this.state = { columns: props.columns || [] }
  }
  render() {
    let colLen = this.state.columns.length - 1
    return (<div className="branch-wrap">
      <div className="branch-box-wrap">
        <div className="branch-box">
          <button className="add-branch" onClick={() => this.addColoum()}>添加分支</button>
          {this.state.columns.map((item, idx) => {
            return <ConditionColoum key={'column-' + idx} isFirst={idx === 0} isLast={idx === colLen} nodes={item.nodes} />
          })}
        </div>
        <AddNodeButton />
      </div>
    </div>)
  }

  addColoum() {
    let columns = this.state.columns
    columns.push('c')
    this.setState({ columns: columns })
  }
}

class ConditionColoum extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props }
  }
  render() {
    return (<div className="col-box">
      {this.state.isFirst && <div className="top-left-cover-line"></div>}
      {this.state.isFirst && <div className="bottom-left-cover-line"></div>}
      <div className="condition-node">
        <div className="condition-node-box">
          <div className="auto-judge">
            <div className="title-wrapper">
              <span className="editable-title float-left">分支条件</span>
              <span className="priority-title float-right">默认优先级</span>
            </div>
            <div className="content">
              请设置条件
            </div>
          </div>
        </div>
      </div>
      {this.state.nodes}
      {this.state.isLast && <div className="top-right-cover-line"></div>}
      {this.state.isLast && <div className="bottom-right-cover-line"></div>}
    </div>)
  }
  componentWillReceiveProps(props) {
    this.setState({ ...props })
  }
}

// 画布
class RbFlow extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div className="box-scale" id="box-scale">
      <Node type="start" />
      <Node type="approver" />
      <Node type="cc" />
      <ConditionNode columns={['a', 'b', 'c']} />
      <div className="end-node">
        <div className="end-node-circle"></div>
        <div className="end-node-text">流程结束</div>
      </div>
    </div>)
  }
}

class DlgAddNode extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return (
      <div className="modal add-nodes" tabIndex="-1" ref={(c) => this._dlg = c}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}><span className="zmdi zmdi-close" /></button>
            </div>
            <div className="modal-body">
              <div className="row">
                <div className="col-4">
                  <button>审批人</button>
                </div>
                <div className="col-4">
                  <button>抄送人</button>
                </div>
                <div className="col-4">
                  <button>条件分支</button>
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
  show() {
    $(this._dlg).modal({ show: true, keyboard: true })
  }
}

let __DlgAddNode
const showDlgAddNode = function () {
  if (__DlgAddNode) __DlgAddNode.show()
  else __DlgAddNode = renderRbcomp(<DlgAddNode />)
}
const hideDlgAddNode = function () {
  if (__DlgAddNode) __DlgAddNode.hide()
}