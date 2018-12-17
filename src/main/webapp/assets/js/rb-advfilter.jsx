// ~~ 高级过滤器
class AdvFilter extends React.Component {
    constructor(props) {
        super(props)
        
        let ext = {}
        if (props.filter) {
            if (!!props.filter.equation){
                ext.enableEquation = true
                ext.equation = props.filter.equation
            }
            this.__items = props.filter.items
        }
        this.state = { ...props, ...ext }
        this.childrenRef = []
    }
    render() {
        let needSave = this.props.needSave == true
        let operBtns = (
            <div className={needSave ? 'float-right' : 'item'}>
                <button className="btn btn-primary" type="button" onClick={()=>this.confirm()}>{needSave ? '保存' : '确定'}</button>
                <button className="btn btn-secondary" type="button" onClick={()=>this.hide(true)}>取消</button>
            </div>
            )

        let advFilter = (
            <div className={'adv-filter-warp ' + (this.props.inModal ? 'in-modal' : 'shadow rounded')}>
                <div className="adv-filter">
                    <div className="filter-option">
                    </div>
                    <div className="filter-items" ref="items">
                        {(this.state.items || []).map((item)=>{
                            return item
                        })}
                        <div className="item plus"><a href="javascript:;" onClick={()=>this.addItem()}><i className="zmdi zmdi-plus-circle icon"></i> 添加条件</a></div>
                    </div>
                </div>
                <div className="adv-filter">
                    <div className="mb-1">
                        <div className="item mt-1">
                            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline mb-2">
                                <input className="custom-control-input" type="checkbox" checked={this.state.enableEquation == true} data-id="enableEquation" onChange={this.handleChange} />
                                <span className="custom-control-label"> 启用高级表达式</span>
                            </label>
                        </div>
                        {this.state.enableEquation !== true ? null :
                        <div className="mb-3">
                            <input className={'form-control form-control-sm' + (this.state.equationError ? ' is-invalid' : '')} value={this.state.equation || ''} data-id="equation" onChange={this.handleChange} />
                        </div>
                        }
                    </div>
                    {needSave ?
                    <div className="item dialog-footer">
                        {rb.isAdminUser !== true ? null :
                        <div className="float-left">
                            <div className="float-left input">
                                <input className="form-control form-control-sm text" maxLength="20" value={this.state.filterName || ''} data-id="filterName" onChange={this.handleChange} placeholder="输入过滤项名称" />
                            </div>
                            <label className="custom-control custom-control-sm custom-checkbox custom-control-inline ml-4 mt-2">
                                <input className="custom-control-input" type="checkbox" checked={this.state.applyToAll == true} data-id="applyToAll" onChange={this.handleChange} />
                                <span className="custom-control-label">共享给全部用户</span>
                            </label>
                        </div>}
                        {operBtns}
                        <div className="clearfix"/>
                    </div>
                    : (<div>{operBtns}</div>) }
                </div>
            </div>
            )

        if (this.props.inModal) return (<RbModal ref="dlg" title={this.props.title || '设置过滤条件'} destroyOnHide={this.props.destroyOnHide == true}>{advFilter}</RbModal>)
        else return advFilter
    }
    componentDidMount() {
        let that = this
        $.get(rb.baseUrl + '/commons/metadata/fields?entity=' + this.props.entity, function(res){
            let valideFs = []
            that.fields = res.data.map((item) => {
                valideFs.push(item.name)
                if (item.type == 'DATETIME') {
                    item.type = 'DATE'
                } else if (item.type == 'REFERENCE') {
                    REFMETA_CACHE[that.props.entity + '.' + item.name] = item.ref
                }
                return item
            })
            
            if (that.__items){
                $(that.__items).each((idx, item) => {
                    if (valideFs.contains(item.field)) that.addItem(item)
                })
            }
        })
    }
    onRef = (child) => {
        this.childrenRef.push(child)
    }
    handleChange = (e) => {
        let val = e.target.value
        let id = e.target.dataset.id
        if (id == 'enableEquation'){
            this.setState({ enableEquation: this.state.enableEquation !== true })
        } else if (id == 'applyToAll') {
            this.setState({ applyToAll: this.state.applyToAll !== true })
        } else {
            let state = {}
            state[id] = val
            this.setState({ ...state })
        }
    }
    
