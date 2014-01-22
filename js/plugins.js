// Avoid `console` errors in browsers that lack a console.
(function() {
    var method;
    var noop = function () {};
    var methods = [
        'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
        'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
        'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
        'timeStamp', 'trace', 'warn'
    ];
    var length = methods.length;
    var console = (window.console = window.console || {});

    while (length--) {
        method = methods[length];

        // Only stub undefined methods.
        if (!console[method]) {
            console[method] = noop;
        }
    }
}());

// All the plugins/helpers

/*
 * jQuery Easing v1.3 - http://gsgd.co.uk/sandbox/jquery/easing/
 *
 * Uses the built In easIng capabilities added In jQuery 1.1
 * to offer multiple easIng options
 *
 * Copyright (c) 2007 George Smith
 * Licensed under the MIT License:
 *   http://www.opensource.org/licenses/mit-license.php
 */

// t: current time, b: begInnIng value, c: change In value, d: duration
jQuery.easing['jswing'] = jQuery.easing['swing'];

jQuery.extend( jQuery.easing,
{
	def: 'easeOutQuad',
	swing: function (x, t, b, c, d) {
		//alert(jQuery.easing.default);
		return jQuery.easing[jQuery.easing.def](x, t, b, c, d);
	},
	easeInQuad: function (x, t, b, c, d) {
		return c*(t/=d)*t + b;
	},
	easeOutQuad: function (x, t, b, c, d) {
		return -c *(t/=d)*(t-2) + b;
	},
	easeInOutQuad: function (x, t, b, c, d) {
		if ((t/=d/2) < 1) return c/2*t*t + b;
		return -c/2 * ((--t)*(t-2) - 1) + b;
	},
	easeInCubic: function (x, t, b, c, d) {
		return c*(t/=d)*t*t + b;
	},
	easeOutCubic: function (x, t, b, c, d) {
		return c*((t=t/d-1)*t*t + 1) + b;
	},
	easeInOutCubic: function (x, t, b, c, d) {
		if ((t/=d/2) < 1) return c/2*t*t*t + b;
		return c/2*((t-=2)*t*t + 2) + b;
	},
	easeInQuart: function (x, t, b, c, d) {
		return c*(t/=d)*t*t*t + b;
	},
	easeOutQuart: function (x, t, b, c, d) {
		return -c * ((t=t/d-1)*t*t*t - 1) + b;
	},
	easeInOutQuart: function (x, t, b, c, d) {
		if ((t/=d/2) < 1) return c/2*t*t*t*t + b;
		return -c/2 * ((t-=2)*t*t*t - 2) + b;
	},
	easeInQuint: function (x, t, b, c, d) {
		return c*(t/=d)*t*t*t*t + b;
	},
	easeOutQuint: function (x, t, b, c, d) {
		return c*((t=t/d-1)*t*t*t*t + 1) + b;
	},
	easeInOutQuint: function (x, t, b, c, d) {
		if ((t/=d/2) < 1) return c/2*t*t*t*t*t + b;
		return c/2*((t-=2)*t*t*t*t + 2) + b;
	},
	easeInSine: function (x, t, b, c, d) {
		return -c * Math.cos(t/d * (Math.PI/2)) + c + b;
	},
	easeOutSine: function (x, t, b, c, d) {
		return c * Math.sin(t/d * (Math.PI/2)) + b;
	},
	easeInOutSine: function (x, t, b, c, d) {
		return -c/2 * (Math.cos(Math.PI*t/d) - 1) + b;
	},
	easeInExpo: function (x, t, b, c, d) {
		return (t==0) ? b : c * Math.pow(2, 10 * (t/d - 1)) + b;
	},
	easeOutExpo: function (x, t, b, c, d) {
		return (t==d) ? b+c : c * (-Math.pow(2, -10 * t/d) + 1) + b;
	},
	easeInOutExpo: function (x, t, b, c, d) {
		if (t==0) return b;
		if (t==d) return b+c;
		if ((t/=d/2) < 1) return c/2 * Math.pow(2, 10 * (t - 1)) + b;
		return c/2 * (-Math.pow(2, -10 * --t) + 2) + b;
	},
	easeInCirc: function (x, t, b, c, d) {
		return -c * (Math.sqrt(1 - (t/=d)*t) - 1) + b;
	},
	easeOutCirc: function (x, t, b, c, d) {
		return c * Math.sqrt(1 - (t=t/d-1)*t) + b;
	},
	easeInOutCirc: function (x, t, b, c, d) {
		if ((t/=d/2) < 1) return -c/2 * (Math.sqrt(1 - t*t) - 1) + b;
		return c/2 * (Math.sqrt(1 - (t-=2)*t) + 1) + b;
	},
	easeInElastic: function (x, t, b, c, d) {
		var s=1.70158;var p=0;var a=c;
		if (t==0) return b;  if ((t/=d)==1) return b+c;  if (!p) p=d*.3;
		if (a < Math.abs(c)) { a=c; var s=p/4; }
		else var s = p/(2*Math.PI) * Math.asin (c/a);
		return -(a*Math.pow(2,10*(t-=1)) * Math.sin( (t*d-s)*(2*Math.PI)/p )) + b;
	},
	easeOutElastic: function (x, t, b, c, d) {
		var s=1.70158;var p=0;var a=c;
		if (t==0) return b;  if ((t/=d)==1) return b+c;  if (!p) p=d*.3;
		if (a < Math.abs(c)) { a=c; var s=p/4; }
		else var s = p/(2*Math.PI) * Math.asin (c/a);
		return a*Math.pow(2,-10*t) * Math.sin( (t*d-s)*(2*Math.PI)/p ) + c + b;
	},
	easeInOutElastic: function (x, t, b, c, d) {
		var s=1.70158;var p=0;var a=c;
		if (t==0) return b;  if ((t/=d/2)==2) return b+c;  if (!p) p=d*(.3*1.5);
		if (a < Math.abs(c)) { a=c; var s=p/4; }
		else var s = p/(2*Math.PI) * Math.asin (c/a);
		if (t < 1) return -.5*(a*Math.pow(2,10*(t-=1)) * Math.sin( (t*d-s)*(2*Math.PI)/p )) + b;
		return a*Math.pow(2,-10*(t-=1)) * Math.sin( (t*d-s)*(2*Math.PI)/p )*.5 + c + b;
	},
	easeInBack: function (x, t, b, c, d, s) {
		if (s == undefined) s = 1.70158;
		return c*(t/=d)*t*((s+1)*t - s) + b;
	},
	easeOutBack: function (x, t, b, c, d, s) {
		if (s == undefined) s = 1.70158;
		return c*((t=t/d-1)*t*((s+1)*t + s) + 1) + b;
	},
	easeInOutBack: function (x, t, b, c, d, s) {
		if (s == undefined) s = 1.70158; 
		if ((t/=d/2) < 1) return c/2*(t*t*(((s*=(1.525))+1)*t - s)) + b;
		return c/2*((t-=2)*t*(((s*=(1.525))+1)*t + s) + 2) + b;
	},
	easeInBounce: function (x, t, b, c, d) {
		return c - jQuery.easing.easeOutBounce (x, d-t, 0, c, d) + b;
	},
	easeOutBounce: function (x, t, b, c, d) {
		if ((t/=d) < (1/2.75)) {
			return c*(7.5625*t*t) + b;
		} else if (t < (2/2.75)) {
			return c*(7.5625*(t-=(1.5/2.75))*t + .75) + b;
		} else if (t < (2.5/2.75)) {
			return c*(7.5625*(t-=(2.25/2.75))*t + .9375) + b;
		} else {
			return c*(7.5625*(t-=(2.625/2.75))*t + .984375) + b;
		}
	},
	easeInOutBounce: function (x, t, b, c, d) {
		if (t < d/2) return jQuery.easing.easeInBounce (x, t*2, 0, c, d) * .5 + b;
		return jQuery.easing.easeOutBounce (x, t*2-d, 0, c, d) * .5 + c*.5 + b;
	}
});

