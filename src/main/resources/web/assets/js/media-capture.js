/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _VIDEO_WIDTH = 768

// eslint-disable-next-line no-unused-vars
class MediaCapture extends RbModal {
  constructor(props) {
    super(props)

    this._mediaRecorder = null
    this.__blobs = []
  }

  renderContent() {
    return (
      <div className={`media-capture ${this.props.type === 'video' ? 'video' : 'image'} ${this.state.captured && 'captured'} ${this.state.recording && 'recording'}`}>
        {this.state.initMsg && <div className="must-center text-muted fs-14">{this.state.initMsg}</div>}

        <video autoPlay ref={(c) => (this._$camera = c)}></video>
        <div className="results">
          {this.props.type === 'video' ? <video controls controlsList="nodownload" ref={(c) => (this._$resVideo = c)}></video> : <canvas ref={(c) => (this._$resImage = c)}></canvas>}
        </div>

        <div className="action">
          <button className="btn btn-primary J_used" type="button" onClick={() => this.handleConfirm()}>
            {$L('确定')}
          </button>
          <button className="btn btn-secondary J_reset" type="button" onClick={() => this.initDevice()}>
            {$L('重拍')}
          </button>
          <button className="btn btn-secondary J_capture" type="button" onClick={() => this.capture()}>
            {this.props.type === 'video' ? (this.state.recording ? $L('停止') : $L('录制')) : $L('拍照')}
          </button>
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      this.initDevice()

      // All
      navigator.mediaDevices
        .enumerateDevices()
        .then((devices) => {
          this.__videoDevices = []
          const videoDevices = devices.filter((device) => device.kind === 'videoinput')
          videoDevices.forEach((device, idx) => {
            this.__videoDevices.push(device.deviceId, device.label || idx)
          })
          console.log(this.__videoDevices)
        })
        .catch((err) => {
          console.log(err)
        })
    } else {
      this.setState({ initMsg: $L('你的浏览器不支持此功能') })
    }
  }

  initDevice(cb, deviceId) {
    this.setState({ captured: false, initMsg: $L('请稍后') })

    if (this._$camera && this._$camera.srcObject) {
      typeof cb === 'function' && cb()
      return
    }

    const ps = { video: true }
    if (deviceId) ps.video = { deviceId: deviceId }
    navigator.mediaDevices
      .getUserMedia(ps)
      .then((stream) => {
        this._$camera.srcObject = stream

        if (this.props.type === 'video') {
          this._mediaRecorder = new MediaRecorder(stream)
          this._mediaRecorder.addEventListener('dataavailable', (e) => {
            if (e.data && e.data.size > 0) this.__blobs.push(e.data)
          })
          this._mediaRecorder.addEventListener('stop', () => {
            this._capturedData = this.__blobs

            const videoBlob = new Blob(this.__blobs, { type: 'video/mp4' })
            const videoBlobURL = URL.createObjectURL(videoBlob)
            this._$resVideo.src = videoBlobURL
            this.setState({ captured: true, initMsg: null })
          })
        }

        this.setState({ initMsg: null })
        typeof cb === 'function' && cb()
      })
      .catch((err) => {
        console.log(err)
        this.setState({ initMsg: $L('无法访问摄像头') })
      })
  }

  componentWillUnmount() {
    if (this._mediaRecorder) this._mediaRecorder.stop()
    this._stopTracks()
  }

  capture() {
    this.initDevice(() => {
      if (this.props.type === 'video') this.captureVideo()
      else this.captureImage()
    })
  }

  captureImage() {
    const context = this._$resImage.getContext('2d')
    this._$resImage.width = _VIDEO_WIDTH
    this._$resImage.height = (_VIDEO_WIDTH / 4) * 3
    context.drawImage(this._$camera, 0, 0, _VIDEO_WIDTH, (_VIDEO_WIDTH / 4) * 3)
    this._capturedData = this._$resImage.toDataURL('image/png')

    this._stopTracks()
    this.setState({ captured: true, initMsg: null })
  }

  captureVideo() {
    console.log('MediaRecorder state :', this._mediaRecorder.state)
    if (this._mediaRecorder.state === 'inactive') {
      this.__blobs = []
      this._mediaRecorder.start()
      this.setState({ recording: true, initMsg: null })
    } else if (this._mediaRecorder.state === 'recording') {
      this._mediaRecorder.stop()
      this._stopTracks()
      this.setState({ recording: false })
      // @see stop event
    }
  }

  _stopTracks() {
    const stream = this._$camera.srcObject
    stream &&
      stream.getTracks().forEach(function (track) {
        track.stop()
      })
    this._$camera.srcObject = null
  }

  handleConfirm() {
    console.log(this._capturedData)
    typeof this.props.callback === 'function' && this.props.callback(this._capturedData)
  }
}
