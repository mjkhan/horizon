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
	 * @param {number} value value to format
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
	 * @param {number} value value to format
	 * @returns {string} formatted value
	 */
	format(value) {
		let date = value instanceof Date ? value : "number" == typeof(value) ? new Date(value) : null;
		return dateFormat.format(date) + " " + date.toLocaleTimeString();
	}
};

/**Manages value formats.
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
	 * @param {object} formats an object whose property names are keys and property values are value formats associated with the keys
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
	 * @param {object} formats value formatters of the user data's property
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

	/**Returns the formatted value of the named property of the user data.
	 * @param {string} property property name
	 * @returns {any} formatted value of the named property of the user data
	 */
	getValue(property) {
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
		let parsed = this._formats.parser(property)(value);
		if (ValueFormat.InvalidValue != parsed)
			this.data[property] = parsed;
		return parsed;
	}

	/**Returns a string converted from the template using the property values of the user data.
	 * In the template, placeholder for the properties of the user data is specified like {property name}.
	 * @param {string} template template string
	 * @returns {string} string converted from the template using the property values of the user data
	 */
	inString(template) {
		let str = template;
		for (let p in this.data) {
			let regexp = this._formats.regexp(p);
			str = str.replace(regexp, this.getValue(p));
		}
		return str;
	}
}

/**Manages user data wrapped in {@link DataItem}s, tracing the state after manipulation performed on them.
 * <p>For a Dataset to work properly, it needs a keymapper to identify user data.
 * And you specify it in a Dataset's configuration.  
 * <pre><code>let dataset = new Dataset({
 *   keymapper: function(info) {return info.keyProperty;},
 *   ...
 * });</code></pre>
 * </p>
 * <p>To help access values of user data, a Dataset offers methods
 * <ul><li>{@link Dataset#getValue}</li>
 *     <li>{@link Dataset#setValue}</li>
 * </ul>
 * Using value formats configured in the Dataset, the methods return formatted value and sets parsed value.
 * <pre><code>let dataset = new Dataset({
 *   formats: {
 *     numericProperty: {@link numberFormat},
 *     customProperty: {
 *       format(value) {...},
 *       parse(value) {...},
 *     }
 *   },
 *   ...
 * });</code></pre>
 * </p>
 * <p>Working with user data that a Dataset holds, you change the Dataset's state.
 * Depending on the type of change, the Dataset calls back approriate methods.
 * By default, the methods log the content of the change.
 * </p>
 * <p>To override the behavior of the callback methods,
 * define a function with the same signature as the method to override
 * and assign it to the Dataset's method.
 * <pre><code>let myFunc = obj => {...};
 * let dataset = new Dataset({...});
 * dataset.onDatasetChange = myFunc;</code></pre>
 * You can make it simple like this:
 * <pre><code>let dataset = new Dataset({...});
 * dataset.onDatasetChange = obj => {};</code></pre>
 * </p>
 * <p>Or you specify an overriding function in the configuration used to create a Dataset.
 * <pre><code>let dataset = new Dataset({
 *     ...
 *     onDatasetChange:obj => {...}
 * });</code></pre>
 * </p>
 */
class Dataset {
	_items;
	_byKeys;
	_current;
	
	/**Dataset configuration
	 */
	conf;
	_formats;
	