/*
 * jQuery Superfish Menu Plugin
 * Copyright (c) 2013 Joel Birch
 *
 * Dual licensed under the MIT and GPL licenses:
 *	http://www.opensource.org/licenses/mit-license.php
 *	http://www.gnu.org/licenses/gpl.html
 */

(function ($) {
	"use strict";

	var methods = (function () {
		// private properties and methods go here
		var c = {
				bcClass: 'sf-breadcrumb',
				menuClass: 'sf-js-enabled',
				anchorClass: 'sf-with-ul',
				menuArrowClass: 'sf-arrows'
			},
			ios = (function () {
				var ios = /iPhone|iPad|iPod/i.test(navigator.userAgent);
				if (ios) {
					// iOS clicks only bubble as far as body children
					$(window).load(function () {
						$('body').children().on('click', $.noop);
					});
				}
				return ios;
			})(),
			wp7 = (function () {
				var style = document.documentElement.style;
				return ('behavior' in style && 'fill' in style && /iemobile/i.test(navigator.userAgent));
			})(),
			toggleMenuClasses = function ($menu, o) {
				var classes = c.menuClass;
				if (o.cssArrows) {
					classes += ' ' + c.menuArrowClass;
				}
				$menu.toggleClass(classes);
			},
			setPathToCurrent = function ($menu, o) {
				return $menu.find('li.' + o.pathClass).slice(0, o.pathLevels)
					.addClass(o.hoverClass + ' ' + c.bcClass)
						.filter(function () {
							return ($(this).children(o.popUpSelector).hide().show().length);
						}).removeClass(o.pathClass);
			},
			toggleAnchorClass = function ($li) {
				$li.children('a').toggleClass(c.anchorClass);
			},
			toggleTouchAction = function ($menu) {
				var touchAction = $menu.css('ms-touch-action');
				touchAction = (touchAction === 'pan-y') ? 'auto' : 'pan-y';
				$menu.css('ms-touch-action', touchAction);
			},
			applyHandlers = function ($menu, o) {
				var targets = 'li:has(' + o.popUpSelector + ')';
				if ($.fn.hoverIntent && !o.disableHI) {
					$menu.hoverIntent(over, out, targets);
				}
				else {
					$menu
						.on('mouseenter.superfish', targets, over)
						.on('mouseleave.superfish', targets, out);
				}
				var touchevent = 'MSPointerDown.superfish';
				if (!ios) {
					touchevent += ' touchend.superfish';
				}
				if (wp7) {
					touchevent += ' mousedown.superfish';
				}
				$menu
					.on('focusin.superfish', 'li', over)
					.on('focusout.superfish', 'li', out)
					.on(touchevent, 'a', o, touchHandler);
			},
			touchHandler = function (e) {
				var $this = $(this),
					$ul = $this.siblings(e.data.popUpSelector);

				if ($ul.length > 0 && $ul.is(':hidden')) {
					$this.one('click.superfish', false);
					if (e.type === 'MSPointerDown') {
						$this.trigger('focus');
					} else {
						$.proxy(over, $this.parent('li'))();
					}
				}
			},
			over = function () {
				var $this = $(this),
					o = getOptions($this);
				clearTimeout(o.sfTimer);
				$this.siblings().superfish('hide').end().superfish('show');
			},
			out = function () {
				var $this = $(this),
					o = getOptions($this);
				if (ios) {
					$.proxy(close, $this, o)();
				}
				else {
					clearTimeout(o.sfTimer);
					o.sfTimer = setTimeout($.proxy(close, $this, o), o.delay);
				}
			},
			close = function (o) {
				o.retainPath = ($.inArray(this[0], o.$path) > -1);
				this.superfish('hide');

				if (!this.parents('.' + o.hoverClass).length) {
					o.onIdle.call(getMenu(this));
					if (o.$path.length) {
						$.proxy(over, o.$path)();
					}
				}
			},
			getMenu = function ($el) {
				return $el.closest('.' + c.menuClass);
			},
			getOptions = function ($el) {
				return getMenu($el).data('sf-options');
			};

		return {
			// public methods
			hide: function (instant) {
				if (this.length) {
					var $this = this,
						o = getOptions($this);
					if (!o) {
						return this;
					}
					var not = (o.retainPath === true) ? o.$path : '',
						$ul = $this.find('li.' + o.hoverClass).add(this).not(not).removeClass(o.hoverClass).children(o.popUpSelector),
						speed = o.speedOut;

					if (instant) {
						$ul.show();
						speed = 0;
					}
					o.retainPath = false;
					o.onBeforeHide.call($ul);
					$ul.stop(true, true).animate(o.animationOut, speed, function () {
						var $this = $(this);
						o.onHide.call($this);
					});
				}
				return this;
			},
			show: function () {
				var o = getOptions(this);
				if (!o) {
					return this;
				}
				var $this = this.addClass(o.hoverClass),
					$ul = $this.children(o.popUpSelector);

				o.onBeforeShow.call($ul);
				$ul.stop(true, true).animate(o.animation, o.speed, function () {
					o.onShow.call($ul);
				});
				return this;
			},
			destroy: function () {
				return this.each(function () {
					var $this = $(this),
						o = $this.data('sf-options'),
						$hasPopUp;
					if (!o) {
						return false;
					}
					$hasPopUp = $this.find(o.popUpSelector).parent('li');
					clearTimeout(o.sfTimer);
					toggleMenuClasses($this, o);
					toggleAnchorClass($hasPopUp);
					toggleTouchAction($this);
					// remove event handlers
					$this.off('.superfish').off('.hoverIntent');
					// clear animation's inline display style
					$hasPopUp.children(o.popUpSelector).attr('style', function (i, style) {
						return style.replace(/display[^;]+;?/g, '');
					});
					// reset 'current' path classes
					o.$path.removeClass(o.hoverClass + ' ' + c.bcClass).addClass(o.pathClass);
					$this.find('.' + o.hoverClass).removeClass(o.hoverClass);
					o.onDestroy.call($this);
					$this.removeData('sf-options');
				});
			},
			init: function (op) {
				return this.each(function () {
					var $this = $(this);
					if ($this.data('sf-options')) {
						return false;
					}
					var o = $.extend({}, $.fn.superfish.defaults, op),
						$hasPopUp = $this.find(o.popUpSelector).parent('li');
					o.$path = setPathToCurrent($this, o);

					$this.data('sf-options', o);

					toggleMenuClasses($this, o);
					toggleAnchorClass($hasPopUp);
					toggleTouchAction($this);
					applyHandlers($this, o);

					$hasPopUp.not('.' + c.bcClass).superfish('hide', true);

					o.onInit.call(this);
				});
			}
		};
	})();

	$.fn.superfish = function (method, args) {
		if (methods[method]) {
			return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
		}
		else if (typeof method === 'object' || ! method) {
			return methods.init.apply(this, arguments);
		}
		else {
			return $.error('Method ' +  method + ' does not exist on jQuery.fn.superfish');
		}
	};

	$.fn.superfish.defaults = {
		popUpSelector: 'ul,.sf-mega', // within menu context
		hoverClass: 'sfHover',
		pathClass: 'overrideThisToUse',
		pathLevels: 1,
		delay: 800,
		animation: {opacity: 'show'},
		animationOut: {opacity: 'hide'},
		speed: 'normal',
		speedOut: 'fast',
		cssArrows: true,
		disableHI: false,
		onInit: $.noop,
		onBeforeShow: $.noop,
		onShow: $.noop,
		onBeforeHide: $.noop,
		onHide: $.noop,
		onIdle: $.noop,
		onDestroy: $.noop
	};

	// soon to be deprecated
	$.fn.extend({
		hideSuperfishUl: methods.hide,
		showSuperfishUl: methods.show
	});

})(jQuery);


