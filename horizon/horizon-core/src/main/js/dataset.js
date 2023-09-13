/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

/**@file Classes and objects to help control user data in HTML pages 
 */

/** value format for numbers */
const numberFormat = {
	/**Parses the value for a number
	 * @param {(string|number)} value value to parse
	 * @returns {number} number parsed from the value
	 */
	parse(value) {
		if (!value) return 0;

		switch (typeof(value)) {
		case "number": return value;
		case "string":
			let num = Number(value.replace(/[\s,]/g, ""));
			if (!isNaN(num))
				return num;
		default: return ValueFormat.InvalidValue;
		}
	},

	/**Formats the value 
	 * @param {number} value value to format
	 * @returns {string} formatted value
	 */
	format(value) {
		let num = numberFormat.parse(value);
		return num != ValueFormat.InvalidValue ? Number(num).toLocaleString() : ValueFormat.InvalidValue;
	}
};

/** value format for dates */
const dateFormat = {
	/**Formats the value 
	 * @param {(number|Date)} value value to format
	 * @returns {string} formatted value
	 */
	format(value) {
		let date = value instanceof Date ? value : "number" == typeof(value) ? new Date(value) : null;
		if (!date) return "";
		
		let	year = date.getFullYear(),
			month = date.getMonth() + 1,
			day = date.getDate();
		if (month < 10)
			month = "0" + month;
		if (day < 10)
			day = "0" + day;
		return year + "-" + month + "-" + day;
	}
};

/** value format for datetimes */
const datetimeFormat = {
	/**Formats the value 
	 * @param {(number|Date)} value value to format
	 * @returns {string} formatted value
	 */
	format(value) {
		let date = value instanceof Date ? value : "number" == typeof(value) ? new Date(value) : null;
		return dateFormat.format(date) + " " + date.toLocaleTimeString();
	}
};

/**Manages value formats.<br />
 * A value format is an object that has functions
 * <ul>	<li><code>parse(arg)</code> that parses the argument for a value</li>
 *		<li><code>format(arg)</code> that formats the argument to a string</li>
 * </ul>
 * And each value format is associatd with a key as follows.
 * <pre><code>let valueFormats = new ValueFormat({
 *   key0: numberFormat,
 *   key1: dateFormat,
 *   key2: {
 *     format(value) {...},	
 *     parse(value) {...}	
 *   }
 * })</code></pre>
 */
class ValueFormat {
	_formats;
	_exprs;

	/**Creates a new ValueFormat.
	 * @param {Object} formats an object whose property names are keys and property values are value formats associated with the keys
	 */
	constructor(formats) {
		this._formats = formats || {};
		this._exprs = {};
	}

	/**Returns a parser associated with the key.
	 * @param {string} key key associated with a value format
	 * @returns {function}
	 * <ul>	<li>parser associated with the key</li>
	 *		<li>if not found for the key, default parser</li>
	 * </ul>
	 */	
	parser(key) {
		let parser = this._formats[key];
		return parser && parser.parse ? parser.parse : ValueFormat.Default.parse;
	}
	
	/**Returns a formatter associated with the key.
	 * @param {string} key key associated with a value format
	 * @returns {function}
	 * <ul>	<li>formatter associated with the key</li>
	 *		<li>if not found for the key, default formatter</li>
	 * </ul>
	 */	
	formatter(key) {
		let formatter = this._formats[key];
		return formatter && formatter.format ? formatter.format : ValueFormat.Default.format;
	}
	
	regexp(key) {
		let expr = this._exprs[key];
		if (!expr)
			this._exprs[key] = expr = new RegExp("{" + key + "}", "g");
		return expr;
	}
}

/**Default ValueFormat
 */
ValueFormat.Default = {
	parse(value) {return value;},
	format(value) {return ifEmpty(value, "");}
};
/** Represents an invalid value. */
ValueFormat.InvalidValue = "^invalid^value^";

/**Wraps a user data and traces the manipulation performed on it and consequent status. 
 */
class DataItem {
	/** index of the DataItem */
	index;
	/** user data */
	data;
	/** value formatters */
	_formats;
	/** whether the user data is selected or not */
	selected;
	/** state of the user data */
	state;
	
	/**Creates a new DataItem.
	 * @param {any} data user data
	 * @param {Object} formats value formatters of the user data's property
	 */
	constructor(data, formats) {
		this.data = data;
		this._formats = formats;
		this.selected = false;
		this.state = null;
	}