    addItem(cfg){
        if (!this.fields) return
        let _items = this.state.items || []
        if (_items.length >= 9){ rb.notice('最多可添加9个条件'); return }
        
        let id = 'item-' + $random()
        let props = { fields: this.fields, $$$parent: this, key: id, id: id, onRef: this.onRef, index: _items.length + 1 }
        if (!!cfg) props = { ...props, ...cfg }
        _items.push(<FilterItem {...props} />)
        
        if (!!!cfg){
            let equation = [] 
            for (let i = 1; i <= _items.length; i++) equation.push(i)
            this.setState({ items: _items, equation: equation.join(' OR ') })
        } else {
            this.setState({ items: _items })
        }
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
        let enable = this.state.enableEquation !== true
        this.setState({ enableEquation: enable })
        if (enable == true && !!!this.state.equation && this.state.items){
            let equation = [] 
            for (let i = 1; i <= this.state.items.length; i++) equation.push(i)
            this.setState({ equation: equation.join(' OR ') })
        }
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
        if (filters.length == 0){ rb.notice('请至少添加1个条件'); return }
        
        let adv = { entity: this.props.entity, items: filters }
        if (this.state.enableEquation == true) adv.equation = this.state.equation
        return adv
    }
    
    confirm() {
        let adv = this.toFilterJson()
        if (!!!adv) return
        if (this.props.confirm) this.props.confirm(adv, this.state.filterName, this.state.applyToAll)
        else{
            $.post(rb.baseUrl + '/app/entity/advfilter/test-parse', JSON.stringify(adv), function(res){
                if (res.error_code == 0) console.log(JSON.stringify(adv) + '\n>> ' + res.data)
                else rb.notice(res.error_msg, 'danger')
            })
        }
    }

    show(state) {
        if (this.props.inModal) this.refs['dlg'].show(state)
    }
    hide(callCancel) {
        if (this.props.inModal) this.refs['dlg'].hide()
        // callback
        if (callCancel == true && this.props.cancel) this.props.cancel()
    }
}

const OP_TYPE = { LK:'包含', NLK:'不包含', IN:'包含', NIN:'不包含', EQ:'等于', NEQ:'不等于', GT:'大于', LT:'小于', BW:'区间', NL:'为空', NT:'不为空', BFD:'...天前', BFM:'...月前', AFD:'...天后', AFM:'...月后', RED:'最近...天', REM:'最近...月', SFU:'本人', SFB:'本部门', SFD:'本部门及子部门' }
const OP_DATE_NOPICKER = ['BFD','BFM','AFD','AFM','RED','REM']
const OP_NOVALUE = ['NL','NT','SFU','SFB','SFD']
const PICKLIST_CACHE = {}
const REFMETA_CACHE = {}
const VALUE_HOLD = {}  // TODO

