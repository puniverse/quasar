# YUIDoc Theme "Dana"

I like [YUIDoc](http://developer.yahoo.com/yui/yuidoc/).  I don't like its
default theme.  Since I couldn't find any other themes on them internets, I
wrote my own, named "Dana".

Since I normally use YUIDoc to document either pure Javascript or jQuery code,
I didn't keep any of the old YUI code; I've ditched pretty much everything and
started from scratch.

Why "Dana"?  Just following the lead of Evan Weaver ([RDoc theme
"Allison"](http://github.com/fauna/allison)) and Mislav ([RDoc theme
"Hanna"](http://github.com/mislav/hanna)), is all.  Also it's the name of my
wife.  Ah, romance.  :)

This is a work in progress. It's reasonably stable and working for me so far.
YMMV. If you encounter errors, please [create a
ticket](http://github.com/carlo/yuidoc-theme-dana/issues).


## Example

Here's the original YUI documentation in its [original
look](http://developer.yahoo.com/yui/docs/index.html).

And here is the very same documentation sporting the new [Dana
theme](http://zottmann.org/yuidoc-theme-dana-example/index.html).

I hope you find the latter more pleasing. :) Click around a bit; check some of
the class documentations for a more in-depth comparison; play with the
filters; feel the luxurious yet cheap plastic underneath.


## Built-in Goodies

I've taken the liberty of implementing an (IMHO) better display of object-type parameters: if several parameters are passed in as properties on a single object, only that object will be displayed as a parameter in the tables' first columns.  As an example, let's say you're documenting three parameters your method is expecting:

    * …
    * @param  obj.param1 {String} A string!
    * @param  obj.param2 {Number} A number!
    * @param  obj.param3 {Boolean} A boolean, surprisingly!
    * …

Here's how these would be displayed in the generated documentation:

Original YUIDoc theme: `method( obj.param1, obj.param2, obj.param3 )`  
Dana theme: `method( obj )`

For a real-life example, see the docs for [YAHOO.widget.DataTable's events](http://zottmann.org/yuidoc-theme-dana-example/YAHOO.widget.DataTable.html#event_cellClickEvent).

This behaviour was added in the templates; I am aware that this is hardly the best place for putting new functionality, but I didn't feel like forking YUIDoc, so have mercy.  ;)


## Installation / Usage

I assume you've got YUIDoc up and running at this point. Just
[download](http://github.com/carlo/yuidoc-theme-dana/downloads) this here
theme, unpack it, and point `yuidoc.py` to it using the script's
`-t/--template` option.

Tested in Safari 5 (OSX), FF3.6 (OSX), IE8 (WinXP).


## Requirements

[YUIDoc](http://developer.yahoo.com/yui/yuidoc/) **>v1.0.0b1**.  The theme is
using template variables introduced in one of the later builds.  It's working
fine with [build yuidoc-50](http://github.com/yui/yuidoc/downloads), so either
get that one or grab the current YUIDoc master branch.


## Acknowledgements

My thanks go to the [YUIDoc](http://developer.yahoo.com/yui/yuidoc/) guys,
obviously. The original templates might be a bit <del>shitty</del> unorthodox
in their free-spirited way of indention and cleanliness, but the overall
system is sound.  ;)

Also, thanks to my good friend [Mike West](http://mikewest.org/), web dev
extraordinaire, who [forked the original
YUIDoc](http://github.com/mikewest/yuidoc) and added Markdown support so I
didn't have to. Big ups, playa!

This software uses...

* the [Fluid 960 Grid System](http://960.gs/) by Nathan Smith,
  [960.gs](http://960.gs/), dual-licensed under MIT & GNU GPL.
* [jQuery](http://jquery.com/), dual-licensed under MIT & GNU GPL.
* [jQuery-cookie](http://stilbuero.de/jquery/cookie/), by Klaus Hartl,
  dual-licensed under MIT & GNU GPL.


## Known issues

Code blocks (i.e. `<pre/>` tags) sometimes cause some minor layout upfuckery
in the tables.


## Author

Carlo Zottmann, [municode.de](http://municode.de/), carlo@municode.de.  Nice
to meet you.


## License

Dual-license, MIT & GNU GPL v2.

