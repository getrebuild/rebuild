/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const wpc = window.__PageConfig
let fieldsCache
let activeNode
let donotCloseSidebar

$(document).ready(() => {
  if (!wpc || !wpc.configId) return
  if (rb.env === 'dev') console.log(wpc.flowDefinition)

  // 备用
  $.get(`/commons/metadata/fields?entity=${wpc.applyEntity}`, (res) => (fieldsCache = res.data))

  renderRbcomp(<RbFlowCanvas />, 'rbflow')

  $(document.body).on('click', function (e) {
    if (donotCloseSidebar) return
    const $target = $(e.target)
    if (e.target && ($target.hasClass('rb-right-sidebar') || $target.parents('div.rb-right-sidebar').length > 0)) return

    $(this).removeClass('open-right-sidebar')
    if (activeNode) {
      activeNode.setState({ active: false })
      activeNode = null
    }
  })
})

// 画布准备完毕
let isCanvasMounted = false
// 节点类型
const NTs = {
  start: ['start', $L('发起人'), $L('记录所属用户')],
  approver: ['approver', $L('审批人'), $L('自选审批人')],
  cc: ['cc', $L('抄送人'), $L('自选抄送人')],
}
// 人员类型
const UTs = {
  ALL: $L('所有人'),
  OWNS: $L('记录所属用户'),
  SELF: $L('发起人自己'),
  SPEC: $L('指定用户'),
}
// 添加节点按钮
const AddNodeButton = function (props) {
  return (
    <div className="add-node-btn-box">
      <div className="add-node-btn">
        <button
          type="button"
          onClick={() => {
            showDlgAddNode(props.addNodeCall)
          }}>
          <i className="zmdi zmdi-plus" />
        </button>
      </div>
    </div>
  )
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

    const that = this
    const call = function (d) {
      that.setState({ data: d, active: false })
    }
    const props = { ...(this.state.data || {}), call: call, key: 'kns-' + this.props.nodeId }

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
      const users = this.state.data ? this.state.data.users : ['']
      if (users[0] === 'SPEC') {
        RbHighbar.create(NTs[this.nodeType][2])
        this.setState({ hasError: true })
        return false
      } else {
        this.setState({ hasError: false })
      }
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
    return (this.state.nodes || []).map((item) => {
      const props = { ...item, key: 'kn-' + item.nodeId, $$$parent: this }
      if (item.type === 'condition') return <ConditionNode {...props} />
      else return <SimpleNode {...props} />
    })
  }

  onRef = (nodeRef, remove) => {
    const nodeId = nodeRef.props.nodeId
    this.__nodeRefs[nodeId] = remove ? null : nodeRef
  }

  addNode = (type, depsNodeId, call) => {
    const n = { type: type, nodeId: $random(type === 'condition' ? 'COND' : 'NODE') }
    const nodes = []
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
    const nodes = []
    this.state.nodes.forEach((item) => {
      if (nodeId !== item.nodeId) nodes.push(item)
    })
    this.setState({ nodes: nodes })
  }

  serialize() {
    const ns = []
    for (let i = 0; i < this.state.nodes.length; i++) {
      const nodeRef = this.__nodeRefs[this.state.nodes[i].nodeId]
      const s = nodeRef.serialize()
      if (!s) return false
      ns.push(s)
    }
    return { nodes: ns, nodeId: this.state.nodeId }
  }
}

// 画布:节点 1:N

// 一般节点
class SimpleNode extends NodeSpec {
  constructor(props) {
    super(props)
    this.nodeType = props.type || 'approver'
  }

