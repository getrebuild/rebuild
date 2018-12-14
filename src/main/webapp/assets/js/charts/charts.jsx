class BaseChart extends React.Component {
    constructor(props) {
        super(props)
        this.state = {}
    }
    render() {
        return (<div className="chart-box">
            <div className="chart-head">
                <div className="chart-title text-truncate">{this.state.title}</div>
                <div className="chart-oper">
                    <a><i className="zmdi zmdi-refresh"/></a>
                    <a href={'chart-design?id=' + this.props.id}><i className="zmdi zmdi-edit"/></a>
                    <a><i className="zmdi zmdi-delete"/></a>
                </div>
            </div>
            <div className={'chart-body rb-loading ' + (!!!this.state.chartbox && ' rb-loading-active')}>{this.state.chartbox || <RbSpinner />}</div>
        </div>)
    }
    componentDidMount() {
        // TODO 获取数据
//        this.renderChart()
    }
    renderChart() {
    }
}

// 指标卡
class ChartIndex extends BaseChart {
    constructor(props) {
        super(props)
    }
    renderChart() {
    }
}