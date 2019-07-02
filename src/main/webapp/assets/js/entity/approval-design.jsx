/* eslint-disable react/prop-types */
const wpc = window.__PageConfig
$(document).ready(() => {
  renderRbcomp(<RbFlowCanvas nodeId="RBFLOW" />, 'rbflow')

  $(document.body).click(function (e) {
    if (e.target && (e.target.matches('div.rb-right-sidebar') || $(e.target).parents('div.rb-right-sidebar').length > 0)) return
    $(this).removeClass('open-right-sidebar')
  })

  window.resize_handler()
})
window.resize_handler = function () {
  $('#rbflow').css('min-height', $(window).height() - 222)
}

// 画布准备完毕
let isCanvasMounted = false
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
  componentDidMount() {
    this.props.$$$parent.onRef(this)
  }
  addNodeQuick = (type) => {
    this.props.$$$parent.addNode(type, this.props.nodeId)
  }
  removeNodeQuick = () => {
    this.props.$$$parent.removeNode(this.props.nodeId)
    this.props.$$$parent.onRef(this, true)
  }
  openConfig = () => {
    let that = this
    let call = function (d) {
      console.log(JSON.stringify(d))
      that.setState({ data: d, active: false }, () => {
        $(document.body).removeClass('open-right-sidebar')
      })
    }

    let props = { ...(this.state.data || {}), call: call }
    if (this.nodeType === 'start') renderRbcomp(<StartNodeConfig {...props} />, 'config-side')
    else if (this.nodeType === 'approver') renderRbcomp(<ApproverNodeConfig {...props} />, 'config-side')
    else if (this.nodeType === 'cc') renderRbcomp(<CCNodeConfig {...props} />, 'config-side')

    $(document.body).addClass('open-right-sidebar')
    this.setState({ active: true })
  }
  serialize() {
    return { type: this.props.type, id: this.props.nodeId, data: this.state.data }
  }
}
// 节点组规范
class NodeGroupSpec extends React.Component {
  constructor(props) {
    super(props)
    this.state = { ...props, nodes: props.nodes || [] }
    this.__nodeRefs = {}
  }
  renderNodes() {
    let nodes = (this.state.nodes || []).map((item) => {
      let props = { ...item, key: 'k-' + item.nodeId, $$$parent: this }
      if (item.type === 'condition') return <ConditionNode {...props} />
      else return <Node {...props} />
    })
    return nodes
  }
  onRef = (nodeRef, remove) => {
    let nodeId = nodeRef.props.nodeId
    this.__nodeRefs[nodeId] = (remove ? null : nodeRef)
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
      nodes.push(n)
      this.state.nodes.forEach((item) => {
        nodes.push(item)
      })
    }
    this.setState({ nodes: nodes }, () => {
      typeof call === 'function' && call(n.nodeId)
      hideDlgAddNode()
      if (isCanvasMounted && type !== 'condition') {
        setTimeout(() => {
          if (this.__nodeRefs[n.nodeId]) this.__nodeRefs[n.nodeId].openConfig()
        }, 200)
      }
    })
  }
  removeNode = (nodeId) => {
    let nodes = []
    this.state.nodes.forEach((item) => {
      if (nodeId !== item.nodeId) nodes.push(item)
    })
    this.setState({ nodes: nodes })
  }
  serialize() {
    let ns = this.state.nodes.map((item) => {
      return this.__nodeRefs[item.nodeId].serialize()
    })
    return { nodes: ns }
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
    let NT = NTs[this.nodeType]
    let data = this.state.data || {}
    let users = NT[2]
    if (data.users && data.users.length > 0) {
      if (data.users[0] === 'SELF') users = '发起人自己'
      else if (data.users[0] !== 'ALL') users = '指定用户 (' + data.users.length + ')'
    }
    if (this.nodeType === 'approver') users += ' ' + (data.signMode === 'AND' ? '会签' : (data.signMode === 'ALL' ? '依次审批' : '或签'))
    else if (this.nodeType === 'cc' && data.users && data.users.length > 0) users += ' ' + (data.selfSelecting === false ? '' : '且允许自选')

    return (<div className="node-wrap">
      <div className={`node-wrap-box ${NT[0]}-node animated fadeIn`}>
        <div className="title">
          <span>{data.nodeName || NT[1]}</span>
          {this.props.nodeId !== 'ROOT' && <i className="zmdi zmdi-close aclose" title="移除" onClick={this.removeNodeQuick} />}
        </div>
        <div className="content" onClick={this.openConfig}>
          <div className="text">{users}</div>
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
    this.state.branches = props.branches || [{ index: 1, nodeId: $random('COND') }, { index: 2, nodeId: $random('COND') }]
    this.branchIndex = this.state.branches.length + 1
    this.__branches = []
  }
  render() {
    let bLen = this.state.branches.length - 1
    return (bLen > 0 && <div className="branch-wrap">
      <div className="branch-box-wrap">
        <div className="branch-box">
          <button className="add-branch" onClick={this.addBranch}>添加分支</button>
          {this.state.branches.map((item, idx) => {
            return <ConditionBranch key={this.props.nodeId + '-b' + idx} isFirst={idx === 0} isLast={idx === bLen} $$$parent={this} {...item} />
          })}
        </div>
        <AddNodeButton addNodeCall={this.addNodeQuick} />
      </div>
    </div>)
  }
  onRef = (branchRef, remove) => {
    if (remove) this.__branches.remove(branchRef)
    else this.__branches.push(branchRef)
  }
  addBranch = () => {
    let bs = this.state.branches
    bs.push({ index: this.branchIndex++, nodeId: $random('COND') })
    this.setState({ branches: bs })
  }
  removeColumn = (nodeId, e) => {
    if (e) {
      e.stopPropagation()
      e.nativeEvent.stopImmediatePropagation()
    }
    let bs = []
    this.state.branches.forEach((item) => {
      if (nodeId !== item.nodeId) bs.push(item)
    })
    this.setState({ branches: bs })
  }
  serialize() {
    let bs = this.__branches.map((b) => {
      return b.serialize()
    })
    return { branches: bs }
  }
}