	/**Creates a new Dataset with a configuration.
	 * The configuration is an object with which you specify
	 * <ul>	<li>keymapper - function that returns a key of a user data. Used to identify user data in the Dataset. Mandatory.</li>
	 * 		<li>dataGetter - function that returns an array of user data from an object. Required if the user data are extracted from an object</li>
	 * 		<li>formats - an object of key-value format pairs where the key corresponds to a property of a user data</li>
	 * 		<li>functions called back on a Dataset's events of
	 *			<ul><li>{@link Dataset#onDatasetChange onDatasetChange}</li>
	 *				<li>{@link Dataset#onCurrentChange onCurrentChange}</li>
	 *				<li>{@link Dataset#onSelectionChange onSelectionChange}</li>
	 *				<li>{@link Dataset#onAppend onAppend}</li>
	 *				<li>{@link Dataset#onModify onModify}</li>
	 *				<li>{@link Dataset#onReplace onReplace}</li>
	 *				<li>{@link Dataset#onRemove onRemove}</li>
	 *				<li>{@link Dataset#onErase onErase}</li>
	 *				<li>{@link Dataset#onDirtiesChange onDirtiesChange}</li>
	 *			</ul> 
	 *		</li>
	 * 		<li>trace - true to enable message logging</li>
	 * </ul> 
	 * @param conf {object} configuration
	 */
	constructor(conf) {
		this._items = [];
		this._byKeys = {};
		this._current = null;
		
		this.conf = notEmpty(conf, "conf is required but missing");
		notEmpty(conf.keymapper, "keymapper is required but missing");
		this._formats = new ValueFormat(conf.formats);
		
		if (!conf.trace)
			this.log = () => {};

		[	"onDatasetChange",
			"onCurrentChange",
			"onSelectionChange",
			"onAppend",
			"onModify",
			"onReplace",
			"onRemove",
			"onErase",
			"onDirtiesChange"
		].forEach(on => {
			let handler = conf[on]
			if (handler && "function" == typeof handler)
				this[on] = handler;
		});
	}

	/**Logs a message to the console.
	 * @param args arguments to log
	 */
	log(...args) {
		console.log.apply(console, args);
	}

	/**Returns the key of a user data.
	 * @param {any|DataItem} info user data or {@link DataItem dataItem} of a user data
	 * @returns {string} key of a user data
	 */
	getKey(info) {
		let data = info ? info.data || info : null;
		return data ? this.conf.keymapper(data) : null;
	}

	/**Returns keys of the Dataset's user data.
	 * @param {string} status option regarding the Dataset's user data
	 * <ul>	<li>none to get keys of all user data</li>
	 *		<li>"selected" to get keys of selected user data</li>
	 *		<li>"added" to get keys of added user data</li>
	 *		<li>"modified" to get keys of modified user data</li>
	 *		<li>"removed" to get keys of removed user data</li>
	 *		<li>"dirty" to get keys of user data that are {@link Dataset#dirty dirty}</li>
	 * </ul>
	 * @returns {array} keys of the Dataset's user data.
	 * @example
	 * //Other than "dirty" status, keys of user data are returned in an array.
	 * let array = dataset.getKeys();
	 * array = dataset.getKeys("selected");
	 * //With "dirty" status, keys of user data are returned in an object of status-array pairs.
	 * let dirties = dataset.getKeys("dirty");
	 * let added = dirties.added;
	 * let modified = dirties.modified;
	 * let removed = dirties.removed;
	 */	
	getKeys(status){
		let dataset = this.getDataset(status);
		if ("dirty" != status)
			return dataset.map(e => this.getKey(e));
		
		let result = {};
		for (let prop in dataset) {
			result[prop] = dataset[prop].map(e => this.getKey(e));
		}
		return result;
	}

	/**Returns user data or dataItem associated with the key.
	 * @param {string} key key to a user data
	 * @param {string} option "item" to get the user data in a dataItem.
	 * @returns {any|DataItem} user data or dataItem associated with the key
	 * @example
	 * //To get user data associated with a key
	 * let data = dataset.getData("key-0");
	 * //To get the user data in a dataItem
	 * let dataItem = dataset.getData("key-0", "item");
	 */
	getData(key, option) {
		let item = this._byKeys["key-" + key];
		if (!item || item.unreachable)
			return null;
		return "item" == option ? item : item.data;
	}

	/**Sets user data to the Dataset.
	 * To get user data from an object, the dataGetter configured is called.
	 * After user data is set, the methods
	 * <ul>	<li>{@link Dataset#onDatasetChange}</li>
	 *		<li>{@link Dataset#onCurrentChange}</li>
	 *		<li>{@link Dataset#onSelectionChange}</li>
	 *		<li>{@link Dataset#onDirtiesChange}</li>
	 * </ul>
	 * are called back.
	 * @param {array|object} obj user data or an object that has user data
	 * @returns {Dataset} the Dataset
	 */
	setData(obj) {
		this._byKeys = {};
		this._current = null;
		
		obj = obj || {};
		let array = Array.isArray(obj) ? obj : this.conf.dataGetter(obj) || [];
		if (!Array.isArray(array))
			throw new Error("The data must be an array");
			
		this._items = array.map(e => new DataItem(e, this._formats));
		this._items.forEach(item => {
			let key = "key-" + this.getKey(item.data);
			this._byKeys[key] = item;
		});
		
		this.onDatasetChange(obj);
		this.setState(!Array.isArray(obj) ? obj.state : null);
		this.onDirtiesChange(this.dirty);
		
		return this;
	}

