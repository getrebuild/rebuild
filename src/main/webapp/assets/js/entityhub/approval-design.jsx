/* eslint-disable react/prop-types */
var wpc = window.__PageConfig
let activeNode
$(document).ready(() => {
  if (!wpc || !wpc.configId) return
  // eslint-disable-next-line no-console
  if (rb.env === 'dev') console.log(wpc.flowDefinition)

  if (wpc.flowDefinition) wpc.flowDefinition = JSON.parse(wpc.flowDefinition)
  renderRbcomp(<RbFlowCanvas />, 'rbflow')
  $(document.body).click(function (e) {
    if (e.target && (e.target.matches('div.rb-right-sidebar') || $(e.target).parents('div.rb-right-sidebar').length > 0)) return
    $(this).removeClass('open-right-sidebar')
    if (activeNode) {
      activeNode.setState({ active: false })
      activeNode = null
    }
  })
  window.resize_handler()
})
window.resize_handler = function () {
  $('#rbflow').css('min-height', $(window).height() - 225)
}

// 画布准备完毕
let isCanvasMounted = false
// 节点类型
const NTs = {
  'start': ['start', '发起人', '记录所属人'],
  'approver': ['approver', '审批人', '自选审批人'],
  'cc': ['cc', '抄送人', '自选抄送人']
}
const UTs = {
  'ALL': '所有人',
  'OWNS': '记录所属人',
  'SELF': '发起人自己',
  'SPEC': '指定用户'
}
// 添加节点按钮
const AddNodeButton = function (props) {
  let c = function () { showDlgAddNode(props.addNodeCall) }
  return (<div className="add-node-btn-box"><div className="add-node-btn"><button type="button" onClick={c}><i className="zmdi zmdi-plus" /></button></div></div>)
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
    if (wpc.preview) return
    let that = this
    let call = function (d) {
      that.setState({ data: d, active: false })
    }
    let props = { ...(this.state.data || {}), call: call, key: 'kns-' + this.props.nodeId }
    if (this.nodeType === 'start') renderRbcomp(<StartNodeConfig {...props} />, 'config-side')
    else if (this.nodeType === 'approver') renderRbcomp(<ApproverNodeConfig {...props} />, 'config-side')
    else if (this.nodeType === 'cc') renderRbcomp(<CCNodeConfig {...props} />, 'config-side')

    $(document.body).addClass('open-right-sidebar')
    this.setState({ active: true })
    activeNode = this
  }
  serialize() {
    // 检查节点是否有必填设置
    if (this.nodeType === 'approver' || this.nodeType === 'cc') {
      let users = this.state.data ? this.state.data.users : ['']
      if (users[0] === 'SPEC') {
        RbHighbar.create(NTs[this.nodeType][2])
        this.setState({ hasError: true })
        return false
      } else this.setState({ hasError: false })
    }
    return { type: this.props.type, nodeId: this.props.nodeId, data: this.state.data }
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
      let props = { ...item, key: 'kn-' + item.nodeId, $$$parent: this }
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
    let ns = []
    for (let i = 0; i < this.state.nodes.length; i++) {
      let nodeRef = this.__nodeRefs[this.state.nodes[i].nodeId]
      let s = nodeRef.serialize()
      if (!s) return false
      ns.push(s)
    }
    return { nodes: ns, nodeId: this.state.nodeId }
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
    if (data.users && data.users.length > 0) users = UTs[data.users[0]] || ('指定用户(' + data.users.length + ')')
    if (data.selfSelecting && !users.contains('自选')) users += '/允许自选'
    if (this.nodeType === 'approver') users += ' ' + (data.signMode === 'AND' ? '会签' : (data.signMode === 'ALL' ? '依次审批' : '或签'))

    return (<div className="node-wrap">
      <div className={`node-wrap-box animated fadeIn ${NT[0]}-node ${this.state.hasError ? 'error' : ''} ${this.state.active ? 'active' : ''}`} title={rb.env === 'dev' ? this.props.nodeId : null}>
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
    this.state.branches = props.branches || [{ nodeId: $random('COND') }, { nodeId: $random('COND') }]
    this.__branchRefs = []
  }
  render() {
    let bLen = this.state.branches.length - 1
    return (bLen >= 0 && <div className="branch-wrap">
      <div className="branch-box-wrap">
        <div className="branch-box">
          <button className="add-branch" onClick={this.addBranch}>添加分支</button>
          {this.state.branches.map((item, idx) => {
            return <ConditionBranch key={'kcb-' + item.nodeId} priority={idx + 1} isFirst={idx === 0} isLast={idx === bLen} $$$parent={this} {...item} />
          })}
        </div>
        <AddNodeButton addNodeCall={this.addNodeQuick} />
      </div>
    </div>)
  }
  onRef = (branchRef, remove) => {
    let nodeId = branchRef.props.nodeId
    this.__branchRefs[nodeId] = (remove ? null : branchRef)
  }
  addBranch = () => {
    let bs = this.state.branches
    bs.push({ nodeId: $random('COND') })
    this.setState({ branches: bs })
  }
  removeBranch = (nodeId, e) => {
    if (e) {
      e.stopPropagation()
      e.nativeEvent.stopImmediatePropagation()
    }
    let bs = []
    this.state.branches.forEach((item) => {
      if (nodeId !== item.nodeId) bs.push(item)
    })
    this.setState({ branches: bs }, () => {
      if (bs.length === 0) {
        this.props.$$$parent.removeNode(this.props.nodeId)
      }
    })
  }
  serialize() {
    let holdANode = null
    let bs = []
    for (let i = 0; i < this.state.branches.length; i++) {
      let branchRef = this.__branchRefs[this.state.branches[i].nodeId]
      if (!holdANode) holdANode = branchRef
      let s = branchRef.serialize()
      if (!s) return false
      bs.push(s)
    }
    // 至少两个分支
    if (bs.length < 2) {
      RbHighbar.create('请至少设置两个并列条件分支')
      if (holdANode) holdANode.setState({ hasError: true })
      return false
    } else if (holdANode) holdANode.setState({ hasError: false })
    return { branches: bs, type: 'condition', nodeId: this.props.nodeId }
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
        <div className="condition-node-box animated fadeIn" title={rb.env === 'dev' ? this.props.nodeId : null}>
          <div className={`auto-judge ${this.state.hasError ? 'error' : ''} ${this.state.active ? 'active' : ''}`} onClick={this.openConfig}>
            <div className="title-wrapper">
              <span className="editable-title float-left">{data.nodeName || '分支条件'}</span>
              <span className="priority-title float-right">默认优先级</span>
              <i className="zmdi zmdi-close aclose" title="移除" onClick={(e) => this.props.$$$parent.removeBranch(this.props.nodeId, e)} />
              <div className="clearfix"></div>
            </div>
            <div className="content">
              <div className="text">{this.state.isLast ? '其他条件' : (filters > 0 ? `已设置条件 (${filters})` : '请设置条件')}</div>
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
  UNSAFE_componentWillReceiveProps(props) {
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
    if (wpc.preview) return
    let that = this
    let call = function (d) {
      that.setState({ data: d, active: false })
    }
    let props = { ...(this.state.data || {}), entity: wpc.applyEntity, call: call }
    renderRbcomp(<ConditionBranchConfig key={'kcbc-' + this.props.nodeId} {...props} isLast={this.state.isLast} />, 'config-side')

    $(document.body).addClass('open-right-sidebar')
    this.setState({ active: true })
    activeNode = this
  }
  serialize() {
    let s = super.serialize()
    if (!s || s.nodes.length === 0) {
      this.setState({ hasError: true })
      if (s !== false) RbHighbar.create('请为分支添加审批人或抄送人')
      return false
    } else this.setState({ hasError: false })

    s.priority = this.props.priority
    if (this.state.data) s.data = this.state.data
    return s
  }
}

// 添加节点
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
  else renderRbcomp(<DlgAddNode call={call} />, null, function () { __DlgAddNode = this })
}
const hideDlgAddNode = function () {
  if (__DlgAddNode) __DlgAddNode.hide()
}

