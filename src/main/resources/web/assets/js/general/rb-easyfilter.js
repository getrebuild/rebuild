/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 简易过滤器

// eslint-disable-next-line no-unused-vars
class EasyFilter extends AdvFilter {
  addItem40(props) {
    return <EasyFilterItem {...props} />
  }

  // -- USAGE

  /**
   * @param {RbForm} formObject
   */
  static check(formObject) {
    console.log(formObject)
  }
}

// eslint-disable-next-line no-undef
class EasyFilterItem extends FilterItem {
  selectOp() {
    const fieldName = this.state.field
    const fieldType = this.state.type

    // default
    let op = ['LK', 'NLK', 'EQ', 'NEQ']
    if (fieldType === 'NUMBER' || fieldType === 'DECIMAL' || fieldType === 'DATE' || fieldType === 'DATETIME' || fieldType === 'TIME') {
      op = ['GT', 'LT', 'EQ', 'GE', 'LE']
    } else if (fieldType === 'BOOL') {
      op = ['EQ']
    } else if (fieldType === 'LOCATION') {
      op = ['LK', 'NLK']
    } else if (fieldType === 'TAG' || fieldType === 'PICKLIST' || fieldType === 'STATE') {
      op = ['IN', 'NIN']
    } else if (fieldType === 'FILE' || fieldType === 'IMAGE' || fieldType === 'AVATAR') {
      op = []
    }

    if (fieldType === 'REFERENCE' && (this.isBizzField('User') || this.isBizzField('Department') || this.isBizzField('Role') || this.isBizzField('Team'))) {
      op = ['IN', 'NIN']
    }

    // eslint-disable-next-line no-undef
    if (fieldType !== 'STATE' && fieldName !== VF_CU43) op.push('NL', 'NT')
    this.__op = op
    return op
  }
}
