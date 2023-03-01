function helpTree(selector, options) {
	if (!options)
		options = {};
	var dur = 200,
		h = {
		_tree: $(selector).jstree(true),
		_folding: "collapsed",
		log:function(msg) {
			if (options.trace)
				console.log(msg);
		},
		open: function(id) {
		   if (!id) {
			   h._tree.open_all(null, dur);
			   this._folding = "expand";
			   return h.log("All nodes opened.");
		   }
		   
		   if ("string" == typeof(id)) {
			   h._tree.open_node(id, null, dur);
			   var parent = h._tree.get_parent(id);
			   if (parent)
				   h.open(parent);
			   h.log("The node('" + id + "') opened.");
		   } else if ("number" == typeof(id)) {
			   
		   }
		},
		close: function(id) {
			if (!id) {
				h._tree.close_all(null, dur);
			   this._folding = "collapsed";
				return h.log("All nodes closed.");
			}
			if ("string" == typeof(id)) {
				h._tree.close_node(id, null, dur);
				h.log("The node('" + id + "') closed.");
			}
			if ("number" == typeof(id)) {

			}
		},
		toggleFolding: function(expand, handler) {
			if (this._folding == "collapsed") {
				this.open();
			} else {
				this.close();
			}
			return h._folding;
			/*
			if (!expand)
				h.close();
			else
				h.open();
			if (handler)
				handler(h._folding = !expand ? "collapsed" : "expanded");
			return h._folding;
*/			
		},
		getChildIDs:function(parentID) {
			var parent = h.getNode(parentID);
			return parent.children;
		},
		getNode: function(id) {return h._tree.get_node(id);},
		selectNode: function(obj) {return h._tree.select_node(obj);},
		checkNodes: function(obj, check) {
			if (obj == false)
				return h._tree.uncheck_all();
			if (obj == true || !obj)
				return h._tree.check_all();
			
			if (check != false) {
				h._tree.check_node(obj);
			} else {
				h._tree.uncheck_node(obj);
			}
		},
		selectedNodes: function() {return h._tree.get_selected();},
		checkedNodes: function() {return h._tree.get_checked();},
		add: function(parent, label, onAdd) {
			if ($.isFunction(parent)) {
				onAdd = parent;
				parent = label = null;
			}
			if (!parent) {
				var selected = h.selectedNodes();
				if (selected)
					parent = selected[0];
			}
			if (!parent)
				return h.log("A parent node is required.");
			
			if (!label)
				label = "New node";
			var added = h._tree.create_node(parent, label, "last", null, true);
			h._tree.open_node(parent);
			h._tree.edit(added, null, function(node){
				h.log("A node added: " + JSON.stringify(node));
				if (onAdd)
					onAdd(node);
			});
		},
		edit: function(node, onEdit) {
			if ($.isFunction(node)) {
				onEdit = node;
				node = null;
			}
			if (!node) {
				var selected = h.selectedNodes();
				if (selected)
					node = selected[0];
			}
			if (!node)
				return h.log("A node is required to edit.");

			h._tree.edit(node, null, function(node, status, cancel) {
				if (cancel) return;
				h.log("A node edited: " + JSON.stringify(node));
				if (onEdit)
					onEdit(node);
			});
		},
		remove: function(id, onRemove) {
			if ($.isFunction(id)) {
				onRemove = id;
				id = null;
			}
			if (!id) {
				var selected = h.selectedNodes();
				if (selected)
					id = selected[0];
			}
			if (!id)
				return h.log("A node is required to remove.");

			var node = h.getNode(id),
				parent = h.getNode(node.parent),
				temp = node.original && node.original.temp;
			h._tree.delete_node(node);
			if (!temp)
				h.log("A node removed: " + JSON.stringify(node));
			if (onRemove)
				onRemove(node);
			if (!temp)
				h.selectNode(parent);
		},
		dragStart: function(evt, dragged, onDrop) {
			h._ondrop = onDrop;
			return $.vakata.dnd.start(evt, {jstree:true, nodes:[{id:true, text:"temporary", dragged:dragged, temp:true}]});
		},
		onNodeSelect: options.onNodeSelect,
		onNodeCheck: options.onNodeCheck,
		onNodeReorder: options.onNodeReorder,
		onNodeMove: options.onNodeMove
	};

	$(selector)
	.on("changed.jstree", function(e, data){
		var selected = !data || !data.selected ? [] : data.selected;
		h.log("Node(s) selected: " + selected);
		if (h.onNodeSelect)
			h.onNodeSelect(selected);
	}).on("move_node.jstree", function(e, data) {
		var move = data.old_parent != data.parent;
		if (move) {
			h.log(data.node.id + " moved to " + data.parent);
			if (h.onNodeMove)
				h.onNodeMove({node:data.node, parent:data.parent});
		} else {
			if (data.old_position == data.position) return;
			
			var offset = data.position - data.old_position;
			h.log(data.node.id + " reorderd in " + data.parent + " with offset: " + offset + ".");
			if (h.onNodeReorder)
				h.onNodeReorder({node:data.node, parent:data.parent, offset:offset});
		}
	}).on("copy_node.jstree", function(e, data) {
		var node = data.node,
			org = node.original;
		if (!node || !org || !org.temp) return;
		
		var target = h.getNode(node.parent),
			dragged = org.dragged;
		h.remove(node.id);
		h.log(dragged + " dropped onto " + JSON.stringify(target) + ".");
		if (h._ondrop)
			h._ondrop({dragged:dragged, target:target});
		delete h._ondrop;
	}).on("check_node.jstree", function(e, data) {
		if (h.onNodeCheck)
			h.onNodeCheck({node:data.node, checked:true});
	}).on("uncheck_node.jstree", function(e, data) {
		if (h.onNodeCheck)
			h.onNodeCheck({node:data.node, checked:false});
	});
	return h;
}

function treeHtml(elements, getters) {
	var length = !elements ? 0 : elements.length;
	if (!length) return "";
	
	if (!getters) 
		getters = {
			id:function(e){return e.id;},
			text:function(e){return e.name;}
		};
	
	var result = "<ul>";
	for (var i = 0; i < length; ++i) {
		var e = elements[i];
		result += "<li id=\"" + getters.id(e) + "\">" + getters.text(e);
		result += treeHtml(e.children, getters);
		result += "</li>";
	}
	return result + "</ul>";
}