	/**selects or unselects the user data.
	 * @param {boolean} select
	 * <ul><li>false to unselect the user data</li>
	 *	   <li>true or undefined to select the user data</li>
	 * </ul>
	 * @returns {boolean} whether selection status is changed
	 * <ul><li>true if selection status is changed</li>
	 *	   <li>false otherwise</li>
	 * </ul>
	 * @example
	 * <ul><li>to select, dataItem.select() or dataItem.select(true)</li>
	 *	   <li>to unselect, dataItem.select(false)</li>
	 * </ul>
	 */
	select(select) {
		let arg = ifEmpty(select, true),
			dirty = this.selected != arg;
		this.selected = arg;
		return dirty;
	}

	/**Returns whether the DataItem is new, or the state is "added".
	 * @returns {boolean} whether the DataItem is new
	 * <ul><li>true if the DataItem is new</li>
	 *	   <li>false otherwise</li>
	 * </ul>
	 */	
	isNew() {return "added" == this.state;}

	/**Returns whether the DataItem is empty, or it has no data.
	 * @returns {boolean} whether the DataItem is empty
	 * <ul><li>true if the DataItem is empty</li>
	 *	   <li>false otherwise</li>
	 * </ul>
	 */	
	get empty() {return isEmpty(this.data);}

	/**Returns whether the user data is either created, modified, or removed.
	 * @returns {boolean}
	 * <ul><li>true if the user data is either created, modified, or removed</li>
	 *	   <li>false otherwise</li>
	 * </ul>
	 */
	get dirty() {
		return ["added", "modified", "removed"].includes(this.state);
	}

	/**Returns whether the user data is unreachable.
	 * @returns {boolean}
	 * <ul><li>true if the user data is unreachable</li>
	 *	   <li>false otherwise</li>
	 * </ul>
	 */
	get unreachable() {
		return ["removed", "ignore"].includes(this.state);
	}

	/**Toggles the selection status.
	 * @returns {boolean} current selection status
	 * <ul><li>true if the user data is selected</li>
	 *	   <li>false otherwise</li>
	 * </ul>
	 */
	toggle() {
		return this.selected = !this.selected;
	}
	
	/**Replaces the current data with the new data and resets the state. 
	 * @param {any} data new data
	 * @returns the DataItem
	 */
	replace(data) {
		this.data = data;
		this.state = null;
		return this;
	}
	
	/**Returns the values of the named properties.
	 * @param {array} names property names
	 * @returns object with the named properties
	 */
	getProperties(names) {
		let properties = names && names.length > 0 ? names : []; 
		return properties.reduce((result, prop) => {
			result[prop] = this.data[prop];
			return result;
		}, {});
	}

	/**Returns whether obj's properties are equal to properties of this.
	 * @param {object} obj an object
	 * @returns {boolean}
	 * <ul><li>true if obj's properties are equal to properties of this</li>
	 *	   <li>false otherwise</li>
	 * </ul>
	 */
	equalProperties(obj) {
		if (!obj || Object.keys(obj).length < 1) return false;
		
		for (let prop in obj) {
			if (obj[prop] != this.data[prop])
				return false;
		}
		return true;
	}

	/**Returns the formatted value of the named property of the user data.
	 * @param {string} property property name
	 * @returns {any} formatted value of the named property of the user data
	 */
	getValue(property) {
		if (!this.data) return "";
		
		let value = this.data[property];
		return this._formats.formatter(property)(value);
	}

	/**Parses and sets the value to the named property of the user data.
	 * @param {string} property property name
	 * @param {any} value value
	 * @return {any}
	 * <ul><li>parsed value</li>
	 *	   <li>ValueFormat.InvalidValue if failed to parse the value</li>
	 * </ul>
	 */
	setValue(property, value) {
		if (!this.data) return;
		
		let parsed = this._formats.parser(property)(value);
		if (ValueFormat.InvalidValue != parsed) {
			this.data[property] = parsed;
		}
		return parsed;
	}

	/**Returns a string converted from the template using the properties of the data.
	 * In the template, placeholder for the properties of the data is specified like {property name}.
	 * @param {string} template template string
	 * @param {function} formatter function of (template, dataItem) => {...; return "...";} that converts custom placeholders of the template 
	 * @returns {string} string converted from the template using the properties of the data
	 */
	inString(template, formatter) {
		let str = template;
		if (formatter) {
			str = formatter(str, this);
		}
		for (let p in this.data) {
			let regexp = this._formats.regexp(p);
			str = str.replace(regexp, this.getValue(p));
		}
		return str.replace(/{index}/gi, this.index);;
	}
}

