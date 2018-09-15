// ~~!v1.0 数据列表
class RbList extends React.Component {
    constructor(props) {
        super(props)
        let fields = props.config.fields
        props.config.fields = null
        this.state = { ...props, fields: fields, rowData: [], noData: false, checkedAll: false, pageNo: 1, pageSize: 3 }
        
        this.toggleAllRow = this.toggleAllRow.bind(this)
        this.setPageNo = this.setPageNo.bind(this)
        this.setPageSize = this.setPageSize.bind(this)
        
        this.__defaultColumnWidth = $('#react-list').width() / 10
        this.__defaultColumnWidth = 130;
    }
    render() {
        let that = this;
        return (
        <div>
            <div className="row rb-datatable-body">
            <div className="col-sm-12">
                <div className="rb-scroller" ref="rblist-scroller">
                    <table className="table table-hover table-striped" ref="rblist-table">
                    <thead>
                        <tr>
                            <th className="column-checkbox">
                                <div><label className="custom-control custom-control-sm custom-checkbox"><input className="custom-control-input" type="checkbox" checked={this.state.checkedAll} onClick={this.toggleAllRow} /><span className="custom-control-label"></span></label></div>
                            </th>
                            {this.state.fields.map((item, index) =>{
                                let columnWidth = (item.width || that.__defaultColumnWidth) + 'px'
                                let styles = { width:columnWidth }
                                let haveSort = item.sort || ''
                                return (<th data-field={item.field} style={styles} className="sortable unselect" onClick={this.fieldSort.bind(this,item.field)}><div style={styles}>{item.label}<i className={'zmdi ' + haveSort}></i><i className="split"></i></div></th>)
                            })}
                            <th className="column-empty"></th>
                        </tr>
                    </thead>
                    <tbody>
                        {this.state.rowData.map((item, index) => {
                            let lastId = item[this.state.fields.length];
                            return (<tr data-id={lastId[0]} onClick={this.clickRow.bind(this, index, false)}>
                                <td className="column-checkbox">
                                    <div><label className="custom-control custom-control-sm custom-checkbox"><input className="custom-control-input" type="checkbox" checked={lastId[1]} onClick={this.clickRow.bind(this, index, true)} /><span className="custom-control-label"></span></label></div>
                                </td>
                                {item.map((cell, index) => {
                                    return this.__renderCell(cell, index)
                                })}
                                <td className="column-empty"></td>
                            </tr>)
                        })}    
                    </tbody>
                    </table>
                </div>
            </div></div>
            <RbListPagination pageNo={this.state.pageNo} pageSize={this.state.pageSize} rowTotal={this.state.rowTotal} $$$parent={this} />
        </div>);
    }
    componentDidMount() {
        const scroller = $(this.refs['rblist-scroller'])
        scroller.perfectScrollbar()
        let that = this;
        scroller.find('th .split').draggable({ containment: '.rb-datatable-body', axis: 'x', helper: 'clone', stop: function(event, ui){
            let field = $(event.target).parent().parent().data('field');
            let left = ui.position.left - 4;
            let fields = that.state.fields;
            for (let i = 0; i < fields.length; i++){
                if (fields[i].field == field){
                    fields[i].width = left;
                    break;
                }
            }
            that.setState({ fields: fields }, function(){
                //scroller.perfectScrollbar('update')
            })
        }})
        this.fetchList()
    }
    componentDidUpdate() {
    }
    
    fetchList() {
        let fields = [];
        let field_sort = null;
        this.state.fields.forEach(function(item){
            fields.push(item.field)
            if (!!item.sort) field_sort = item.field + ':' + item.sort.replace('sort-', '')
        });
        let query = {
            entity: this.props.config.entity,
            fields: fields,
            pageNo: this.state.pageNo,
            pageSize: this.state.pageSize,
            sort: field_sort,
            reload: true,
        };
        let that = this;
        $('#react-list').addClass('rb-loading-active')
        $.post(rb.baseUrl + '/app/entity/record-list', JSON.stringify(query), function(res){
            if (res.error_code == 0){
                let _rowData = res.data.data;
                if (_rowData.length == 0) {
                    that.setState({ noData: true });
                } else {
                    let lastIndex = _rowData[0].length - 1;
                    _rowData = _rowData.map((item) => {
                        item[lastIndex] = [item[lastIndex], false]  // [ID, Checked?]
                        return item;
                    })
                }
                that.setState({ rowData: _rowData, rowTotal: res.data.total });
            }else{
                rb.notice(res.error_msg || '数据加载失败，请稍后重试', 'error')
            }
            $('#react-list').removeClass('rb-loading-active')
        });
    }
    
    // 渲染表格及相关事件处理
    
