### 1.1 (01-09-2010)

* Methods, constructors, events: if there are several parameters in the style
  `obj.property`, only a sole `obj` will be displayed as parameter in the
  tables' first columns.  
  Before: `method( obj.param1, obj.param2, obj.param3 )`.  
  Now: `method( obj )`.

* Fixed freestanding comma issues in methods, constructors and events tables.  
  Before: `method( param1 , param2 , param3 )`.  
  Now: `method( param1, param2, param3 )`.


### 1.0.1 (31-08-2010)

* FIX: Fixed link in footer.
* ADD: Added note re IE8 to `README.markdown`.
