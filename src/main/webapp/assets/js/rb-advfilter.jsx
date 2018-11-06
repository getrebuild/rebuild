// ~~ 高级过滤器
class AdvFilter extends React.Component {
    constructor(props) {
        super(props)
        
        // TODO parse exists items
        let items = []
        this.filterItems = {}
        this.state = { ...props, items: items }
        
        this.childrenRef = []
    }
    render() {
        return (
            <div className="adv-filter-warp shadow rounded">
                <div className="adv-filter">
                    <div className="filter-option">
                    </div>
                    <div className="filter-items" ref="items">
                        {this.state.items.map((item)=>{
                            return item
                            //return React.cloneElement(item, { ref: item.id })
                        })}
                        <div className="item plus"><a href="javascript:;" onClick={()=>this.addItem()}><i className="zmdi zmdi-plus-circle icon"></i> 添加条件</a></div>
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
                        <input className="form-control form-control-sm form-control-success" ref="adv-exp" value={this.state.advexp} onChange={()=>this.handleChange()} />
                    </div>
                    }
                    <div className="item">
                        <button className="btn btn-primary" type="button" onClick={()=>this.toFilterJson()}>应用</button>
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
            that.fields = res.data.map((item) => {
                if (item.type == 'DATETIME') item.type = 'DATE'
                return item
            })
        })
    }
    onRef = (child) => {
        console.log('onRef ... ' + child)
        this.childrenRef.push(child)
    }
    handleChange(event) {
        let v = event.target.value
        console.log(v)
    }
    
    addItem(){
        if (!this.fields) return
        let _items = this.state.items || []
        if (_items.length >= 10){ rb.notice('最多可添加10个条件'); return }
        
        let id = 'item-' + $random()
        _items.push(<FilterItem index={_items.length + 1} fields={this.fields} $$$parent={this} key={id} id={id} onRef={this.onRef} />)
        
        let advexp = [] 
        for (let i = 1; i <= _items.length; i++) advexp.push(i)
        this.setState({ items: _items, advexp: advexp.join(' OR ') })
    }
    removeItem(id){
        let _items = []
        this.state.items.forEach((item)=>{
            if (item.props.id != id) _items.push(item)
        })
        let _children = []
        this.childrenRef.forEach((item)=>{
            if (item.props.id != id) _children.push(item)
        })
        this.childrenRef = _children
        
        let that = this
        let advexp = [] 
        for (let i = 1; i <= _items.length; i++) advexp.push(i)
        this.setState({ items: _items, advexp: advexp.join(' OR ') }, ()=>{
            that.childrenRef.forEach((child, idx)=>{
                child.setIndex(idx + 1)
            })
        })
    }
    
    toggleAdvexp() {
        this.setState({ enableAdvexp: this.state.enableAdvexp !== true })
    }
    
    toFilterJson() {
        let filters = []
        let hasError = false
        for (let i = 0; i < this.childrenRef.length; i++){
            let fj = this.childrenRef[i].getFilterJson()
            if (!!!fj) hasError = true
            else filters.push(fj)
        }
        if (hasError){ rb.notice('部分条件设置有误，请检查'); return }
        
        let adv = { items: filters }
        if (this.state.enableAdvexp == true) adv.equation = this.state.advexp
        console.log(JSON.stringify(adv))
    }
}

const OP_TYPE = { LK:'包含', NLK:'不包含', EQ:'等于', NEQ:'不等于', GT:'大于', LT:'小于', BW:'区间', NL:'为空', NT:'不为空', BFD:'...天前', BFM:'...月前', AFD:'...天后', AFM:'...月后' }
const OP_DATE_NOPICKER = ['BFD','BFM','AFD','AFM']
const PICKLIST_CACHE = {}