/**Manages user data wrapped in {@link DataItem}s and offers methods to manipulate the data, tracing the state change of the data.<br />
 * In response to the state change, a Dataset fires events and calls handlers of the events whereby the state change is propagated to UI facilities.
 * <p>On creation, provide a configuration where you specify
 * <ul><li>keys of data</li>
 *     <li>formats</li>
 *     <li>event handlers</li>
 * </ul>
 * which are all optional. For example,
 * <pre><code>let example = new Dataset({
 *     keys: ["property0", "property1"],
 *     formats: {
 *         property2: numberFormat,
 *         property3: datetimeFormat
 *     },
 *     onDatasetChange: (dataset) => {...},
 *     onCurrentChange: (item) => {...},
 *     onSelectionChange: (selected) => {...}
 * });</code></pre>
 * You can also specify the event handlers on the dataset itself.
 * <pre><code>example.onDatasetChange = (dataset) => {...};
 * example.onCurrentChange = (item) => {...};
 * example.onSelectionChange = (selected) => {...};</code></pre> 
 * </p>
 * <p>You {@link Dataset#setData set data} to the Dataset like
 * <pre><code>example.setData([...]);</code></pre>
 * To {@link Dataset#addData append data} to the Dataset
 * <pre><code>example.addData([...]);</code></pre>
 * To add data that is new and dirty(see below)
 * <pre><code>example.addData([...] || {...}, {local: true});</code></pre>
 * </p>
 * <p>You {@link Dataset#getData get desired data} with 
 * <pre><code>let filter = ...;
 * let dataList = example.getData(filter);</code></pre>
 * For {@link Dataset#getItems desired DataItems},
 * <pre><code>let dataItems = example.getItems(filter);</code></pre>
 * To {@link Dataset#getInfo get a single data}
 * <pre><code>filter = ...;
 * let result = example.getInfo(filter);</code></pre>
 * {@link Dataset#getItem For a DataItem},
 * <pre><code>let item = example.getItem(filter);</code></pre>
 * </p>
 * <p>A <b>current</b> data or DataItem refers to the one that you are now seeing or working on.<br />
 * To {@link Dataset#setCurrent set a data or DataItem as current}, use the setCurrent(...) method.
 * <pre><code>example.setCurrent(filter);</code></pre>
 * To {@link Dataset#getCurrent get a current data or DataItem}, call
 * <pre><code>let current = example.getCurrent();</code></pre>
 * Or
 * <pre><code>let current = example.getCurrent("item");</code></pre>
 * </p>
 * <p>You get property values of the Dataset's data in one of the following ways:
 * <pre><code>let item = example.getItem(filter);
 * let info = item.data;
 * let property2 = info.property2;
 * </code></pre>
 * <code>item.data</code> refers to the raw data set to the Dataset.
 * <code>info.property2</code> refers to the raw value of the data's 'property2' property.
 * To get the {@link DataItem#getValue formatted value}
 * <pre><code>property2 = item.getValue("property2");</code></pre> 
 * Or {@link Dataset#getValue likewise with the Dataset}
 * <pre><code>property2 = example.getValue(filter, "property2");</code></pre> 
 * </p>
 * <p><b>Dirty</b> data refer to those that are added, modified, or removed but not yet saved to a storage.
 * </p>
 * <p>To change property values of the Dataset's data:
 * <pre><code>info.property2 = 2000000;</code></pre>
 * A DataItem can {@link DataItem#setValue parse and set the new value}
 * <pre><code>item.setValue("property2", "2,000,000");</code></pre>
 * Although effective, these go unnoticed by the Dataset, which does not fire the relevant events.<br />
 * To fix this, use the {@link Dataset#serValue Dataset's method}
 * <pre><code>example.modify(filter, (item) => {
 *     let info = item.data;
 *     info.property2 = 2000000;
 * });</code></pre>
 * Or shortly
 * <pre><code>example.setValue(filter, "property2", "2,000,000");</code></pre>
 * The data modified is dirty.
 * </p>
 * <p>To remove data from a Dataset, call
 * <pre><code>example.remove(filter);</code></pre>
 * which leaves the Dataset and removed data dirty.
 * </p>
 * <p>To {@link Dataset#select select or unselect data} for further processing, call
 * <pre><code>example.select(filter);
 * example.select(filter, false);
 * </code></pre>
 * </p>
 * <p>Use the {@link Dataset#inStrings inStrings(...)} method to convert a Dataset's data to a string representation, typically HTML fragments.</p>
 */
