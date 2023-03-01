function wait(show) {
	if (show == false)
		$(".wait").hide();
	else
		$(".wait").show();
}

function onError(xhr, options, error) {
	if (xhr.readyState == 0)
		return alert("Unable to access the server.");

	var resp = JSON.parse(xhr.responseText);
	if (resp.handler)
		return eval(resp.handler);

	var	msgs = [];
	for (key in resp)
		msgs.push(resp[key])
	msgs = msgs.join("\n\n");
	if (msgs)
		alert(msgs);
}

function ajax(options) {
	options.beforeSend = function(xhr) {
		wait();
	}

	var handleError = options.error || onError;
	options.error = function(xhr, options, error) {
		wait(false);
		handleError(xhr, options, error);
	}

	var handleComplete = options.complete || function(){};
	options.complete = function() {
		wait(false);
		handleComplete();
	};

	$.ajax(options);
}

function json(options) {
	options.dataType = "json";
	if (typeof(options.data) == "string") {
		options.contentType = "application/json; charset=UTF-8";
	} else if (typeof(options.data) == "object") {
		options.data.json = true;
	}
	ajax(options);
}

function ignore() {
	console.log.apply(console, arguments);
}

var dialog = {
	title:"Horizon Example",
	template:null,
	open:function(conf) {
		if (this.template)
			this.create(conf);
		else {
			var self = this;
			ajax({
				url:"/example/asset/html/dialog.html",
				success:function(resp) {
					self.template = resp;
					self.create(conf);
				}
			});
		}
	},
	close:function(id) {
		$("#close" + id).click();
	},
	create:function(conf) {
		conf = conf || {};
		var id = conf.id,
			tmpl = this.template
				.replace(/{id}/g, id)
				.replace(/{title}/g, conf.title || dialog.title)
				.replace(/{content}/g, conf.content || ""),
			dlg = $(tmpl).appendTo("body"),
			onClose = function() {
				$("#" + id + ", .modal-backdrop").remove();
				if (conf.onClose)
					conf.onClose();
			};
			$("#close" + id).click(onClose);
		if (conf.onOK) {
			$("#" + id + " #button").show();
			$("#" + id + " #button button").unbind("click").click(function() {
				if (conf.getData) {
					var selected = conf.getData.apply();
					if (!selected) return;
					
					conf.onOK(selected);
					dialog.close(id);
				} else {
					conf.onOK();
					dialog.close(id);
				}
			});
		} else {
			setTimeout(function(){dialog.close(id);}, conf.timeout || 2000);
		}
		$("#" + id).modal("show");
	},
	alert:function(conf) {
		var container = "<div class='container-fluid text-center' style='font-size:1.4em;'>{content}</div>";
		if ("string" == typeof(conf)) {
			conf = {
				content:container.replace(/{content}/g, conf)
			};
		} else {
			conf.content = container.replace(/{content}/g, conf.content);
		}
		conf.id = "dialog-alert";
		this.open(conf);
	}
};