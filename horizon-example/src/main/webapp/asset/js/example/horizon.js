/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

/**@file Support functions to control user interface elements in HTML pages.
 */

/**Returns the trimmed str.
 * @param {string} str a string
 * @returns {string} trimmed string
 */
function trim(str) {
	if (!str) return "";
	if ("string" != typeof str) return str;
	return str.replace(/^\s+/, "").replace(/\s+$/, "");
}

/**Returns whether v is empty.
 * v is empty when it is undefined, null, or blank
 * @param {any} v a value
 * @returns {boolean}
 * <ul><li>true when v is empty</li>
 * 	   <li>false otherwise</li>
 * </ul>
 */
function isEmpty(v) {
	if (v == undefined
	 || v == "undefined"
	 || v == null
	 || v == "null"
	) return true;

	switch (typeof v) {
	case "string": return "" == v || "" == trim(v);
	default: return false;
	}
}

/**Returns v if v is not empty.<br />
 * If v is empty, returns nv or, if nv is a function, the value nv returns.
 * @param {any} v a value
 * @param {any|function} nv an alternate value or a function that returns an alternate value
 * @returns {any}
 * <ul><li>v when v is not empty</li>
 * 	   <li>nv or the value nv returns if nv is a function</li>
 * </ul>
 */
function ifEmpty(v, nv) {
	if (!isEmpty(v)) return v;
	
	if (typeof(nv) == "function")
		return nv.apply();
	else
		return nv;
}

/**If obj is empty, throws an Error with the message.
 * Otherwise returns obj.
 * @param {any} obj a value
 * @param {string} msg message used when obj is empty
 * @returns {any} obj
 */
function notEmpty(obj, msg) {
	if (isEmpty(obj))
		throw new Error(msg);
	return obj;
}

/**Sets a function to execute when the 'ENTER' key is pressed on the selected elements.
 * @param {string} selector element selector
 * @param {function} handler function to execute when the 'ENTER' key is pressed on the selected elements.
 * The signature of the function is:
 * <pre><code>function myHandler(input){...}</code></pre>
 */
function onEnterPress(selector, handler) {
	document.querySelectorAll(selector).forEach(input => {
		input.addEventListener("keypress", evt => {
			if (evt.which != 13) return;
			handler(input);
		});
	});
}

/**Returns the text of the label associated with an input object.
 * If the label is not found, the title or placeholder of the input object is returned.
 * @param {string|object} input
 * <ul><li>an input object</li>
 * 	   <li>id or name of an input object</li>
 * </ul>
 * @returns {string}
 * <ul><li>text of the label associated with an input object</li>
 * 	   <li>With no label associated, title or placeholder</li>
 * </ul>
 */
function labelFor(input) {
	let selector = "label[for='{id}']",
		key = "string" == typeof(input) ? input
			: input.id || input.name;
	if (!key) return "";

	selector = selector.replace(/{id}/gi, key);
	let label = document.querySelector(selector),
		text = label ? label.innerText : "";
	return text || input.title || input.placeholder;
}

/**Returns an object that handles input validation failure.
 * <p>On failure, the object displays a message and puts the focus back to the input object.<br />
 * For the handler to display a message appropriate for the failure,
 * it is recommended to have a label associated with the input object.
 * </p>
 * @returns {object} object that handles input validation failure
 * @see {@link validInput}
 * @see {@link labelFor}
 */
function validationFailureHandler() {
	let handler = {
			notice: function(msg, input) {
				alert(msg);
				if (input.focus)
					input.focus();
				else if (input.onFailure)
					input.onFailure(input);
			},
			valueMissing: function(input) {
				handler.notice("Please enter the '" + labelFor(input) + "'.", input);
			},
			typeMismatch: function(input) {
				handler.notice(input.value + " is not a valid " + labelFor(input) + ".", input);
			},
			tooLong: function(input) {
				handler.notice(input.value + " is too long.", input);
			},
			patternMismatch: function(input) {
				handler.notice(input.value + " is not a valid " + labelFor(input) + ".", input);
			},
			rangeOverflow: function(input) {
				handler.notice(labelFor(input) + " must be equal to or less than " + input.max + ".", input);
			},
			rangeUnderflow: function(input) {
				handler.notice(labelFor(input) + " must be equal to or greater than " + input.min + ".", input);
			},
			stepMismatch: function(input) {
				handler.notice(labelFor(input) + " must be incremented by " + input.step + ".", input);
			}
		};
	return handler;
}

/**Returns the result of validation on the input object.<br />
 * The types of validation checks are those defined by input objects' basic validity property, which are
 * <ul><li>valueMissing</li>
 * 	   <li>typeMismatch</li>
 * 	   <li>tooLong</li>
 * 	   <li>patternMismatch</li>
 * 	   <li>rangeOverflow</li>
 * 	   <li>rangeUnderflow</li>
 * 	   <li>stepMismatch</li>
 * </ul>
 * To be effective, the input object must be set with attributes of
 * <ul><li>required</li>
 * 	   <li>type</li>
 * 	   <li>maxLength</li>
 * 	   <li>pattern</li>
 * 	   <li>max</li>
 * 	   <li>min</li>
 * 	   <li>step</li>
 * </ul>
 * The failureHandler is used to handle the cases of validation failure.
 * @param {object} input input object
 * @param {object} failureHandler object that handles validation failure.<br />
 * 		  If not provided, the default handler from {@link validationFailureHandler} is used.
 * @returns {boolean}
 * <ul><li>true if the value of the input object is valid</li>
 * 	   <li>false otherwise</li>
 * </ul>
 */
