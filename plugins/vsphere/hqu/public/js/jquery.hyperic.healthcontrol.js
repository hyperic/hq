// Depends on date.format.js
// Data - user specified value
/* 
 * data: {
 *	"metrics":[
 *		{"start":<number>,"end":<number>,"units":<number>,"status":"red|yellow|green|grey"},
 *      ...
 *  ],
 *	"start":<number>,
 *	"end":<number>,
 *	"status":"red|yellow|green|grey"
 *	}
 *
 */

(function($) {
	$.fn.healthcontrol = function(params) {
		var opts = $.extend({}, $.fn.healthcontrol.defaults, params);
		
		return this.each(function() {
			var $this = $(this);
			var id = $this.attr('id'); // id attribtue is required
			
			if (id) {
				var mainContainer   = $('<div></div>').addClass('rle-box')
				                                      .attr('id', id + '_health');
				var rleContainer    = $('<div></div>').addClass('rle-cont');
				var dataContainer   = $('<div></div>').addClass('rle-data')
				                                      .attr('id', id + '_data')
				                                      .css('position', 'relative');
				var rleNowContainer = $('<div></div>').addClass('rle-now ' + opts.data.status + '-avail')
				                                      .attr('id', id + '_now')
				                                      .html('&nbsp');
				var rulerContainer  = $('<div></div>').addClass('rle-rule');
				var legendContainer = $('<div></div>').addClass('rle-legend')
				                                      .attr('id', id + '_legend');
				var startTime       = new Date(opts.data.start);
				var endTime         = new Date(opts.data.end);
				var startTimeLabel  = $('<span></span>').addClass('ll')
				                                        .text(startTime.format('default'));
				var endTimeLabel    = $('<span></span>').addClass('rl')
				                                        .text(endTime.format('default'));

				_createHealthBar(opts.data.metrics, dataContainer);
				
				mainContainer.append(rleContainer.append(dataContainer)
				                                 .append(rleNowContainer)
				                                 .append(rulerContainer)
				                                 .append(legendContainer.append(startTimeLabel)
				                                                        .append(endTimeLabel)));
				$this.append(mainContainer);
			}
		});
	};
	
	function _createHealthBar(data, $container) {
		var iStart = iEnd = units = section = null;
		var leftPos = 0;
		
        for (var i = 0; i < data.length; i++) {
            iStart = new Date(data[i].start);
            iEnd = new Date(data[i].end);
			units = data[i].units;
			
        	section = $('<div></div>').addClass('rle-metric ' + data[i].status)
        	                          .css('width', units + '%')
        	                          .css('position', 'absolute')
        	                          .css('left', leftPos + '%')
        	                          .attr('title', iStart.format('default') + ' - ' + iEnd.format('default'))
        	                          .html('&nbsp;');
        	
        	$container.append(section);
        	
        	leftPos += units;
        }
	};
	
	$.fn.healthcontrol.defaults = {
	};
})(jQuery);