class FilterItem extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
        console.log(props)
        
        this.handleChange = this.handleChange.bind(this)
    }
    render() {
        return (
            <div className="row item">
                <div className="col-sm-5 field">
                    <em className={this.state.hasError ? 'text-danger' : ''}>{this.state.index}</em>
                    <i className="zmdi zmdi-minus-circle" title="移除条件" onClick={()=>this.props.$$$parent.removeItem(this.props.id)}></i>
                    <select className="form-control form-control-sm" ref="filter-field" value={this.state.field}>
                        {this.state.fields.map((item)=>{
                            return <option value={item.name + '----' + item.type} key={'field-' + item.name}>{item.label}</option>
                        })}
                    </select>
                </div>
                <div className="col-sm-2 op">
                    {this.renderOp()}
                </div>
                <div className={'col-sm-5 val' + (this.state.op == 'NL' || this.state.op == 'NT' ? ' hide' : '')}>
                    {this.renderVal()}
                </div>
            </div>
        )
    }
    renderOp(){
        let op = [ 'LK', 'NLK', 'EQ', 'NEQ' ]
        if (this.state.type == 'NUMBER' || this.state.type == 'DECIMAL'){
            op = [ 'GT', 'LT', 'BW', 'EQ' ]
        } else if (this.state.type == 'DATE' || this.state.type == 'DATETIME'){
            op = [ 'GT', 'LT', 'BW', 'BFD', 'BFM', 'AFD', 'AFM' ]
        } else if (this.state.type == 'FILE' || this.state.type == 'IMAGE'){
            op = []
        } else if (this.state.type == 'PICKLIST'){
            op = [ 'LK', 'NLK' ]
        }
        op.push('NL', 'NT')
        this.__op = op
        
        return (
            <select className="form-control form-control-sm" ref="filter-op" value={this.state.op}>
                {op.map((item)=>{
                    return <option value={item} key={'op-' + item}>{OP_TYPE[item]}</option>
                })}
            </select>
        )
    }
    renderVal(){
        let val = <input className="form-control form-control-sm" ref="filter-val" onChange={this.handleChange} value={this.state.value || ''} />
        if (this.state.op == 'BW'){
            val = (
                <div className="val-range">
                    <input className="form-control form-control-sm" ref="filter-val" onChange={this.handleChange} value={this.state.value || ''} />
                    <input className="form-control form-control-sm" ref="filter-val2" onChange={this.handleChange} data-at="2" value={this.state.value2 || ''} />
                    <span>起</span>
                    <span className="end">止</span>
                </div>)
        } else if (this.state.type == 'PICKLIST'){
            val = (
                <select className="form-control form-control-sm" multiple="true" ref="filter-val">
                    {(this.state.picklist || []).map((item) => {
                        return <option value={item.id} key={'val-' + item.id}>{item.text}</option>
                    })}
                </select>)
        }
        return (val)
    }
    
    componentDidMount() {
        this.props.onRef(this)
        
        let that = this
        let s2field = $(this.refs['filter-field']).select2({
            language: 'zh-CN',
            placeholder: '选择字段',
            width: '100%',
        }).on('change.select2', function(e){
            let ft = e.target.value.split('----')
            that.setState({ field: ft[0], type: ft[1] }, function(){
                s2op.val(that.__op[0]).trigger('change')
            })
        })
        let s2op = $(this.refs['filter-op']).select2({
            language: 'zh-CN',
            placeholder: '选择操作',
            width: '100%',
        }).on('change.select2', function(e){
            that.setState({ op: e.target.value }, function(){
                $setTimeout(function(){
                    //ReactDOM.findDOMNode(that.refs['filter-val']).focus()
                }, 200, 'filter-val-focus')
            })
        })
        this.__select2 = [s2field, s2op]
        s2field.trigger('change')
    }
    componentDidUpdate(prevProps, prevState) {
        let thisEnter = this.state.field + '----' + this.state.type + '----' + (this.state.op == 'BW')/*区间*/ + '----' + (OP_DATE_NOPICKER.contains(this.state.op))
        if (this.__lastEnter == thisEnter) return
        console.log(thisEnter)
        let lastType = this.__lastEnter ? this.__lastEnter.split('----')[1] : null
        this.__lastEnter = thisEnter
        
        if (this.state.type == 'PICKLIST') {
            this.renderPickList(this.state.field)
        } else if (lastType == 'PICKLIST') {
            this.removePickList()
        }
        
        if (this.state.type == 'DATE') {
            this.removeDatepicker()
            if (OP_DATE_NOPICKER.contains(this.state.op)){
                // 无需日期组件
            } else {
                this.renderDatepicker()
            }
        } else if (lastType == 'DATE'){
            this.removeDatepicker()
        }
    }
    componentWillUnmount() {
        this.__select2.forEach((item, index) => {
            item.select2('destroy')
        })
        this.__select2 = null
        this.removePickList()
        this.removeDatepicker()
    }
    
    handleChange(event) {
        let that = this
        let val = event.target.value
        if (event.target.dataset.at == 2) {
            this.setState({ value2: val }, function(){
            })
        } else {
            this.setState({ value: val }, function(){
            })
        }
    }
    
    renderPickList(field) {
        let that = this
        if (PICKLIST_CACHE[field]) {
            this.setState({ picklist: PICKLIST_CACHE[field] }, function(){
                that.renderPickListAfter()
            })
        } else {
            $.get(rb.baseUrl + '/commons/metadata/picklist?entity=' + this.props.$$$parent.props.entity + '&field=' + field, function(res){
                if (res.error_code == 0){
                    PICKLIST_CACHE[field] = res.data
                    that.setState({ picklist: PICKLIST_CACHE[field] }, function(){
                        that.renderPickListAfter()
                    })
                } else{
                    rb.notice(res.error_msg, 'danger')
                }
            })
        }
    }
    renderPickListAfter(){
        console.log('render PickList ...')
        let that = this
        let s2val = $(this.refs['filter-val']).select2({
            language: 'zh-CN',
            placeholder: '选择值',
            width: '100%',
        }).on('change.select2', function(e){
            let val = s2val.val()
            that.setState({ value: val.join(',') }, function(){
            })
        })
        this.__select2_PickList = s2val
    }
    removePickList(){
        if (this.__select2_PickList) {
            console.log('remove PickList ...')
            this.__select2_PickList.select2('destroy')
            this.__select2_PickList = null
        }
    }
    
    renderDatepicker(){
        console.log('render Datepicker ...')
        let cfg = {
            componentIcon:'zmdi zmdi-calendar',
            navIcons: { rightIcon:'zmdi zmdi-chevron-right', leftIcon:'zmdi zmdi-chevron-left'},
            format: 'yyyy-mm-dd',
            minView: 2,
            startView: 'month',
            weekStart: 1,
            autoclose: true,
            language: 'zh',
            todayHighlight: true,
            showMeridian: false,
            keyboardNavigation: false,
        }
        
        let that = this
        let dp1 = $(this.refs['filter-val']).datetimepicker(cfg)
        dp1.on('change.select2', function(e){
            that.setState({ value: e.target.value }, function(){
            })
        })
        this.__datepicker = [dp1]
        
        if (this.refs['filter-val2']) {
            let dp2 = $(this.refs['filter-val2']).datetimepicker(cfg)
            dp2.on('change.select2', function(e){
                that.setState({ value2: e.target.value }, function(){
                })
            })
            this.__datepicker.push(dp2)
        }
    }
    removeDatepicker(){
        if (this.__datepicker) {
            console.log('remove Datepicker ...')
            this.__datepicker.forEach((item) => {
                item.datetimepicker('remove')
            })
            this.__datepicker = null
        }
    }
    
    setIndex(idx) {
        this.setState({ index: idx })
    }
    getFilterJson(){
        let s = this.state
        if (!!!s.value) {
            if (s.op == 'NL' || s.op == 'NT'){
                // 允许无值
            } else {
                this.setState({ hasError: true })
                return
            }
        }
        if (s.op == 'BW' && !!!s.value2){
            this.setState({ hasError: true })
            return
        }
        
        let item = { index: s.index, field: s.field, op: s.op, value: s.value }
        if (s.value2) item.value2 = s.value2
        this.setState({ hasError: false })
        return item
    }
}

// -- Usage

rb.AdvFilter = {
        
}