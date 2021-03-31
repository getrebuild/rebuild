/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// ~~ 自动新建（记录）
// eslint-disable-next-line no-undef
class ContentAutoCreate extends ActionContentSpec {
  constructor(props) {
    super(props)
  }

  render() {
    return <div>TODO</div>
  }

  buildContent() {
    return false
  }
}

// eslint-disable-next-line no-undef
renderContentComp = function (props) {
  renderRbcomp(<ContentAutoCreate {...props} />, 'react-content', function () {
    // eslint-disable-next-line no-undef
    contentComp = this
  })
}