class FilterItem extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
        
        this.$$$entity = this.props.$$$parent.props.entity
        
        this.valueHandle = this.valueHandle.bind(this)
        this.valueCheck = this.valueCheck.bind(this)
        
        this.loadedPickList = false
        this.loadedBizzSearch = false
        
        if (props.field && props.value) VALUE_HOLD[props.field] = props.value
    }
    render() {
        return (
            <div className="row item">
                <div className="col-sm-5 field">
                    <em>{this.state.index}</em>
                    <i className="zmdi zmdi-minus-circle" title="移除条件" onClick={()=>this.props.$$$parent.removeItem(this.props.id)}></i>
                    <select className="form-control form-control-sm" ref="filter-field">
                        {this.state.fields.map((item)=>{
                            return <option value={item.name + '----' + item.type} key={'field-' + item.name}>{item.label}</option>
                        })}
                    </select>
                </div>
                <div className="col-sm-2 op">
                    <select className="form-control form-control-sm" ref="filter-op">
                        {this.selectOp().map((item)=>{
                            return <option value={item} key={'op-' + item}>{OP_TYPE[item]}</option>
                        })}
                    </select>
                </div>
                <div className={'col-sm-5 val' + (OP_NOVALUE.contains(this.state.op) ? ' hide' : '')}>
                    {this.renderValue()}
                </div>
            </div>
        )
    }
    selectOp(){
        let op = [ 'LK', 'NLK', 'EQ', 'NEQ' ]
        if (this.state.type == 'NUMBER' || this.state.type == 'DECIMAL'){
            op = [ 'GT', 'LT', 'BW', 'EQ' ]
        } else if (this.state.type == 'DATE' || this.state.type == 'DATETIME'){
            op = [ 'GT', 'LT', 'BW', 'RED', 'REM', 'BFD', 'BFM', 'AFD', 'AFM' ]
        } else if (this.state.type == 'FILE' || this.state.type == 'IMAGE'){
            op = []
        } else if (this.state.type == 'PICKLIST'){
            op = [ 'IN', 'NIN' ]
        } else if (this.isBizzField('User')){
            op = [ 'IN', 'NIN', 'SFU', 'SFB' ]
        } else if (this.isBizzField('Department')){
            op = [ 'IN', 'NIN', 'SFB', 'SFD' ]
        }
        op.push('NL', 'NT')
        this.__op = op
        return op
    }
    renderValue(){
        let val = <input className="form-control form-control-sm" ref="filter-val" onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value || ''} />
        if (this.state.op == 'BW'){
            val = (
                <div className="val-range">
                    <input className="form-control form-control-sm" ref="filter-val" onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value || ''} />
                    <input className="form-control form-control-sm" ref="filter-val2" onChange={this.valueHandle} onBlur={this.valueCheck} value={this.state.value2 || ''} data-at="2" />
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
        
        VALUE_HOLD[this.state.field] = this.state.value
        return (val)
    }
    // 引用 User/Department
    isBizzField(entity) {
        if (this.state.type == 'REFERENCE'){
            const fRef = REFMETA_CACHE[this.$$$entity + '.' + this.state.field]
            if (!!!entity) return fRef && (fRef[0] == 'User' || fRef[0] == 'Department')
            else return fRef[0] == entity
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
        
        // Load
        if (this.props.field) {
            let field = this.props.field
            $(this.state.fields).each(function(){
                if (this.name == field) {
                    field = field + '----' + this.type
                    return false
                }
            })
            s2field.val(field).trigger('change')
            setTimeout(()=>{ s2op.val(that.props.op).trigger('change') }, 100)
        } else {
            s2field.trigger('change')
        }
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
            const fRef = REFMETA_CACHE[this.$$$entity + '.' + this.state.field]
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
        el.removeClass('is-invalid')
        if (!!!val){
            el.addClass('is-invalid')
        } else {
            if (this.isNumberValue()) {
                if ($regex.isDecimal(val) == false) el.addClass('is-invalid')
            } else if (this.state.type == 'DATE') {
                if ($regex.isUTCDate(val) == false) el.addClass('is-invalid')
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
        
        // Load
        if (this.props.value && this.loadedPickList == false) {
            console.log(this.props.value)
            let val = this.props.value.split('|')
            s2val.val(val).trigger('change')
            this.loadedPickList = true
        }
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
        
        // Load
        if (this.props.value && this.loadedBizzSearch == false) {
            console.log(this.props.value)
            $.get(rb.baseUrl + '/app/entity/reference-label?ids=' + $encode(this.props.value), function(res){
                for (let kid in res.data) {
                    let option = new Option(res.data[kid], kid, true, true)
                    s2val.append(option)
                }
            })
            this.loadedBizzSearch = true
        }
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
            if (OP_NOVALUE.contains(s.op)){
                // 允许无值
            } else {
                return
            }
        } else if (OP_NOVALUE.contains(s.op)){
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
