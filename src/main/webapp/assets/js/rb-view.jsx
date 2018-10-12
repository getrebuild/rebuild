//~~ 视图
class RbViewForm extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
        return (
            <div className="rbview-form" ref="reviewForm">
                {this.state.formComponent}
            </div>
        )
    }
    componentDidMount() {
        let that = this
        $.get(rb.baseUrl + '/app/' + this.props.entity + '/view-model?id=' + this.props.id, function(res){
            let elements = res.data.elements
            const FORM = <div className="row">{elements.map((item) => {
                return detectViewElement(item)
            })}</div>
            that.setState({ formComponent: FORM }, function(){
                $('.invisible').removeClass('invisible')
                if (parent && parent.RbViewModal_Comp) {
                    parent.RbViewModal_Comp.hideLoading(true)
                }
                
                $(that.refs['reviewForm']).find('.type-NTEXT .form-control-plaintext').perfectScrollbar()
            })
        });
    }
}

const detectViewElement = function(item){
    item.onView = true
    item.viewMode = true
    return (<div className={'col-12 col-sm-' + (item.isFull ? 12 : 6)}>{detectElement(item)}</div>)
}

// -- Usage

var rb = rb || {}

// props = { entity, recordId }
rb.RbViewForm = function(props, target){
    return renderRbcomp(<RbViewForm {...props}  />, target || 'tab-rbview')
}