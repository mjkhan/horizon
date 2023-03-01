function treeSupport(conf) {
	var selector = conf.selector,
		dur = 200,
		support = {
		_obj:$(selector).html(conf.data).jstree(conf),
		_tree: $(selector).jstree(true),
		_folding: "collapsed",
		setData:function(elements) {
			if (support._obj)
				support._obj.jstree("destroy");
			support._obj = $(selector).html(elements).jstree(conf);
			support._tree = $(selector).jstree(true);
			return support;
		},
		log:function(msg) {
			if (conf.trace)
				console.log(msg);
		},
		open: function(id) {
		   if (!id) {
			   support._tree.open_all(null, dur);
			   this._folding = "expand";
			   return support.log("All nodes opened.");
		   }
		   
		   if ("string" == typeof(id)) {
			   support._tree.open_node(id, null, dur);
			   var parent = support._tree.get_parent(id);
			   if (parent)
				   support.open(parent);
			   support.log("The node('" + id + "') opened.");
		   } else if ("number" == typeof(id)) {
			   
		   }
		},
		close: function(id) {
			if (!id) {
				support._tree.close_all(null, dur);
			   this._folding = "collapsed";
				return support.log("All nodes closed.");
			}
			if ("string" == typeof(id)) {
				support._tree.close_node(id, null, dur);
				support.log("The node('" + id + "') closed.");
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
			return support._folding;
			/*
			if (!expand)
				support.close();
			else
				support.open();
			if (handler)
				handler(support._folding = !expand ? "collapsed" : "expanded");
			return support._folding;
			 */			
		},
		getChildIDs:function(parentID) {
			var parent = support.getNode(parentID);
			return parent.children;
		},
		getNode: function(id) {return support._tree.get_node(id);},
		selectNode: function(obj) {return support._tree.select_node(obj);},
		checkNodes: function(obj, check) {
			if (obj == false)
				return support._tree.uncheck_all();
			if (obj == true || !obj)
				return support._tree.check_all();
			
			if (check != false) {
				support._tree.check_node(obj);
			} else {
				support._tree.uncheck_node(obj);
			}
		},
		selectedNodes: function() {return support._tree.get_selected();},
		checkedNodes: function() {return support._tree.get_checked();},
		add: function(parent, label, onAdd) {
			if ($.isFunction(parent)) {
				onAdd = parent;
				parent = label = null;
			}
			if (!parent) {
				var selected = support.selectedNodes();
				if (selected)
					parent = selected[0];
			}
			if (!parent)
				return support.log("A parent node is required.");
			
			if (!label)
				label = "New node";
			var added = support._tree.create_node(parent, label, "last", null, true);
			support._tree.open_node(parent);
			support._tree.edit(added, null, function(node){
				support.log("A node added: " + JSON.stringify(node));
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
				var selected = support.selectedNodes();
				if (selected)
					node = selected[0];
			}
			if (!node)
				return support.log("A node is required to edit.");

			support._tree.edit(node, null, function(node, status, cancel) {
				if (cancel) return;
				support.log("A node edited: " + JSON.stringify(node));
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
				var selected = support.selectedNodes();
				if (selected)
					id = selected[0];
			}
			if (!id)
				return support.log("A node is required to remove.");

			var node = support.getNode(id),
				parent = support.getNode(node.parent),
				temp = node.original && node.original.temp;
			support._tree.delete_node(node);
			if (!temp)
				support.log("A node removed: " + JSON.stringify(node));
			if (onRemove)
				onRemove(node);
			if (!temp)
				support.selectNode(parent);
		},
		dragStart: function(evt, dragged, onDrop) {
			support._ondrop = onDrop;
			return $.vakata.dnd.start(evt, {jstree:true, nodes:[{id:true, text:"temporary", dragged:dragged, temp:true}]});
		},
		onNodeSelect: conf.onNodeSelect,
		onNodeCheck: conf.onNodeCheck,
		onNodeReorder: conf.onNodeReorder,
		onNodeMove: conf.onNodeMove
	};

	$(selector)
	.on("changed.jstree", function(e, data){
		var selected = !data || !data.selected ? [] : data.selected;
		support.log("Node(s) selected: " + selected);
		if (support.onNodeSelect)
			support.onNodeSelect(selected);
	}).on("move_node.jstree", function(e, data) {
		var move = data.old_parent != data.parent;
		if (move) {
			support.log(data.node.id + " moved to " + data.parent);
			if (support.onNodeMove)
				support.onNodeMove({node:data.node, parent:data.parent});
		} else {
			if (data.old_position == data.position) return;
			
			var offset = data.position - data.old_position;
			support.log(data.node.id + " reorderd in " + data.parent + " with offset: " + offset + ".");
			if (support.onNodeReorder)
				support.onNodeReorder({node:data.node, parent:data.parent, offset:offset});
		}
	}).on("copy_node.jstree", function(e, data) {
		var node = data.node,
			org = node.original;
		if (!node || !org || !org.temp) return;
		
		var target = support.getNode(node.parent),
			dragged = org.dragged;
		support.remove(node.id);
		support.log(dragged + " dropped onto " + JSON.stringify(target) + ".");
		if (support._ondrop)
			support._ondrop({dragged:dragged, target:target});
		delete support._ondrop;
	}).on("check_node.jstree", function(e, data) {
		if (support.onNodeCheck)
			support.onNodeCheck({node:data.node, checked:true});
	}).on("uncheck_node.jstree", function(e, data) {
		if (support.onNodeCheck)
			support.onNodeCheck({node:data.node, checked:false});
	});
//	delete conf.data;
	return support;
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