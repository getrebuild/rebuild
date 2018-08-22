// ~~!v1.0 表单
class RbForm extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
    }
    render() {
        return (
            <div className="rb-form">
            <form>
                {this.props.children}
            </form>
            </div>
        );
    }
}

class RbFormElement extends React.Component {
    constructor(props) {
        super(props);
        this.state = { ...props };
    }
}

class RbFormText extends RbFormElement {
    constructor(props) {
        super(props);
    }
    render() {
        return (
            <div className="form-group row">
                <label className="col-12 col-sm-2 col-form-label text-sm-right">{this.state.fieldLabel}</label>
                <div className="col-12 col-sm-8 col-lg-4">
                    <input className="form-control form-control-sm" type="text" id="{this.state.fieldName}" value="${this.state.defaultValue}" maxlength="{this.state.maxLength || 200}"/>
                </div>
            </div>
        )
    }
    getValue() {
    }
    validate() {
    }
}

class RbFormTextarea extends RbFormElement {
    constructor(props) {
        super(props);
    }
    render() {
        return (
            <div className="form-group row">
                <label className="col-12 col-sm-2 col-form-label text-sm-right">{this.state.fieldLabel}</label>
                <div className="col-12 col-sm-8 col-lg-4">
                    <textarea class="form-control form-control-sm row2" id="{this.state.fieldName}" maxlength="{this.state.maxLength || 200}">${this.state.defaultValue}</textarea>
                </div>
            </div>
        )
    }
    getValue() {
    }
    validate() {
    }
}

const renderRbform = function(config) {
    const form = (<RbForm>
            </RbForm>);
        
    ReactDOM.render(form, document.getElementById('form-container'));
    return form;
};