  render() {
    const NT = NTs[this.nodeType]
    const data = this.state.data || {}
    const descs = data.users && data.users.length > 0 ? [UTs[data.users[0]] || `${$L('指定用户')}(${data.users.length})`] : [NT[2]]

    if (data.selfSelecting && data.users.length > 0) descs.push($L('允许自选'))
    if (data.ccAutoShare) descs.push($L('自动共享'))
    if (data.accounts && data.accounts.length > 0) descs.push(`${$L('外部人员')}(${data.accounts.length})`)
    if (data.allowReferral) descs.push($L('允许转审'))
    if (data.allowCountersign) descs.push($L('允许加签'))
    if (this.nodeType === 'approver') descs.push(data.signMode === 'AND' ? $L('会签') : data.signMode === 'ALL' ? $L('依次审批') : $L('或签'))

    return (
      <div className="node-wrap">
        <div className={`node-wrap-box animated fadeIn ${NT[0]}-node ${this.state.hasError ? 'error' : ''} ${this.state.active ? 'active' : ''}`} data-node={this.props.nodeId}>
          <div className="title">
            <span>{data.nodeName || NT[1]}</span>
            {this.props.nodeId !== 'ROOT' && <i className="zmdi zmdi-close aclose" title={$L('移除')} onClick={this.removeNodeQuick} />}
          </div>
          <div className="content" onClick={this.openConfig}>
            <div className="text">{descs.join(' / ')}</div>
            <i className="zmdi zmdi-chevron-right arrow" />
          </div>
        </div>
        <AddNodeButton addNodeCall={this.addNodeQuick} />
      </div>
    )
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
    const branchIdx = this.state.branches.length - 1
    return (
      branchIdx >= 0 && (
        <div className="branch-wrap">
          <div className="branch-box-wrap">
            <div className="branch-box">
              <button className="add-branch" onClick={this.addBranch}>
                {$L('添加分支')}
              </button>
              {this.state.branches.map((item, idx) => {
                return <ConditionBranch key={item.nodeId} priority={idx + 1} isFirst={idx === 0} isLast={idx === branchIdx} $$$parent={this} {...item} />
              })}
            </div>
            <AddNodeButton addNodeCall={this.addNodeQuick} />
          </div>
        </div>
      )
    )
  }

  onRef = (branchRef, remove) => {
    const nodeId = branchRef.props.nodeId
    this.__branchRefs[nodeId] = remove ? null : branchRef
  }

  addBranch = () => {
    const bs = this.state.branches
    bs.push({ nodeId: $random('COND') })
    this.setState({ branches: bs })
  }

  removeBranch = (nodeId, e) => {
    $stopEvent(e)

    const bs = []
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
    const bs = []
    for (let i = 0; i < this.state.branches.length; i++) {
      let branchRef = this.__branchRefs[this.state.branches[i].nodeId]
      if (!holdANode) holdANode = branchRef

      let s = branchRef.serialize()
      if (!s) return false
      bs.push(s)
    }

    // 至少两个分支
    if (bs.length < 2) {
      RbHighbar.create($L('请至少设置两个并列条件分支'))
      if (holdANode) holdANode.setState({ hasError: true })
      return false
    } else if (holdANode) {
      holdANode.setState({ hasError: false })
    }

    return { branches: bs, type: 'condition', nodeId: this.props.nodeId }
  }
}

// 条件节点序列
class ConditionBranch extends NodeGroupSpec {
  constructor(props) {
    super(props)
  }