// 条件节点序列
class ConditionBranch extends NodeGroupSpec {
  constructor(props) {
    super(props)
  }
  render() {
    let data = this.state.data || {}
    let filters = data.filter ? data.filter.items.length : 0
    return (<div className="col-box">
      {this.state.isFirst && <div className="top-left-cover-line"></div>}
      {this.state.isFirst && <div className="bottom-left-cover-line"></div>}
      <div className="condition-node">
        <div className="condition-node-box animated fadeIn">
          <div className="auto-judge" onClick={this.openConfig}>
            <div className="title-wrapper">
              <span className="editable-title float-left">{data.nodeName || `分支条件${this.props.index}`}</span>
              <span className="priority-title float-right">默认优先级</span>
              <i className="zmdi zmdi-close aclose" title="移除" onClick={(e) => this.props.$$$parent.removeColumn(this.props.nodeId, e)} />
            </div>
            <div className="content">
              <div className="text">{filters > 0 ? `已设置条件 (${filters})` : '请设置条件'}</div>
              <i className="zmdi zmdi-chevron-right arrow"></i>
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
  componentDidMount() {
    this.props.$$$parent.onRef(this)
  }
  componentWillReceiveProps(props) {
    this.setState({ ...props, nodes: this.state.nodes })
  }
  addNode(type, depsNodeId) {
    super.addNode(type, depsNodeId || 'COND')
  }
  removeNode(nodeId) {
    super.removeNode(nodeId)
    this.props.$$$parent.onRef(this, true)
  }
  openConfig = () => {
    let that = this
    let call = function (d) {
      that.setState({ data: d, active: false }, () => {
        $(document.body).removeClass('open-right-sidebar')
      })
    }

    renderRbcomp(<ConditionNodeConfig entity={wpc.sourceEntity} call={call} data={this.state.data} />, 'config-side')

    $(document.body).addClass('open-right-sidebar')
    this.setState({ active: true })
  }
  serialize() {
    let s = super.serialize()
    s.condition = this.state.data
    return s
  }
}

// 画布
class RbFlowCanvas extends NodeGroupSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div className="box-scale">
      <Node type="start" $$$parent={this} nodeId="ROOT" ref={(c) => this._root = c} />
      {this.renderNodes()}
      <div className="end-node">
        <div className="end-node-circle"></div>
        <div className="end-node-text">流程结束</div>
      </div>
    </div>)
  }
  componentDidMount() {
    this.addNode('approver', null, (prevNodeId) => {
      this.addNode('cc', prevNodeId)
      setTimeout(() => isCanvasMounted = true, 400)
    })
    $('.box-scale').draggable({ cursor: 'move', axis: 'x', scroll: false })
    $('#rbflow').removeClass('rb-loading-active')

    $('.J_save').click(() => {
      let s = this.serialize()
      console.log(JSON.stringify(s))
    })
  }
  serialize() {
    let ns = super.serialize()
    ns.nodes.insert(0, this._root.serialize())
    return ns
  }
  let
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

