// https://github.com/likemusic/tablecellsselection/
'use strict';

(function ($, window, document, undefined) {

  var settings

  var systemSettings = {
    dataKey: 'cellsSelector',
    eventNamespace: 'cellsselector'
  }

  var isMouseDown = false//нажата ли левая клавиша мыши

  var methods = {
    init: function (options) {
      //console.log('method>init');
      var defaultSettings = {
        selectableTableClass: 'tcs',//класс добавляемый к таблицам
        selectedCellClass: 'tcs-selected',//класс добавляемый к выделенным ячейкам таблицы
        selectionEnabled: 'tcs-selection-enabled' //???
      }

      settings =
                (!options) ? defaultSettings
                  : $.extend(defaultSettings, options)
      //todo: set listenets on all document for handle all tables added to html
      return this.filter('table').each(initTableIfNeed)
    },

    destroy: function () {
      //console.log('method>destroy');
      return this.each(function () {
        destroyTable($(this))
        //$(window).unbind('.cellsselector');
      })
    },

    selectedCells: function () {
      //this - $table
      //console.log('selectedCells');
      return getSelectedCells(this)
    },
    /*
            getSelectedCells: function(){},
            setSelectedCells: function(){},
            selectCells: function(){},
            selectCell: function(){},

            selectAll: function(){},
            deselectAll: function(){},
            */
    //todo: implement
    removeDocumentHtmlChanges: removeDocumentHtmlChanges,
    addDocumentHtmlChanges: addDocumentHtmlChanges
  }

  //----------- INIT

  /** Инициализирует таблицу если она неинициализированна */
  function initTableIfNeed(index, table) {
    var $table = $(table),
      data = $table.data(systemSettings.dataKey)

    //плагин еще не проинициализирован
    if (!data) {
      //инициализируем
      initTable($table)
      $table.data(systemSettings.dataKey, getInitialData())
    }

    //initTableCellsSelector(el);
    //$(this).__proto__.csHandler = new Handler(settings, this, document);
    //if(!$(this).csHandler) $(this).__proto__.csHandler = new Handler(settings, this, document);
    //else $(this).csHandler.restart();

  }

  /** Возварщает исходные данные таблицы */
  function getInitialData() {
    return {
      selFrom: false,
      selTo: false,
      isHighlighted: undefined,
      $currTR: $(),//todo: нужны ли?
      $currTH: $(),//todo: нужны ли?
      $currCell: $() //todo: нужны ли?
    }
  }

  /** Инициализирует таблицу */
  function initTable($table) {
    //addData()???
    addTableHtmlChanges($table)

    addEventListeners($table)

    function addEventListeners($table) {
      //console.log('addInitialEventListeners');
      addInitialSelectionEventListeners($table)
      addInitialCopyPasteEventListeners($table)

      function addInitialSelectionEventListeners($table) {
        //console.log('addInitialSelectionEventListeners');

        $table
          .on(getEventNameWithPluginNamespace('mouseover'), onMouseOver)//.mouseover(onMouseOver)
          .on(getEventNameWithPluginNamespace('mousedown'), onMouseDown)//.mousedown(onMouseDown)
        //.on(getEventNameWithPluginNamespace('dragstart'),onDragStart)
          .on(getEventNameWithPluginNamespace('mouseup'), onMouseUp)//.mouseup(onMouseUp);

        //клик на документе вне таблицы
        $($table[0].ownerDocument).on(getEventNameWithPluginNamespace('click'), onOutTableClick)
        //or $table.closest(":root"); - html
        //or $('html')
        //or $(document)

        function onMouseOver(event) {
          //console.log('mouseover table');
          var pluginData = getPluginDataByEvent(event)

          var $target = pluginData.$target

          //таблица
          var $table = pluginData.$table

          //ячейку
          var $cell = pluginData.$cell

          if ($cell.length == 0) return//событие сработало не для ячейки (самой таблицы или других её элементов)

          var data = pluginData.data
          data.$currCell = $cell

          //если клавиша мыши не нажата, значит не выделение ячеек
          if (!isMouseDown) return false
          //todo: переделать на глобальный индикатор нажатия кнопки мыши

          //скрываем стандартное выделение в таблице
          var selectionEnableClass = settings.selectionEnabled
          $table.find('.' + selectionEnableClass).removeClass(selectionEnableClass)

          data.selTo = getCoordinates($cell)

          setPointCoordinates(data, $cell)

          //todo: move to bottom
          function coordinateManipulateMagic() {
            var i = 1
            while (i > 0) {
              i = 0//wtf???
              getTableCells($table).each(function (key, cell) {
                data.$elemX = $(cell).offset().left
                data.$elemXwidth = $(cell).offset().left + $(cell).width()

                data.$elemY = $(cell).offset().top
                data.$elemYheight = $(cell).offset().top + $(cell).height()

                if ((data.$elemX < data.$pointXmin) && (data.$elemXwidth >= data.$pointXmin) && (data.$elemXwidth <= data.$pointXwidth) && (data.$elemYheight <= data.$pointYheight) && (data.$elemYheight > data.$pointYmin)) {
                  data.$temp = data.$elemX
                  if (data.$temp != data.$pointXmin) {
                    data.$pointXmin = data.$temp
                    i = 1
                  }
                }

                if ((data.$elemX >= data.$pointXmin) && (data.$elemX < data.$pointXwidth) && (data.$elemXwidth >= data.$pointXwidth) && (data.$elemYheight <= data.$pointYheight) && (data.$elemYheight > data.$pointYmin)) {
                  data.$temp = data.$elemXwidth
                  if (data.$temp != data.$pointXwidth) {
                    data.$pointXwidth = data.$temp
                    i = 1
                  }
                }

                if ((data.$elemY < data.$pointYmin) && (data.$elemYheight >= data.$pointYmin) && (data.$elemYheight <= data.$pointYheight) && (data.$elemXwidth <= data.$pointXwidth) && (data.$elemXwidth > data.$pointXmin)) {
                  data.$temp = data.$elemY
                  if (data.$temp != data.$pointYmin) {
                    data.$pointYmin = data.$temp
                    i = 1
                  }
                }

                if ((data.$elemY >= data.$pointYmin) && (data.$elemY < data.$pointYheight) && (data.$elemYheight >= data.$pointYheight) && (data.$elemXwidth <= data.$pointXwidth) && (data.$elemXwidth > data.$pointXmin)) {
                  data.$temp = data.$elemYheight
                  if (data.$temp != data.$pointYheight) {
                    data.$pointYheight = data.$temp
                    i = 1
                  }
                }

                if ((data.$elemX < data.$pointXmin) && (data.$elemXwidth >= data.$pointXwidth) && (data.$elemYheight <= data.$pointYheight) && (data.$elemYheight > data.$pointYmin)) {
                  data.$temp = data.$elemX
                  if (data.$temp < data.$pointXmin) {
                    data.$pointXmin = data.$temp
                    i = 1
                  }
                }
                if ((data.$elemX > data.$pointXmin) && (data.$elemX < data.$pointXwidth) && (data.$elemY < data.$pointYheight) && (data.$elemY > data.$pointYmin)) {
                  data.$temp = data.$elemXwidth
                  if (data.$temp > data.$pointXwidth) {
                    data.$pointXwidth = data.$temp
                    i = 1
                  }
                }

                if ((data.$elemY < data.$pointYmin) && (data.$elemYheight >= data.$pointYmin) && (data.$elemYheight <= data.$pointYheight) && (data.$elemXwidth <= data.$pointXwidth) && (data.$elemXwidth > data.$pointXmin)) {
                  data.$temp = data.$elemY
                  if (data.$temp < data.$pointYmin) {
                    data.$pointYmin = data.$temp
                    i = 1
                  }
                }
                if ((data.$elemY > data.$pointYmin) && (data.$elemYheight <= data.$pointYheight) && (data.$elemX <= data.$pointXwidth)) {
                  data.$temp = data.$elemYheight
                  if (data.$temp > data.$pointYheight) {
                    data.$pointYheight = data.$temp
                    i = 1
                  }
                }
              })
            }
          }

          coordinateManipulateMagic()

          selectCells($table, data)
          $table.data(data)
          return true

          function setPointCoordinates(data, $cell) {
            data.$pointX2 = $cell.offset().left
            data.$pointX2width = $cell.offset().left + $cell.width()

            data.$pointY2 = $cell.offset().top
            data.$pointY2height = $cell.offset().top + $cell.height()

            if (data.$pointX1 < data.$pointX2) {
              data.$pointXmin = data.$pointX1
              data.$pointXmax = data.$pointX2
            } else {
              data.$pointXmin = data.$pointX2
              data.$pointXmax = data.$pointX1
            }
            if (data.$pointX1width > data.$pointX2width) {
              data.$pointXwidth = data.$pointX1width
            } else {
              data.$pointXwidth = data.$pointX2width
            }
            if (data.$pointY1 < data.$pointY2) {
              data.$pointYmin = data.$pointY1
              data.$pointYmax = data.$pointY2
            } else {
              data.$pointYmin = data.$pointY2
              data.$pointYmax = data.$pointY1
            }
            if (data.$pointY1height > data.$pointY2height) {
              data.$pointYheight = data.$pointY1height
            } else {
              data.$pointYheight = data.$pointY2height
            }
          }
        }

        function onMouseDown(event) {
          //console.log('mousedown table');
          var pluginData = getPluginDataByEvent(event)

          //если клик правой кнопкой мыши - ничего не делаем
          if (isRightMouseButton(event)) return true

          //таблица
          var $table = pluginData.$table

          //получаем ячейку
          var $cell = pluginData.$cell
          //self.$currCell = $cell;

          if ($cell.length == 0) return//событие сработало не для ячейки (самой таблицы или других её элементов)

          //event.stopPropagation();//надо ли?
          //event.stopImmediatePropagation();//тем более это?

          $cell.addClass(settings.selectionEnabled)

          var data = pluginData.data

          isMouseDown = true
          data.selFrom = getCoordinates($cell)
          data.selFrom.$el = $cell

          var selectedCellClass = settings.selectedCellClass
          if ($cell.hasClass(selectedCellClass) && deselectAll($table) === 1) {
            $cell.removeClass(selectedCellClass)
          } else {
            deselectAll($table)
            $cell.addClass(selectedCellClass)
          }

          data.$pointX1 = $cell.offset().left
          data.$pointX1width = $cell.offset().left + $cell.width()
          data.$pointY1 = $cell.offset().top
          data.$pointY1height = $cell.offset().top + $cell.height()

          data.isHighlighted = $cell.hasClass(selectedCellClass)
          $table.data(data)
          return true
        }


        /*function onDragStart(event)
                        {
                            //console.log('dragstart table');
                            //event.preventDefault();//todo: надо ли?
                            return true;
                        }*/

        function onMouseUp(event) {
          //console.log('mouseup table');
          var pluginData = getPluginDataByEvent(event)
          var data = pluginData.data
          isMouseDown = false
          data.selFrom = false
          data.selTo = false
          pluginData.$table.data(data)
        }

        //клик на документе вне таблицы
        function onOutTableClick(event) {
          //console.log('click (out of table)');
          isMouseDown = false
          if ($(event.target).closest($table).length == 0) deselectAll($table)
        }
      }

      function addInitialCopyPasteEventListeners($table) {
        //console.log('addInitialCopyPasteEventListeners');

        //this.$table.off('copy');
        $table.on('copy', onCopy)

        function onCopy(e) {
          e.originalEvent.clipboardData.setData('text/plain', handler.getSelectedDataAsText())
          e.preventDefault() // We want our data, not data from any selection, to be written to the clipboard
          //var handler = $(e.target).csHandler;
          //e.originalEvent.clipboardData.setData('text/csv', handler.getSelectedDataAsText());
          //e.originalEvent.clipboardData.setData('Text', handler.getSelectedDataAsText());
          //e.originalEvent.clipboardData.setData('text/html', '<b>Hello, world!text/html</b>');
          //e.originalEvent.clipboardData.setData('text/html', handler.getSelectedDataAsHtml());
          //console.log(e.originalEvent.clipboardData.types);
          //console.log('copy:'+ e.target.innerHTML);
          //console.log('copy');
          //console.log(e);
        }

        //-------- Copy/Paste event-listeners

        /*$table.on('beforecopy', function(e){
                            //console.log('beforecopy');

                            /*if(weHaveDataToCopy()){ // use your web app's internal logic to determine if something can be copied
                             e.preventDefault(); // enable copy UI and events
                             }
                        });*/


        /*        table.addEventListener('copy', function(e){
                         //var handler = e.target.csHandler;
                         //var handler = $(e.target).csHandler;
                         //e.clipboardData.setData('text/plain', handler.getSelectedDataAsText());
                         //e.clipboardData.setData('text/html', handler.getSelectedDataAsHtml());
                         //e.clipboardData.setData('text/plain', 'getSelectedDataAsText');
                         //e.clipboardData.setData('text/html', 'getSelectedDataAsHtml');
                         //console.log('copy:'+ e.target.innerHTML);
                         console.log('copy');
                         console.log(e);
                         //e.preventDefault(); // We want our data, not data from any selection, to be written to the clipboard
                         });
                         */

        //this.$table.on('paste',onPaste);
        /*$table.on('paste', function(e){
                            //console.log('paste-Listener');
                            //console.log(e);
                            /*if(e.clipboardData.types.indexOf('text/html') > -1){
                             processDataFromClipboard(e.clipboardData.getData('text/html'));
                             e.preventDefault(); // We are already handling the data from the clipboard, we do not want it inserted into the document
                             }
                        });*/


        /*function onPaste(e){
                            //console.log('paste-onPaste');
                            //console.log(e);
                        }*/
      }
    }
  }


  //----------- DESTROY

  function destroyTable($table) {
    //console.log('destroyTable');
    removeEventListeners($table)
    removeData($table)
    removeTableHtmlChanges($table)

    function removeEventListeners($table) {
      $table.unbind('.' + systemSettings.eventNamespace)
    }

    function removeData($table) {
      $table.removeData(systemSettings.dataKey)
    }
  }

  $.fn.tableCellsSelection = function (method) {
    if (methods[method]) {
      return methods[method].apply(this, Array.prototype.slice.call(arguments, 1))
    } else if (typeof method === 'object' || !method) {
      return methods.init.apply(this, arguments)
    } else {
      $.error('Метод с именем ' + method + ' не существует для jQuery.tooltip')
    }
  }

  //**************** Table/Cells funcs
  function removeDocumentHtmlChanges($document) {
    var $tables = $document.find('table')
    removeTableHtmlChanges($tables)
  }

  function addDocumentHtmlChanges($document) {
    var $tables = $document.find('table')
    addTableHtmlChanges($tables)
  }

  /**
     * Удаляет внесенные изменения в html-коде (в данный момент удалет служебные классы)
     * @param $table - отдельная таблица или весь документ
     */
  //todo: разделить на ф-цию чистки всего документа и таблиц.
  //todo: В документе удалять используемые классы только у таблиц.
  function removeTableHtmlChanges($table) {
    $table.removeClass(settings.selectableTableClass)
    removeChildrensClass($table, settings.selectedCellClass)
    removeChildrensClass($table, settings.selectionEnabled)

    function removeChildrensClass($table, className) {
      $table.find('.' + className).removeClass(className)
    }
  }

  /**
     * Добавляет изменения в html-код
     * в соответствии с текущим состоянием таблицы.
     * На данный момент добаляет служебные классы к таблице  и её ячейкам.
     * @param $table
     */
  function addTableHtmlChanges($table) {
    $table.addClass(settings.selectableTableClass)
    //todo: добавить подсветку выделенных ячеек
    /*
                $selectedCells = getSelectedCells();
                $selectedCells.adddClass(settings.selectedCellClass);
                $selectedCells.adddClass(settings.selectionEnabled);
             */
  }

  function getSelectedDataAsText($table) {
    //console.log('getSelectedDataAsText');
    var selectedCells = getSelectedCells($table)//выделенные ячейки
    var selectedRows = selectedCells.closest('tr')//строки с выделенными ячейками

    var ret = ''
    //для каждой строки получаем значения ячеек
    for (var i = 0; i < selectedRows.length; i++) {
      var rowValues = []
      var rowSelectedCells = selectedRows.eq(i).find(selectedCells)
      for (var j = 0; j < rowSelectedCells.length; j++) {
        rowValues.push(rowSelectedCells[j].innerText)
      }
      ret += rowValues.join('\t') + '\r\n'
    }
    return ret
    //if (allTables) return this.$doc.find('.cell-active');
    //return this.$table.find('.cell-active');
  }

  /*function getSelectedDataAsHtml($table) {
          //console.log('getSelectedDataAsHtml');
          return '<b>getSelectedDataAsHtml</b>';
          //if (allTables) return this.$doc.find('.cell-active');
          //return this.$table.find('.cell-active');
      };*/

  //todo: нужно ли?
  /*
      function restart($table) {
          this.$table.addClass(settings.selectableTableClass);
          this.allCells() = this.$table.find('th, td');
          this.init();
      };*/

  /*function getCell($table, x, y) {
          if (y == 1) return $table.find('tr[data_y="1"] > th[data_x="'+x+'"]');
          return this.$table.find('tr[data_y="'+y+'"] > td[data_x="'+x+'"]');
      }*/

  function isSelectedCell($cell) {
    return $cell.hasClass(settings.selectedCellClass)
  }

  /*function selectCell($table,$cell) {
          //todo: implement
          if (isSelectedCell($cell)) return deselectCell($cell);
          deselectAll($table);

          if ($cell.is('td, th')) $cell.addClass(settings.selectedCellClass);
      }*/

  function selectCells($table, data) {
    deselectAll($table)

    getTableCells($table).each(function (key, cell) {
      data.$elemX = $(cell).offset().left
      data.$elemXwidth = $(cell).offset().left + $(cell).width()
      data.$elemY = $(cell).offset().top
      data.$elemXheight = $(cell).offset().top + $(cell).height()

      if ((data.$elemX >= data.$pointXmin) && (data.$elemXwidth <= data.$pointXwidth)
                && (data.$elemXwidth <= data.$pointXwidth) && (data.$elemY >= data.$pointYmin)
                && (data.$elemY <= data.$pointYheight) && (data.$elemYheight >= data.$pointYheight)) {
        $(cell).addClass(settings.selectedCellClass)
      }
    })
    $table.trigger('selectionchange.' + systemSettings.eventNamespace)
  }

  //todo: удалить параметр allTables
  function getSelectedCells($table) {
    return $table.find('.' + settings.selectedCellClass)
  }

  function getCoordinates($cell) {
    return {
      x: parseInt($cell.attr('data_x')),//todo: заменить на DOM-свойства вроде colNumber
      y: parseInt($cell.parent('tr').attr('data_y')),
      colspan: $cell.attr('colspan') ? parseInt($cell.attr('colspan')) : 0,
      rowspan: $cell.attr('rowspan') ? parseInt($cell.attr('rowspan')) : 0
    }
  }

  function getTableCells($table) {
    return $table.find('th, td')
  }

  function deselectAll($table) {
    var selectedCells = getSelectedCells($table)//TODO:исправить на локальные изменения только
    var length = 0

    /*selectedCells.each(function(i, cell) {
                length++;
                $(cell).removeClass(settings.selectedCellClass);
            });*/
    //or
    selectedCells.removeClass(settings.selectedCellClass)

    return length
  }


  //**************** PLUGIN

  function getEventNameWithPluginNamespace(event) {
    return event + '.' + systemSettings.eventNamespace
  }

  function isRightMouseButton(event) {
    var isRightMB
    event = event || window.event
    if ('which' in event)
      isRightMB = event.which == 3
    else if ('button' in event)
      isRightMB = event.button == 2
    return isRightMB
  }

  function getPluginDataByEvent(event) {
    var $target = $(event.target)
    var $table = $target.closest('table')
    return {
      $target: $target,
      $table: $table,
      $cell: $target.closest('td,th'),
      data: $table.data(systemSettings.dataKey)
    }
  }
})(jQuery, window, document)