  render() {
    const data = this.state.data || {}
    const filters = data.filter ? data.filter.items.length : 0

    return (
      <div className="col-box">
        {this.state.isFirst && <div className="top-left-cover-line" />}
        {this.state.isFirst && <div className="bottom-left-cover-line" />}
        <div className="condition-node">
          <div className="condition-node-box animated fadeIn" data-node={this.props.nodeId}>
            <div className={`auto-judge ${this.state.hasError ? 'error' : ''} ${this.state.active ? 'active' : ''}`} onClick={this.openConfig}>
              <div className="title-wrapper">
                <span className="editable-title float-left">{data.nodeName || $L('分支条件')}</span>
                <span className="priority-title float-right">{$L('默认优先级')}</span>
                <i className="zmdi zmdi-close aclose" title={$L('移除')} onClick={(e) => this.props.$$$parent.removeBranch(this.props.nodeId, e)} />
                <div className="clearfix" />
              </div>
              <div className="content">
                <div className="text">{this.state.isLast ? $L('其他条件') : filters > 0 ? `${$L('已设置条件')} (${filters})` : $L('请设置条件')}</div>
                <i className="zmdi zmdi-chevron-right arrow" />
              </div>
            </div>
            <AddNodeButton addNodeCall={this.addNode} />
          </div>
        </div>
        {this.renderNodes()}
        {this.state.isLast && (
          <React.Fragment>
            <div className="top-right-cover-line" />
            <div className="bottom-right-cover-line" />
          </React.Fragment>
        )}
      </div>
    )
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

    const that = this
    const call = function (d) {
      that.setState({ data: d, active: false })
    }
    const props = { ...(this.state.data || {}), entity: wpc.applyEntity, call: call }

    renderRbcomp(<ConditionBranchConfig key={'kcbc-' + this.props.nodeId} {...props} isLast={this.state.isLast} />, 'config-side')

    $(document.body).addClass('open-right-sidebar')
    this.setState({ active: true })
    activeNode = this
  }

  serialize() {
    const s = super.serialize()
    if (!s || s.nodes.length === 0) {
      this.setState({ hasError: true })
      if (s !== false) RbHighbar.create($L('请为分支添加审批人或抄送人'))
      return false
    } else {
      this.setState({ hasError: false })
    }

    s.priority = this.props.priority
    if (this.state.data) s.data = this.state.data
    return s
  }
}