// 发起人
class StartNodeConfig extends RbFormHandler {
  constructor(props) {
    super(props)
    this.state.users = (props.users || ['OWNS'])[0]
    if (!UTs[this.state.users]) this.state.users = 'SPEC'

    if (props.selfSelecting === false) this.state.selfSelecting = false
    else this.state.selfSelecting = true
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
            <input className="custom-control-input" type="radio" name="users" value="OWNS" onChange={this.handleChange} checked={this.state.users === 'OWNS'} />
            <span className="custom-control-label">记录所属人</span>
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
    let d = {
      nodeName: this.state.nodeName,
      users: this.state.users === 'SPEC' ? this._users.getSelected() : [this.state.users]
    }
    if (this.state.users === 'SPEC' && d.users.length === 0) {
      RbHighbar.create('请选择用户')
      return
    }
    typeof this.props.call && this.props.call(d)
    this.cancel()
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
    this.state.users = (props.users || ['SPEC'])[0]
    if (!this.state.users || this.state.users.length === 20) this.state.users = 'SPEC'
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
            <input className="custom-control-input" type="radio" name="users" value="SELF" onChange={this.handleChange} checked={this.state.users === 'SELF'} />
            <span className="custom-control-label">发起人自己</span>
          </label>
          <label className="custom-control custom-control-sm custom-radio mb-2">
            <input className="custom-control-input" type="radio" name="users" value="SPEC" onChange={this.handleChange} checked={this.state.users === 'SPEC'} />
            <span className="custom-control-label">指定审批人</span>
          </label>
        </div>
        {this.state.users === 'SPEC' && <div className="form-group mb-3">
          <UserSelector selected={this.state.selectedUsers} ref={(c) => this._users = c} />
        </div>}
        <div className="form-group mb-0">
          <label className="custom-control custom-control-sm custom-checkbox">
            <input className="custom-control-input" type="checkbox" name="selfSelecting" checked={this.state.selfSelecting} onChange={this.handleChange} />
            <span className="custom-control-label">同时允许自选</span>
          </label>
        </div>
        <div className="form-group mt-4">
          <label className="text-bold">当有多人审批时</label>
          <label className="custom-control custom-control-sm custom-radio mb-2 hide">
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
    let d = {
      nodeName: this.state.nodeName,
      users: this.state.users === 'SPEC' ? this._users.getSelected() : [this.state.users],
      signMode: this.state.signMode,
      selfSelecting: this.state.selfSelecting
    }
    if (d.users.length === 0 && !d.selfSelecting) {
      RbHighbar.create('请选择审批人或允许自选')
      return
    }
    typeof this.props.call && this.props.call(d)
    this.cancel()
  }
}

