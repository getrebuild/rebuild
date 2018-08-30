// ~~!v1.0 数据列表
class RbList extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props, inLoad: true, rowData: [] };
        this.__fields = this.props.config.fields;
        
        this.clickRow = this.clickRow.bind(this);
        this.toggleAllRow = this.toggleAllRow.bind(this);
    }
    render() {
        return (<table className="table table-hover" ref="rblist-table">
            <thead>
                <tr>
                    <th width="50">
                        <label className="custom-control custom-control-sm custom-checkbox"><input className="custom-control-input" type="checkbox" onClick={this.toggleAllRow}/><span className="custom-control-label"></span></label>
                    </th>
                    {this.__fields.map((item) =>{
                        return (<th data-field={item.field}>{item.label}</th>)
                    })}
                </tr>
            </thead>
            <tbody>
                {this.state.rowData.map((column) => {
                    return (<tr data-id={column[this.__fields.length]} onClick={this.clickRow}>
                        <td><label className="custom-control custom-control-sm custom-checkbox"><input className="custom-control-input" type="checkbox" /><span className="custom-control-label"></span></label></td>
                        {column.map((cell, index) => {
                            return this.__renderCell(cell, index)
                        })}
                    </tr>)
                })}    
            </tbody>
        </table>);
    }
    componentDidMount() {
        this.__fetchList();
    }
    __fetchList() {
        let fields = [];
        this.__fields.forEach(function(item){
            fields.push(item.field)
        });
        let query = {
            entity: this.props.config.entity,
            fields: fields,
        };
        let that = this;
        $.post(rb.baseUrl + '/app/record-list', JSON.stringify(query), function(res){
            if (res.error_code == 0){
                that.setState({ rowData: res.data.data });
            }else{
                rb.notice(res.error_msg || '数据加载失败，请稍后重试', 'error')
            }
            $('#react-list').parent().removeClass('rb-loading-active')
        });
    }
    __renderCell(cellVal, index) {
        if (this.__fields.length == index) return null;
        if (!!!cellVal) return <td>-</td>;
        
        console.log(cellVal)
        let ft = this.__fields[index].type;
        if (ft == 'IMAGE') {
            cellVal = JSON.parse(cellVal)
            return <td>{cellVal.map((item)=>{
                return <a href={'#/Preview/' + item} className="img-thumbnail img-zoom"><img src={rb.storageUrl + item} /></a>
            })}</td>;
        } else if (ft == 'FILE') {
            cellVal = JSON.parse(cellVal);
            return <td>{cellVal.map((item)=>{
                let fileName = item.split('/');
                if (fileName.length > 1) fileName = fileName[fileName.length - 1];
                fileName = fileName.substr(15);
                return <a href={'#/Preview/' + item}>{fileName}</a>
            })}</td>;
        } else if ($.type(cellVal) == 'array'){
            return <td><a href={'#/View/' + cellVal[2] + '/' + cellVal[0]} onClick={() => this.clickView(cellVal)}>{cellVal[1]}</a></td>;
        } else {
            return <td>{cellVal || '-'}</td>;
        }
    }
    toggleAllRow(e) {
        console.log(e.currentTarget)
        let checked = $(e.currentTarget).prop('checked') == true;
        $(this.refs['rblist-table']).find('.custom-control-input').attr({ checked: checked });
    }
    clickRow(e) {
        console.log(e.currentTarget)
        $(this.refs['rblist-table']).find('.custom-control-input').attr({ checked: false });
        let checkbox = $(e.currentTarget).find('.custom-control-input');
        checkbox.attr({ checked: true });
        e.stopPropagation();
        return false;
    }
    clickView(cellVal) {
        console.log(cellVal)
        return false;
    }
}