// 添加节点
class DlgAddNode extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      call:
        props.call ||
        function (t) {
          alert(t)
        },
    }
  }

  render() {
    return (
      <div className="modal add-node" tabIndex="-1" ref={(c) => (this._dlg = c)}>
        <div className="modal-dialog modal-dialog-centered">
          <div className="modal-content">
            <div className="modal-header pb-0">
              <button className="close" type="button" onClick={() => this.hide()}>
                <span className="zmdi zmdi-close" />
              </button>
            </div>
            <div className="modal-body">
              <div className="row">
                <div className="col-4">
                  <a className="approver" onClick={() => this.state.call('approver')}>
                    <div>
                      <i className="zmdi zmdi-account" />
                    </div>
                    <p>{$L('审批人')}</p>
                  </a>
                </div>
                <div className="col-4">
                  <a className="cc" onClick={() => this.state.call('cc')}>
                    <div>
                      <i className="zmdi zmdi-mail-send" />
                    </div>
                    <p>{$L('抄送人')}</p>
                  </a>
                </div>
                <div className="col-4">
                  <a className="condition" onClick={() => this.state.call('condition')}>
                    <div>
                      <i className="mdi mdi-source-merge" />
                    </div>
                    <p>{$L('条件分支')}</p>
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
  if (__DlgAddNode) {
    __DlgAddNode.show(call)
  } else {
    renderRbcomp(<DlgAddNode call={call} />, null, function () {
      __DlgAddNode = this
    })
  }
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
    this.state.selfSelecting = props.selfSelecting !== false
    this.state.submitFilter = props.filter || null
  }

  render() {
    const hasFilters = this.state.submitFilter ? this.state.submitFilter.items.length : 0

    return (
      <div>
        <div className="header">
          <h5>{$L('发起人')}</h5>
        </div>
        <div className="form">
          <div className="form-group mb-0">
            <label className="text-bold">{$L('谁可以发起这个审批')}</label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="ALL" onChange={this.handleChange} checked={this.state.users === 'ALL'} />
              <span className="custom-control-label">{$L('所有人')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="OWNS" onChange={this.handleChange} checked={this.state.users === 'OWNS'} />
              <span className="custom-control-label">{$L('记录所属用户')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="SPEC" onChange={this.handleChange} checked={this.state.users === 'SPEC'} />
              <span className="custom-control-label">{$L('指定用户')}</span>
            </label>
          </div>
          <div className={`form-group ${this.state.users === 'SPEC' ? '' : 'hide'}`}>
            <UserSelector ref={(c) => (this._UserSelector = c)} />
          </div>

          <div className="form-group mt-4 mb-0">
            <label className="text-bold mb-1">{$L('发起条件')}</label>
            <div className="">
              <a className="btn btn-sm btn-link pl-0 text-left" onClick={() => this._handleFilter()}>
                {hasFilters > 0 ? `${$L('已设置条件')} (${hasFilters})` : $L('点击设置')}
              </a>
              <p className="form-text m-0">{$L('符合条件的记录才可以使用/选择此流程')}</p>
            </div>
          </div>
        </div>

        {this.renderButton()}
      </div>
    )
  }

  renderButton() {
    return (
      <div className="footer">
        <button type="button" className="btn btn-primary" onClick={this.save}>
          {$L('确定')}
        </button>
        <button type="button" className="btn btn-secondary" onClick={this.cancel}>
          {$L('取消')}
        </button>
      </div>
    )
  }

  componentDidMount() {
    if (this.state.users === 'SPEC' && this.props.users) {
      $.post(`/admin/robot/approval/user-fields-show?entity=${this.props.entity || wpc.applyEntity}`, JSON.stringify(this.props.users), (res) => {
        if ((res.data || []).length > 0) this._UserSelector.setState({ selected: res.data })
      })
    }
  }

  _handleFilter() {
    donotCloseSidebar = true

    if (this._AdvFilter) {
      this._AdvFilter.show()
    } else {
      const that = this
      renderRbcomp(
        <AdvFilter
          entity={wpc.applyEntity}
          filter={that.state.submitFilter}
          onConfirm={(f) => {
            donotCloseSidebar = false
            that.setState({ submitFilter: f })
          }}
          onCancel={() => (donotCloseSidebar = false)}
          canNoFilters
          inModal
          title={$L('发起条件')}
        />,
        null,
        function () {
          that._AdvFilter = this
        }
      )
    }
  }

  save = () => {
    const d = {
      nodeName: this.state.nodeName,
      users: this.state.users === 'SPEC' ? this._UserSelector.getSelected() : [this.state.users],
      filter: this.state.submitFilter,
    }
    if (this.state.users === 'SPEC' && d.users.length === 0) {
      RbHighbar.create($L('请选择用户'))
      return
    }

    typeof this.props.call === 'function' && this.props.call(d)
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
    if (!props.users || props.users.length === 0) this.state.users = 'SPEC'
    else if (props.users[0] === 'SELF') this.state.users = 'SELF'
    else this.state.users = 'SPEC'
  }

  render() {
    return (
      <div>
        <div className="header">
          <input type="text" placeholder={$L('审批人')} data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
          <i className="zmdi zmdi-edit icon" />
        </div>
        <div className="form rb-scroller">
          <div className="form-group mb-0">
            <label className="text-bold">{$L('由谁审批')}</label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="SELF" onChange={this.handleChange} checked={this.state.users === 'SELF'} />
              <span className="custom-control-label">{$L('发起人自己')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="users" value="SPEC" onChange={this.handleChange} checked={this.state.users === 'SPEC'} />
              <span className="custom-control-label">{$L('指定审批人')}</span>
            </label>
          </div>
          <div className={`form-group mb-3 ${this.state.users === 'SPEC' ? '' : 'hide'}`}>
            <UserSelectorWithField ref={(c) => (this._UserSelector = c)} />
          </div>
          <div className="form-group">
            <label className="custom-control custom-control-sm custom-checkbox">
              <input className="custom-control-input" type="checkbox" name="selfSelecting" checked={this.state.selfSelecting === true} onChange={this.handleChange} />
              <span className="custom-control-label">{$L('同时允许自选')}</span>
            </label>
          </div>

          <div className="form-group mb-0">
            <label className="custom-control custom-control-sm custom-checkbox mb-2">
              <input className="custom-control-input" type="checkbox" name="allowReferral" checked={this.state.allowReferral === true} onChange={this.handleChange} />
              <span className="custom-control-label">
                {$L('允许审批人转审')} <sup className="rbv" />
              </span>
            </label>
          </div>
          <div className="form-group mb-0">
            <label className="custom-control custom-control-sm custom-checkbox">
              <input className="custom-control-input" type="checkbox" name="allowCountersign" checked={this.state.allowCountersign === true} onChange={this.handleChange} />
              <span className="custom-control-label">
                {$L('允许审批人加签')} <sup className="rbv" />
              </span>
            </label>
          </div>

          <div className="form-group mt-4">
            <label className="text-bold">{$L('当有多人审批时')}</label>
            <label className="custom-control custom-control-sm custom-radio mb-2 hide">
              <input className="custom-control-input" type="radio" name="signMode" value="ALL" onChange={this.handleChange} checked={this.state.signMode === 'ALL'} />
              <span className="custom-control-label">{$L('依次审批')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="signMode" value="AND" onChange={this.handleChange} checked={this.state.signMode === 'AND'} />
              <span className="custom-control-label">{$L('会签 (需所有审批人同意)')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-radio mb-2">
              <input className="custom-control-input" type="radio" name="signMode" value="OR" onChange={this.handleChange} checked={this.state.signMode === 'OR'} />
              <span className="custom-control-label">{$L('或签 (一名审批人同意或拒绝)')}</span>
            </label>
          </div>

          <div className="form-group mt-4 bosskey-show" title="FIXME 默认启用无需配置">
            <label className="text-bold">{$L('驳回时')}</label>
            <label className="custom-control custom-control-sm custom-checkbox">
              <input className="custom-control-input" type="checkbox" name="rejectStep" checked={this.state.rejectStep !== false} onChange={this.handleChange} />
              <span className="custom-control-label">{$L('允许退回到指定步骤 (否则为整体驳回)')}</span>
            </label>
          </div>

          <div className="form-group mt-4">
            <label className="text-bold">{$L('可修改字段')}</label>
            <div style={{ position: 'relative' }}>
              <table className={`table table-sm fields-table ${(this.state.editableFields || []).length === 0 && 'hide'}`}>
                <tbody ref={(c) => (this._editableFields = c)}>
                  {(this.state.editableFields || []).map((item) => {
                    return (
                      <tr key={`field-${item.field}`}>
                        <td>{this.__fieldLabel(item.field)}</td>
                        <td width="100">
                          <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
                            <input className="custom-control-input" type="checkbox" name="notNull" defaultChecked={item.notNull === true} data-field={item.field} />
                            <span className="custom-control-label">{$L('必填')}</span>
                          </label>
                        </td>
                        <td width="40">
                          <a className="close" title={$L('移除')} onClick={() => this.removeEditableField(item.field)}>
                            <i className="zmdi icon zmdi-close" />
                          </a>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              <div className="pb-4">
                <button className="btn btn-secondary btn-sm" onClick={() => renderRbcomp(<DlgFields selected={this.state.editableFields} call={(fs) => this.setEditableFields(fs)} />)}>
                  + {$L('选择字段')}
                </button>
              </div>
            </div>
          </div>
        </div>
        {this.renderButton()}
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    const h = $('#config-side').height() - 120
    $('#config-side .form.rb-scroller').height(h).perfectScrollbar()

    $(this._editableFields)
      .sortable({
        cursor: 'move',
        axis: 'y',
        width: '100%',
      })
      .disableSelection()
  }

  save = () => {
    const editableFields = []
    $(this._editableFields)
      .find('input')
      .each(function () {
        const $this = $(this)
        editableFields.push({ field: $this.data('field'), notNull: $this.prop('checked') })
      })

    const d = {
      nodeName: this.state.nodeName,
      users: this.state.users === 'SPEC' ? this._UserSelector.getSelected() : [this.state.users],
      signMode: this.state.signMode,
      selfSelecting: this.state.selfSelecting,
      editableFields: editableFields,
      rejectStep: this.state.rejectStep,
      allowReferral: this.state.allowReferral,
      allowCountersign: this.state.allowCountersign,
    }

    if (d.users.length === 0 && !d.selfSelecting) {
      RbHighbar.create($L('请选择审批人或允许自选'))
      return
    }

    if (rb.commercial < 1 && (d.allowReferral || d.allowCountersign)) {
      RbHighbar.error(WrapHtml($L('免费版不支持转审/加签功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    typeof this.props.call === 'function' && this.props.call(d)
    this.cancel()
  }

  setEditableFields(fs) {
    const fsNew = fs.map((item) => {
      return { field: item, notNull: false }
    })
    this.setState({ editableFields: fsNew })
  }

  removeEditableField(field) {
    const fs = []
    this.state.editableFields.forEach((item) => {
      if (item.field !== field) fs.push(item)
    })
    this.setState({ editableFields: fs })
  }

  __fieldLabel(name) {
    const field = fieldsCache.find((x) => x.name === name)
    return field ? field.label : `[${name.toUpperCase()}]`
  }
}

// 抄送人
class CCNodeConfig extends StartNodeConfig {
  render() {
    return (
      <div>
        <div className="header">
          <input type="text" placeholder={$L('抄送人')} data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
          <i className="zmdi zmdi-edit icon" />
        </div>
        <div className="form">
          <div className="form-group mb-3">
            <label className="text-bold">{$L('审批结果抄送给谁')}</label>
            <UserSelectorWithField ref={(c) => (this._UserSelector = c)} />
          </div>
          <div className="form-group mb-0">
            <label className="custom-control custom-control-sm custom-checkbox mb-2">
              <input className="custom-control-input" type="checkbox" name="selfSelecting" checked={this.state.selfSelecting === true} onChange={(e) => this.handleChange(e)} />
              <span className="custom-control-label">{$L('同时允许自选')}</span>
            </label>
            <label className="custom-control custom-control-sm custom-checkbox">
              <input className="custom-control-input" type="checkbox" name="ccAutoShare" checked={this.state.ccAutoShare === true} onChange={(e) => this.handleChange(e)} />
              <span className="custom-control-label">{$L('抄送人无读取权限时自动共享')}</span>
            </label>
          </div>

          <div className="form-group mt-3">
            <label className="text-bold">
              {$L('抄送给外部人员')} <sup className="rbv" />
            </label>
            <UserSelectorWithField ref={(c) => (this._UserSelector2 = c)} userType={2} hideUser hideDepartment hideRole hideTeam />
            <p className="form-text">{$L('选择外部人员的电话 (手机) 或邮箱字段')}</p>
          </div>
        </div>

        {this.renderButton()}
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    if ((this.props.accounts || []).length > 0) {
      $.post(`/commons/search/user-selector?entity=${this.props.entity || wpc.applyEntity}`, JSON.stringify(this.props.accounts), (res) => {
        if (res.error_code === 0 && res.data.length > 0) {
          this._UserSelector2.setState({ selected: res.data })
        }
      })
    }
  }

  save = () => {
    const d = {
      nodeName: this.state.nodeName,
      users: this._UserSelector.getSelected(),
      selfSelecting: this.state.selfSelecting,
      ccAutoShare: this.state.ccAutoShare,
      accounts: this._UserSelector2.getSelected(),
    }

    if (d.accounts.length > 1 && rb.commercial < 1) {
      RbHighbar.error(WrapHtml($L('免费版不支持抄送给外部人员功能 [(查看详情)](https://getrebuild.com/docs/rbv-features)')))
      return
    }

    if (d.users.length === 0 && !d.selfSelecting) {
      RbHighbar.create($L('请选择抄送人或允许自选'))
      return
    }

    typeof this.props.call === 'function' && this.props.call(d)
    this.cancel()
  }
}

// 条件
class ConditionBranchConfig extends StartNodeConfig {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div>
        <div className="header">
          <input type="text" placeholder={$L('分支条件')} data-id="nodeName" value={this.state.nodeName || ''} onChange={this.handleChange} maxLength="20" />
          <i className="zmdi zmdi-edit icon" />
        </div>
        {this.state.isLast && <div className="alert alert-warning">{$L('该分支将作为最终分支匹配其他条件')}</div>}
        <AdvFilter filter={this.state.filter} entity={this.props.entity} onConfirm={this.save} onCancel={this.cancel} canNoFilters />
      </div>
    )
  }

  save = (filter) => {
    const d = { nodeName: this.state.nodeName, filter: filter }
    typeof this.props.call === 'function' && this.props.call(d)
    this.cancel()
  }
}

// 画布
class RbFlowCanvas extends NodeGroupSpec {
  constructor(props) {
    super(props)
  }

  render() {
    return (
      <div>
        <div className="zoom">
          <a className="zoom-in" onClick={() => this.zoom(10)}>
            <i className="zmdi zmdi-plus" />
          </a>
          {this.state.zoomValue || 100}%
          <a className="zoom-out" onClick={() => this.zoom(-10)}>
            <i className="zmdi zmdi-minus" />
          </a>
        </div>
        <div className={'box-scale' + (wpc.preview ? ' preview' : '')} style={this.state.zoomStyle}>
          <SimpleNode type="start" $$$parent={this} nodeId="ROOT" ref={(c) => (this._root = c)} />
          {this.renderNodes()}
          <div className="end-node">
            <div className="end-node-circle" />
            <div className="end-node-text">{$L('流程结束')}</div>
          </div>
        </div>
      </div>
    )
  }

  componentDidMount() {
    if (wpc.flowDefinition) {
      const flowNodes = wpc.flowDefinition.nodes
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

    const $btns = $('.J_save').on('click', () => {
      const s = this.serialize()
      if (!s) return

      let data = {
        flowDefinition: s,
        metadata: { id: wpc.configId },
      }
      data = JSON.stringify(data)
      const noApproverNode = !data.includes('"approver"')

      $btns.button('loading')
      $.post('/app/entity/common-save', data, (res) => {
        if (res.error_code === 0) {
          const msg = (
            <RF>
              <strong>{$L('保存成功')}</strong>
              {noApproverNode && <p className="text-warning m-0 mt-1">{$L('由于未添加任何审批节点，因此此流程暂不可用')}</p>}
            </RF>
          )
          RbAlert.create(msg, {
            icon: 'info-outline',
            cancelText: $L('返回列表'),
            cancel: () => location.replace('../approvals'),
            confirmText: $L('继续编辑'),
            confirm: () => location.reload(),
          })
        } else {
          RbHighbar.error(res.error_msg)
        }
        $btns.button('reset')
      })
    })

    $('.J_copy').on('click', () => renderRbcomp(<DlgCopy father={wpc.configId} name={wpc.name + '(2)'} isDisabled />))
  }

  zoom(v) {
    let zv = (this.state.zoomValue || 100) + v
    if (zv < 20) zv = 20
    else if (zv > 200) zv = 200
    this.setState({ zoomValue: zv, zoomStyle: { transform: `scale(${zv / 100})` } })
  }

  serialize() {
    const ns = super.serialize()
    if (!ns) return false
    ns.nodes.insert(0, this._root.serialize())
    return ns
  }
}

// ~ 流程中可编辑字段
class DlgFields extends RbModalHandler {
  constructor(props) {
    super(props)
    donotCloseSidebar = true
    this._selected = (props.selected || []).map((item) => {
      return item.field
    })
  }

  render() {
    return (
      <RbModal title={$L('选择可修改字段')} ref={(c) => (this._dlg = c)} disposeOnHide onHide={() => (donotCloseSidebar = false)}>
        <div className="row p-1" ref={(c) => (this._fields = c)}>
          {fieldsCache.map((item) => {
            if (item.type === 'BARCODE') return null
            return (
              <div className="col-3" key={`field-${item.name}`}>
                <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-1">
                  <input className="custom-control-input" type="checkbox" disabled={!item.updatable} value={item.name} defaultChecked={item.updatable && this._selected.includes(item.name)} />
                  <span className="custom-control-label">{item.label}</span>
                </label>
              </div>
            )
          })}
        </div>
        <div className="dialog-footer">
          <button className="btn btn-primary btn-space" type="button" onClick={this.confirm}>
            {$L('确定')}
          </button>
          <button className="btn btn-secondary btn-space" type="button" onClick={this.hide}>
            {$L('取消')}
          </button>
        </div>
      </RbModal>
    )
  }

  confirm = () => {
    const selected = []
    $(this._fields)
      .find('input:checked')
      .each(function () {
        selected.push(this.value)
      })

    typeof this.props.call === 'function' && this.props.call(selected)
    this.hide()
  }
}

// ~~ 另存为
class DlgCopy extends ConfigFormDlg {
  constructor(props) {
    super(props)
    this.title = $L('另存为')
  }

  renderFrom() {
    return (
      <React.Fragment>
        <div className="form-group row">
          <label className="col-sm-3 col-form-label text-sm-right">{$L('新名称')}</label>
          <div className="col-sm-7">
            <input type="text" className="form-control form-control-sm" data-id="name" onChange={this.handleChange} value={this.state.name || ''} />
          </div>
        </div>
        <div className="form-group row">
          <div className="col-sm-7 offset-sm-3">
            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-0">
              <input className="custom-control-input" type="checkbox" checked={this.state.isDisabled === true} data-id="isDisabled" onChange={this.handleChange} />
              <span className="custom-control-label">{$L('同时禁用当前审批流程')}</span>
            </label>
          </div>
        </div>
      </React.Fragment>
    )
  }

  confirm = () => {
    const approvalName = this.state.name
    if (!approvalName) {
      RbHighbar.create($L('请输入新名称'))
      return
    }

    this.disabled(true)
    $.post(`/admin/robot/approval/copy?father=${this.props.father}&disabled=${this.state.isDisabled}&name=${$encode(approvalName)}`, (res) => {
      if (res.error_code === 0) {
        RbHighbar.success($L('另存为成功'))
        setTimeout(() => location.replace('./' + res.data.approvalId), 500)
      } else {
        RbHighbar.error(res.error_msg)
      }
      this.disabled()
    })
  }
}

// 用户选择器
class UserSelectorWithField extends UserSelector {
  constructor(props) {
    super(props)
    this._useTabs.push(['FIELDS', $L('使用字段')])
  }

  componentDidMount() {
    super.componentDidMount()

    this._fields = []

    // 外部人员
    if (this.props.userType === 2) {
      $.get(`/commons/metadata/fields?deep=2&entity=${this.props.entity || wpc.applyEntity}`, (res) => {
        res.data &&
          res.data.forEach((item) => {
            if (item.type === 'PHONE' || item.type === 'EMAIL') {
              this._fields.push({ id: item.name, text: item.label })
            }
          })
        this.switchTab()
      })
    } else {
      $.get(`/admin/robot/approval/user-fields?entity=${this.props.entity || wpc.applyEntity}`, (res) => {
        this._fields = res.data || []
      })
    }
  }

  switchTab(type) {
    if (this.props.userType === 2) {
      this.setState({ tabType: 'FIELDS', items: this._fields || [] })
      return
    }

    type = type || this.state.tabType
    if (type === 'FIELDS') {
      const q = this.state.query
      const ckey = type + '-' + q
      this.setState({ tabType: type, items: this._cached[ckey] }, () => {
        if (!this._cached[ckey]) {
          if (!q) {
            this._cached[ckey] = this._fields
          } else {
            const fs = []
            $(this._fields).each(function () {
              if (this.text.contains(q)) fs.push(this)
            })
            this._cached[ckey] = fs
          }
          this.switchTab(type)
        }
      })
    } else {
      super.switchTab(type)
    }
  }
}
