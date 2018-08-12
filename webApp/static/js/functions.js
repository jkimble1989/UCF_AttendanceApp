$(document).ready(function(){
    //Search function for student dashboard
    $("#myInput").on("keyup", function() {
        var value = $(this).val().toLowerCase();
        $("#classTableData tr").filter(function() {
            $(this).toggle($(this).text().toLowerCase().indexOf(value) > -1)
        });
    });


	
	// Checks for students in a professor's class
/*$( ".attendanceButton" ).click(function() {
	 $.ajax({
	  url: 'viewAttendance', //server url
	  type: 'POST',    //passing data as post method
	  dataType: 'json', // returning data as json
	  success:function(data)
	  {
		var jsonObject = data;
		  
		console.log(jsonObject);  //response from the server given as alert message  
		
		/* var content = "<table><tr><th>Student Name</th></tr>";
		$.each(jsonObject, function(i, val) {
			content += "<tr><td>" + val.studentName + "</td></tr>";
		});
		content += "</table>";
		
		$("#attendance").append(content);
	  },
	   error: function(XMLHttpRequest, textStatus, errorThrown) { 
                    alert("Status: " + textStatus); alert("Error: " + errorThrown); 
                }
	}); 
	event.preventDefault();
});*/   
	$('.attendanceForm').on('submit', function(event) {
		
		$( "#attendance" ).empty();
		var classID = event.target.id;
		console.log(classID);
       $.ajax({
           url: 'viewAttendance',
		   headers: {'Content-Type': 'application/json'}, 
           data: JSON.stringify( {'classID' : classID} ),
           type: 'POST',
		   dataType: 'json', // returning data as json
           success: function(jsonObject) {
               console.log(jsonObject);
			   
			   var content = "";
			    if(jsonObject == ""){
						content = "<div class='noAttendance'>No Attendance Recorded Yet</div>";
				}
				else{
					content = '<div id="accordion">';
				
					$.each(jsonObject, function(i, val) {
						//HEADER BOX
						content += '<div class="card"><div class="card-header" id="heading'+ i +'"><h5 class="mb-0">';
						content += '<button class="btn btn-link" data-toggle="collapse" data-target="#collapse'+ i +'" aria-expanded="false" aria-controls="collapse'+ i +'">'+ val.date +'</button>';
						content += '</button></h5></div>';
					
						//BODY BOX
						content += '<div id="collapse'+ i +'" class="collapse" aria-labelledby="heading'+ i +'" data-parent="#accordion"><div class="card-body">';
					
						content += '<table><tr><th>PID</th><th>First Name</th><th>Last Name</th></tr>';
						$.each(val.dateArray, function(j, student){
							content += '<tr><td>'+ student.pid +'</td><td>'+ student.studentFirstName +'</td><td>'+ student.studentLastName +'</td></tr>';	
						});	
						content += '</table>';
					
						content += '</div></div></div>';
					});
				}
				$("#attendance").append(content);
           },
		   error: function(XMLHttpRequest, textStatus, errorThrown) { 
                    alert("Status: " + textStatus); alert("Error: " + errorThrown); 
                }
       })
	   event.preventDefault();
	   
   });
});


//**********AJAX CALLS**********//
//AJAX Call to getClasses (for professor view)
$.ajax({
  url: 'getClasses', //server url
  type: 'POST',    //passing data as post method
  dataType: 'json', // returning data as json
  success:function(data)
  {
    var jsonObject = data;
      
    //console.log(jsonObject);  //response from the server given as alert message  
	
	var content = "<table><tr><th>Class Name</th><th>Class Code</th><th>View Attendance</th></tr>";
	$.each(jsonObject, function(i, val) {
		content += "<tr><td>" + val.className + "</td><td>" + val.classCode + "</td><td>" + 
		"<form action='/viewAttendance' method='POST' class='attendanceForm' id='" + val.classID + "'><input type='hidden' id='" + val.classID + "' name='classNum' value='" + val.classID + "'>" + 
		"<input type='submit' value='View' class='attendanceButton btn btn-primary btn-large btn-block' ></form></td></tr>";
	});
	content += "</table>";
	
	$("#classDisplay").append(content);
  }
});

//AJAX Call to getAllClasses (for student view)
$.ajax({
  url: 'getAllClasses', //server url
  type: 'POST',    //passing data as post method
  dataType: 'json', // returning data as json
  success:function(data)
  {
    var jsonObject = data;
      
    //console.log(jsonObject);  //response from the server given as alert message  
	var currentContent = document.getElementById("classStudentDisplay");
	var content = "";
	$.each(jsonObject, function(i, val) {
		content += "<tr><td>" + val.className + "</td><td>" + val.classCode + "</td><td>" + val.professorName + "</td><td>" + val.professorEmail + "</td><td>" +
		"<form action='/joinClass' method='POST'><input type='hidden' name='classID' value='" + val.classID + "'>" + 
		"<input type='hidden' name='className' value='" + val.className + "'>" + 
		"<input type='hidden' name='classCode' value='" + val.classCode + "'>" +
		"<input type='hidden' name='professorName' value='" + val.professorName + "'>" + 
		"<input type='hidden' name='professorEmail' value='" + val.professorEmail + "'>" + 
		"<input type='submit' value='Join' class='btn btn-primary btn-large btn-block' ></form></td></tr>";
	});
	//console.log(content);
	$("#classTableData").append(content);
  }
});  

// AJAX call to show students what they are enrolled in
$.ajax({
  url: 'myClasses', //server url
  type: 'POST',    //passing data as post method
  dataType: 'json', // returning data as json
  success:function(data)
  {
    var jsonObject = data;
      
    //console.log(jsonObject);  //response from the server given as alert message  
	
	var content = "";
	$.each(jsonObject, function(i, val) {
		content += "<tr><td>" + val.className + "</td><td>" + val.classCode + "</td></tr>";
	});
	
	$("#studentsClassesData").append(content);
  }
});




