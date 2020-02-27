(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    define([], factory());
  } else if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root['watermark'] = factory();
  }
}(this, function () {

  var watermark = {};

  var defaultSettings = {
    watermark_id: 'wm_div_id',
    watermark_prefix: 'mask_div_id',
    watermark_txt: ['WATERMARK'],
    watermark_x: 20,
    watermark_y: 20,
    watermark_rows: 0,
    watermark_cols: 0,
    watermark_x_space: 50,
    watermark_y_space: 50,
    watermark_font: 'arial',
    watermark_color: 'black',
    watermark_fontsize: '18px',
    watermark_alpha: 0.15,
    watermark_width: 100,
    watermark_height: 100,
    watermark_angle: 15,
    watermark_parent_width: 0,
    watermark_parent_height: 0,
    watermark_parent_node: null,
    monitor: true,
  };

  var wmMinotorInterval

  var loadMark = function (settings) {
    if (arguments.length === 1 && typeof arguments[0] === "object") {
      var src = arguments[0] || {};
      for (var key in src) {
        if (src[key] && defaultSettings[key] && src[key] === defaultSettings[key]) continue;
        else if (src[key] || src[key] === 0) defaultSettings[key] = src[key];
      }
    }

    if (wmMinotorInterval) {
      clearInterval(wmMinotorInterval)
      wmMinotorInterval = null
    }

    var watermark_element = document.getElementById(defaultSettings.watermark_id);
    watermark_element && watermark_element.parentNode && watermark_element.parentNode.removeChild(watermark_element);

    var watermark_parent_element = document.getElementById(defaultSettings.watermark_parent_node);
    var watermark_hook_element = watermark_parent_element ? watermark_parent_element : document.body;

    var page_width = Math.max(watermark_hook_element.scrollWidth, watermark_hook_element.clientWidth);
    var page_height = Math.max(watermark_hook_element.scrollHeight, watermark_hook_element.clientHeight);

    var setting = arguments[0] || {};
    var parentEle = watermark_hook_element;

    var page_offsetTop = 0;
    var page_offsetLeft = 0;
    if (setting.watermark_parent_width || setting.watermark_parent_height) {
      if (parentEle) {
        page_offsetTop = parentEle.offsetTop || 0;
        page_offsetLeft = parentEle.offsetLeft || 0;
        defaultSettings.watermark_x = defaultSettings.watermark_x + page_offsetLeft;
        defaultSettings.watermark_y = defaultSettings.watermark_y + page_offsetTop;
      }
    } else {
      if (parentEle) {
        page_offsetTop = parentEle.offsetTop || 0;
        page_offsetLeft = parentEle.offsetLeft || 0;
      }
    }

    var otdiv = document.getElementById(defaultSettings.watermark_id);
    var shadowRoot = null;

    if (!otdiv) {
      otdiv = document.createElement('div');
      otdiv.id = defaultSettings.watermark_id;
      otdiv.setAttribute('style', 'pointer-events:none;display:block;position:fixed;width:100%;height:100%;top:0;left:0;overflow:hidden;z-index:9999;');
      if (typeof otdiv.attachShadow === 'function') {
        shadowRoot = otdiv.attachShadow({ mode: 'open' });
      } else {
        shadowRoot = otdiv;
      }
      var nodeList = watermark_hook_element.children;
      var index = Math.floor(Math.random() * (nodeList.length - 1));
      if (nodeList[index]) {
        watermark_hook_element.insertBefore(otdiv, nodeList[index]);
      } else {
        watermark_hook_element.appendChild(otdiv);
      }
    } else if (otdiv.shadowRoot) {
      shadowRoot = otdiv.shadowRoot;
    }

    defaultSettings.watermark_cols = parseInt((page_width - defaultSettings.watermark_x) / (defaultSettings.watermark_width + defaultSettings.watermark_x_space));
    var temp_watermark_x_space = parseInt((page_width - defaultSettings.watermark_x - defaultSettings.watermark_width * defaultSettings.watermark_cols) / (defaultSettings.watermark_cols));
    defaultSettings.watermark_x_space = temp_watermark_x_space ? defaultSettings.watermark_x_space : temp_watermark_x_space;
    var allWatermarkWidth;

    defaultSettings.watermark_rows = parseInt((page_height - defaultSettings.watermark_y) / (defaultSettings.watermark_height + defaultSettings.watermark_y_space));
    var temp_watermark_y_space = parseInt((page_height - defaultSettings.watermark_y - defaultSettings.watermark_height * defaultSettings.watermark_rows) / (defaultSettings.watermark_rows));
    defaultSettings.watermark_y_space = temp_watermark_y_space ? defaultSettings.watermark_y_space : temp_watermark_y_space;
    var allWatermarkHeight;

    if (watermark_parent_element) {
      allWatermarkWidth = defaultSettings.watermark_x + defaultSettings.watermark_width * defaultSettings.watermark_cols + defaultSettings.watermark_x_space * (defaultSettings.watermark_cols - 1);
      allWatermarkHeight = defaultSettings.watermark_y + defaultSettings.watermark_height * defaultSettings.watermark_rows + defaultSettings.watermark_y_space * (defaultSettings.watermark_rows - 1);
    } else {
      allWatermarkWidth = page_offsetLeft + defaultSettings.watermark_x + defaultSettings.watermark_width * defaultSettings.watermark_cols + defaultSettings.watermark_x_space * (defaultSettings.watermark_cols - 1);
      allWatermarkHeight = page_offsetTop + defaultSettings.watermark_y + defaultSettings.watermark_height * defaultSettings.watermark_rows + defaultSettings.watermark_y_space * (defaultSettings.watermark_rows - 1);
    }

    var x;
    var y;
    for (var i = 0; i < defaultSettings.watermark_rows; i++) {
      if (watermark_parent_element) {
        y = page_offsetTop + defaultSettings.watermark_y + (page_height - allWatermarkHeight) / 2 + (defaultSettings.watermark_y_space + defaultSettings.watermark_height) * i;
      } else {
        y = defaultSettings.watermark_y + (page_height - allWatermarkHeight) / 2 + (defaultSettings.watermark_y_space + defaultSettings.watermark_height) * i;
      }
      for (var j = 0; j < defaultSettings.watermark_cols; j++) {
        if (watermark_parent_element) {
          x = page_offsetLeft + defaultSettings.watermark_x + (page_width - allWatermarkWidth) / 2 + (defaultSettings.watermark_width + defaultSettings.watermark_x_space) * j;
        } else {
          x = defaultSettings.watermark_x + (page_width - allWatermarkWidth) / 2 + (defaultSettings.watermark_width + defaultSettings.watermark_x_space) * j;
        }
        var mask_div = document.createElement('div');
        var oText = defaultSettings.watermark_txt[Math.floor(Math.random() * (defaultSettings.watermark_txt.length + 1))] || new Date().toLocaleString()
        oText = document.createTextNode(oText);
        mask_div.appendChild(oText);
        mask_div.id = defaultSettings.watermark_prefix + i + j;
        mask_div.style.webkitTransform = "rotate(-" + defaultSettings.watermark_angle + "deg)";
        mask_div.style.MozTransform = "rotate(-" + defaultSettings.watermark_angle + "deg)";
        mask_div.style.msTransform = "rotate(-" + defaultSettings.watermark_angle + "deg)";
        mask_div.style.OTransform = "rotate(-" + defaultSettings.watermark_angle + "deg)";
        mask_div.style.transform = "rotate(-" + defaultSettings.watermark_angle + "deg)";
        mask_div.style.visibility = "";
        mask_div.style.position = "absolute";
        mask_div.style.left = x + 'px';
        mask_div.style.top = y + 'px';
        mask_div.style.overflow = "hidden";
        mask_div.style.zIndex = "9999999";
        mask_div.style.opacity = defaultSettings.watermark_alpha;
        mask_div.style.fontSize = defaultSettings.watermark_fontsize;
        mask_div.style.fontFamily = defaultSettings.watermark_font;
        mask_div.style.color = defaultSettings.watermark_color;
        mask_div.style.textAlign = "center";
        mask_div.style.width = defaultSettings.watermark_width + 'px';
        mask_div.style.height = defaultSettings.watermark_height + 'px';
        mask_div.style.display = "block";
        mask_div.style['-ms-user-select'] = "none";
        mask_div.style.cursor = "default";
        mask_div.style.textTransform = "uppercase";
        shadowRoot.appendChild(mask_div);
      }
    }

    var minotor = settings.monitor === undefined ? defaultSettings.monitor : settings.monitor;
    if (minotor) {
      var cs = shadowRoot.childNodes.length
      var ss = document.getElementById(defaultSettings.watermark_id).getAttribute('style')
      var times = 0
      wmMinotorInterval = setInterval(function () {
        var check = document.getElementById(defaultSettings.watermark_id)
        if (times++ >= 60 || !check || check.getAttribute('style') !== ss || shadowRoot.childNodes.length < cs) loadMark(settings)
      }, 2000)
    }
  };

  var removeMark = function () {
    var watermark_element = document.getElementById(defaultSettings.watermark_id);
    if (watermark_element) {
      var _parentElement = watermark_element.parentNode;
      _parentElement.removeChild(watermark_element);
    }
  };

  var globalSetting;
  watermark.init = function (settings) {
    globalSetting = settings;
    loadMark(settings);

    var resizeTimer
    window.addEventListener('resize', function () {
      if (resizeTimer) {
        clearTimeout(resizeTimer)
        resizeTimer = null
      }
      resizeTimer = setTimeout(function () {
        loadMark(settings);
      }, 200)
    });
  };

  watermark.load = function (settings) {
    globalSetting = settings;
    loadMark(settings);
  };

  watermark.remove = function () {
    removeMark();
  };

  return watermark;
}));

// ~ https://github.com/saucxs/watermark-dom
// ~ better by REBUILD