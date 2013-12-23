var isMSIE = /*@cc_on!@*/0;
var showAvatars = true;

Event.observe(window, "load", function() {
	// Init dynamic content
	if(!Object.isUndefined(errorMsg) && errorMsg != 'none') {
		$("errorSpan").innerHTML = errorMsg;
		$("errorSpan").style.display="block";
	}
	
	// Scroll log to bottom
	scrollLogIfEnabled();
	getIndexJSON();
	
	// Init tooltips
	$$("button").each(function(button) {
		new Tooltip(button, {mouseFollow: false, delay:0, backgroundColor:"#4D3324", textColor:"#FCEEC7", textShadowColor:"#000000", opacity:1 });
	});
	//Init bukkitCommand field
	$("customCommandField").observe("keypress", function(event) {
	    if(event.keyCode == Event.KEY_RETURN) {
	    	sendBukkitCommand();
	    }
	});
});

function getIndexJSON() {

	new Ajax.Request("/indexJSON", {
		method: 'get',
		requestHeaders: {Accept: 'application/json'},
		asynchronous: true,
		onSuccess: function(transport) {
			 var json = transport.responseText.evalJSON(true);
			 $("serverStatus").innerHTML = json.status;
			 $("statusIcon").src = "/images/status_" + json.status + ".png";
			 
			 // Update log
			 $("logArea").innerHTML = "";
			 var count = 0;
			 json.log.each(function(lineJSON) {
				 var line = lineJSON.line;
				 var div = document.createElement('div');
				 var txt = document.createTextNode(line);
				 div.appendChild(txt);
				 if(count%2 == 0) {
					 div.className = "pair";
				 }
				 count++;
				 if(lineJSON.isWarn) {
					 div.addClassName("warnline");
				 } else if(lineJSON.isError) {
					 div.addClassName("errorline");
				 }
				 $("logArea").appendChild(div);
			 });
			 if(json.status != "OFF") {
				 if(json.showAvatars == "true") {
					 showAvatars = true;
				 } else {
					 showAvatars = false
			     }
				 
				 // Update status
				 if(json.status == "ON") {
//					 $("stop").style.display = "";
					 $("hold").style.display = "";
					 $("restart").style.display = "";
					 $("save").style.display = "";
					 $("backup").style.display = "";
					 $("start").style.display = "none";
				 }
				 
				 if(!Object.isUndefined(json.bukkitVersion)) {
					//Update informations values
					 $("bukkitVersion").innerHTML = json.bukkitVersion;
					 $("curPlayers").innerHTML = json.curPlayers;
					 $("maxPlayers").innerHTML = json.maxPlayers;
					 
					 //Update memory and CPU
					 $("memory").innerHTML = json.usedMemory + "MB <span style='color: #FF7B2C'>/</span> " + json.totalMemory + " MB";
					 myJsProgressBarHandler.setPercentage('memoryBar', Math.round(json.memPercentage));
					 
					 // Update player list
					 $("ulPlayerList").innerHTML = "";
					 var playersJSON = json.playerList;

					 for(var i=0; i<playersJSON.length; i++) {
						 var playerJSON = playersJSON[i];

						 var liText = playerJSON.name + (playerJSON.op == true ? " [OP]" : "");
						 var aLink = "<a id='ctx_" + playerJSON.name + "' href='#' onclick=\"setSelectedPlayer(this);ShowContent('hiddenPlayerMenu');\">" + liText + "</a>";
						 
						 var avatar = "";
						 if(showAvatars) {
							 avatar = "<img src='https://minotar.net/helm/" + playerJSON.name + "/18.png' />&nbsp;";
						 }
						 var className = "player" + (playerJSON.op == true ? " op" : "");
						 $("ulPlayerList").innerHTML += ("<li id='player_" + playerJSON.name + "' class='" + className + "'>" + avatar + aLink + "</li>");
					 }
					 $("totalMemory").innerHTML = json.totalMemory + " MB";
				 } else {
					 $("bukkitVersion").innerHTML = "?";
					 $("curPlayers").innerHTML = "?";
					 $("maxPlayers").innerHTML = "?";
					 $("memory").innerHTML = "? MB <span style='color: #FF7B2C'>/</span> ? MB";
					 myJsProgressBarHandler.setPercentage('memoryBar', '0');
					 $("totalMemory").innerHTML = "? MB";
				 }
			 } else {
				 $("start").style.display = "";
//				 $("stop").style.display = "none";
				 $("hold").style.display = "none";
				 $("restart").style.display = "none";
				 $("save").style.display = "none";
				 $("backup").style.display = "none";
			 }
			 var date = new Date();
			 $("lastUpdate").innerHTML = date.format("isoDate") + " " + date.format("isoTime");
			 // Scroll log to bottom
			 scrollLogIfEnabled();
		}
	});
	// Clear all process
	$$("span.processSpan").each(function(span) {
		span.innerHTML = "";
	});
	setTimeout("getIndexJSON()", 2000);
}

function sendRTKCommand(buttonId, command) {
	$(buttonId + "_processing").innerHTML = "<img src='/images/status_LOADING.gif' />";
	new Ajax.Request("/execCommand", {
		method: 'post',
		parameters : {
			"command" : command
		}
	});
}

function sendBukkitCommand() {
	var command = $("customCommandField").value;
	if(command != "") {
		new Ajax.Request("/execBukkitCommand", {
			method: 'post',
			parameters : {
				"command" : command
			}
		});
		$("customCommandField").value = "";
	}
}

function sendBackupCommand() {
	$("backup_processing").innerHTML = "<img src='/images/status_LOADING.gif' />";
	new Ajax.Request("/backupServer", {
		method: 'post'
	});
}

function sendCommandForCurrentPlayer(command) {
	if($("selectedPlayer").value != "") {
		var message = $("whispContent").value;
		$("whispContent").value = "";
		var finalCommand = command + " " + $("selectedPlayer").value + " " + message;
		if(command != "") {
			new Ajax.Request("/execBukkitCommand", {
				method: 'post',
				parameters : {
					"command" : finalCommand
				}
			});
		}
	}
}

function scrollLogIfEnabled() {
	if(!$("loglock").checked) {
		$("logArea").scrollTop = $("logArea").scrollHeight;
	}
}

function setSelectedPlayer(alink) {
	var playerName = alink.id.replace("ctx_", "");
	$("selectedPlayer").value = playerName;
	$("hiddenPlayerName").innerHTML = playerName;
	if(showAvatars) {
		$("userAvatar").src = "https://minotar.net/helm/" + playerName + "/100.png";
	 }
}