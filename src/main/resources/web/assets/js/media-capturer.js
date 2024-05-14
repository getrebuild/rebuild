/*!
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

const _IDEAL_VIDW = 1024
const _IDEAL_VIDH = 768

// eslint-disable-next-line no-unused-vars
class MediaCapturer extends RbModal {
  constructor(props) {
    super(props)

    this._recVideo = props.type === 'video' || props.type === '*'
    this._recImage = props.type === 'image' || props.type === '*'
    this._mediaRecorder = null
    this._blobs = []

    // 1024, 768
    this._videoWidth = props.width || _IDEAL_VIDW
    this._imageCapture = false
  }

  renderContent() {
    return (
      <div className={`media-capture ${this._videoWidth < _IDEAL_VIDW && 'media-capture-md'} ${this.state.captured && 'captured'} ${this.state.recording && 'recording'}`}>
        {this.state.initMsg && <div className="must-center text-muted fs-14">{this.state.initMsg}</div>}

        <video autoPlay ref={(c) => (this._$camera = c)} controls={false}></video>
        <div className="results">
          <video controls controlsList="nodownload" ref={(c) => (this._$resVideo = c)} className={this.state.recType === 'video' ? '' : 'hide'}></video>
          <canvas ref={(c) => (this._$resImage = c)} className={this.state.recType === 'image' ? '' : 'hide'}></canvas>
          <div className={this.state.recType === 'image' ? '' : 'hide'}>
            <img ref={(c) => (this._$resImage2 = c)} />
          </div>
        </div>

        <div className={`action ${this.state.unsupportted && 'hide'}`} ref={(c) => (this._$btn = c)}>
          <input type="file" className="hide" ref={(c) => (this._$fileinput = c)} />
          <button className="btn btn-primary J_used" type="button" onClick={() => this.handleConfirm()}>
            <i className="icon mdi mdi-check" /> {$L('使用')}
          </button>
          <button className="btn btn-secondary J_reset w-auto" type="button" onClick={() => this.initDevice(null, $storage.get('MediaCapturerDeviceId'))} title={$L('重拍')}>
            <i className="icon mdi mdi-restore" />
          </button>

          {this._recVideo && (
            <button className="btn btn-secondary J_capture-video" type="button" onClick={() => this.takeVideo()}>
              {this.state.recording ? $L('停止') : $L('录制')}
              {this.state.recording && <span className="ml-1">{$sec2Time(this.state.recording)}</span>}
            </button>
          )}
          {this._recImage && (
            <button className="btn btn-secondary J_capture-image" type="button" onClick={() => this.takeImage()} disabled={this.state.recording}>
              <i className="icon mdi mdi-camera" /> {$L('拍照')}
            </button>
          )}

          {this.state.webcamList && this.state.webcamList.length > 0 && (
            <span className="dropdown">
              <button className="btn btn-secondary dropdown-toggle w-auto J_webcam" type="button" data-toggle="dropdown" title={$L('选择设备')} disabled={this.state.recording}>
                <i className="icon mdi mdi-webcam" />
              </button>
              <div className="dropdown-menu dropdown-menu-right">
                {this.state.webcamList.map((c) => {
                  return (
                    <a className="dropdown-item" key={c.deviceId} onClick={() => this.initDevice(null, c.deviceId)}>
                      {c.label || '0'}
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

    if (!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)) {
      this.setState({ initMsg: $L('你的浏览器不支持此功能'), unsupportted: true })
      return
    }

    this.initDevice(null, $storage.get('MediaCapturerDeviceId'))

    navigator.mediaDevices
      .enumerateDevices()
      .then((devices) => {
        const vidDevices = devices.filter((device) => device.deviceId && device.kind === 'videoinput')
        this.setState({ webcamList: vidDevices })
      })
      .catch((err) => {
        console.log('enumerateDevices fails :', err)
      })

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

    // https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia
    const constraints = {
      video: {
        width: { ideal: _IDEAL_VIDW * 3 },
        height: { ideal: _IDEAL_VIDH * 3 },
      },
      audio: this.props.recordAudio === true,
    }
    if (deviceId) constraints.video = { deviceId: deviceId }

    navigator.mediaDevices
      .getUserMedia(constraints)
      .then((s) => {
        this._$camera.srcObject = s
        this._deviceId = s.id

        try {
          const track = s.getVideoTracks()[0]
          this._cameraSettings = {
            width: track.getSettings().width || _IDEAL_VIDW,
            height: track.getSettings().height || _IDEAL_VIDH,
          }

          if (this._imageCapture !== false) {
            this._imageCapture = new ImageCapture(track)
            this._imageCapture.getPhotoCapabilities().then((c) => {
              let { imageWidth, imageHeight } = c
              if (imageWidth && imageWidth.max && imageHeight && imageHeight.max) {
                this._cameraSettings = {
                  width: imageWidth.max,
                  height: imageHeight.max,
                }
              }
            })
          }

          console.log('Use webcam size :', this._cameraSettings)
        } catch (err) {
          // ignored
        }

        if (this._recVideo) {
          this._mediaRecorder = new MediaRecorder(s)
          this._mediaRecorder.addEventListener('dataavailable', (e) => {
            if (e.data && e.data.size > 0) this._blobs.push(e.data)
          })
          this._mediaRecorder.addEventListener('stop', () => {
            this._capturedData = new Blob(this._blobs, { type: 'video/mp4' })
            this._$resVideo.src = URL.createObjectURL(this._capturedData)
            this.setState({ captured: true, initMsg: null })
          })
        }

        this.setState({ initMsg: null })
        typeof cb === 'function' && cb()
      })
      .catch((err) => {
        console.log('getUserMedia fails :', err)
        this.setState({ initMsg: $L('无法访问摄像头') })
      })
  }

  componentWillUnmount() {
    if (this._mediaRecorder) this._mediaRecorder.stop()
    this._stopTracks()
  }

  takeImage() {
    this.initDevice(() => {
      // https://developer.mozilla.org/en-US/docs/Web/API/ImageCapture
      if (this._imageCapture) {
        this._takeImageUseImageCapture()
        return
      }
      $(this._$resImage2).parent().remove()

      const canvasWidth = this._videoWidth
      const canvasHeight = (canvasWidth / 4) * 3
      this._$resImage.width = canvasWidth
      this._$resImage.height = canvasHeight
      this._$resImage.style.width = canvasWidth
      this._$resImage.style.height = canvasHeight

      let width = this._videoWidth
      let height = (width / 4) * 3
      if (this._cameraSettings && this._cameraSettings.width && this._cameraSettings.height) {
        width = this._cameraSettings.width
        height = this._cameraSettings.height
      }

      // const ratio = Math.max(window.devicePixelRatio || 1, 2)
      // scale
      const scale = Math.min(canvasWidth / width, canvasHeight / height)
      width = width * scale
      height = height * scale
      // center
      const x = (canvasWidth - width) / 2
      const y = (canvasHeight - height) / 2

      const ctx2d = this._$resImage.getContext('2d')
      ctx2d.clearRect(0, 0, width, height)
      ctx2d.drawImage(this._$camera, x, y, width, height)
      // 水印
      if (this.props.watermark) {
        ctx2d.font = '16px Arial'
        ctx2d.fillStyle = 'white'
        ctx2d.fillText(moment().format('YYYY-MM-DD HH:mm:ss'), 20, 30)
        ctx2d.fillText('DeviceNo : ' + this._deviceId || '', 20, 30 + 20)
      }
      this._capturedData = this._$resImage.toDataURL('image/jpeg', 1.0)

      this._stopTracks()
      this.setState({ captured: true, initMsg: null, recType: 'image' })
    })
  }

  _takeImageUseImageCapture() {
    const that = this
    this._imageCapture
      .takePhoto({
        imageWidth: this._cameraSettings.width,
        imageHeight: this._cameraSettings.height,
      })
      .then((blob) => {
        const reader = new FileReader()
        reader.readAsDataURL(blob)
        reader.onloadend = function () {
          that._$resImage2.src = reader.result // base64
          that._capturedData = reader.result

          that._stopTracks()
          that.setState({ captured: true, initMsg: null, recType: 'image' })
        }
      })
      .catch((err) => {
        console.log('takePhoto fails :', err)

        // Use canvas
        $(that._$resImage2).parent().remove()
        that._imageCapture = false
        that.takeImage()
      })
  }

  takeVideo() {
    this.initDevice(() => {
      if (this._mediaRecorder.state === 'inactive') {
        this._blobs = []
        this._mediaRecorder.start()
        this.setState({ recording: '0', initMsg: null, recType: 'video' })
        this._recordingTimer = setInterval(() => {
          this.setState({ recording: ~~this.state.recording + 1 })
        }, 980)
      } else if (this._mediaRecorder.state === 'recording') {
        this._mediaRecorder.stop()
        this._stopTracks()
        this.setState({ recording: false })
        if (this._recordingTimer) {
          clearInterval(this._recordingTimer)
          this._recordingTimer = null
        }
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
