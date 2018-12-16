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
                    <a onClick={()=>this.delete()}><i className="zmdi zmdi-delete"/></a>
                </div>
            </div>
            <div className={'chart-body rb-loading ' + (!!!this.state.chartdata && ' rb-loading-active')}>{this.state.chartdata || <RbSpinner />}</div>
        </div>)
    }
    componentDidMount() {
        this.loadChartData()
    }
    componentWillUnmount(){
        if (this.__echarts) this.__echarts.dispose()
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
    resize() {
        if (this.__echarts) this.__echarts.resize()
    }
    delete() {
        if (!confirm('确认删除？')) return
    }
    
    renderError(msg) {
        this.setState({ chartdata: (<h4 className="chart-undata">{msg || '图表加载失败'}</h4>) })
    }
    renderChart(data) {
        this.setState({ chartdata: (<div>{JSON.stringify(data)}</div>) })
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

// 表格
class ChartTable extends BaseChart {
    constructor(props) {
        super(props)
    }
}

// 折线图
class ChartLine extends BaseChart {
    constructor(props) {
        super(props)
    }
    renderChart(data) {
        let that = this
        let elid = 'echarts-line-' + (this.state.id || 'id')
        this.setState({ chartdata: (<div className="chart line" id={elid}></div>) }, ()=>{
            for (let i = 0; i < data.yyyAxis.length; i++){
                let yAxis = data.yyyAxis[i]
                yAxis.type = 'line'
                yAxis.smooth = true
                data.yyyAxis[i] = yAxis
            }
            
            let opt = {
                xAxis: {
                    type: 'category',
                    data: data.xAxis
                },
                yAxis: {
                    type: 'value'
                },
                series: data.yyyAxis,
                tooltip: {
                    trigger: 'item',
                    formatter: '{a} <br/>{b} : {c}',
                    textStyle: {
                        fontSize: 12, lineHeight: 1.3
                    }
                },
                textStyle: {
                    fontFamily: 'Roboto, "Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
                }
            }
            let c = echarts.init(document.getElementById(elid), 'light')
            c.setOption(opt)
            that.__echarts = c
        })
    }
}

// 柱状图
class ChartBar extends BaseChart {
    constructor(props) {
        super(props)
    }
    renderChart(data) {
        let that = this
        let elid = 'echarts-line-' + (this.state.id || 'id')
        this.setState({ chartdata: (<div className="chart line" id={elid}></div>) }, ()=>{
            for (let i = 0; i < data.yyyAxis.length; i++){
                let yAxis = data.yyyAxis[i]
                yAxis.type = 'bar'
                yAxis.smooth = true
                data.yyyAxis[i] = yAxis
            }
            
            let opt = {
                xAxis: {
                    type: 'category',
                    data: data.xAxis
                },
                yAxis: {
                    type: 'value'
                },
                series: data.yyyAxis,
                tooltip: {
                    trigger: 'item',
                    formatter: '{a} <br/>{b} : {c}',
                    textStyle: {
                        fontSize: 12, lineHeight: 1.3
                    }
                },
                textStyle: {
                    fontFamily: 'Roboto, "Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
                }
            }
            let c = echarts.init(document.getElementById(elid), 'light')
            c.setOption(opt)
            that.__echarts = c
        })
    }
}

// 饼图
class ChartPie extends BaseChart {
    constructor(props) {
        super(props)
    }
    renderChart(data) {
        let that = this
        let elid = 'echarts-pie-' + (this.state.id || 'id')
        this.setState({ chartdata: (<div className="chart pie" id={elid}></div>) }, ()=>{
            data = { ...data, type: 'pie', radius: '55%' }
            let opt = {
                series: [ data ],
                tooltip: {
                    trigger: 'item',
                    formatter: '{a} <br/>{b} : {c} ({d}%)',
                    textStyle: {
                        fontSize: 12, lineHeight: 1.3
                    }
                },
                textStyle: {
                    fontFamily: 'Roboto, "Hiragina Sans GB", San Francisco, "Helvetica Neue", Helvetica, Arial, PingFangSC-Light, "WenQuanYi Micro Hei", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif',
                }
            }
            let c = echarts.init(document.getElementById(elid), 'light')
            c.setOption(opt)
            that.__echarts = c
        })
    }
}

// 漏斗圖
class ChartFunnel extends BaseChart {
    constructor(props) {
        super(props)
    }
}

// 确定图表类型
const detectChart = function(cfg, id){
    let props = { config: cfg, id: id, title: cfg.title }
    if (cfg.type == 'INDEX'){
        return <ChartIndex { ...props } />
    } else if (cfg.type == 'TABLE'){
        return <ChartTable { ...props } />
    } else if (cfg.type == 'LINE'){
        return <ChartLine { ...props } />
    } else if (cfg.type == 'BAR'){
        return <ChartBar { ...props } />
    } else if (cfg.type == 'PIE'){
        return <ChartPie { ...props } />
    } else if (cfg.type == 'FUNNEL'){
        return <ChartFunnel { ...props } />
    }
}