	/**Clears the Dataset's user data.
	 * @returns {Dataset} the Dataset  
	 */
	clear() {
		this.setData(null);
		return this;
	}

	/**Returns the length or count of the Dataset's user data.
	 * @returns {number} length or count of the Dataset's user data
	 */
	get length(){return this.getDataset("item").length;};

	/**Returns whether the Dataset has no user data or not.
	 * @returns {boolean}
	 * <ul>	<li>true if the Dataset has no user data</li>
	 *		<li>false if the Dataset has any user data</li>
	 * </ul>
	 */
	get empty(){
		return this.length < 1;
	}

	/**Returns the current user data or dataItem.
	 * @param {string} option "item" to get the current dataItem
	 * @returns {any|DataItem} current user data or dataItem
	 * @example
	 * //To get the current user data
	 * let current = dataset.getCurrent();
	 * //To get the current user data in a dataItem
	 * let current = dataset.getCurrent("item");
	 */
	getCurrent(option){
		let current = this._current;
		if (!current || current.unreachable)
			return null;
		return "item" == option ? current : current.data;
	}

	/**Sets the user data as current that is associated with the key.
	 * @param {string} key key to a user data
	 * After the data is set, the method 
	 * <ul>	<li>{@link Dataset#onCurrentChange}</li>
	 * </ul>
	 * is called back.
	 */
	setCurrent(key, fire) {
		let current = this.getCurrent("item"),
			item = this.getData(key, "item") || new DataItem({}, this._formats),
			diff = current !== item;

		this._current = item;

		if (diff || fire)
			this.onCurrentChange(item);
	}

	/**Returns the Dataset's current state in an object.
	 * The object has the properties as follows.
	 * <ul>	<li>currentKey - key of the current user data</li>
	 *		<li>selectedKeys - array of keys to the selected user data</li>
	 * </ul>
	 * @returns {object} Dataset's current state
	 */
	get state() {
		let empty = this.empty,
			self = this;
		return {
			currentKey:!empty ?  self.getKey(self.getCurrent()) : null,
			selectedKeys:!empty ? self.getKeys("selected") : []
		};
	}

	/**Sets the state to the Dataset.
	 * After the state is set, the methods 
	 * <ul>	<li>{@link Dataset#onCurrentChange}</li>
	 *		<li>{@link Dataset#onSelectionChange}</li>
	 * </ul>
	 * are called back.
	 * @param {object} state state of the Dataset
	 * The state is an object of the following properties.
	 * <ul>	<li>currentKey - key of the current user data</li>
	 *		<li>selectedKeys - array of keys to the selected user data</li>
	 * </ul>
	 * @returns {Dataset} the Dataset
	 */
	setState(state) {
		if (this.empty) {
			this.onCurrentChange(null);
			this.onSelectionChange([]);
			this.onDirtiesChange(false);
		} else {
			state = state || this.state;
			let current = this.getData(state.currentKey) || this.getDataset()[0],
				currentKey = this.getKey(current);
			this.setCurrent(currentKey, true);
			this.select(state.selectedKeys || [], true, true);
		}
		return this;
	}

