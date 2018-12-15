class BaseChart extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
        return (<div className="chart-box">
            <div className="chart-head">
                <div className="chart-title text-truncate">{this.state.title}</div>
                <div className="chart-oper">
                    <a onClick={()=>this.loadChartData()}><i className="zmdi zmdi-refresh"/></a>
                    <a href={'chart-design?id=' + this.props.id}><i className="zmdi zmdi-edit"/></a>
                    <a onClick={()=>this.deleteChart()}><i className="zmdi zmdi-delete"/></a>
                </div>
            </div>
            <div className={'chart-body rb-loading ' + (!!!this.state.chartdata && ' rb-loading-active')}>{this.state.chartdata || <RbSpinner />}</div>
        </div>)
    }
    componentDidMount() {
        this.loadChartData()
    }
    loadChartData() {
        this.setState({ chartdata: null })
        let url = !!this.state.id ? ('/dashboard/chart-data?id=' + this.state.id) : '/dashboard/chart-preview' 
        let that = this
        $.post(rb.baseUrl + url, JSON.stringify(this.state.config || {}), (res)=>{
            if (res.error_code == 0) that.renderChart(res.data)
            else that.renderError(res.error_msg)
        })
    }
    deleteChart() {
        if (!confirm('确认删除？')) return
        
    }
    
    renderChart(data) {
        this.setState({ chartdata: (<div>{JSON.stringify(data)}</div>) })
    }
    renderError(msg) {
        this.setState({ chartdata: (<h3 className="undata">{msg || '图表加载错误'}</h3>) })
    }
}

// 指标卡
class ChartIndex extends BaseChart {
    constructor(props) {
        super(props)
        this.label = this.state.title
        this.state.title = null
    }
    renderChart(data) {
        let chartdata = (<div className="chart index">
            <div className="data-item must-center text-truncate">
                <p>{data.index.label || this.label}</p>
                <strong>{data.index.data}</strong>
            </div>
        </div>)
        this.setState({ chartdata: chartdata })
    }
}

// 确定图表类型
const detectChart = function(cfg, id){
    if (cfg.type == 'INDEX'){
        return <ChartIndex config={cfg} id={id} title={cfg.title} />
    }
}