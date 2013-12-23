Event.observe(window, "load", function() {
	if(!Object.isUndefined(errorMsg) && errorMsg != 'none') {
		$("errorSpan").innerHTML = errorMsg;
		$("errorSpan").style.display="block";
	}
	
	$("loginField").focus();
});