	/**Returns an array of user data or dataItems.
	 * @param {string} status
	 * <ul>	<li>undefined to get all user data</li>
	 *		<li>"selected" to get selected user data</li>
	 *		<li>"added" to get added user data</li>
	 *		<li>"modified" to get modified user data</li>
	 *		<li>"removed" to get removed user data</li>
	 *		<li>"dirty" to get dirty user data</li>
	 * </ul>
	 * @param {string} option
	 * <ul>	<li>undefined to get array of user data</li>
	 *		<li>"item" to get array of dataItems</li>
	 * </ul>
	 * @returns {array|object}
	 * <ul>	<li>array of user data or dataItems</li>
	 *		<li>with status of "dirty", object of status and array of user data</li>
	 * </ul>
	 * @example
	 * //To get all user data
	 * let dataList = dataset.getDataset();
	 * //To get all user data in dataItems
	 * let dataItems = dataset.getDataset("item");
	 * //To get selected user data
	 * let dataList = dataset.getDataset("selected");
	 * //To get selected user data in dataItems
	 * let dataItems = dataset.getDataset("selected", "item");
	 * //To get user data that are either added, modified, or removed
	 * let dataList = dataset.getDataset("added");
	 * dataList = dataset.getDataset("modified");
	 * dataList = dataset.getDataset("removed");
	 * //To get user data in dataItems that are either added, modified, or removed.
	 * let dataItems = dataset.getDataset("added", "item");
	 * dataItems = dataset.getDataset("modified", "item");
	 * dataItems = dataset.getDataset("removed", "item");
	 * //To get dirty user data
	 * let dirties = dataset.getDataset("dirty");
	 * let added = dirties.added;
	 * let modified = dirties.modified;
	 * let removed = dirties.removed;
	 * //To get dirty user data in dataItems
	 * let dirties = dataset.getDataset("dirty", "item");
	 * let added = dirties.added;
	 * let modified = dirties.modified;
	 * let removed = dirties.removed;
	 */
	getDataset(status, option) {
		let items = this._items,
			result = null;
		if ("item" == status)
			option = "item";
		switch (status) {
			case "selected" : result = items.filter(item => item.selected && !item.unreachable); break;
			case "added":
			case "modified":
			case "removed": result = items.filter(item => status == item.state); break;
			case "dirty":
				result = {};
				items.filter(item => item.dirty)
					 .forEach(item => {
					 	let state = item.state,
					 		array = result[state];
					 	if (!array)
					 		result[state] = array = [];
					 	array.push(item);
					 });
				break;
			case "item":
			default: result = items.filter(item => !item.unreachable); break;
		}
		if ("item" == option)
			return result;
			
		let getData = item => item.data;
		if ("dirty" != status)
			return "item" == option ? result : result.map(e => getData(e));

		for (let prop in result) {
			result[prop] = result[prop].map(e => getData(e));
		}
		return result;
	}

	/**Returns whether the Dataset is dirty.
	 * A Dataset is dirty if it has user data that is either added, modified, or removed.
	 * @returns {boolean} whether the Dataset is dirty
	 * <ul>	<li>true if the Dataset is dirty</li>
	 *		<li>false otherwise</li>
	 * </ul>
	 */
	get dirty() {
		return this._items
			.filter(item => item.dirty)
			.length > 0;
	}

	/**Selects or unselects user data depending on the arguments.
	 * After the selection change, the method
	 * <ul>	<li>{@link Dataset#onSelectionChange}</li>
	 * </ul>
	 * is called.
	 * @example
	 * //To select a user data
	 * dataset.select("key0")</code> or <code>dataset.select("key0", true)
	 * //To select multiple user data
	 * dataset.select(["key0", "key1"])</code> or <code>dataset.select(["key0", "key1"], true)
	 * //To select all user data
	 * dataset.select()</code> or <code>dataset.select(true)
	 * //To unselect a user data
	 * dataset.select("key0", false)
	 * //To unselect multiple user data
	 * dataset.select(["key0", "key1"], false)
	 * //To unselect all user data
	 * dataset.select(false)
	 */
	select(...args) {
		let arg0 = ifEmpty(args[0], true),
			arg1 = ifEmpty(args[1], true),
			dirty = false,
			fire = false;
		if ("boolean" == typeof arg0) {
			for (let i = 0, length = this.length; i < length; ++i) {
				dirty = this._items[i].select(arg0) || dirty;
			}
			fire = args[1];
		} else {
			let keys = Array.isArray(arg0) ? arg0 : [arg0];
			for (let i = 0; i < keys.length; ++i) {
				let item = this.getData(keys[i], "item");
				if (item)
					dirty = item.select(arg1) || dirty;
			}
			fire = args[2];
		}
		if (dirty || fire) {
			this.onSelectionChange(this.getDataset("selected"));
		}
		return dirty;
	}

