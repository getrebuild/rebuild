/* eslint-disable react/prop-types */
/* eslint-disable no-unused-vars */
// 审批流程
class ApprovalProcessor extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return (<div className="approval-form">
      {this.renderStateDarft()}
    </div>)
  }

  renderStateDarft() {
    <div className="alert alert-warning">
      <button className="close btn btn-secondary">立即提交</button>
      <div className="icon"><span className="zmdi zmdi-info-outline"></span></div>
      <div className="message">
        当前记录尚未提交审核，请在信息完善后尽快提交。
      </div>
    </div>
  }

  renderStateApproved() {
    <div className="alert alert-warning">
      <button className="close btn btn-secondary">立即提交</button>
      <div className="icon"><span className="zmdi zmdi-info-outline"></span></div>
      <div className="message">
        当前记录尚未提交审核，请在信息完善后尽快提交。
      </div>
    </div>
  }

  componentDidMount() {

  }
}