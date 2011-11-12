// Ajaxsc.js
// doSuggest
/*
   Writen by Mohamed Sami
   Developer
   Information Technology
   mail :msi_333@yahoo.com
   */
var userList;
function doSuggest(word,textElementId) {
  if(word.length>0)
  {
var request=null;
if (window.XMLHttpRequest) {
request = new XMLHttpRequest();
} else if (window.ActiveXObject) {
request = new ActiveXObject("Microsoft.XMLHTTP");
}

   if(request)
   {
    var url="/services/ajaxDeneme";
    url+="?suggestword="+word;
   
    request.open("POST",url);
    request.onreadystatechange = function()
    {
      
   if(request.readyState==4)
      {
	   var resultObject = eval('(' + request.responseText + ')');
	   resultString=""+resultObject;
	   userList=resultString.split(',');
	   resultHTML="";
	  //elementId=eval(textElementId);
	   //alert(elementId);
	   for(var i=0;i<userList.length;i++){
		   resultHTML+="<a onclick='userClicked("+i+")'>"+userList[i]+"</a>";
		   if(i!=userList.length){
			   resultHTML+="<br/>";
		   }
	   }
	   	document.getElementById("theResults").innerHTML=resultHTML;
	   	document.getElementById("theResults").style.visibility="visible";
      }

   }
   request.send(null);
   
   }
   else
   {
    alert("Your browser don't support AJax !");
   }
   }
   else
   {
    document.getElementById("theResults").innerHTML="";
    document.getElementById("theResults").style.visibility="hidden";
    }
   
} 
function userClicked(index){
	document.getElementById("text1").value=userList[index];
	 document.getElementById("theResults").style.visibility="hidden";
}