	/**Toggles selection of the user data associated with the key.
	 * After the selection change, the method
	 * <ul>	<li>{@link Dataset#onSelectionChange}</li>
	 * </ul>
	 * is called.
	 * @param {string} key key associated with user data
	 * @returns {boolean} selection status of the user data
	 * <ul>	<li>true if the user data is selected</li>
	 *		<li>false otherwise</li>
	 * </ul>
	 */
	toggle(key) {
		let item = this.getData(key, "item"),
			status = item ? item.toggle() : false;
		this.onSelectionChange(this.getDataset("selected"));
		return status;
	}

	/**appends user data to the Dataset.
	 * After the user data is appended, the methods
	 * <ul>	<li>{@link Dataset#onAppend}</li>
	 *		<li>{@link Dataset#onCurrentChange}</li>
	 *		<li>{@link Dataset#onSelectionChange}</li>
	 *		<li>{@link Dataset#onDirtiesChange}(if the Dataset gets dirty)</li>
	 * </ul>
	 * are called.
	 * @param data {object|array} user data or array of user data
	 * @returns the Dataset
	 */
	append(data) {
		if (!data) return this;
		
		let notDirty = !this.dirty,
			array = Array.isArray(data) ? data : [data];
		array.forEach(e => {
			let item = new DataItem(e, this._formats);
			this._items.push(item);
			
			let key = this.getKey(e);
			this._byKeys["key-" + key] = item;
			item.state = "added";
		});

		let state = this.state;
		this.onAppend(array);
		state.currentKey = this.getKey(array[array.length - 1]);
		this.setState(state);
		
		if (notDirty)
			this.onDirtiesChange(true);
		
		return this;
	};

	/**Modifies user data associated with the key using the modifier.
	 * After user data modification, the methods
	 * <ul>	<li>{@link Dataset#onModify}</li>
	 *		<li>{@link Dataset#onDirtiesChange}(if the Dataset gets dirty)</li>
	 * </ul>
	 * are called.
	 * @param {string} key key to a Dataset's user data
	 * @param {function} modifier function that modifies the user data.
	 * The function must have a sigunature that accepts a DataItem.
	 * If the modification fails, it must return ValueFormat.InvalidValue.
	 * @returns the Dataset
	 * @example
	 * let v = ...;
	 * let modifier = (dataItem) => {
	 *   if (v !== ...)
	 *     return ValueFormat.InvalidValue;
	 *   let data = dataItem.data;
	 *   data.prop = v;
	 * };
	 * ...
	 * dataset.modify("key0", modifier);
	 */
	modify(key, modifier) {
		if (!modifier) return this;
		
		let item = this.getData(key, "item");
		if (!item)
			throw new Error("Item not found with " + key);
		
		let notDirty = !this.dirty,
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
			if (notDirty)
				this.onDirtiesChange(true);
		} else if (revert) {
			this.onModify(Object.getOwnPropertyNames(data), item, current);
		}

