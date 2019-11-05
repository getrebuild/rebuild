/* eslint-disable react/jsx-no-undef */

class RbFeeds extends React.Component {
  constructor(props) {
    super(props)
  }
  render() {
    return <React.Fragment>
      <FeedsPost ref={(c) => this._post = c} call={this.postAfter} />
      <FeedsList ref={(c) => this._list = c} />
    </React.Fragment>
  }
  postAfter = () => this._list.fetchFeeds()
}

$(document).ready(function () {
  renderRbcomp(<RbFeeds />, 'rb-feeds')
})