var maxPlayersSlider;
var viewDistanceSlider;
var maxHeightSlider;

Event.observe(window, "load", function() {
	
	// Sliders
	maxPlayersSlider = new Control.Slider('max-playersHandle', 'max-playersTrack', {
		onSlide: function(v) {
			$("max-players").value = (v*200).toFixed();
		},
		onChange: function(v) { 
			$("max-players").value = (v*200).toFixed();
		}
	});
	viewDistanceSlider = new Control.Slider('view-distanceHandle', 'view-distanceTrack', {
		onSlide: function(v) {
			$("view-distance").value = ((v*12)+3).toFixed();
		},
		onChange: function(v) { 
			$("view-distance").value = ((v*12)+3).toFixed();
		}
	});
	maxHeightSlider = new Control.Slider('max-build-heightHandle', 'max-build-heightTrack', {
		onSlide: function(v) {
			$("max-build-height").value = (v*256).toFixed();
		},
		onChange: function(v) { 
			$("max-build-height").value = (v*256).toFixed();
		}
	});
	
	// Init tooltips
	$$("img.infoTT").each(function(img) {
		new Tooltip(img, {mouseFollow: false, delay:0, backgroundColor:"#4D3324", textColor:"#FCEEC7", textShadowColor:"#000000", opacity:1 });
	});
	
	// Init configuration fields
	loadFields();
});
function setSliderValue(slider, value, name) {
	if (value == '') return;
	if (isNaN(value)) {
		slider.setValue(0);
	}
	else {
		if(name == "max-players") {
			slider.setValue(value/200);
		} else if(name == "max-build-height") {
			slider.setValue(value/256);
		} else {
			slider.setValue((value-3)/12);
		}
	}
}

function loadFields() {
	new Ajax.Request("/bukkitconfJSON", {
		method: 'get',
		asynchronous: false,
		requestHeaders: {Accept: 'application/json'},
		onSuccess: function(transport) {
			var json = transport.responseText.evalJSON(true);
			$("server-ip").value = strValue(json, "server-ip");
			$("server-port").value = strValue(json, "server-port");
			$("level-name").value = strValue(json, "level-name");
			$("level-seed").value = strValue(json, "level-seed");
			$("motd").value = strValue(json, "motd");
			
			$("gamemode" + json["gamemode"]).checked = true;
			$("difficulty" + json["difficulty"]).checked = true;
			
			if(json["hellworld"] == "true") {
				$("hellWorldTrue").checked = true;
			} else {
				$("hellWorldFalse").checked = true;
			}
			if(json["allow-nether"] == "true") {
				$("allowNetherTrue").checked = true;
			} else {
				$("allowNetherFalse").checked = true;
			}
			setSliderValue(maxPlayersSlider, json["max-players"], 'max-players');
			setSliderValue(viewDistanceSlider, json["view-distance"], 'view-distance');
			setSliderValue(maxHeightSlider, json["max-build-height"], 'max-build-height');
			
			if(json["online-mode"] == "true") {
				$("onlineModeTrue").checked = true;
			} else {
				$("onlineModeFalse").checked = true;
			}
			if(json["white-list"] == "true") {
				$("whiteListTrue").checked = true;
			} else {
				$("whiteListFalse").checked = true;
			}
			if(json["spawn-monsters"] == "true") {
				$("spawnMonsterTrue").checked = true;
			} else {
				$("spawnMonsterFalse").checked = true;
			}
			if(json["spawn-animals"] == "true") {
				$("spawnAnimalsTrue").checked = true;
			} else {
				$("spawnAnimalsFalse").checked = true;
			}
			if(json["generate-structures"] == "true") {
				$("generateStructuresTrue").checked = true;
			} else {
				$("generateStructuresFalse").checked = true;
			}
			if(json["spawn-npcs"] == "true") {
				$("spawnNpcsTrue").checked = true;
			} else {
				$("spawnNpcsFalse").checked = true;
			}
			if(json["pvp"] == "true") {
				$("pvpTrue").checked = true;
			} else {
				$("pvpFalse").checked = true;
			}
			if(json["allow-flight"] == "true") {
				$("allowFlightTrue").checked = true;
			} else {
				$("allowFlightFalse").checked = true;
			}
			if(json["enable-query"] == "true") {
				$("queryTrue").checked = true;
			} else {
				$("queryFalse").checked = true;
			}
			if(json["enable-rcon"] == "true") {
				$("rconTrue").checked = true;
			} else {
				$("rconFalse").checked = true;
			}
			$("level-type" + json["level-type"]).checked = true;
			$("rcon.port").value = strValue(json, "rcon.port");
			$("rcon.password").value = strValue(json, "rcon.password");
			$("query.port").value = strValue(json, "query.port");
		}
	});
}

function strValue(json, key) {
	if(!Object.isUndefined(json[key]) && json[key] != "undefined") {
		// Key exists, and is not "corrupted" with the wrong value (from previous versions)
		return json[key];
	} else {
		return "";
	}
}

function serializeAndSubmit() {
	var dataConf = $("confForm").serialize();
	new Ajax.Request("/saveConf", {
		method: 'post',
		asynchronous: true,
		encoding: 'UTF-8',
		parameters: {
			"dataConf" : dataConf
		},
		onSuccess: function(transport) {
			$("successSpan").innerHTML = "Configuration has been successfully saved !";
			$("successSpan").style.display="block";
			setTimeout("hideSuccess()", 2500);
		}
	});
}

function hideSuccess() {
	Effect.Fade('successSpan', { duration: 2.0 });
}