class Dataset {
	/**Creates a new Dataset with a configuration.
	 * @param {Object} conf configuration that specifies
	 * <ul><li>keys - property names used as keys to find data, optional</li>
	 *     <li>formats - an object of {"property": valueFormat} pairs where the "property corresponds to a property name of data and value-format to a {@link ValueFormat}</li>
	 *     <li>event handlers - functions that handles the Dataset's events of
	 *			<ul><li>{@link Dataset#onDatasetChange onDatasetChange}</li>
	 *				<li>{@link Dataset#onCurrentChange onCurrentChange}</li>
	 *				<li>{@link Dataset#onSelectionChange onSelectionChange}</li>
	 *				<li>{@link Dataset#onModify onModify}</li>
	 *				<li>{@link Dataset#onRemove onRemove}</li>
	 *				<li>{@link Dataset#onDirtyStateChange onDirtyStateChange}</li>
	 *			</ul>
	 *          optional
	 *     </li>
	 * 	   <li>trace - true to log the events of the Dataset, optional</li>
	 * </ul>
	 */
	constructor(conf) {
		this._items = [];
		this._current = null;

		this.conf = conf || {};
		this._keys = this.conf.keys || [];
		this._formats = new ValueFormat(this.conf.formats);
		this._dirty = false;

		if (!this.conf.trace)
			this.log = () => {};

		[	"onDatasetChange",
			"onCurrentChange",
			"onSelectionChange",
			"onModify",
			"onRemove",
			"onDirtyStateChange"
		].forEach(on => {
			let handler = this.conf[on]
			if (handler && "function" == typeof handler)
				this[on] = handler;
		});
	}

	/**Returns data or DataItems that matches the filter.
	 * @param {any} filter filter that tests a data or DataItem to include in the result.
	 * For filter, you provide
	 * <ul><li>index or an array of index to DataItems</li>
	 *     <li>"selected" to get selected data</li>
	 *     <li>"dirty" to get new, modified or removed data</li>
	 *     <li>object(s) of key-value pairs matching the properties of the desired data</li>
	 *     <li>function of (dataItem) => {...; return true||false;} to test a DataItem</li>
	 * </ul>
	 * Not provided, all data or dataItems are returned.
	 * @param {string} option "item" to get DataItems, optional.
	 * <ul><li>none to get data(default)</li>
	 *     <li>"item" to get DataItems</li>
	 * </ul>
	 * @returns {(array|Object)} data or DataItems that matches the filter
	 * <ul><li>if filter is "dirty", object that holds array of data or DataItems by state</li>
	 *     <li>otherwise, array of data or DataItems</li>
	 * </ul>
	 * @example
	 * //To get all data
	 * let datalist = dataset.getData();
	 * //To get all DataItems
	 * let dataItems = dataset.getData("item");
	 * 
	 * //To get data matching the DataItem's index
	 * let index = ...; // You get the index from a DataItem like dataItem.index
	 * dataList = dataset.getData(index);
	 * dataList = dataset.getData([index, index1]);
	 * //To get DataItems matching the DataItem's index
	 * dataItems = dataset.getData(index, "item");
	 * dataItems = dataset.getData([index, index1], "item");
	 * 
	 * //To get selected data
	 * datalist = dataset.getData("selected");
	 * //To get selected DataItems
	 * dataItems = dataset.getData("selected", "item");
	 * 
	 * //To get dirty data that are added, modified, or removed
	 * let dirtyData = dataset.getData("dirty");
	 * //To get dirty DataItems that are added, modified, or removed
	 * let dirtyItems = dataset.getData("dirty", "item");
	 * //The returned dirtyData or dirtyItems are like:
	 * //{empty:true||false, added:[...], modified:[...], removed:[...]}
	 * 
	 * //To get data of particular properties
	 * datalist = dataset.getData({prop0: "value0", prop1: "value1"});
	 * datalist = dataset.getData([
	 *     {prop0: "value0", prop1: "value1"},
	 *     {prop2: "value2", prop3: "value3"}
	 * ]);
	 * //To get DataItems of particular properties
	 * dataItems = dataset.getData({prop0: "value0", prop1: "value1"}, "item");
	 * dataItems = dataset.getData([
	 *     {prop0: "value0", prop1: "value1"},
	 *     {prop2: "value2", prop3: "value3"}
	 * ], "item");
	 * 
	 * //To get data that matches a custom filter.
	 * dataList = dataset.getData((item) => {...; return true||false;});
	 * //To get dataItems that matches a custom filter.
	 * dataItems = dataset.getData((item) => {...; return true||false;}, "item");
	 */
	getData(filter, option) {
		filter = filter || "all";
		if (filter == "item") {
			filter = "all";
			option = "item";
		}
		let filterType = Array.isArray(filter) ? "array" : typeof filter,
			test = null;

		if ("string" == filterType) {
			switch (filter) {
				case "selected": test = item => !item.unreachable && item.selected; break;
				case "dirty": test = item => item.dirty; break;
				case "all": test = item => !item.unreachable; break;
				default: test = item => !item.unreachable && filter == item.index; break;
			}
		} else if ("object" == filterType) {
			test = item => item.equalProperties(filter)
		} else if ("array" == filterType) {
			let etype = typeof filter[0];
			test = item => {
				if (item.unreachable) return false;
				
				switch (etype) {
					case "string": return filter.includes(item.index);
					case "object": return filter.reduce((match, properties) => match || item.equalProperties(properties), false);
					default: return false;
				}
			};
		} else {
			if ("function" != filterType)
				throw "filter must be a predicate function";
			test = filter;
		}

		let found = (this._items || []).filter(test);
		if (found.length < 1)
			return "dirty" != filter ? found: {empty:true, added:[], modified:[], removed:[]};

		if ("dirty" != filter)
			return "item" == option ? found : found.map(item => item.data);

		return found.reduce(
			(result, item) => {
				result[item.state].push("item" == option ? item : item.data);
				return result; 
			},
			{empty:false, added:[], modified:[], removed:[]}
		);
	}