		return this;
	}

	/**Replaces the Dataset's user data with the replacement.
	 * After replacement, the methods
	 * <ul>	<li>{@link Dataset#onReplace}</li>
	 *		<li>{@link Dataset#onDirtiesChange}(if the Dataset gets dirty or not dirty)</li>
	 * </ul>
	 * are called.
	 * @param {object|array} replacement
	 * replacement is an object or an array of objects of the following properties.
	 * <ul>	<li>key - key to the user data to be replaced</li>
	 *		<li>data - new user data</li>
	 * </ul>
	 * @returns {Dataset} the Dataset
	 * @example
	 * //To replace old-keyed user data with new-keyed user data.
	 * dataset.replace({key:"old-key", data:{id:"new-key", ...}});
	 * //or
	 * dataset.replace([
	 *	{key:"old-key0", data:{id:"new-key0", ...}},
	 *	{key:"old-key1", data:{id:"new-key1", ...}},
	 * ]);
	 * //To replace user data with equal-keyed user data
	 * dataset.replace({data:{id:"key0", ...}});
	 * //or
	 * dataset.replace([
	 *	{data:{id:"key0", ...}},
	 *	{data:{id:"key1", ...}},
	 * ]);
	 */
	replace(replacement) {
		if (isEmpty(replacement)) return this;
		
		let before = this.dirty,
			replacements = Array.isArray(replacement) ? replacement : [replacement],
			replacing = [];
		replacements.forEach(obj => {
			let data = obj.data;
			if (!data) return;
			
			let	key = obj.key || this.getKey(data);
			if (!key) return;
			
			let oldItem = this.getData(key, "item"),
				newItem = new DataItem(data, this._formats),
				pos = oldItem ? this._items.indexOf(oldItem) : -1;
			
			newItem.selected = oldItem && oldItem.selected;	
			if (pos > -1)
				this._items[pos] = newItem;
			else
				this._items.push(newItem);
			
			delete this._byKeys["key-" + key];
			this._byKeys["key-" + this.getKey(data)] = newItem;
			
			if (this._current == oldItem)
				this._current = newItem;

			replacing.push(newItem);
		});
		this.onReplace(replacing);
		let after = this.dirty;
		if (before != after)
			this.onDirtiesChange(after);

		return this;
	}

	/**Removes user data associated with the key.
	 * After user data removal, the methods
	 * <ul>	<li>{@link Dataset#onRemove}</li>
	 *		<li>{@link Dataset#onCurrentChange}</li>
	 *		<li>{@link Dataset#onSelectionChange}</li>
	 *		<li>{@link Dataset#onDirtiesChange}(if the Dataset gets dirty or not dirty)</li>
	 * </ul>
	 * are called.
	 * @param {string|array} key key or keys to user data
	 * @returns {Dataset} the Dataset
	 * @example
	 * dataset.remove("key0");
	 * dataset.remove(["key0", "key1"]);
	 */
	remove(key) {
		if (!key || this.empty) return this;
		
		let before = this.dirty,
			keys = Array.isArray(key) ? key : [key],
			removed = this._items.filter(item => {
				let k = this.getKey(item.data),
					remove = keys.includes(k);
				if (remove) {
					item.state = "added" == item.state ? "ignore" : "removed";
				}
				return remove;
			}),
			currentPos = this._items.indexOf(this._current),
			state = this.state;
			
		if (currentPos > -1) {
			let newKey = null;
			for (let i = currentPos, length = this._items.length; i < length; ++i) {
				let item = this._items[i];
				if (item.unreachable) continue;
				
				newKey = this.getKey(item);
				break;
			}
			if (!newKey)
				for (let i = this._items.length - 1; i > 0; --i) {
					let item = this._items[i];
					if (item.unreachable) continue;
					
					newKey = this.getKey(item);
					break;
				}
			state.currentKey = newKey;
		}
		this.onRemove(removed);
		this.setState(state);
		let after = this.dirty;
		if (before != after)
			this.onDirtiesChange(after);
		
		return this;
	}

	/**Erases user data associated with the key.
	 * After user data removal, the methods
	 * <ul>	<li>{@link Dataset#onErase}</li>
	 *		<li>{@link Dataset#onCurrentChange}</li>
	 *		<li>{@link Dataset#onSelectionChange}</li>
	 *		<li>{@link Dataset#onDirtiesChange}(if the Dataset gets dirty or not dirty)</li>
	 * </ul>
	 * are called.
	 * Note that unlike {@link Dataset#remove} this method deletes user data completely from the Dataset
	 * and the erased user data are not traced as dirty user data.  
	 * @param {string|array} key key or keys to user data
	 * @returns {Dataset} the Dataset
	 * @example
	 * dataset.erase("key0");
	 * dataset.erase(["key0", "key1"]);
	 */
	erase(key) {
		if (!key || this.empty) return;
		
		let before = this.dirty,
			keys = Array.isArray(key) ? key : [key],
			erased = this._items.filter(item => {
				let k = this.getKey(item.data),
					erase = keys.indexOf(k) > -1;
				if (erase) {
					delete this._byKeys["key-" + k];
				}
				return erase;
			});
		
		let	currentPos = erased.indexOf(this._current) > -1 ? this._items.indexOf(this._current) : -1,
			state = this.state;
			
		if (currentPos > -1) {
			let newKey = null;
			for (let i = currentPos + 1, length = this._items.length; i < length; ++i) {
				let item = this._items[i];
				if (item.unreachable || erased.includes(item)) continue;
				
				newKey = this.getKey(item);
				break;
			}
			if (!newKey)
				for (let i = this._items.length - 1; i > 0; --i) {
					let item = this._items[i];
					if (item.unreachable || erased.includes(item)) continue;
					
					newKey = this.getKey(item);
					break;
				}
			state.currentKey = newKey;
		}
		this._items = this._items.filter(function(item){return !erased.includes(item);});

		this.onErase(erased);
		this.setState(state);
		let after = this.dirty;
		if (before != after)
			this.onDirtiesChange(after);
	}

	/**Returns an array of strings converted from the template using the property values of the Dataset's user data.
	 * In the template, placeholder for the properties of the user data is specified like {property name}.
	 * @param {string} template template string
	 * @returns {array} array of strings converted from the template using the property values of the user data
	 */
	inStrings(template) {
		return this.getDataset("item")
			.map(item => item.inString(template));
	}

	/**Returns a property value of user data.
	 * If a value format is associated with the property, the value is formatted.
	 * @param args See the example
	 * @returns {any|string} property value of a user data
	 * @example
	 * //To get a property value of user data associated with a key
	 * let value = dataset.getValue("key0", "property0");
	 * //To get a property value of current user data
	 * let value = dataset.getValue("property0");
	 */
	getValue(...args) {
		let key = null,
			property = null;
		switch (args.length) {
		case 1:
			key = this.getKey(this.getCurrent());
			property = args[0];
			break;
		case 2:
			key = args[0];
			property = args[1];
			break;
		default: return null;
		}
		
		let item = this.getData(key, "item");
		return item ? item.getValue(property) : undefined;
	}

	/**Sets a value to a property of user data.
	 * If a value format is associated with the property, the value is parsed before setting to user data.
	 * After setting the value, the methods
	 * <ul>	<li>{@link Dataset#onModify}</li>
	 *		<li>{@link Dataset#onDirtiesChange}(if the Dataset gets dirty)</li>
	 * </ul>
	 * are called.
	 * @param args See the example
	 * @example
	 * //To set a value to a property of user data associated with a key
	 * dataset.setValue("key0", "property0", "value0");
	 * //To set a value to a property of current user data
	 * dataset.setValue("property0", "value0");
	 */
	setValue(...args) {
		let key = null,
			property = null, 
			value = null;
		switch (args.length) {
		case 2:
			key = this.getKey(this.getCurrent());
			property = args[0];
			value = args[1];
			break;
		case 3:
			key = args[0];
			property = args[1];
			value = args[2];
			break;
		default: return this;
		}
		return this.modify(key, function(item){
			return item.setValue(property, value);
		});
	}

	/**Called back when user data are set.
	 * @param {object|array} obj object that has user data or an array of user data
	 */
	onDatasetChange(obj) {this.log("Dataset changed", obj);}
	
	/**Called back when current user data is changed.
	 * @param {DataItem} currentItem current dataItem
	 */
	onCurrentChange(currentItem) {this.log("Current changed", currentItem);}

	/**Called back when user data selection changes.
	 * @param {array} selected array of selected user data 
	 */
	onSelectionChange(selected) {this.log("Selection changed", selected);}

	/**Called back when user data is appended.
	 * @param {object|array} appended user data or array of user data 
	 */
	onAppend(appended) {this.log("Data appended", appended);}

	/**Called back when user data is modified.
	 * @param {array} props names of changed properties 
	 * @param {DataItem} modified modified user dataItem
	 * @param {boolean} current whether current user data is modified
	 */
	onModify(props, modified, current) {this.log("Data modified", props, modified, current ? "current" : "");}

	/**Called back when user data are replaced.
	 * @param {array} replacing array of user dataItems replacing the old ones 
	 */
	onReplace(replacing) {this.log("Data replaced", replacing);}

	/**Called back when user data are removed.
	 * @param {array} removed array of removed dataItems 
	 */
	onRemove(removed) {this.log("Data removed", removed)}

	/**Called back when user data are erased.
	 * @param {array} erased array of erased dataItems 
	 */
	onErase(erased) {this.log("Data erased", erased)}

	/**Called back when the Dataset gets dirty or not dirty.
	 * @param {boolean} dirty 
	 * <ul>	<li>true if the Dataset is dirty</li>
	 *		<li>false otherwise</li>
	 * </ul>
	 */
	onDirtiesChange(dirty){this.log("Dirties change", dirty);}
}