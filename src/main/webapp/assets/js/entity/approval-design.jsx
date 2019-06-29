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
  return (<div className="add-node-btn-box"><div className="add-node-btn"><button type="button"><i className="zmdi zmdi-plus" /></button></div></div>)
}

// ~ 节点
class Node extends React.Component {
  constructor(props) {
    super(props)
    this.nodeType = NTs[props.type || 'start']
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
      <AddNodeButton />
    </div>)
  }
}

// 条件分支节点
class ConditionNode extends React.Component {
  constructor(props) {
    super(props)
    this.state = { cols: props.cols }
  }
  render() {
    let colLength = (this.state.cols || []).length
    return (<div className="branch-wrap">
      <div className="branch-box-wrap">
        <div className="branch-box">
          <button className="add-branch">添加分支</button>
          {(this.state.cols || []).map((col, idx) => {
            return (<div className="col-box" key={'col-' + idx}>
              {idx === 0 && <div className="top-left-cover-line"></div>}
              {idx === 0 && <div className="bottom-left-cover-line"></div>}
              <div className="condition-node">
                <div className="condition-node-box">
                  <div className="auto-judge">
                    <div className="title-wrapper">
                      <span className="editable-title float-left">条件1</span>
                      <span className="priority-title float-right">优先级1</span>
                    </div>
                    <div className="content">
                      请设置条件
                    </div>
                  </div>
                </div>
              </div>
              {idx === colLength - 1 && <div className="top-right-cover-line"></div>}
              {idx === colLength - 1 && <div className="bottom-right-cover-line"></div>}
            </div>)
          })}
        </div>
        <AddNodeButton />
      </div>
    </div>)
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
      <ConditionNode cols={['a', 'b', 'c']} />
      <div className="end-node">
        <div className="end-node-circle"></div>
        <div className="end-node-text">流程结束</div>
      </div>
    </div>)
  }
}