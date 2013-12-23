Event.observe(window, "load", function() {
	$("version").innerHTML = version;
	
	new Ajax.Request("/license", {
		method: 'get',
		asynchronous: true,
		onComplete: function(transport) {
			
			$("loadingGif").style.display = "none";
			$("licenseArea").value = transport.responseText;
			$("licenseArea").style.display = "";
		}
	});
});