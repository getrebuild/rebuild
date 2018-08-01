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

