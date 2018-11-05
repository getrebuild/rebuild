// ~~ 高级过滤器
class AdvFilter extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
        return (
            <div className="adv-filter-warp shadow rounded">
                <div className="adv-filter">
                    <div className="filter-option">
                    </div>
                    <div className="filter-items">
                        {(this.state.items || []).map((item)=>{
                            return item
                        })}
                        <div className="item plus"><a href="javascript:;" onClick={()=>this.addItem()}><i className="zmdi zmdi-plus-circle icon"></i> 新增条件</a></div>
                    </div>
                </div>
                <div className="adv-filter">
                    <div className="item">
                        <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
                            <input className="custom-control-input" type="checkbox" onClick={()=>this.toggleAdvexp()} />
                            <span className="custom-control-label"> 启用高级表达式</span>
                        </label>
                    </div>
                    {this.state.enableAdvexp !== true ? null :
                    <div className="mb-3">
                        <input className="form-control form-control-sm form-control-success" ref="adv-exp" value={this.state.advexp} />
                    </div>
                    }
                    <div className="item">
                        <button className="btn btn-primary" type="button">应用</button>
                        &nbsp;&nbsp;
                        <button className="btn btn-secondary" type="button">取消</button>
                    </div>
                </div>
            </div>
        )
    }
    componentDidMount() {
        let that = this
        $.get(rb.baseUrl + '/commons/metadata/fields?entity=' + this.state.entity, function(res){
            that.fields = res.data
        })
    }
    
    addItem(){
        if (!this.fields) return
        let _items = this.state.items || []
        if (_items.length >= 10){ rb.notice('最多可设置10个条件'); return}
        
        _items.push(<FilterItem index={_items.length + 1} fields={this.fields} $$$parent={this} key={'item-' + $random()} />)
        let advexp = [] 
        for (let i = 1; i <= _items.length; i++) advexp.push(i)
        this.setState({ items: _items, advexp: advexp.join(' OR ') })
    }
    delItem(del){
        let _items = []
        for (let i = 0; i < this.state.items.length; i++){
            let item = this.state.items[i]
            if (item.props.index != del.props.index) _items.push(item)
        }
        let advexp = []
        for (let i = 1; i <= _items.length; i++) advexp.push(i)
        this.setState({ items: _items, advexp: advexp.join(' OR ') })
    }
    
    toggleAdvexp() {
        this.setState({ enableAdvexp: this.state.enableAdvexp !== true })
    }
}

class FilterItem extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
        return (
            <div className="row item" key={'item-' + this.state.index}>
                <div className="col-sm-5 field">
                    <em>{this.state.index}</em>
                    <i className="zmdi zmdi-minus-circle" title="移除条件" onClick={()=>this.props.$$$parent.delItem(this)}></i>
                    <select className="form-control form-control-sm" ref="filter-field">
                        {this.state.fields.map((item)=>{
                            return <option value={item.name + '----' + item.type} key={'field-' + item.name}>{item.label}</option>
                        })}
                    </select>
                </div>
                <div className="col-sm-2 op">
                    {this.renderOp()}
                </div>
                <div className="col-sm-5 val">
                    {this.renderVal()}
                </div>
            </div>
        )
    }
    renderOp(){
        let op = [ ['lk','包含'], ['nlk','不包含'], ['eq','等于'], ['neq','不等于'], ['nl','为空'], ['nt','不为空'] ]
        if (this.state.type == 'NUMBER' || this.state.type == 'DECIMAL'){
            op = [ ['gt','大于'], ['lt','小于'], ['bw','区间'], ['eq','等于'] ]
        } else if (this.state.type == 'DATE' || this.state.type == 'DATETIME'){
            op = [ ['gt','大于'], ['lt','小于'], ['bw','区间'], ['bfd','...天前'], ['bfm','...月前'], ['afd','...天后'], ['afm','...月后'] ]
        } else if (this.state.type == 'FILE' || this.state.type == 'IMAGE'){
            op = [ ['nl','为空'], ['nt','不为空'] ]
        }
        this.__op = op
        
        return (
            <select className="form-control form-control-sm" ref="filter-op">
                {op.map((item)=>{
                    return <option value={item[0]} key={'op-' + item.join('-')}>{item[1]}</option>
                })}
            </select>
        )
    }
    renderVal(){
        return this.state.op != 'bw' ? (this.state.op == 'nl' || this.state.op == 'nt' ? null : <input className="form-control form-control-sm" ref="filter-val" />) : (<div>
            <input className="form-control form-control-sm" ref="filter-val" />
            <input className="form-control form-control-sm" ref="filter-val-2" />
        </div>)
    }
    
    componentDidMount() {
        let that = this
        let s2field = $(this.refs['filter-field']).select2({
            language: 'zh-CN',
            placeholder: '选择字段',
            width: '100%',
        }).on('change.select2', function(e){
            let ft = e.target.value.split('----')
            that.setState({ field: ft[0], type: ft[1] }, function(){
                s2op.val(that.__op[0][0]).trigger('change')
            })
        })
        let s2op = $(this.refs['filter-op']).select2({
            language: 'zh-CN',
            placeholder: '选择操作',
            width: '100%',
        }).on('change.select2', function(e){
            that.setState({ op: e.target.value }, function(){
                $(that.refs['filter-val']).focus()
            })
        })
    }
}

// -- Usage

rb.AdvFilter = {
        
}