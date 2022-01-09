/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/
/* eslint-disable no-unused-vars */

// ~~ 高级表格

class ProTable extends React.Component {
  constructor(props) {
    super(props)
    this.state = {}
  }

  render() {
    return <div className="protable">ProTable</div>
  }

  componentDidMount() {
    const entity = this.props.entity
    const initialValue = {
      '$MAINID$': this.props.mainid || '$MAINID$',
    }

    $.post(`/app/${entity.entity}/form-model?id=`, JSON.stringify(initialValue), (res) => {
      console.log(res)
    })
  }
}

class ProTableForm extends RbForm {
  constructor(props) {
    super({ ...props, rawModel: {} })
    this.state = { editable: props.editable === true }
  }

  render() {
    return (
      <tr>
        {this.props.fields.map((item) => {
          return this.state.editable ? this.renderCell(item) : this.renderViewCell(item)
        })}
      </tr>
    )
  }
}
