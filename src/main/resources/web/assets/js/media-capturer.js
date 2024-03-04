/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

// css width
const _VIDEO_WIDTH = 1000

// eslint-disable-next-line no-unused-vars
class MediaCapturer extends RbModal {
  constructor(props) {
    super(props)

    this._recVideo = props.type === 'video' || props.type === '*'
    this._recImage = props.type === 'image' || props.type === '*'
    this._mediaRecorder = null
    this._blobs = []
  }

  renderContent() {
    return (
      <div className={`media-capture ${this.state.captured && 'captured'} ${this.state.recording && 'recording'}`}>
        {this.state.initMsg && <div className="must-center text-muted fs-14">{this.state.initMsg}</div>}

        <video autoPlay ref={(c) => (this._$camera = c)} controls={false}></video>
        <div className="results">
          <video controls controlsList="nodownload" ref={(c) => (this._$resVideo = c)} className={this.state.recType === 'video' ? '' : 'hide'}></video>
          <canvas ref={(c) => (this._$resImage = c)} className={this.state.recType === 'image' ? '' : 'hide'}></canvas>
        </div>

        <div className="action" ref={(c) => (this._$btn = c)}>
          <input type="file" className="hide" ref={(c) => (this._$fileinput = c)} />
          <button className="btn btn-primary J_used" type="button" onClick={() => this.handleConfirm()}>
            <i className="icon mdi mdi-check" /> {$L('使用')}
          </button>
          <button className="btn btn-secondary J_reset w-auto" type="button" onClick={() => this.initDevice(null, $storage.get('MediaCapturerDeviceId'))} title={$L('重拍')}>
            <i className="icon mdi mdi-restore" />
          </button>

          {this._recVideo && (
            <button className="btn btn-secondary J_capture-video" type="button" onClick={() => this.captureVideo()}>
              {this.state.recording ? $L('停止') : $L('录制')}
            </button>
          )}
          {this._recImage && (
            <button className="btn btn-secondary J_capture-image" type="button" onClick={() => this.captureImage()} disabled={this.state.recording === true}>
              <i className="icon mdi mdi-camera" /> {$L('拍照')}
            </button>
          )}

          {this.state.webcamList && this.state.webcamList.length > 0 && (
            <span className="dropdown">
              <button className="btn btn-secondary dropdown-toggle w-auto J_webcam" type="button" data-toggle="dropdown" title={$L('选择设备')} disabled={this.state.recording === true}>
                <i className="icon mdi mdi-webcam" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                {this.state.webcamList.map((c) => {
                  return (
                    <a className="dropdown-item" key={c[0]} onClick={() => this.initDevice(null, c[0])}>
                      {c[1]}
                    </a>
                  )
                })}
              </div>
            </span>
          )}
        </div>
      </div>
    )
  }

  componentDidMount() {
    super.componentDidMount()

    if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
      this.initDevice(null, $storage.get('MediaCapturerDeviceId'))

      navigator.mediaDevices
        .enumerateDevices()
        .then((devices) => {
          const devices2 = devices.filter((device) => device.kind === 'videoinput')
          const devices3 = []
          devices2.forEach((device, idx) => {
            devices3.push([device.deviceId, device.label || idx])
          })
          this.setState({ webcamList: devices3 })
        })
        .catch((err) => {
          console.log(err)
        })
    } else {
      this.setState({ initMsg: $L('你的浏览器不支持此功能') })
    }

    if (this.props.forceFile) {
      $initUploader(
        this._$fileinput,
        () => {
          if (!$mp.isStarted()) {
            $mp.start()
            $(this._$btn).button('loading')
          }
        },
        (res) => {
          $mp.end()
          $(this._$btn).button('reset')
          typeof this.props.callback === 'function' && this.props.callback(res.key)
          this.hide()
        },
        () => {
          $mp.end()
          $(this._$btn).button('reset')
        }
      )
    }
  }

  initDevice(cb, deviceId) {
    this.setState({ captured: false, initMsg: $L('请稍后') })
    if (deviceId) {
      this._stopTracks()
      $storage.set('MediaCapturerDeviceId', deviceId)
    }

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

        if (this._recVideo) {
          this._mediaRecorder = new MediaRecorder(stream)
          this._mediaRecorder.addEventListener('dataavailable', (e) => {
            if (e.data && e.data.size > 0) this._blobs.push(e.data)
          })
          this._mediaRecorder.addEventListener('stop', () => {
            this._capturedData = new Blob(this._blobs, { type: 'video/mp4' })
            const videoBlobURL = URL.createObjectURL(this._capturedData)
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

  captureImage() {
    this.initDevice(() => {
      const ratio = Math.max(window.devicePixelRatio || 1, 2)
      this._$resImage.style.width = _VIDEO_WIDTH
      this._$resImage.style.height = (_VIDEO_WIDTH / 4) * 3
      this._$resImage.width = _VIDEO_WIDTH * ratio
      this._$resImage.height = (_VIDEO_WIDTH / 4) * 3 * ratio

      const context = this._$resImage.getContext('2d')
      context.scale(ratio, ratio)
      context.drawImage(this._$camera, 0, 0, _VIDEO_WIDTH, (_VIDEO_WIDTH / 4) * 3)
      this._capturedData = this._$resImage.toDataURL('image/jpeg')

      this._stopTracks()
      this.setState({ captured: true, initMsg: null, recType: 'image' })
    })
  }

  captureVideo() {
    console.log('MediaRecorder state :', this._mediaRecorder.state)
    this.initDevice(() => {
      if (this._mediaRecorder.state === 'inactive') {
        this._blobs = []
        this._mediaRecorder.start()
        this.setState({ recording: true, initMsg: null, recType: 'video' })
      } else if (this._mediaRecorder.state === 'recording') {
        this._mediaRecorder.stop()
        this._stopTracks()
        this.setState({ recording: false })
        // @see stop event
      }
    })
  }

  _stopTracks() {
    const stream = this._$camera.srcObject
    stream &&
      stream.getTracks().forEach(function (track) {
        track.stop()
      })
    this._$camera.srcObject = null
  }

  _dataurl2File(dataurl, filename) {
    const arr = dataurl.split(',')
    const mime = arr[0].match(/:(.*?);/)[1]
    const bstr = atob(arr[1])
    let n = bstr.length
    const u8arr = new Uint8Array(n)
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n)
    }
    return new File([u8arr], filename, { type: mime })
  }

  handleConfirm() {
    let dataOrFile = this._capturedData
    if (this.props.forceFile) {
      const time = moment().format('YYYYMMDDHHmmss')
      if (this.state.recType === 'video') {
        dataOrFile = new File([dataOrFile], `RBVID-${time}.mp4`, { type: 'video/mp4' })
      } else {
        dataOrFile = this._dataurl2File(dataOrFile, `RBIMG-${time}.jpg`)
      }

      const dataTransfer = new DataTransfer()
      dataTransfer.items.add(dataOrFile)
      this._$fileinput.files = dataTransfer.files
      $(this._$fileinput).trigger('change')
    } else {
      typeof this.props.callback === 'function' && this.props.callback(dataOrFile)
      this.hide()
    }
  }
}
