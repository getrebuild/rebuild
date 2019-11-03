function FeedsPost(props) {

    return <div>
        <ul>
            <li><a>动态</a></li>
            <li><a>跟进</a></li>
        </ul>
        <div>
            <textarea className="form-control"></textarea>
        </div>
        <div>
            <div className="float-right">
                <button className="btn btn-primary">发布</button>
            </div>
            <div className="clearfix" />
        </div>
    </div>
}

$(document).ready(function () {
    renderRbcomp(<FeedsPost />,'feedsPost')
})