function validInput(input, failureHandler) {
	let validity = input.validity;
	if (validity && validity.valid) return true;

	if (input instanceof Validity) {
		input._onFailure(input);
	} else {
		for (let key in validity) {
			if (!validity[key]) continue;
	
			if (!failureHandler)
				failureHandler = validationFailureHandler();
			let handler = failureHandler[key];
			if (handler)
				handler(input);
			else {
				console.log("Handler not found for validation failure of " + key);
			}
			break;
		}
	}
	return false;
}

/**Holds the result of data validation.<br />
 * The intention is to help user-defined objects and functions to perform validation
 * and use the {@link validInput validInput(...)} for failure report as follows.
 * <pre><code>function myValidation() {
 *   let valid = ...// performs validation
 *   if (valid)
 *     return new Validity();
 *   else
 *     return new Validity().valueMissing("property-name");
 * }
 * ...
 * let validity = myValidation();
 * validInput(validity);</code></pre>
 */
class Validity {
	/** key, property, or identifier of data for validation */
	id;
	/** value for validation */
	value;
	/** step that increments the value */
	step;
	/** minimum value */
	min;
	/** maximum value */
	max;
	/** (0-based) index of the value if it is from multiple values */
	index;
	/** result of data validation */
	validity;
	
	/**Creates a new Validity.
	 */
	constructor() {
		this.id = null;
		this.value = null;
		this.step = null;
		this.min = null;
		this.max = null;
		this.index = null;
			
		this.validity = {
			valueMissing: false,
			typeMismatch: false,
			tooLong: false,
			patternMismatch: false,
			rangeOverflow: false,
			rangeUnderflow: false,
			stepMismatch: false,
			
			get valid() {
				return !this.valueMissing
					&& !this.typeMismatch
					&& !this.tooLong
					&& !this.patternMismatch
					&& !this.rangeOverflow
					&& !this.rangeUnderflow
					&& !this.stepMismatch;
			}
		};
	}
	
	/**Returns a validity failure of valueMissing
	 * @param {string} id identifier of value
	 * @param {number} index (0-based) index of the value if the value is of multiple values
	 * @returns {Validity} the Validity
	 */
	valueMissing(id, index) {
		this.id = id;
		this.index = index;
		this.validity.valueMissing = true;
		return this;
	}
	
	/**Returns a validity failure of typeMismatch
	 * @param {string} id identifier of value
	 * @param {number} index (0-based) index of the value if the value is of multiple values
	 * @returns {Validity} the Validity
	 */
	typeMismatch(id, index) {
		this.id = id;
		this.index = index;
		this.validity.typeMismatch = true;
		return this;
	}
	
	/**Returns a validity failure of tooLong
	 * @param {string} id identifier of value
	 * @param {any} value value
	 * @param {number} index (0-based) index of the value if the value is of multiple values
	 * @returns {Validity} the Validity
	 */
	tooLong(id, value, index) {
		this.id = id;
		this.value = value;
		this.index = index;
		this.validity.tooLong = true;
		return this;
	}
	
	/**Returns a validity failure of patternMismatch
	 * @param {string} id identifier of value
	 * @param {any} value value
	 * @param {number} index (0-based) index of the value if the value is of multiple values
	 * @returns {Validity} the Validity
	 */
	patternMismatch(id, value, index) {
		this.id = id;
		this.value = value;
		this.index = index;
		this.validity.patternMismatch = true;
		return this;
	}
	
	/**Returns a validity failure of rangeOverflow
	 * @param {string} id identifier of value
	 * @param {any} max maximum value
	 * @param {number} index (0-based) index of the value if the value is of multiple values
	 * @returns {Validity} the Validity
	 */
	rangeOverflow(id, max, index) {
		this.id = id;
		this.max = max;
		this.index = index;
		this.validity.rangeOverflow = true;
		return this;
	}

	/**Returns a validity failure of rangeUnderflow
	 * @param {string} id identifier of value
	 * @param {any} min minum value
	 * @param {number} index (0-based) index of the value if the value is of multiple values
	 * @returns {Validity} the Validity
	 */
	rangeUnderflow(id, min, index) {
		this.id = id;
		this.min = min;
		this.index = index;
		this.validity.rangeUnderflow = true;
		return this;
	}
	
	/**Returns a validity failure of stepMismatch
	 * @param {string} id identifier of value
	 * @param {number} step incremental step
	 * @param {number} index (0-based) index of the value if the value is of multiple values
	 * @returns {Validity} the Validity
	 */
	stepMismatch(id, step, index) {
		this.id = id;
		this.step = step;
		this.index = index;
		this.validity.stepMismatch = true;
		return this;
	}
	