	/**Returns DataItems that matches the filter.
	 * Convenience method for {@link Dataset#getData getData(filter, "item")}
	 * @param {any} filter see {@link Dataset#getData getData}
	 * @returns {array} DataItems
	 */
	getItems(filter) {
		return this.getData(filter, "item");
	}

	/**Returns a DataItem that matches the filter.
	 * @param {any} filter {@link Dataset#getData filter} that matches to a single item
	 * @param {boolean} strict true to throw an exception if no item is found with the filter
	 * @returns {Object} a DataItem
	 */
	getItem(filter, strict) {
		let found = this.getItems(filter || "emptyFilter"),
			length = !found ? 0 : found.length;
		if (strict) {
			if (length < 1)
				throw "Item not found: " + filter;
			if (length > 1)
				throw "Multiple items found: " + filter;
		}
		return length > 0 ? found[0] : null;
	}

	/**Returns a data that matches the filter.
	 * @param {any} filter {@link Dataset#getData filter} that matches to a single data
	 * @param {boolean} strict true to throw an exception if no data is found with the filter
	 * @returns {Object} a DataItem
	 */
	getInfo(filter, strict) {
		let found = this.getItem(filter, strict);
		return found ? found.data : null;
	}

	/**Sets data to the Dataset and fires the events of
	 * <ul><li>{@link Dataset#onDatasetChange onDatasetChange}</li>
	 *     <li>{@link Dataset#onCurrentChange onCurrentChange}</li>
	 *     <li>{@link Dataset#onSelectionChange onSelectionChange}</li>
	 *     <li>{@link Dataset#onDirtyStateChange onDirtyStateChange}</li>
	 * </ul>
	 * @param {array} obj  array of data
	 * @param {Object} option {stateful:true} to keep the previous state, if possible
	 * @returns {Dataset} this Dataset
	 */
	setData(obj, option) {
		option = option || {};
		let state = option.stateful ? this.getState(true) : null;
		this._current = null;

		let empty = !obj,
			items = !empty ? this._getDataItems(obj, option) : [];
		this._items = items;

		this.setState(!empty? state : null);
		this.dirty = false;
		return this;
	}

	_getDataItems(obj, option) {
		let _items = (Array.isArray(obj) ? obj : [obj]).map(e => {
				let item = new DataItem(e, this._formats);
				if (option && option.local)
					item.state = "added";
				return item;
			}),
			_prefix = "ndx-" + new Date().getTime();
			
		_items.forEach((item, index) => {
			item.index = _prefix + index;
		});

		return _items;
	}

	/**Appends data to the Dataset and fires the events of
	 * <ul><li>{@link Dataset#onDatasetChange onDatasetChange}</li>
	 *     <li>{@link Dataset#onCurrentChange onCurrentChange}</li>
	 *     <li>{@link Dataset#onSelectionChange onSelectionChange}</li>
	 *     <li>{@link Dataset#onDirtyStateChange onDirtyStateChange}</li>
	 * </ul>
	 * @param {array} obj array of data
	 * @param {Object} option {local:true} to add the data as new and dirty
	 * @returns {Dataset} this Dataset
	 */
	addData(obj, option) {
		if (!obj) return this;
		if (this.empty)
			return this.setData(obj, option);

		let state = this.getState(),
			items = this._getDataItems(obj, option);

		this._items = this._items.concat(items);
		state.currentIndex = items[0].index;
		this.setState(state);
		if (option && option.local)
			this.dirty = true;

		return this;
	}

