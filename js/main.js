$(document).ready(function() {	
	/****************************** mobile device detection ***************************/
	var isMobile = {
		Android: function() {
			return navigator.userAgent.match(/Android/i);
		},
		BlackBerry: function() {
			return navigator.userAgent.match(/BlackBerry/i);
		},
		iOS: function() {
			return navigator.userAgent.match(/iPhone|iPad|iPod/i);
		},
		Opera: function() {
			return navigator.userAgent.match(/Opera Mini/i);
		},
		Windows: function() {
			return navigator.userAgent.match(/IEMobile/i);
		},
		any: function() {
			return (isMobile.Android() || isMobile.BlackBerry() || isMobile.iOS() || isMobile.Opera() || isMobile.Windows());
		}
	};	
	
	$('#nav').superfish();
	
	if(!isMobile.any()){
		$(window).scroll(function() {
			var offset = window.pageYOffset;
			
			//console.log(offset);
			// -x+(offset/y) -x is for the starting point | y is for the speed smaller value is faster
			
			if($('body').hasClass('documentation'))
				var top_offset = 10;
			else
				var top_offset = 120;
			
			if(offset>top_offset){
				$('body').addClass('fixed-head');
			} else {
				$('body').removeClass('fixed-head reverse-scroll');
			}
			
			if(offset<3586){
				$('body').addClass('fixed');
				$('body').removeClass('noscroll');
				
				//intro animation
				if( offset < 70 ){
					$('#intro').stop().animate({"top" : 255},'ease');	
				} else if( offset > 70 ){
					$('#intro').stop().animate({"top" : 900},'ease');	
				}
				
				//first part
				//start from 70px scroll
				if( offset > 100 ){
					$('#part1').stop().animate({"top" : 0, "right" : 5},'ease');	
				} else if( offset < 500 ) {
					$('#part1').stop().animate({"top" : -680, "right" : -680},'ease');	
				}
				
				//first part | title
				//start from 150px scroll
				if( offset > 500 && offset < 800 ){
					$('#title1').stop().animate({"opacity" : 1});	
				} else {
					$('#title1').stop().animate({"opacity" : 0});
				}
					
				//second part
				//start from 200px scroll
				if( offset > 800 ){					
					$('#part2').stop().animate({"top" : 17, "right" : 8},'ease');
				} else if( offset < 1200 ) {
					$('#part2').stop().animate({"top" : -680, "right" : -680},'ease');	
				}
				
				//second part | title
				//start from 230px scroll
				if( offset > 1200 && offset < 1500 ){
					$('#title2').stop().animate({"opacity" : 1});	
				} else {
					$('#title2').stop().animate({"opacity" : 0});
				}
					
				//third part
				//start from 200px scroll
				if( offset > 1500 ){
					$('#part3').stop().animate({"top" : 23, "right" : 8},'ease');
				} else if( offset < 1900 ) {
					$('#part3').stop().animate({"top" : -680, "right" : -680},'ease');	
				}			
				
				//third part | title
				//start from 250px scroll
				if( offset > 1900 && offset < 2200 ){
					$('#title3').stop().animate({"opacity" : 1});	
				} else {
					$('#title3').stop().animate({"opacity" : 0});
				}
					
				//fourth part
				//start from 200px scroll
				if( offset > 2200 ){
					$('#part4').stop().animate({"top" : 20, "right" : 0},'ease');
				} else if( offset < 2600 ) {
					$('#part4').stop().animate({"top" : -680, "right" : -680},'ease');	
				}
				
				//engine move
				if( offset > 2600 && offset < 3300 ){
					$('#engine').stop().animate({"right" : 340});	
				} else {
					$('#engine').stop().animate({"right" : 0});	
				}
				
				//fourth part | title
				//start from 230px scroll
				if( offset > 2900 && offset < 3300 ){
					$('#title4').stop().animate({"opacity" : 1});	
				} else {
					$('#title4').stop().animate({"opacity" : 0});
				}			

				//fadein title
				if(offset > 3300)
					$('#banner-title').fadeIn();	
				else
					$('#banner-title').fadeOut();
				
				//fadein arrow
				if(offset > 3400)
					$('#section1 .scroll-to-arrow').stop().animate({"opacity":1});
				else if(offset < 3400)
					$('#section1 .scroll-to-arrow').stop().animate({"opacity":0});
					
				
			} else {
				$('body').removeClass('fixed');
			}
		
		});	
		
		var offset = window.pageYOffset;	
		if(offset>3586) {
			$('body').addClass('noscroll reverse-scroll');	
		}
	} else {
		$('body').addClass('noscroll reverse-scroll mobile');	
	}
	
	
	$('#tabs-btns a').click(function(){
		if( !$(this).parent().hasClass('active') ){
			$('#tabs-btns li').removeClass('active');
			$(this).parent().addClass('active');
			$('.tab-cont').hide();
			$($(this).attr('href')).fadeIn();
		}
		return false;
	});
	$('#tabs-btns li:first-child a').trigger('click');
	
	$('.scroll-to').click(function(){
		$('html,body').animate({scrollTop: $( $(this).attr('href') ).offset().top - 55},500);
		return false;
	});
	
	$('.acc h4').click(function() {		
		if( $(this).next().is(':visible')==false ){
			$('.acc-cont').slideUp();
			$(this).next().slideDown();
			$('.acc').removeClass('active');			
			$(this).parent().addClass('active');
		}else{
			$(this).next().slideUp();
			$(this).parent().removeClass('active');
		}		
		return false;
	});	
	$('.acc:first h4').trigger('click');	
	
	if( $('.subnav').length ){
		$(".subnav").hcSticky({top: 55,noContainer:true});
	}

	var menuHeight = $("#docs-sidemenu").height();
	function startScrolling(){
		//console.log(menuHeight + ' ' + $(window).height())
		if(menuHeight+123 > $(window).height())
			$("#docs-sidemenu").addClass('scrolling').removeClass('stay');
		else
			$("#docs-sidemenu").addClass('scrolling-big').removeClass('stay');
	}
	function stopScrolling(){
		$("#docs-sidemenu").removeClass('scrolling').removeClass('scrolling-big');	
	}
	function stickBottom(){
		$("#docs-sidemenu").removeClass('scrolling').removeClass('scrolling-big').addClass('stay');
	}	
	
	//$("#docs-sidemenu").hcSticky({top: 123,onStart:startScrolling,onStop:stopScrolling});
	
	function makeSidebarSticky(){
		var offset = window.pageYOffset;
		var bottom = parseInt($(document).scrollTop())+parseInt($(window).height());
		var docHeight = $(document).outerHeight();
		var sidebarBottomOffset = $('.sidebar').offset().top + $('.sidebar').height();
		var est = $(window).height() - menuHeight-123;
		
		$("#docs-sidemenu").css({"left": $('.sidebar').offset().left});
		if(offset>=75){
			if(bottom + ($(document).outerHeight() - sidebarBottomOffset ) > docHeight && menuHeight>$(window).height()){
				stickBottom();
			} else if(bottom + ($(document).outerHeight() - sidebarBottomOffset - est ) > docHeight && menuHeight<$(window).height()){
				stickBottom();
			} else {
				startScrolling();
			}
		} else {
			stopScrolling();
		}		
	}
	
	$(window).scroll(makeSidebarSticky).resize(makeSidebarSticky).load(makeSidebarSticky);
	
	

	$('#docs-sidemenu').onePageNav({
		currentClass: 'current',
		changeHash: false,
		scrollSpeed: 100,
		scrollOffset: 127,
		scrollThreshold: 0.05
	});
	
	if(window.location.hash) {
	  var hash = window.location.hash.substring(1);
	  $(window).load(function(){
	  	$('html,body').animate({scrollTop: $( '#'+hash ).offset().top - 127},500);
	  });
	}
	//$(window).load(function(){ $('#docs-cont .sidebar').height( $('#docs-cont .main').outerHeight() ); });
	
				
});