	_onFailure(validity) {
		console.log("Validation failed", validity);
	}

	/** Sets the handler to deal with validation failure.
	 * @param {function} handler handler to deal with validation handler.
	 * The handler is provided with the Validity as an argument
	 * @returns {Validity} the Validity
	 */
	onInvalid(handler) {
		this._onFailure = handler;
		return this;
	}
}

/**Returns the result of input validation on the selected input objects.
 * @param {string} selector element selector
 * @param {object} failureHandler object that handles validation failure.<br />
 * 		  If not provided, the default handler from <code>validationFailureHandler()</code> is used.
 * @returns {boolean}
 * <ul><li>true if the input values of the selected objects are valid</li>
 * 	   <li>false otherwise</li>
 * </ul>
 * @see {@link validInput}
 */
function validInputs(selector, failureHandler) {
	return Array.from(document.querySelectorAll(selector))
		.reduce(
			(valid, input) => valid && validInput(input, failureHandler),
			true
		);
}

/**Generates and returns string in html for pagination links.
 * @param {object} config configuration for pagination links.<br />
 * The config's properties are:
 * <pre>{ start:0,	//start (0-based) index of target data
 *  fetchSize:10,	//number of the data that are fetched at once
 *  totalSize:135,	//total number of the data that should be fetched
 *  links:5,		//number of visible links
 *  first: function(index, label){return "..."},		//function that returns string for the link to the first page
 *  previous: function(index, label){return "..."},	//function that returns string for the link to the previous pages
 *  link: function(index, label){return "..."},		//function that returns string for the numbered links
 *  current: function(index, label){return "..."},	//function that returns string for the link to the current page
 *  next: function(index, label){return "..."},		//function that returns string for the link to the next pages
 *  last: function(index, label){return "..."}		//function that returns string for the link to the last page
 * }</pre>
 * @returns string in html for pagination links
 */
function paginate(config) {
	let rc = config.totalSize;
	if (!rc) return "";

	let fetchCount = config.fetchSize;
	if (!fetchCount) return "";

	let fetch = {
		all:0,
		none:-1,
		count(elementCount, size) {
			if (!elementCount || size == fetch.all) return 1;
			return parseInt((elementCount / size) + ((elementCount % size) == 0 ? 0 : 1));
		},
		end(elementCount, size, start) {
			if (size < fetch.all) throw new Error("Invalid size: " + size);
			if (elementCount < 0) throw new Error("Invalid elementCount: " + elementCount);
			let last = elementCount - 1;
			if (size == fetch.all) return last;
			return Math.min(last, start + size -1);
		},
		page(current, count) {
			return parseInt(count < 1 ? 0 : current / count);
		},
		band(page, visibleLinks) {
			return parseInt(visibleLinks < 1 ? 0 : page / visibleLinks);
		}
	};
	let lc = fetch.count(rc, fetchCount);
	if (lc < 2) return "";

	let links = ifEmpty(config.links, fetch.all),
		page = fetch.page(ifEmpty(config.start, 0), fetchCount),
		band = fetch.band(page, links),
		tags = {
			link(tag, index, label) {
				return !tag ? "" : tag(index, label);
			},
			first() {
				return band < 2 ? "" : tags.link(config.first, 0, 1);
			},
			previous() {
				if (band < 1) return "";
			    let prevBand = band - 1,
					prevPage = (prevBand * links) + (links - 1),
			        fromRec = prevPage * fetchCount;
			    return tags.link(config.previous, fromRec, prevPage + 1);
			},
			visibleLinks() {
				let s = "",
					fromPage = links == fetch.all ? 0 : band * links,
					toPage = links == fetch.all ? lc : Math.min(lc, fromPage + links);
				for (let i = fromPage; i < toPage; ++i) {
					let fromRec = i * fetchCount,
						label = i + 1;
					s += tags.link(i == page ? config.current : config.link, fromRec, label);
				}
				return s;
			},
			next(bandCount) {
				bandCount = parseInt(bandCount);
				if (bandCount - band < 2) return "";

				let nextBand = band + 1,
					page = nextBand * links,
					fromRec = page * fetchCount;
				return tags.link(config.next, fromRec, page + 1);
			},
			last(bandCount) {
				bandCount = parseInt(bandCount);
			    let lastBand = bandCount - 1;
			    if (lastBand - band < 2) return "";

			    let pages = lastBand * links,
			        fromRec = pages * fetchCount;
				return tags.link(config.last, fromRec, pages + 1);
			}
		},
		tag = "";
	if (links != fetch.all) {
		tag += tags.first();
		tag += tags.previous();
	}
	tag += tags.visibleLinks();
	if (links != fetch.all) {
        let bandCount = parseInt(lc / links);
        bandCount += lc % links == 0 ? 0 : 1;
		tag += tags.next(bandCount);
		tag += tags.last(bandCount);
	}
	return tag;
}