// 抄送人
class CCNodeConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div>
      <div className="header">
        <input type="text" placeholder="抄送人" data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
      </div>
      <div className="form">
        <div className="form-group mb-3">
          <label className="text-bold">审批结果抄送给谁</label>
          <UserSelector selected={this.state.selectedUsers} ref={(c) => this._users = c} />
        </div>
        <div className="form-group mb-0">
          <label className="custom-control custom-control-sm custom-checkbox">
            <input className="custom-control-input" type="checkbox" name="selfSelecting" checked={this.state.selfSelecting} onChange={this.handleChange} />
            <span className="custom-control-label">同时允许自选</span>
          </label>
        </div>
      </div>
      {this.renderButton()}
    </div>)
  }
  save = () => {
    let d = {
      nodeName: this.state.nodeName,
      users: this._users.getSelected(),
      selfSelecting: this.state.selfSelecting
    }
    if (d.users.length === 0 && !d.selfSelecting) {
      RbHighbar.create('请选择抄送人或允许自选')
      return
    }
    typeof this.props.call && this.props.call(d)
    this.cancel()
  }
}

// 条件
class ConditionBranchConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div>
      <div className="header">
        <input type="text" placeholder="分支条件" data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
      </div>
      {this.state.isLast && <div className="alert alert-warning">该条件分支将作为最终分支匹配其他条件</div>}
      <AdvFilter filter={this.state.filter} entity={this.props.entity} confirm={this.save} cancel={this.cancel} canNoFilters={true} />
    </div>)
  }
  save = (filter) => {
    let d = { nodeName: this.state.nodeName, filter: filter }
    typeof this.props.call && this.props.call(d)
    this.cancel()
  }
}

