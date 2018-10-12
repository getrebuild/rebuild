// 简单过滤器
const SimpleFilter = {
    init(el, entity) {
        this.root = $(el)
        this.entity = entity
        
        this.initEvent()
        this.loadFilter()
    },
    initEvent() {
        let that = this
        let btn = this.root.find('.J_search-btn').click(function(){
            let val = $val(that.root.find('.J_search-text'))
            that.fireFilter(val)
        })
        this.root.find('.J_search-text').keydown(function(event){
            if (event.which == 13) btn.trigger('click')
        })
        this.root.find('.J_qfields').click(function(event){
            that.showQFieldsModal()
        })
    },
    
    loadFilter() {
        let that = this
        $.get(`${rb.baseUrl}/app/${this.entity}/advfilter/simple`, function(res){
            that.filterExp = res.data || { items: [] }
            let qFields = []
            that.filterExp.items.forEach(function(item){
                qFields.push(item.label)
            })
            that.root.find('.J_search-text').attr('placeholder', '搜索 ' + qFields.join('/'))
        })
    },
    fireFilter(val) {
        if (!this.filterExp || this.filterExp.items.length == 0){
            rb.notice('请先设置查询字段')
            let that = this
            $setTimeout(function(){
                that.showQFieldsModal()
            }, 1500)
            return
        }
        
        this.filterExp.values = { 1: val }
        if (window.rbList) rbList.search(this.filterExp)
        else console.log('No rbList - ' + this.filterExp)
    },
    
    showQFieldsModal() {
        if (this.qfieldsModal) this.qfieldsModal.show()
        else this.qfieldsModal = rb.modal(`${rb.baseUrl}/page/general-entity/query-fields?entity=${this.entity}`, '设置查询字段')
    },
    hideQFieldsModal() {
        if (this.qfieldsModal) this.qfieldsModal.hide()
    }
};

// ~~ 高级过滤器
class AdvFilter extends React.Component {
    constructor(props) {
        super(props)
        this.state = { ...props }
    }
    render() {
    }
}