    __renderCell(cellVal, index) {
        if (this.state.fields.length == index) return null;
        if (!!!cellVal) return <td><div></div></td>;
        
        let ft = this.state.fields[index].type;
        let styles = { width: (this.state.fields[index].width || this.__defaultColumnWidth) + 'px' }
        if (ft == 'IMAGE') {
            cellVal = JSON.parse(cellVal)
            return <td><div style={styles}>{cellVal.map((item)=>{
                return <a href={'#!/Preview/' + item} className="img-thumbnail img-zoom"><img src={rb.storageUrl + item} /></a>
            })}<div className="clearfix" /></div></td>;
        } else if (ft == 'FILE') {
            cellVal = JSON.parse(cellVal);
            return <td><div style={styles}>{cellVal.map((item)=>{
                let fileName = item.split('/');
                if (fileName.length > 1) fileName = fileName[fileName.length - 1];
                fileName = fileName.substr(15);
                return <a href={'#!/Preview/' + item}>{fileName}</a>
            })}</div></td>;
        } else if ($.type(cellVal) == 'array'){
            return <td><div style={styles}><a href={'#!/View/' + cellVal[2] + '/' + cellVal[0]} onClick={() => this.clickView(cellVal)}>{cellVal[1]}</a></div></td>;
        } else {
            return <td><div style={styles}>{cellVal || ''}</div></td>;
        }
    }
    toggleAllRow(e) {
        let checked = this.state.checkedAll == false;
        let _rowData = this.state.rowData;
        _rowData = _rowData.map((item) => {
            item[item.length - 1][1] = checked;  // Checked?
            return item;
        });
        this.setState({ checkedAll: checked, rowData: _rowData });
        return false;
    }
    clickRow(rowIndex, holdOthers, e) {
        if (e.target.tagName == 'SPAN') return false;
        e.stopPropagation()
        e.nativeEvent.stopImmediatePropagation()
        
        let _rowData = this.state.rowData;
        let lastIndex = _rowData[0].length - 1;
        if (holdOthers == true){
            let item = _rowData[rowIndex];
            item[lastIndex][1] = item[lastIndex][1] == false;  // Checked?
            _rowData[rowIndex] = item;
        } else {
            _rowData = _rowData.map((item, index) => {
                item[lastIndex][1] = index == rowIndex;
                return item;
            })
        }
        this.setState({ rowData: _rowData });
        return false;
    }
    clickView(cellVal) {
        console.log(cellVal)
        return false;
    }
    
    // 分页
    
    setPageNo(pageNo) {
        console.log(pageNo)
        let that = this
        this.setState({ pageNo: pageNo || 1 }, function(){
            that.fetchList()
        })
    }
    setPageSize(pageSize) {
        console.log(pageSize)
        let that = this
        this.setState({ pageNo: 1, pageSize: pageSize || 20 }, function(){
            that.fetchList()
        })
    }
    
    // 外部接口
    
    getSelectedId() {
        let ids = []
        let lastIndex = this.state.rowData[0].length - 1;
        for (let i = 0; i < this.state.rowData.length; i++) {
            let last = this.state.rowData[i][lastIndex];
            if (last[1] == true) ids.push(last[0]);
        }
        return ids;
    }
    
    search() {
    }
    
    reload() {
    }
    
    // 配置相关
    
    fieldSort(field, e) {
        let fields = this.state.fields;
        for (let i = 0; i < fields.length; i++){
            if (fields[i].field == field){
                if (fields[i].sort == 'sort-asc') fields[i].sort = 'sort-desc';
                else fields[i].sort = 'sort-asc';
            } else {
                fields[i].sort = null
            }
        }
        let that = this
        this.setState({ fields: fields }, function(){
            that.fetchList()
        })
        
        e.stopPropagation()
        e.nativeEvent.stopImmediatePropagation()
        return false
    }
}

// 分页组件
class RbListPagination extends React.Component {
    constructor(props) {
        super(props)
        this.prevPage = this.prevPage.bind(this)
        this.nextPage = this.nextPage.bind(this)
    }
    
    render() {
        this.__pageTotal = Math.ceil(this.props.rowTotal / this.props.pageSize);
        if (this.__pageTotal <= 0) this.__pageTotal = 1;
        let pageTotalShow = [];
        for (let i = 1; i <= this.__pageTotal; i++) pageTotalShow.push(i)
        
        return (
        <div className="row rb-datatable-footer">
            <div className="col-sm-5">
                <div className="dataTables_info">共 {this.props.rowTotal} 条数据</div>
            </div>
            <div className="col-sm-7">
                <div className="dataTables_paginate paging_simple_numbers">
                    <ul className="pagination">
                        <li className="paginate_button page-item previous disabled"><a href="javascript:;" className="page-link" onClick={this.prevPage}><span className="icon zmdi zmdi-chevron-left"></span></a></li>
                        {pageTotalShow.map((item) => {
                            return <li className={'paginate_button page-item ' + (this.props.pageNo == item && 'active')}><a href="javascript:;" className="page-link" onClick={this.gotoPage.bind(this, item)}>{item}</a></li>
                        })}
                        <li className="paginate_button page-item next"><a href="javascript:;" className="page-link" onClick={this.nextPage}><span className="icon zmdi zmdi-chevron-right"></span></a></li>
                    </ul>
                </div>
            </div>
        </div>
        )
    }
    
    prevPage() {
        if (this.props.pageNo == 1) return;
        else this.props.$$$parent.setPageNo(this.props.pageNo - 1)
    }
    
    nextPage() {
        if (this.props.pageNo == __pageTotal) return;
        else this.props.$$$parent.setPageNo(this.props.pageNo + 1)
    }
    
    gotoPage(pageNo) {
        if (this.props.pageNo == pageNo) return;
        else this.props.$$$parent.setPageNo(pageNo)
    }
}