	/**Returns the count of reachable DataItems.
	 * @returns {number} count of reachable DataItems
	 */
	get length(){return this.getItems().length;};

	/**Returns whether the Dataset is empty.
	 * @returns {boolean} whether the Dataset is empty
	 */
	get empty(){return this.length < 1;}

	/**Returns the current data or DataItem.
	 * @param {string} option "item" to get a DataItem
	 * @returns {any} data or DataItem
	 */
	getCurrent(option) {
		let current = this._current;
		if (!current || current.unreachable)
			return null;
		return "item" == option ? current : current.data;
	}

	/**Sets the DataItem that matches the filter as current
	 * and fires the {@link Dataset#onCurrentChange onCurrentChange} event.
	 * @param {any} filter see {@link Dataset#getData}
	 * @returns {Dataset} this Dataset
	 */
	setCurrent(filter, fire) {
		let item = this.getItem(filter, true),
			current = this.getCurrent("item"),
			diff = current !== item;
		this._current = item;

		if (diff || fire)
			this.onCurrentChange(item);
		return this;
	}

	/**Scrolls up or down depending on the offset and fires the {@link Dataset#onCurrentChange onCurrentChange} event.
	 * If the new position surpasses either top or bottom of the Dataset, the first or last DataItem is set current.
	 * @param {number} offset
	 * <ul><li>negative integer to scroll up</li>
	 *     <li>positive integer to scroll down</li>
	 * </ul>
	 * @returns {Dataset} this Dataset
	 */
	scroll(offset) {
		if (!offset)
			return this;
		let items = this.getItems(),
			length = items.length;
		if (length < 2)
			return this;
		
		let current = items.indexOf(this.getCurrent("item")),
			pos = current + offset;
		pos = offset < 0 ? Math.max(0, pos) : Math.min(length - 1, pos);
		let item = items[pos];
		if (!item)
			return this;

		this.onCurrentChange(this._current = item);
		
		return this;
	}

	/**Returns the current state of the Dataset.
	 * The state is an object of, by default, index to current and selected DataItems.
	 * <pre><code>{
	 *     currentIndex: "ndx-..0",
	 *     selectedIndex: ["ndx-..1", "ndx-..2", ...]
	 * }</code></pre>
	 * With 'asKeys' of true and the 'keys' configured on the Dataset construction,
	 * keys to current and selected DataItems are provided.
	 * <pre><code>{
	 *     asKeys: true,
	 *     currentKey: {"kep-prop0": "key-value0", "key-prop1": "key-value1"},
	 *     selectedKeys: [
	 *         {"kep-prop0": "key-value0-0", "key-prop1": "key-value0-1"},
	 *         {"kep-prop0": "key-value1-0", "key-prop1": "key-value1-1"},
	 *         ...
	 *     ]
	 * }</code></pre>
	 * @param {boolean} asKeys
	 * <ul><li>none or false to get the index to current and selected DataItems</li>
	 * 	   <li>true to get the keys of DataItems</li>
	 * </ul>
	 * @returns {Object} current state
	 */
	getState(asKeys) {
		if (this.empty)
			return !asKeys ?
				{currentIndex: null, selectedIndex: []} :
				{asKeys: true, currentKey: null, selectedKeys: []};
			
		let current = this._current,
			selected = this.getItems("selected"),
			keys = this._keys;
			
		if (!asKeys || keys.length < 1)
			return {
				currentIndex: current ?  current.index : null,
				selectedIndex: selected.map(item => item.index)
			};
		
		return {
			asKeys: true,
			currentKey: current.getProperties(keys),
			selectedKeys: selected.map(item => item.getProperties(keys))
		};
	}

	/**Sets the {@link Dataset#getState state} and fires the events of   
	 * <ul><li>{@link Dataset#onDatasetChange onDatasetChange}</li>
	 *     <li>{@link Dataset#onCurrentChange onCurrentChange}</li>
	 *     <li>{@link Dataset#onSelectionChange onSelectionChange}</li>
	 * </ul>
	 * @param {Object} state a state. Not provided, the current state is used. 
	 * @returns {Dataset} this Dataset
	 */
	setState(state) {
		this.onDatasetChange(this);
		if (this.empty) {
			this.onCurrentChange(new DataItem());
			this.onSelectionChange([]);
		} else {
			state = state || this.getState();
			if (state.asKeys) {
				let found = this.getItem(state.currentKey),
					selected = this.getItems(state.selectedKeys);
				state.currentIndex = found ? found.index : null;
				state.selectedIndex = selected.map(item => item.index);
			}
			let current = this.getItem(state.currentIndex) || this.getItems()[0];
			this.setCurrent(current.index, true);
			this.select(state.selectedIndex || [], true, true);
		}
		return this;
	}