// 画布
class RbFlowCanvas extends NodeGroupSpec {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div>
      <div className="zoom">
        <a className="zoom-in" onClick={() => this.zoom(10)}><i className="zmdi zmdi-plus" /></a>
        {this.state.zoomValue || 100}%
        <a className="zoom-out" onClick={() => this.zoom(-10)}><i className="zmdi zmdi-minus" /></a>
      </div>
      <div className={'box-scale' + (wpc.preview ? ' preview' : '')} style={this.state.zoomStyle}>
        <Node type="start" $$$parent={this} nodeId="ROOT" ref={(c) => this._root = c} />
        {this.renderNodes()}
        <div className="end-node">
          <div className="end-node-circle"></div>
          <div className="end-node-text">流程结束</div>
        </div>
      </div>
    </div>)
  }
  componentDidMount() {
    if (wpc.flowDefinition) {
      let flowNodes = wpc.flowDefinition.nodes
      this._root.setState({ data: flowNodes[0].data })
      flowNodes.remove(flowNodes[0])
      this.setState({ nodes: flowNodes }, () => {
        isCanvasMounted = true
      })
    } else {
      isCanvasMounted = true
    }

    $('.box-scale').draggable({ cursor: 'move', axis: 'x', scroll: false })
    $('#rbflow').removeClass('rb-loading-active')

    let _btn = $('.J_save').click(() => {
      let s = this.serialize()
      if (!s) return
      let _data = { flowDefinition: s }
      _data.metadata = { entity: 'RobotApprovalConfig', id: wpc.configId }

      _btn.button('loading')
      $.post(`${rb.baseUrl}/app/entity/record-save`, JSON.stringify(_data), (res) => {
        if (res.error_code === 0) {
          RbAlert.create('保存并发布成功', {
            type: 'primary',
            cancelText: '返回列表',
            cancel: () => location.replace('../approvals'),
            confirmText: '继续编辑',
            confirm: () => location.reload()
          })
        } else RbHighbar.error(res.error_msg)
        _btn.button('reset')
      })
    })

    $('.J_copy').click(() => {
      renderRbcomp(<DlgCopy father={wpc.configId} name={wpc.name + '(2)'} isDisabled={true} />)
    })
  }
  zoom(v) {
    let zv = (this.state.zoomValue || 100) + v
    if (zv < 20) zv = 20
    else if (zv > 200) zv = 200
    this.setState({ zoomValue: zv, zoomStyle: { transform: `scale(${zv / 100})` } })
  }
  serialize() {
    let ns = super.serialize()
    if (!ns) return false
    ns.nodes.insert(0, this._root.serialize())
    return ns
  }
}


class DlgCopy extends RbFormHandler {
  constructor(props) {
    super(props)
  }
  render() {
    return (<RbModal title="另存为" ref={(c) => this._dlg = c} disposeOnHide={true}>
      <div className="form">
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">新流程名称</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        <div className="form-group row">
          <div className="col-sm-7 offset-sm-3">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
              <span className="custom-control-label">同时禁用当前流程</span>
            </label>
          </div>
        </div>
        <div className="form-group row footer">
          <div className="col-sm-7 offset-sm-3" ref={(c) => this._btns = c}>
            <button className="btn btn-primary" type="button" onClick={this.save}>确定</button>
          </div>
        </div>
      </div>
    </RbModal>)
  }
  save = () => {
    let approvalName = this.state.name
    if (!approvalName) { RbHighbar.create('请输入新流程名称'); return }
    let _btns = $(this._btns).find('.btn').button('loading')
    $.post(`${rb.baseUrl}/admin/robot/approval/copy?father=${this.props.father}&disabled=${this.state.isDisabled}&name=${$encode(approvalName)}`, (res) => {
      if (res.error_code === 0) {
        RbHighbar.success('另存为成功')
        setTimeout(() => location.replace('./' + res.data.approvalId), 500)
      } else RbHighbar.error(res.error_msg)
      _btns.button('reset')
    })
  }
}