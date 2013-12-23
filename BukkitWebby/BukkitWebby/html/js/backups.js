Event.observe(window, "load", function() {
	
	refreshList();
});

function refreshList() {
	$("backupList").innerHTML = "";
	
	new Ajax.Request("/backupsJSON", {
		method: 'get',
		asynchronous: true,
		onSuccess: function(transport) {
			try {
				var json = transport.responseText.evalJSON(true);
				var daysCount = json.dates.length;
				if(daysCount == 0) {
					$("backupList").innerHTML = "<span id='noBackupSpan'>No backups found. The 'Backups' directory is empty</span>";
					$("bottomButtons").style.display = "none";
				} else {
					handleJSON(json, daysCount);
				}
			} catch(e)  {
				alert(e);
			}
		}
	});
}

function handleJSON(json, daysCount) {
	
	// For each day
	for(var i=0; i<daysCount; i++) {
		
		// Write a title
		var dayJSON = json.dates[i];
		var h2 = document.createElement('h2');
		var txt = document.createTextNode(dayJSON.day);
		h2.appendChild(txt);
		$("backupList").appendChild(h2);
		

		// Create a table
		var table = document.createElement('table');
		table.id = 'backupTable';
			
		var trHead = document.createElement('tr');
		var th1 = document.createElement('th');
		var th2 = document.createElement('th');
		var th3 = document.createElement('th');
		var th4 = document.createElement('th');
		
		trHead.appendChild(th1);
		trHead.appendChild(th2);
		trHead.appendChild(th3);
		trHead.appendChild(th4);
		
		th1.innerHTML = "Backup Date";
		th2.innerHTML = "File";
		th3.innerHTML = "Backup Size";
		th4.innerHTML = "Actions";
		
		table.appendChild(trHead);
			
		// For each backup
		for(var j=0; j<dayJSON.backups.length; j++) {
			
			var backupJSON = dayJSON.backups[j];
			
			var tr = document.createElement('tr');
			tr.id = "tr_" + backupJSON.name;
			
			var tdDate = document.createElement('td');
			var tdName = document.createElement('td');
			var tdSize = document.createElement('td');
			var tdAction = document.createElement('td');
			
			tr.appendChild(tdDate);
			tr.appendChild(tdName);
			tr.appendChild(tdSize);
			tr.appendChild(tdAction);
			
			table.appendChild(tr);
			
			var size = backupJSON.size;
			var date = backupJSON.date;
			var name = backupJSON.name;
			var actions = 
				"<img src='images/database_go.png' id='img_restore_" + name + "' title='Restore backup' onclick=\"restoreBackup(this)\" />&nbsp;" +
				"<img src='images/database_delete.png' id='img_del_" + name + "' title='Delete backup' onclick=\"deleteBackup(this)\" />";
			
			tdName.innerHTML = name;
			tdSize.innerHTML = size;
			tdDate.innerHTML = date;
			tdAction.innerHTML = actions;
			
			$("backupList").appendChild(table);
		}
	}
}

function sendBackupCommand() {
	dhtmlx.message({ 
        type:"notification", 
        text:"Backup request sent to server. The list will be refresh as soon as the backup is finished. "
    });
	new Ajax.Request("/backupServer", {
		method: 'post',
		asynchronous: true,
		onSuccess: function(transport) {
		    refreshList();
		}
	});
}

function restoreBackup(img) {
	var fileName = img.id.replace("img_restore_", "");
	dhtmlx.confirm({
	    type:"confirm",
	    text: "<strong>Do you wish to restore the backup file " + fileName + "? </strong>.<br />" +
	    	  "This will : <ul style='text-align: left;'><li>Save the server and create a backup <em>(Just in case...)</em></li>" +
	    	  "<li>Restore the backup files</li><li>Restart</li></ul><br />You will then be redirected to the index page.",
	    callback: function(result){
	        if(result == true) {
	        	showLoading("Processing...");
	        	new Ajax.Request("/restoreBackup", {
	        		parameters : {
	        			"fileName" : fileName
	        		},
	        		method: 'post',
	        		asynchronous: true,
	        		onSuccess: function(transport) {
	        			var json = transport.responseText.evalJSON(true);
	        			if(json.success == true) {
	        				window.location.href = "/index";
	        			} else {
	        				dhtmlx.alert({type:"alert-error", title:"Unable to restore backup", text:"An error occured while restoring. Please check the console log."});
	        			}
	        		}
	        	});
	        }// else: cancel
	    }
	});
}

function deleteBackup(img) {
	var fileName = img.id.replace("img_del_", "");
	dhtmlx.confirm({
	    type:"confirm",
	    text: "Are you sure you want to delete the backup file " + fileName + "?", 
	    callback: function(result){
	    	if(result == true) {
	    		new Ajax.Request("/deleteBackup", {
	    			parameters : {
	    				"fileName" : fileName
	    			},
	    			method: 'post',
	    			asynchronous: true,
	    			onSuccess: function(transport) {
	    				var json = transport.responseText.evalJSON(true);
	    				if(json.success == true) {
	    					showSuccess("Backup successfully deleted from disk.");
	    					$("tr_" + fileName).remove();
	    				} else {
	    					showError("Unable to remove backup (maybe the file can't be read or has already been removed). Please check console log.");
	    				}
	    			}
	    		});
	    	}
	    }
	});
}

function showSuccess(message) {
	$("successSpan").innerHTML = message;
	$("successSpan").style.display="block";
	setTimeout("hideSuccess()", 2500);
}

function showError(message) {
	$("errorSpan").innerHTML = message;
	$("errorSpan").style.display="block";
	setTimeout("hideError()", 2500);
}

function hideSuccess() {
	Effect.Fade('successSpan', { duration: 2.0 });
}

function hideError() {
	Effect.Fade('errorSpan', { duration: 2.0 });
}