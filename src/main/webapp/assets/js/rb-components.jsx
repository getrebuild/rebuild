// 弹出窗口
class RbModal extends React.Component {
	render() {
		return (
			<div className="modal fade colored-header colored-header-primary" id={this.props.id}>
		        <div className="modal-dialog">
    		        <div className="modal-content">
        		        <div className="modal-header modal-header-colored">
            		        <h3 className="modal-title">{this.props.title}</h3>
            		        <button className="close md-close" type="button" data-dismiss="modal"><span className="zmdi zmdi-close"></span></button>
            		    </div>
        		        <div className="modal-body">
        		        </div>
        		        <div className="modal-footer">
            		        <button className="btn btn-secondary md-close" type="button" data-dismiss="modal">取消</button>
            		        <button className="btn btn-primary md-close" type="button">保存</button>
            		    </div>
    		        </div>
		        </div>
			</div>
		)
	}
}

// 提示框
class RbAlter extends React.Component {
    constructor(props) {
       super(props);
       this.state = {};
    }
    render() {
        return (
            <div className="modal fade" ref="rbalter">
                <div className="modal-dialog">
                    <div className="modal-content">
                        <div className="modal-header">
                            <button className="close" type="button" onClick={()=>this.hide()}><span className="zmdi zmdi-close"></span></button>
                        </div>
                        <div className="modal-body">
                            <div className="text-center">
                                <h3>提示</h3>
                                <p>{this.state.message || '提示内容'}</p>
                                <div class="mt-8">
                                    <button className="btn btn-space btn-secondary" type="button" ref="rbalter.confirm" onClick={()=>this.hide()}>确定</button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        )
    }
    show(message) {
        let that = this;
        this.setState({ message: message }, function(){
            $(that.refs['rbalter']).modal('show');
            $(that.refs['rbalter.confirm']).focus();
        })
    }
    hide() {
        $(this.refs['rbalter']).modal('hide');
    }
}