	/**Returns whether the Dataset has dirty DataItems.
	 * @returns {boolean}
	 * <ul><li>true if the Dataset has dirty DataItems</li>
	 *     <li>false otherwise</li>
	 * </ul>
	 */
	get dirty() {
		this.dirty = !this.getItems("dirty").empty;
		return this._dirty;
	}

	/**Sets whether the Dataset has dirty DataItems
	 * and fires the {@link Dataset#onDirtyStateChange onDirtyStateChange} event.
	 * @param {boolean} value
	 * <ul><li>true if the Dataset has dirty DataItems</li>
	 *     <li>false otherwise</li>
	 * </ul>
	 */
	set dirty(value) {
		if (this._dirty == value) return;

		this.onDirtyStateChange(this._dirty = value);
	}

	/**Selects or unselects DataItems matching the filter
	 * and fires the {@link Dataset#onSelectionChange onSelectionChange} event.
	 * @param {...*} args
	 * To select DataItems matching the {@link Dataset#getData filter}
	 * <pre><code>dataset.select(filter)</code></pre>
	 * To unselect DataItems matching the filter
	 * <pre><code>dataset.select(filter, false)</code></pre>
	 * To select the current DataItem
	 * <pre><code>dataset.select()</code></pre>
	 * To unselect the current DataItem
	 * <pre><code>dataset.select(false)</code></pre>
	 * @returns {boolean} whether selection status has changed
	 * <ul><li>true if selection status has changed</li>
	 *     <li>false otherwise</li>
	 * </ul>
	 */
	select(...args) {
		let arg0 = ifEmpty(args[0], true),
			arg1 = ifEmpty(args[1], true),
			fire = false,
			filter = null,
			selected = null;
		if ("boolean" == typeof arg0) {
			filter = item => true;
			selected = arg0;
			fire = arg1;
		} else {
			filter = arg0;
			selected = arg1;
			fire = args[2];
		}
		let dirty = this.getItems(filter)
			.reduce(((dirty, item) => item.select(selected) || dirty), false);
		if (dirty || fire) {
			this.onSelectionChange(this.getItems("selected"));
		}
		return dirty;
	}

	/**Toggles selection of the DataItems matching the filter.
	 * @param {any} filter see {@link Dataset#getData}
	 * @returns {boolean} whether selection status has changed
	 * <ul><li>true if selection status has changed</li>
	 *     <li>false otherwise</li>
	 * </ul>
	 */
	toggle(filter) {
		let status = this.getItems(filter).reduce(((dirty, item) => item.toggle() || dirty), false);
		this.onSelectionChange(this.getItems("selected"));
		return status;
	}

	/**Modifies a DataItem matching the filter and fires the {@link Dataset#onModify onModify} event.
	 * @param {any} filter {@link Dataset#getData filter} to get a DataItem
	 * @param {function} modifier function (dataIetm) => {...} that modifies a DataItem's data.
	 * The function returns ValueFormat.InvalidValue to reject the modification.
	 * @returns {Dataset} this Dataset
	 */
	modify(filter, modifier) {
		if (!modifier) return this;

		let item = this.getItem(filter, true),
			data = item.data,
			prev = Object.assign({}, data),
			modifiedProps = (prev, data) => {
				let changed = [];
				for (let prop in data) {
					let oldVal = prev[prop],
						newVal = data[prop];
					if (oldVal != newVal)
						changed.push(prop);
				}
				return changed;
			};
			
		let current = data == this.getCurrent(),
			revert = modifier(item) == ValueFormat.InvalidValue,
			changed = modifiedProps(prev, data);
		
		if (changed.length > 0) {
			if (!item.state)
				item.state = "modified";
			this.onModify(changed, item, current);
//			this.setState();
			this.dirty = true;
		} else if (revert) {
			this.onModify(Object.getOwnPropertyNames(data), item, current);
		}

		return this;
	}