// jQuery hcSticky
// =============
// Version: 1.1.96
// Copyright: Some Web Media
// Author: Some Web Guy
// Author URL: http://twitter.com/some_web_guy
// Website: http://someweblog.com/
// Plugin URL: http://someweblog.com/hcsticky-jquery-floating-sticky-plugin/
// License: Released under the MIT License www.opensource.org/licenses/mit-license.php
// Description: Makes elements on your page float as you scroll

(function($, undefined){
    /*----------------------------------------------------
                        GLOBAL FUNCTIONS
    ----------------------------------------------------*/
    
    // check for scroll direction and speed
    var getScroll = function() {
	var pageXOffset = window.pageXOffset !== undefined ? window.pageXOffset : window.document.documentElement.scrollLeft;
	var pageYOffset = window.pageYOffset !== undefined ? window.pageYOffset : window.document.documentElement.scrollTop;
	
        if (typeof getScroll.x == 'undefined') {
            getScroll.x = pageXOffset;
            getScroll.y = pageYOffset;
        }
        if (typeof getScroll.distanceX == 'undefined') {
            getScroll.distanceX = pageXOffset;
            getScroll.distanceY = pageYOffset;
        } else {
            getScroll.distanceX = pageXOffset - getScroll.x;
            getScroll.distanceY = pageYOffset - getScroll.y;
        }
        var diffX = getScroll.x - pageXOffset,
            diffY = getScroll.y - pageYOffset;
        getScroll.direction = diffX < 0 ? 'right' :
            diffX > 0 ? 'left' :
            diffY <= 0 ? 'down' :
            diffY > 0 ? 'up' : 'first';
        getScroll.x = pageXOffset;
        getScroll.y = pageYOffset;
    };
    $(window).on('scroll', getScroll);
    
    // get original css (auto, %)
    var getCSS = function(el,style) {
	// check if we already cloned this element
        if (typeof el.cssClone == 'undefined') {
            el.cssClone = el.clone().css('display','none');
	    // change the name of cloned radio buttons, otherwise selections get screwed
	    el.cssClone.find('input:radio').attr('name','sfd4fgdf');
	    // insert clone to DOM
            el.after(el.cssClone);
        }
        var clone = el.cssClone[0];
        if(typeof style != 'undefined') {
            var value;
            if (clone.currentStyle) {
		// regex e.g. padding-left to paddingLeft
                value = clone.currentStyle[style.replace(/-\w/g, function(s){return s.toUpperCase().replace('-','')})];
            } else if (window.getComputedStyle) {
                value = document.defaultView.getComputedStyle(clone,null).getPropertyValue(style);
            }
	    // check for margin:auto
            value = (/margin/g.test(style)) ? ((parseInt(value) === el[0].offsetLeft) ? value : 'auto') : value;
        }
        return {
            value: value || null,
            remove: function() {
                el.cssClone.remove();
            }
        };
    };
    
    /*----------------------------------------------------
                        JQUERY PLUGIN
    ----------------------------------------------------*/
    
    $.fn.extend({
        
        hcSticky: function(options, reinit){
            
	    // check if selected element exist in DOM, user doesn't have to worry about that
	    if (this.length == 0)
		return this;
	    
            var settings = options || {},
                running = (this.data('hcSticky')) ? true : false,
                $window = $(window),
                $document = $(document);
            
            if (typeof settings == 'string') {
                switch(settings){
                    case 'reinit':
                        // detach scroll event
                        $window.off('scroll', this.data('hcSticky').f);
                        // call itself to start again
                        return this.hcSticky({},true);
                        break;
                    case 'off':
                        this.data('hcSticky', $.extend(this.data('hcSticky'),{on:false}));
                        break;
                    case 'on':
                        this.data('hcSticky', $.extend(this.data('hcSticky'),{on:true}));
                        break;
                }
                return this;
            } else if (typeof settings == 'object') {
                if (!running) {
                    // these are the default settings
                    this.data('hcSticky', $.extend({
                        top: 0,
                        bottom: 0,
                        bottomEnd: 0,
                        bottomLimiter: null,
                        innerTop: 0,
                        innerSticker: null,
                        className: 'sticky',
                        wrapperClassName: 'wrapper-sticky',
                        noContainer: false,
			parent: null,
			responsive: true,
                        followScroll: true,
			onStart: function(){},
			onStop: function(){},
                        on: true
                    }, settings));
                    // check for bottom limiter
                    var $bottom_limiter = this.data('hcSticky').bottomLimiter;
                    if ($bottom_limiter !== null && this.data('hcSticky').noContainer) {
                        this.data('hcSticky', $.extend(this.data('hcSticky'),{bottomEnd:$document.height() - $($bottom_limiter).offset().top}));
                    }
                } else {
                    // update existing settings
                    this.data('hcSticky', $.extend(this.data('hcSticky'),settings));
                }
                // if already running and not reinited don't go further all we needed was to update settings
                if (running && !reinit) {
                    return this;
                }
            }
	    
	    // do our thing
            return this.each(function(){
                
                var $this = $(this),
		    $parent = $this.data('hcSticky').parent ? $($this.data('hcSticky').parent) : $this.parent(),
		    // get wrapper if already created, if not create it
		    $wrapper = (function(){
                            // wrapper exists
                            var $this_wrapper = $this.parent('.'+$this.data('hcSticky').wrapperClassName);
                            if ($this_wrapper.length > 0) {
                                $this_wrapper.css({
                                    'height': $this.outerHeight(true),
                                    'width': (function(){
                                        // check if wrapper already has width in %
                                        var width = getCSS($this_wrapper,'width').value;
                                        getCSS($this_wrapper).remove();
                                        if (width.indexOf('%') >= 0 || width == 'auto') {
                                            $this.css('width',$this_wrapper.width());
                                            return width;
                                        } else {
                                            return $this.outerWidth(true);
                                        }
                                    })()
                                });
                                return $this_wrapper;
                            } else {
                                return false;
                            }
                        })() || (function(){
                            // wrapper doesn't exist, create it
                            var $this_wrapper = $('<div>',{'class':$this.data('hcSticky').wrapperClassName}).css({
                                'height': $this.outerHeight(true),
                                'width': (function(){
                                    // check if element has width in %
                                    var width = getCSS($this,'width').value;
                                    if (width.indexOf('%') >= 0 || width == 'auto') {
                                        $this.css('width',parseFloat($this.css('width')));
                                        return width;
                                    } else {
                                        // check if margin is set to 'auto'
                                        var margin = getCSS($this,'margin-left').value;
                                        return (margin == 'auto') ? $this.outerWidth() : $this.outerWidth(true);
                                    }
                                })(),
                                'margin': (getCSS($this,'margin-left').value) ? 'auto' : null,
                                'position': (function(){
                                    var position = $this.css('position');
                                    return position == 'static' ? 'relative' : position;
                                })(),
                                'float': $this.css('float') || null,
                                'left': getCSS($this,'left').value,
                                'right': getCSS($this,'right').value,
                                'top': getCSS($this,'top').value,
                                'bottom': getCSS($this,'bottom').value
                            });
                            $this.wrap($this_wrapper);
                            // return appended element
                            return $this.parent();
                        }
                    )(),
                    // functions for attachiung and detaching sticky
                    setFixed = function(args){
			// check if already floating
			if ($this.hasClass($this.data('hcSticky').className))
			    return;
                        args = args || {};
                        $this.css({
                            position: 'fixed',
                            top: args.top || 0,
                            left: args.left || $wrapper.offset().left
                        }).addClass($this.data('hcSticky').className);
			// run function for start event of sticky
			$this.data('hcSticky').onStart.apply(this);
                    },
                    reset = function(args){
                        args = args || {};
                        $this.css({
                            position: args.position || 'absolute',
                            top: args.top || 0,
                            left: args.left || 0
                        }).removeClass($this.data('hcSticky').className);
			// run function for stop event of sticky
			$this.data('hcSticky').onStop.apply(this);
                    };
                
                // clear clone element we created for geting real css value
                getCSS($this).remove();
                // reset sticky content
                $this.css({top:'auto',bottom:'auto',left:'auto',right:'auto'});
                
		// before anything, check if element height is bigger than the content
		if ($this.outerHeight(true) > $parent.height())
		    return this;
		// also attach event on entire page load, maybe some images inside element has been delayd, so chek widths again
		$(window).load(function(){
		    if ($this.outerHeight(true) > $parent.height()) {
			$wrapper.css('height', $this.outerHeight(true));
			$this.hcSticky('reinit');
		    }
		});
		
                // start the magic
                var f = function(init){
                    
                    // get referring element
                    $referrer = ($this.data('hcSticky').noContainer) ? $document : ($this.data('hcSticky').parent ? $($this.data('hcSticky').parent) : $wrapper.parent());
                    
                    // check if we need to run sticky
                    if (!$this.data('hcSticky').on || $this.outerHeight(true) >= $referrer.height())
                        return;

                    var top_spacing = ($this.data('hcSticky').innerSticker) ? $($this.data('hcSticky').innerSticker).position().top : (($this.data('hcSticky').innerTop) ? $this.data('hcSticky').innerTop : 0),
                        //wrapper_inner_top = $wrapper.offset().top + ($this.data('hcSticky').noContainer ? 0 : (parseInt($referrer.css('borderTopWidth')) + parseInt($referrer.css('padding-top')) + parseInt($referrer.css('margin-top')))), 
			wrapper_inner_top = $wrapper.offset().top,
                        bottom_limit = $referrer.height() - $this.data('hcSticky').bottomEnd + ($this.data('hcSticky').noContainer ? 0 : wrapper_inner_top),
                        top_limit = $wrapper.offset().top - $this.data('hcSticky').top + top_spacing,
                        this_height = $this.outerHeight(true) + $this.data('hcSticky').bottom,
                        window_height = $window.height(),
                        offset_top = $window.scrollTop(),
                        this_document_top = $this.offset().top,
                        this_window_top = this_document_top - offset_top,
                        bottom_distance; // this is for later
                    
                    
                    if (offset_top >= top_limit) {
						
						//********************* this is my fix *******************************************************
						//if (bottom_limit + $this.data('hcSticky').bottom - ($this.data('hcSticky').followScroll ? 0 : $this.data('hcSticky').top) <= offset_top + this_height - top_spacing - ((this_height - top_spacing > window_height - (top_limit - top_spacing) && $this.data('hcSticky').followScroll) ? (((bottom_distance = this_height - window_height - top_spacing) > 0) ? bottom_distance : 0) : 0)) {
                        // I have no idea what am I checking here, but it works
			// http://geek-and-poke.com/geekandpoke/2012/7/27/simply-explained.html
                        //if (bottom_limit + $this.data('hcSticky').bottom - $this.data('hcSticky').top <= offset_top + this_height - top_spacing - ((this_height - top_spacing > window_height - (top_limit - top_spacing) && $this.data('hcSticky').followScroll) ? (((bottom_distance = this_height - window_height - top_spacing) > 0) ? bottom_distance : 0) : 0)) {
			if (bottom_limit + $this.data('hcSticky').bottom - ($this.data('hcSticky').followScroll ? 0 : $this.data('hcSticky').top) <= offset_top + $this.data('hcSticky').top + this_height - top_spacing - ((this_height - top_spacing > window_height - (top_limit - top_spacing) && $this.data('hcSticky').followScroll) ? (((bottom_distance = this_height - window_height - top_spacing) > 0) ? bottom_distance : 0) : 0)) {
                            // bottom reached end
                            reset({
                                top: bottom_limit - this_height + $this.data('hcSticky').bottom - wrapper_inner_top
                            });
                        } else if (this_height - top_spacing > window_height && $this.data('hcSticky').followScroll ) {

                            // sidebar bigger than window with follow scroll on
                            if (this_window_top + this_height <= window_height) {
                                if (getScroll.direction == 'down') {
                                    // scroll down
                                    setFixed({
                                        top: window_height - this_height
                                    });
                                } else {
                                    // scroll up
                                    if (this_window_top < 0 && $this.css('position') == 'fixed') {
                                        reset({
                                            top: this_document_top - (top_limit + $this.data('hcSticky').top - top_spacing) - getScroll.distanceY
                                        });
                                    }
                                }
                            // sidebar smaller than window or follow scroll turned off
                            } else {
                                if (getScroll.direction == 'up' && this_document_top >= offset_top + $this.data('hcSticky').top - top_spacing) {
                                    // scroll up
                                    
                                    setFixed({
                                        top: $this.data('hcSticky').top - top_spacing
                                    });
                                } else if (getScroll.direction == 'down' && this_document_top + this_height > window_height && $this.css('position') == 'fixed') {
                                    // scroll down
                                    reset({
                                        top: this_document_top - (top_limit + $this.data('hcSticky').top - top_spacing) - getScroll.distanceY
                                    });
                                }
                            }
                        } else {
                            // starting (top) fixed position
                            setFixed({
                                top: $this.data('hcSticky').top - top_spacing 
                            });
                        }
                    } else {
                        // reset bar
                        reset();
                    }

                    // just in case someone set "top" larger than elements style top
                    if (init === true) {
			$this.css('top', ($this.css('position') == 'fixed') ? $this.data('hcSticky').top - top_spacing : 0);
                        //$this.css('top', ($this.css('position') == 'fixed') ? $wrapper.offset().top : 0);
                    }

                };
		
		// store resize data in case responsive is on
                var resize_timeout = false,
		    $resize_clone = false;
		
		function onResize(){
		    // check for width change (css media queries)
		    if ($this.data('hcSticky').responsive) {
			// clone element and make it invisible
			if (!$resize_clone) {
			    $resize_clone = $this.clone().attr('style','').css({visibility:'hidden',height:0,overflow:'hidden',paddingTop:0,paddingBottom:0,marginTop:0,marginBottom:0});
			    $wrapper.after($resize_clone);
			}
			
			if (getCSS($resize_clone,'width').value != getCSS($wrapper,'width').value)
			    $wrapper.width(getCSS($resize_clone,'width').value);
			// remove wrapper clone
			getCSS($wrapper).remove();
			
			// clear previous timeout
			if (resize_timeout) {
			    clearTimeout(resize_timeout);
			}
			// timedout destroing of cloned elements
			resize_timeout = setTimeout(function(){
			    // clear timeout id
			    resize_timeout = false;
			    // destroy cloned elements
			    getCSS($resize_clone).remove();
			    $resize_clone.remove();
			    $resize_clone = false;
			}, 100);
		    }
		    
		    // set left position
                    if ($this.css('position') == 'fixed') {
                        $this.css('left', $wrapper.offset().left);
                    } else {
                        $this.css('left', 0);
                    }
		    // recalculate inner element width (maybe original width was in %)
		    if ($this.width() != $wrapper.width())
			$this.css('width', $wrapper.width());
                }
		// attach resize event
                $window.on('resize', onResize);
                
                // set scroll empty function in case we need to reinit plugin
                $this.data('hcSticky', $.extend($this.data('hcSticky'),{f:function(){}}));
                // set scroll function
                $this.data('hcSticky', $.extend($this.data('hcSticky'),{f:f}));
                // run it for the first time to disable glitching
                $this.data('hcSticky').f(true);
                // attach function to scroll event
                $window.on('scroll', $this.data('hcSticky').f);
		
            });
        }
    });

})(jQuery);
	    