// 发起人
class StartNodeConfig extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state.users = 'ALL'
    if (props.users) this.state.users = props.users[0] === 'ALL' ? 'ALL' : 'SPEC'
  }
  render() {
    return (<div>
      <div className="header"><h5>发起人</h5></div>
      <div className="form">
        <div className="form-group mb-0">
          <label className="text-bold">谁可以发起这个审批</label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="users" value="ALL" onChange={this.handleChange} checked={this.state.users === 'ALL'} />
            <span className="custom-control-label">所有人</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="users" value="SPEC" onChange={this.handleChange} checked={this.state.users === 'SPEC'} />
            <span className="custom-control-label">指定用户</span>
          </label>
        </div>
        {this.state.users === 'SPEC' && <div className="form-group">
          <UserSelector selected={this.state.selectedUsers} ref={(c) => this._users = c} />
        </div>}
      </div>
      {this.renderButton()}
    </div>)
  }
  renderButton() {
    return <div className="footer">
      <button type="button" className="btn btn-primary" onClick={this.save}>确定</button>
      <button type="button" className="btn btn-secondary" onClick={this.cancel}>取消</button>
    </div>
  }
  componentDidMount() {
    if (this.state.users === 'SPEC' && this.props.users) {
      $.post(`${rb.baseUrl}/commons/search/user-selector?entity=`, JSON.stringify(this.props.users), (res) => {
        if (res.data.length > 0) this.setState({ selectedUsers: res.data })
      })
    }
  }
  save = () => {
    let d = { nodeName: this.state.nodeName, users: this.state.users === 'ALL' ? ['ALL'] : this._users.getSelected() }
    if (d.users.length === 0) {
      rb.highbar('请选择用户')
      return
    }
    typeof this.props.call && this.props.call(d)
  }
  cancel = () => {
    $(document.body).removeClass('open-right-sidebar')
  }
}

// 审批人
class ApproverNodeConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
    this.state.signMode = props.signMode || 'OR'
    if (props.users && props.users[0] === 'SELF') this.state.users = 'SELF'
  }
  render() {
    return (<div>
      <div className="header">
        <input type="text" placeholder="审批人" data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
      </div>
      <div className="form">
        <div className="form-group mb-0">
          <label className="text-bold">由谁审批</label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="users" value="ALL" onChange={this.handleChange} checked={this.state.users === 'ALL'} />
            <span className="custom-control-label">发起人自选</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="users" value="SELF" onChange={this.handleChange} checked={this.state.users === 'SELF'} />
            <span className="custom-control-label">发起人自己</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="users" value="SPEC" onChange={this.handleChange} checked={this.state.users === 'SPEC'} />
            <span className="custom-control-label">指定审批人</span>
          </label>
        </div>
        {this.state.users === 'SPEC' && <div className="form-group">
          <UserSelector selected={this.state.selectedUsers} ref={(c) => this._users = c} />
        </div>}
        <div className="form-group mt-4">
          <label className="text-bold">当有多人审批时</label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="signMode" value="ALL" onChange={this.handleChange} checked={this.state.signMode === 'ALL'} />
            <span className="custom-control-label">依次审批</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="signMode" value="AND" onChange={this.handleChange} checked={this.state.signMode === 'AND'} />
            <span className="custom-control-label">会签（需所有审批人同意）</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="signMode" value="OR" onChange={this.handleChange} checked={this.state.signMode === 'OR'} />
            <span className="custom-control-label">或签（一名审批人同意或拒绝）</span>
          </label>
        </div>
      </div>
      {this.renderButton()}
    </div>)
  }
  save = () => {
    let d = { nodeName: this.state.nodeName, users: this.state.users === 'SPEC' ? this._users.getSelected() : [this.state.users], signMode: this.state.signMode }
    if (d.users.length === 0) {
      rb.highbar('请选择审批人')
      return
    }
    typeof this.props.call && this.props.call(d)
  }
}

// 抄送人
class CCNodeConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
    this.state.selfSelecting = true
    if (props.data && props.data.selfSelecting === false) this.state.selfSelecting = false
  }
  render() {
    return (<div>
      <div className="header">
        <input type="text" placeholder="抄送人" data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
      </div>
      <div className="form">
        <div className="form-group">
          <label className="text-bold">审批结果抄送给谁</label>
          <UserSelector selected={this.state.selectedUsers} ref={(c) => this._users = c} />
        </div>
        <div className="form-group mb-0">
          <label className="custom-control custom-control-sm custom-checkbox">
            <input className="custom-control-input" type="checkbox" name="selfSelecting" checked={this.state.selfSelecting} onChange={this.handleChange} />
            <span className="custom-control-label">同时允许发起人自选抄送人</span>
          </label>
        </div>
      </div>
      {this.renderButton()}
    </div >)
  }
  save = () => {
    let d = { nodeName: this.state.nodeName, users: this._users.getSelected(), selfSelecting: this.state.selfSelecting }
    if (d.users.length === 0 && !d.selfSelecting) {
      rb.highbar('请选择抄送人或允许自选抄送人')
      return
    }
    typeof this.props.call && this.props.call(d)
  }
}

// 条件
class ConditionNodeConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div>
      <div className="header">
        <input type="text" placeholder="分支条件" data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
      </div>
      <AdvFilter entity={this.props.entity} confirm={this.save} cancel={this.cancel} canNoFilters={true} />
    </div>)
  }
  save = (filter) => {
    let d = { nodeName: this.state.nodeName, filter: filter }
    typeof this.props.call && this.props.call(d)
  }
}