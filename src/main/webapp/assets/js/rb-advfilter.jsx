// ~~ 高级过滤器
class AdvFilter extends React.Component {
    constructor(props) {
        super(props)
        
        // TODO parse exists items
        let items = []
        this.state = { ...props, items: items }
        this.handleChange = this.handleChange.bind(this)
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
                        })}
                        <div className="item plus"><a href="javascript:;" onClick={()=>this.addItem()}><i className="zmdi zmdi-plus-circle icon"></i> 添加条件</a></div>
                    </div>
                </div>
                <div className="adv-filter">
                    <div className="item">
                        <label className="custom-control custom-control-sm custom-checkbox custom-control-inline">
                            <input className="custom-control-input" type="checkbox" onClick={()=>this.toggleEquation()} />
                            <span className="custom-control-label"> 启用高级表达式</span>
                        </label>
                    </div>
                    {this.state.enableEquation !== true ? null :
                    <div className="mb-3">
                        <input className={'form-control form-control-sm' + (this.state.equationError ? ' is-invalid' : '')} value={this.state.equation || ''} onChange={this.handleChange} />
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
        $.get(rb.baseUrl + '/commons/metadata/fields?entity=' + this.props.entity, function(res){
            that.fields = res.data.map((item) => {
                if (item.type == 'DATETIME') {
                    item.type = 'DATE'
                } else if (item.type == 'REFERENCE') {
                    REFFIELD_CACHE[that.props.entity + '.' + item.name] = item.ref
                }
                return item
            })
        })
    }
    onRef = (child) => {
        this.childrenRef.push(child)
    }
    handleChange(e) {
        let val = e.target.value
        let that = this
        this.setState({ equation: val }, function(){
            let token = val.toLowerCase().split(' ')
            let hasError = false
            for (let i = 0; i < token.length; i++) {
                let t = $.trim(token[i])
                if (!!!t) continue
                if (!(t == '(' || t == ')' || t == 'or' || t == 'and' || (~~t > 0))) {
                    hasError = true
                    break
                }
            }
            that.setState({ equationError: hasError })
        })
    }
    
    addItem(){
        if (!this.fields) return
        let _items = this.state.items || []
        if (_items.length >= 10){ rb.notice('最多可添加10个条件'); return }
        
        let id = 'item-' + $random()
        _items.push(<FilterItem index={_items.length + 1} fields={this.fields} $$$parent={this} key={id} id={id} onRef={this.onRef} />)
        
        let equation = [] 
        for (let i = 1; i <= _items.length; i++) equation.push(i)
        this.setState({ items: _items, equation: equation.join(' OR ') })
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
        let equation = [] 
        for (let i = 1; i <= _items.length; i++) equation.push(i)
        this.setState({ items: _items, equation: equation.join(' OR ') }, ()=>{
            that.childrenRef.forEach((child, idx)=>{
                child.setIndex(idx + 1)
            })
        })
    }
    
    toggleEquation() {
        this.setState({ enableEquation: this.state.enableEquation !== true })
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
        
        let adv = { entity: this.props.entity, items: filters }
        if (this.state.enableEquation == true) adv.equation = this.state.equation
        
        $.post(rb.baseUrl + '/app/entity/test-advfilter', JSON.stringify(adv), function(res){
            console.log(JSON.stringify(adv) + ' >> ' + res.data)
        })
    }
}

const OP_TYPE = { LK:'包含', NLK:'不包含', IN:'包含', NIN:'不包含', EQ:'等于', NEQ:'不等于', GT:'大于', LT:'小于', BW:'区间', NL:'为空', NT:'不为空', BFD:'...天前', BFM:'...月前', AFD:'...天后', AFM:'...月后' }
const OP_DATE_NOPICKER = ['BFD','BFM','AFD','AFM']
const PICKLIST_CACHE = {}
const REFFIELD_CACHE = {}