/**
 * Copyright (c) 2007-2012 Ariel Flesler - aflesler(at)gmail(dot)com | http://flesler.blogspot.com
 * Dual licensed under MIT and GPL.
 * @author Ariel Flesler
 * @version 1.4.3
 */
;(function($){var h=$.scrollTo=function(a,b,c){$(window).scrollTo(a,b,c)};h.defaults={axis:'xy',duration:parseFloat($.fn.jquery)>=1.3?0:1,limit:true};h.window=function(a){return $(window)._scrollable()};$.fn._scrollable=function(){return this.map(function(){var a=this,isWin=!a.nodeName||$.inArray(a.nodeName.toLowerCase(),['iframe','#document','html','body'])!=-1;if(!isWin)return a;var b=(a.contentWindow||a).document||a.ownerDocument||a;return/webkit/i.test(navigator.userAgent)||b.compatMode=='BackCompat'?b.body:b.documentElement})};$.fn.scrollTo=function(e,f,g){if(typeof f=='object'){g=f;f=0}if(typeof g=='function')g={onAfter:g};if(e=='max')e=9e9;g=$.extend({},h.defaults,g);f=f||g.duration;g.queue=g.queue&&g.axis.length>1;if(g.queue)f/=2;g.offset=both(g.offset);g.over=both(g.over);return this._scrollable().each(function(){if(!e)return;var d=this,$elem=$(d),targ=e,toff,attr={},win=$elem.is('html,body');switch(typeof targ){case'number':case'string':if(/^([+-]=)?\d+(\.\d+)?(px|%)?$/.test(targ)){targ=both(targ);break}targ=$(targ,this);if(!targ.length)return;case'object':if(targ.is||targ.style)toff=(targ=$(targ)).offset()}$.each(g.axis.split(''),function(i,a){var b=a=='x'?'Left':'Top',pos=b.toLowerCase(),key='scroll'+b,old=d[key],max=h.max(d,a);if(toff){attr[key]=toff[pos]+(win?0:old-$elem.offset()[pos]);if(g.margin){attr[key]-=parseInt(targ.css('margin'+b))||0;attr[key]-=parseInt(targ.css('border'+b+'Width'))||0}attr[key]+=g.offset[pos]||0;if(g.over[pos])attr[key]+=targ[a=='x'?'width':'height']()*g.over[pos]}else{var c=targ[pos];attr[key]=c.slice&&c.slice(-1)=='%'?parseFloat(c)/100*max:c}if(g.limit&&/^\d+$/.test(attr[key]))attr[key]=attr[key]<=0?0:Math.min(attr[key],max);if(!i&&g.queue){if(old!=attr[key])animate(g.onAfterFirst);delete attr[key]}});animate(g.onAfter);function animate(a){$elem.animate(attr,f,g.easing,a&&function(){a.call(this,e,g)})}}).end()};h.max=function(a,b){var c=b=='x'?'Width':'Height',scroll='scroll'+c;if(!$(a).is('html,body'))return a[scroll]-$(a)[c.toLowerCase()]();var d='client'+c,html=a.ownerDocument.documentElement,body=a.ownerDocument.body;return Math.max(html[scroll],body[scroll])-Math.min(html[d],body[d])};function both(a){return typeof a=='object'?a:{top:a,left:a}}})(jQuery);

