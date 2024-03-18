/* eslint-disable eqeqeq */
/* eslint-disable quotes */
/* eslint-disable no-unused-vars */
;(function ($) {
  $.fn.html5Uploader = function (options) {
    var crlf = '\r\n'
    var boundary = 'iloveigloo'
    var dashes = '--'
    var settings = {
      'name': 'uploadedFile',
      'postUrl': 'Upload.aspx',
      'onClientAbort': null,
      'onClientError': null,
      'onClientLoad': null,
      'onClientLoadEnd': null,
      'onClientLoadStart': null,
      'onClientProgress': null,
      'onServerAbort': null,
      'onServerError': null,
      'onServerLoad': null,
      'onServerLoadStart': null,
      'onServerProgress': null,
      'onServerReadyStateChange': null,
      'onSuccess': null,
      'onSelectError': null,
    }
    if (options) {
      $.extend(settings, options)
    }
    return this.each(function (options) {
      var $this = $(this)
      if ($this.is("[type='file']")) {
        var accept = $this.attr('accept')
        var maxsize = $this.attr('data-maxsize')
        $this.bind('change', function () {
          var files = this.files
          for (var i = 0; i < files.length; i++) {
            var file = files[i]
            if (checkAccept(file, accept) == false) {
              if (settings.onSelectError) {
                settings.onSelectError(file, 'ErrorType')
              }
              break
            } else {
              if (checkMaxsize(file, maxsize) == false) {
                if (settings.onSelectError) {
                  settings.onSelectError(file, 'ErrorMaxSize')
                }
                break
              }
            }
            fileHandler(file)
          }
        })
      } else {
        $this
          .bind('dragenter dragover', function () {
            $(this).addClass('hover')
            return false
          })
          .bind('dragleave', function () {
            $(this).removeClass('hover')
            return false
          })
          .bind('drop', function (e) {
            $(this).removeClass('hover')
            var files = e.originalEvent.dataTransfer.files
            for (var i = 0; i < files.length; i++) {
              fileHandler(files[i])
            }
            return false
          })
      }
    })
    function checkAccept(file, accept) {
      return html5Uploader_checkAccept(file, accept)
    }
    function checkMaxsize(file, maxsize) {
      if (!maxsize || maxsize <= 0) {
        return true
      }
      return file.size <= maxsize
    }
    function fileHandler(file) {
      var fileReader = new FileReader()
      fileReader.onabort = function (e) {
        if (settings.onClientAbort) {
          settings.onClientAbort(e, file)
        }
      }
      fileReader.onerror = function (e) {
        if (settings.onClientError) {
          settings.onClientError(e, file)
        }
      }
      fileReader.onload = function (e) {
        if (settings.onClientLoad) {
          settings.onClientLoad(e, file)
        }
      }
      fileReader.onloadend = function (e) {
        if (settings.onClientLoadEnd) {
          settings.onClientLoadEnd(e, file)
        }
      }
      fileReader.onloadstart = function (e) {
        if (settings.onClientLoadStart) {
          settings.onClientLoadStart(e, file)
        }
      }
      fileReader.onprogress = function (e) {
        if (settings.onClientProgress) {
          settings.onClientProgress(e, file)
        }
      }
      fileReader.readAsDataURL(file)
      var xmlHttpRequest = new XMLHttpRequest()
      xmlHttpRequest.upload.onabort = function (e) {
        if (settings.onServerAbort) {
          settings.onServerAbort(e, file)
        }
      }
      xmlHttpRequest.upload.onerror = function (e) {
        if (settings.onServerError) {
          settings.onServerError(e, file)
        }
      }
      xmlHttpRequest.upload.onload = function (e) {
        if (settings.onServerLoad) {
          settings.onServerLoad(e, file)
        }
      }
      xmlHttpRequest.upload.onloadstart = function (e) {
        if (settings.onServerLoadStart) {
          settings.onServerLoadStart(e, file)
        }
      }
      xmlHttpRequest.upload.onprogress = function (e) {
        if (settings.onServerProgress) {
          settings.onServerProgress(e, file)
        }
      }
      xmlHttpRequest.onreadystatechange = function (e) {
        if (settings.onServerReadyStateChange) {
          settings.onServerReadyStateChange(e, file, xmlHttpRequest.readyState)
        }
        if (settings.onSuccess && xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 200) {
          settings.onSuccess(e, file, xmlHttpRequest.responseText)
        }
        if (settings.onServerError && xmlHttpRequest.readyState == 4 && xmlHttpRequest.status === 413) {
          settings.onServerError(xmlHttpRequest, file)
        }
      }
      xmlHttpRequest.open('POST', settings.postUrl, true)
      if (file.getAsBinary) {
        var data =
          dashes +
          boundary +
          crlf +
          'Content-Disposition: form-data;' +
          'name="' +
          settings.name +
          '";' +
          'filename="' +
          unescape(encodeURIComponent(file.name)) +
          '"' +
          crlf +
          'Content-Type: application/octet-stream' +
          crlf +
          crlf +
          file.getAsBinary() +
          crlf +
          dashes +
          boundary +
          dashes
        xmlHttpRequest.setRequestHeader('Content-Type', 'multipart/form-data;boundary=' + boundary)
        xmlHttpRequest.sendAsBinary(data)
      } else {
        if (window.FormData) {
          var formData = new FormData()
          formData.append(settings.name, file)
          xmlHttpRequest.send(formData)
        }
      }
    }
  }
})(jQuery)

// 文件类型检查（扩展名）
function html5Uploader_checkAccept(file, accept) {
  if (!accept || accept === '*' || accept === '*/*') {
    return true
  }

  // eg. image/*,video/*
  var acceptList = accept.split(',')
  for (var i = 0; i < acceptList.length; i++) {
    var a = acceptList[i]
    if (a.split('/')[1] === '*' && a.split('/')[0] === file.type.split('/')[0]) {
      return true
    }
  }

  var fileExt = file.name.split('.')
  fileExt = ('.' + fileExt[fileExt.length - 1]).toLowerCase()
  var isAccept = false
  $(accept.split(',')).each(function () {
    if (fileExt === $trim(this).toLowerCase()) {
      isAccept = true
      return false
    }
  })
  return isAccept
}

// ~ better by REBUILD