class FilterItem extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
        console.log(props)
        this.__entity = this.props.$$$parent.props.entity
        
        this.valueHandle = this.valueHandle.bind(this)
        this.valueCheck = this.valueCheck.bind(this)
    }
    render() {
        return (
            <div className="row item">
                <div className="col-sm-5 field">
                    <em>{this.state.index}</em>
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
        } else if (this.state.type == 'PICKLIST' || this.isBizzField()){
            op = [ 'IN', 'NIN' ]
        }
        // TODO 根据引用字段类型
        
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
        let val = <input className="form-control form-control-sm" ref="filter-val" onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value || ''} />
        if (this.state.op == 'BW'){
            val = (
                <div className="val-range">
                    <input className="form-control form-control-sm" ref="filter-val" onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value || ''} />
                    <input className="form-control form-control-sm" ref="filter-val2" onChange={this.valueHandle} onBlur={this.valueCheck} data-at="2" value={this.state.value2 || ''} />
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
        } else if (this.isBizzField()){
            val = <select className="form-control form-control-sm" multiple="true" ref="filter-val" />
        }
        return (val)
    }
    
    // 引用 User/Department
    isBizzField() {
        if (this.state.type == 'REFERENCE'){
            const fRef = REFFIELD_CACHE[this.__entity + '.' + this.state.field]
            return fRef && (fRef[0] == 'User' || fRef[0] == 'Department')
        }
        return false
    }
    // 数字值
    isNumberValue(){
        if (this.state.type == 'NUMBER' || this.state.type == 'DECIMAL'){
            return true
        } else if (this.state.type == 'DATE' && OP_DATE_NOPICKER.contains(this.state.op)){
            return true
        }
        return false
    }
    
    componentDidMount() {
        this.props.onRef(this)
        
        let that = this
        let s2field = $(this.refs['filter-field']).select2({
            language: 'zh-CN',
            width: '100%',
        }).on('change.select2', function(e){
            let ft = e.target.value.split('----')
            that.setState({ field: ft[0], type: ft[1] }, function(){
                s2op.val(that.__op[0]).trigger('change')
            })
        })
        let s2op = $(this.refs['filter-op']).select2({
            language: 'zh-CN',
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
        
        if (this.isBizzField()){
            const fRef = REFFIELD_CACHE[this.__entity + '.' + this.state.field]
            this.renderBizzSearch(fRef[0])
        } else if (lastType == 'REFERENCE') {
            this.removeBizzSearch()
        }
        
        if (this.state.value) this.valueCheck($(this.refs['filter-val']))
        if (this.state.value2 && this.refs['filter-val2']) this.valueCheck($(this.refs['filter-val2']))
    }
    componentWillUnmount() {
        this.__select2.forEach((item, index) => { item.select2('destroy') })
        this.__select2 = null
        this.removePickList()
        this.removeDatepicker()
        this.removeBizzSearch()
    }
    
    valueHandle(e) {
        let that = this
        let val = e.target.value
        if (e.target.dataset.at == 2) this.setState({ value2: val })
        else this.setState({ value: val })
    }
    // @e = el or event
    valueCheck(e){
        let el = e.target ? $(e.target) : e
        let val = e.target ? e.target.value : e.val()
        if (!!!val){
            el.addClass('is-invalid')
        } else {
            if (this.isNumberValue() && $regex.isDecimal(val) == false){
                el.addClass('is-invalid')
            } else if (this.state.type == 'DATE' && $regex.isUTCDate(val) == false) {
                el.addClass('is-invalid')
            } else {
                el.removeClass('is-invalid')
            }
        }
    }
    
    renderPickList(field) {
        let that = this
        const plKey = this.props.$$$parent.props.entity + '.' + field
        if (PICKLIST_CACHE[plKey]) {
            this.setState({ picklist: PICKLIST_CACHE[plKey] }, function(){
                that.renderPickListAfter()
            })
        } else {
            $.get(rb.baseUrl + '/commons/metadata/picklist?entity=' + this.props.$$$parent.props.entity + '&field=' + field, function(res){
                if (res.error_code == 0){
                    PICKLIST_CACHE[plKey] = res.data
                    that.setState({ picklist: PICKLIST_CACHE[plKey] }, function(){
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
            width: '100%',
        }).on('change.select2', function(e){
            let val = s2val.val()
            that.setState({ value: val.join('|') })
        })
        this.__select2_PickList = s2val
    }
    removePickList(){
        if (this.__select2_PickList) {
            console.log('remove PickList ...')
            this.__select2_PickList.select2('destroy')
            this.__select2_PickList = null
            this.setState({ value: null })
        }
    }
    
    renderBizzSearch(entity){
        console.log('render BizzSearch ...')
        let that = this
        let s2val = $(this.refs['filter-val']).select2({
            language: 'zh-CN',
            width: '100%',
            minimumInputLength: 1,
            ajax: {
                url: rb.baseUrl + '/commons/search',
                delay: 300,
                data: function(params) {
                    let query = {
                        entity: entity,
                        fields: entity == 'User' ? 'loginName,fullName,email' : 'name',
                        q: params.term,
                    }
                    return query
                },
                processResults: function(data){
                    let rs = data.data.map((item) => { return item })
                    return { results: rs }
                }
            }
        }).on('change.select2', function(e){
            let val = s2val.val()
            that.setState({ value: val.join('|') })
        })
        this.__select2_BizzSearch = s2val
    }
    removeBizzSearch(){
        if (this.__select2_BizzSearch){
            console.log('remove BizzSearch ...')
            this.__select2_BizzSearch.select2('destroy')
            this.__select2_BizzSearch = null
            this.setState({ value: null })
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
            that.setState({ value: e.target.value }, ()=>{
                that.valueCheck($(that.refs['filter-val']))
            })
        })
        this.__datepicker = [dp1]
        
        if (this.refs['filter-val2']) {
            let dp2 = $(this.refs['filter-val2']).datetimepicker(cfg)
            dp2.on('change.select2', function(e){
                that.setState({ value2: e.target.value }, ()=>{
                    that.valueCheck($(that.refs['filter-val2']))
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
                return
            }
        } else if (s.op == 'NL' || s.op == 'NT'){
            s.value = null
        }
        
        if (s.op == 'BW' && !!!s.value2){
            return
        }
        
        if (!!s.value && ($(this.refs['filter-val']).hasClass('is-invalid') || $(this.refs['filter-val2']).hasClass('is-invalid'))) {
            return
        }
        
        let item = { index: s.index, field: s.field, op: s.op }
        if (s.value) item.value = s.value
        if (s.value2) item.value2 = s.value2
        this.setState({ hasError: false })
        return item
    }
}

// -- Usage

rb.AdvFilter = {
        
}