/*
 * jQuery One Page Nav Plugin
 * http://github.com/davist11/jQuery-One-Page-Nav
 *
 * Copyright (c) 2010 Trevor Davis (http://trevordavis.net)
 * Dual licensed under the MIT and GPL licenses.
 * Uses the same license as jQuery, see:
 * http://jquery.org/license
 *
 * @version 2.2
 *
 * Example usage:
 * $('#nav').onePageNav({
 *   currentClass: 'current',
 *   changeHash: false,
 *   scrollSpeed: 750
 * });
 */

;(function($, window, document, undefined){

	// our plugin constructor
	var OnePageNav = function(elem, options){
		this.elem = elem;
		this.$elem = $(elem);
		this.options = options;
		this.metadata = this.$elem.data('plugin-options');
		this.$nav = this.$elem.find('a');
		this.$win = $(window);
		this.sections = {};
		this.didScroll = false;
		this.$doc = $(document);
		this.docHeight = this.$doc.height();
	};

	// the plugin prototype
	OnePageNav.prototype = {
		defaults: {
			currentClass: 'current',
			changeHash: false,
			easing: 'swing',
			filter: '',
			scrollSpeed: 750,
			scrollOffset: 0,
			scrollThreshold: 0.5,
			begin: false,
			end: false,
			scrollChange: false
		},

		init: function() {
			var self = this;
			
			// Introduce defaults that can be extended either
			// globally or using an object literal.
			self.config = $.extend({}, self.defaults, self.options, self.metadata);
			
			//Filter any links out of the nav
			if(self.config.filter !== '') {
				self.$nav = self.$nav.filter(self.config.filter);
			}
			
			//Handle clicks on the nav
			self.$nav.on('click.onePageNav', $.proxy(self.handleClick, self));

			//Get the section positions
			self.getPositions();
			
			//Handle scroll changes
			self.bindInterval();
			
			//Update the positions on resize too
			self.$win.on('resize.onePageNav', $.proxy(self.getPositions, self));

			return this;
		},
		
		adjustNav: function(self, $parent) {
			self.$elem.find('.' + self.config.currentClass).removeClass(self.config.currentClass);
			$parent.addClass(self.config.currentClass);
			//console.log();
			//my hack to auto scroll to selected
			self.$elem.scrollTo( self.$elem.find('.' + self.config.currentClass), 300 );
		},
		
		bindInterval: function() {
			var self = this;
			var docHeight;
			
			self.$win.on('scroll.onePageNav', function() {
				self.didScroll = true;
			});
			
			self.t = setInterval(function() {
				docHeight = self.$doc.height();
				
				//If it was scrolled
				if(self.didScroll) {
					self.didScroll = false;
					self.scrollChange();
				}
				
				//If the document height changes
				if(docHeight !== self.docHeight) {
					self.docHeight = docHeight;
					self.getPositions();
				}
			}, 250);
		},
		
		getHash: function($link) {
			return $link.attr('href').split('#')[1];
		},
		
		getPositions: function() {
			var self = this;
			var linkHref;
			var topPos;
			var $target;
			
			self.$nav.each(function() {
				linkHref = self.getHash($(this));
				$target = $('#' + linkHref);

				if($target.length) {
					topPos = $target.offset().top;
					self.sections[linkHref] = Math.round(topPos) - self.config.scrollOffset;
				}
			});
		},
		
		getSection: function(windowPos) {
			var returnValue = null;
			var windowHeight = Math.round(this.$win.height() * this.config.scrollThreshold);

			for(var section in this.sections) {
				if((this.sections[section] - windowHeight) < windowPos) {
					returnValue = section;
				}
			}
			
			return returnValue;
		},
		
		handleClick: function(e) {
			var self = this;
			var $link = $(e.currentTarget);
			var $parent = $link.parent();
			var newLoc = '#' + self.getHash($link);



				if(!$parent.hasClass(self.config.currentClass)) {
					//Start callback
					if(self.config.begin) {
						self.config.begin();
					}
					
					//Change the highlighted nav item
					self.adjustNav(self, $parent);
					
					//Removing the auto-adjust on scroll
					self.unbindInterval();
					
					//Scroll to the correct position
					$.scrollTo(newLoc, self.config.scrollSpeed, {
						axis: 'y',
						easing: self.config.easing,
						offset: {
							top: -self.config.scrollOffset
						},
						onAfter: function() {
							//Do we need to change the hash?
							if(self.config.changeHash) {
								window.location.hash = newLoc;
							}
							
							//Add the auto-adjust on scroll back in
							self.bindInterval();
							
							//End callback
							if(self.config.end) {
								self.config.end();
							}
						}
					});
				}
	
				e.preventDefault();
			
		},
		
		scrollChange: function() {
			var windowTop = this.$win.scrollTop();
			var position = this.getSection(windowTop);
			var $parent;
			
			//If the position is set
			if(position !== null) {
				$parent = this.$elem.find('a[href$="#' + position + '"]').parent();
				
				//If it's not already the current section
				if(!$parent.hasClass(this.config.currentClass)) {
					//Change the highlighted nav item
					this.adjustNav(this, $parent);
					
					//If there is a scrollChange callback
					if(this.config.scrollChange) {
						this.config.scrollChange($parent);
					}
				}
			}
		},
		
		unbindInterval: function() {
			clearInterval(this.t);
			this.$win.unbind('scroll.onePageNav');
		}
	};

	OnePageNav.defaults = OnePageNav.prototype.defaults;

	$.fn.onePageNav = function(options) {
		return this.each(function() {
			new OnePageNav(this, options).init();
		});
	};
	
})( jQuery, window , document );