	/**Removes the DataItems matching the filter and fires the events of
	 * <ul><li>{@link Dataset#onRemove onRemove}</li>
	 *     <li>{@link Dataset#onDatasetChange onDatasetChange}</li>
	 *     <li>{@link Dataset#onCurrentChange onCurrentChange}</li>
	 *     <li>{@link Dataset#onSelectionChange onSelectionChange}</li>
	 * </ul>
	 * @param filter see {@link Dataset#getData}
	 * @returns {Dataset} this Dataset
	 */
	remove(filter) {
		if (!filter || this.empty) return this;

		let state = this.getState(),
			current = this.getCurrent("item"),
			currentPos = this.getItems().indexOf(current),
			removed = this.getItems(filter)
				.map(item => {
					item.state = "added" == item.state ? "ignore" : "removed";
					return item;
				});
		if (removed.length < 1) return this;
		
		if (removed.includes(current)) {
			let rest = this.getItems(),
				newPos = Math.min(currentPos, rest.length - 1);
			state.currentIndex = newPos < 0 ? null : rest[newPos].index;
		}
		
		this.onRemove(removed);
		this.setState(state);
		this.dirty = true;
		
		return this;
	}

	/**Empties the Dataset and fires the events of
	 * <ul><li>{@link Dataset#onDatasetChange onDatasetChange}</li>
	 *     <li>{@link Dataset#onCurrentChange onCurrentChange}</li>
	 *     <li>{@link Dataset#onSelectionChange onSelectionChange}</li>
	 * </ul>
	 * @returns {Dataset} this Dataset 
	 */
	clear() {
		this.setData(null);
		return this;
	}

	/**Returns an array of strings converted from the template using the properties of the Dataset's data.
	 * In the template, placeholder for the properties of the data is specified like {property name}.
	 * @param {string} template template string
	 * @param {function} formatter function of (template, dataItem) => {...; return "...";} that converts custom placeholders of the template 
	 * @returns {array} array of strings converted from the template using the properties of the Dataset
	 */
	inStrings(template, formatter) {
		return this.getItems()
			.map(item => item.inString(template, formatter));
	}

	/**Returns the formatted value of the named property of a DataItem's data.
	 * @param {...*} args
	 * To get a property value of a DataItem matching a {@link Dataset#getData filter}
	 * <pre><code>let val = dataset.getValue(filter, "propertyName");</code></pre>
	 * To get a property value of the current DataItem
	 * <pre><code>let val = dataset.getValue("propertyName");</code></pre>
	 * @returns {any} formatted value of the named property of a DataItem's data
	 */
	getValue(...args) {
		let item = null,
			property = null;
		switch (args.length) {
		case 1:
			item = this.getCurrent("item");
			property = args[0];
			break;
		case 2:
			item = this.getItem(args[0]);
			property = args[1];
			break;
		default: return null;
		}
		return item ? item.getValue(property) : "";
	}

	/**Parses and sets a value to the named property of a DataItem's data,
	 * which fires the {@link Dataset#onModify onModify} event.
	 * @param {...*} args
	 * To set a property value to a DataItem matching a {@link Dataset#getData filter}
	 * <pre><code>dataset.setValue(filter, "propertyName", "propertyValue");</code></pre>
	 * To set a property value to the current DataItem
	 * <pre><code>dataset.setValue("propertyName", "propertyValue");</code></pre>
	 * @returns {Dataset} this Dataset
	 */
	setValue(...args) {
		let filter = null,
			property = null, 
			value = null;
		switch (args.length) {
		case 2:
			let item = this.getCurrent("item");
			if (!item) {
				this.log("WARNING", "Current item is missing");
				return this;
			}
			filter = item.index;
			property = args[0];
			value = args[1];
			break;
		case 3:
			filter = args[0];
			property = args[1];
			value = args[2];
			break;
		default: return this;
		}
		return this.modify(filter, function(item){
			return item.setValue(property, value);
		});
	}

	log(...args) {
		console.log.apply(console, args);
	}

	/**Handler called back on the dataset change event.
	 * @param {Dataset} dataset this Dataset
	 */
	onDatasetChange(dataset) {this.log("Dataset changed", dataset);}

	/**Handler called back on the current change event.
	 * @param {DataItem} item current DataItem
	 */
	onCurrentChange(item) {this.log("Current changed", item);}

	/**Handler called back on the selection change event
	 * @param {array} selected selected DataItems
	 */
	onSelectionChange(selected) {this.log("Selection changed", selected);}

	/**Handler called back on the modify event.
	 * @param {array} props names of modified properties
	 * @param {DataItem} modified modified DataItem
	 * @param {boolean} current whether the current DataIetm is modified
	 */
	onModify(props, modified, current) {this.log("Data modified", props, modified, current ? "current" : "");}

	/**Handler called back on the remove event.
	 * @param {array} removed removed DataItems
	 */
	onRemove(removed) {this.log("Data removed", removed);}

	/**Handler called back on the dirty state change event.
	 * @param {boolean} dirty dirty state
	 */
	onDirtyStateChange(dirty) {this.log("Dirty state changed", dirty);}
}