from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from table import *
from flask import Flask
from flask import Flask, flash, redirect, render_template, request, session, abort, jsonify
import os, bcrypt, datetime, json, html, re

app = Flask(__name__)

email_regex = re.compile(r"[^@]+@[^@]+\.[^@]+")
mac_regex = re.compile(r"([0-9a-fA-F]:?){12}")

# Sends the user to the login page upon opening the app. If already logged in, send to home page where the user
# can update their information, provide a Bluetooth MAC etc
@app.route('/')
def home():
	if not session.get('logged_in'):
		session['logged_in'] = False
		return render_template('login.html')
	else:
		return dashboard() # Need a home page to be redirected to where the student can access their dashboard
		# This will include options such as editing relevant information, adding classes, perhaps checking their attendance records

@app.route('/logout')
def logout():
	session['logged_in'] = False
	session['username'] = ""
	return home()

@app.route('/login', methods=['POST'])
def login():
	Session = sessionmaker(bind=engine)
	s = Session()
	if request.form['email']:
		email = html.escape(request.form['email'])
		if request.form['password']:
			password = html.escape(request.form['password'])
			if email_regex.match(email):
				emailCheck = email[-16:]
				if not emailCheck == '@knights.ucf.edu':
					query = s.query(Professor).get(email)
					if not query:
						return "Please register an account" + home()
					if query:
						#if (bcrypt.hashpw(password.encode('utf8'), query.salt) == query.password):
						if (bcrypt.hashpw(password.encode('utf8'), query.salt.encode('utf8')) == query.password):
							session['username'] = email
							session['logged_in'] = True
							return "You are now logged in as a professor, please click here if you are not redirected to the <a href='/dashboard'>dashboard</a>. <script>window.location = '/dashboard';</script>"
						else:
							return "Password input is incorrect." + home()

				queryStu = s.query(Student).filter(Student.email == email).first()
				if not queryStu:
					return "Please register an account" + home()
				if queryStu:
					#if (bcrypt.hashpw(password.encode('utf8'), queryStu.salt) == queryStu.password):
					if (bcrypt.hashpw(password.encode('utf8'), queryStu.salt.encode('utf8')) == queryStu.password):
						session['username'] = queryStu.pid
						session['logged_in'] = True
                                                return "Welcome student! <script>window.location = '/dashboard';</script>"
					else: 
						return "Incorrect password" + home()
				else:
					return "Email credential not found. Please register before logging in." + home()

		elif not request.form['password']:
				return "Password input cannot be left blank" + home()
	else:
			return "Login credential cannot be left blank." + home()
	
	s.close()
	return home()

@app.route('/register')
def render_register():
	if not session.get('logged_in'):
		return render_template('register.html')
	else:
		return "You are already logged in as " + session['username'] + "." + home()

@app.route('/register', methods=['POST'])
def register():
	userType = request.form['userType']
	#print ("----------------------------------------" + userType)
	if userType == "1":
		#print ("++++----------------------------------------" + userType)
		profEmail = html.escape(request.form['email'])

		profPid = html.escape(request.form['pid'])
		profFirstName = html.escape(request.form['FirstName'])
		profLastName = html.escape(request.form['LastName'])
		if not email_regex.match(profEmail):
			return "Please enter a valid email structure." + render_register()

		emailCheck = profEmail[-8:]
		if not emailCheck == '@ucf.edu':
			return "Email must end in '@ucf.edu'" + render_register()

		ppw = request.form['password'].encode('utf8')
		cpw = request.form['confirm'].encode('utf8')
		Session = sessionmaker(bind=engine)
		s = Session()
		query = s.query(Professor).get(profEmail)
		if query:
			return "This professor has already been registered." + render_register()
		elif not ppw:
			return "Please enter a password." + render_register()
		elif not profFirstName or not profLastName:
			return "Please enter your First and Last name." + render_register()
		elif ppw == cpw:
			salt = bcrypt.gensalt()
			newProf = Professor(email=profEmail,pid=profPid, name=profFirstName,lastName=profLastName, password=bcrypt.hashpw(ppw, salt), salt=salt) 
			s.add(newProf)
			s.commit()
			s.close()
			session['username'] = profEmail
			session['logged_in'] = True
			return "You are now registered and logged in as " + session['username'] + ". <script>window.location = '/dashboard';</script>"
		else:
			return "Your password does not match!" + render_template('register.html')
		
	elif userType == "0":
		#print ("----------------------------------------" + userType)
		stuEmail = html.escape(request.form['email'])
		if not email_regex.match(stuEmail):
			return "Please enter a valid email structure." + render_register()

		emailCheck = stuEmail[-16:]
		if not emailCheck == '@knights.ucf.edu':
			return "Email must end in '@knights.ucf.edu'" + render_register()

		pid = html.escape(request.form['pid'])
		stuFirstName = html.escape(request.form['FirstName'])
		stuLastName = html.escape(request.form['LastName'])
		if not pid.isnumeric():
			return "Please input the numeric portion of your PID." + render_register()
		spw = request.form['password'].encode('utf8')
		cpw = request.form['confirm'].encode('utf8')
		Session = sessionmaker(bind=engine)
		s = Session()
		check = s.query(Student).filter(Student.email == stuEmail).first()
		if check:
			return "This email has already been registered." + render_register()
		query = s.query(Student).get(pid)
		if query:
			return "This student has already been registered." + render_register()
		elif not spw:
			return "Please enter a password." + render_register()
		elif not stuFirstName or not stuLastName:
			return "Please enter your First and Last name." + render_register()
		elif spw == cpw:
			salt = bcrypt.gensalt()
			newStu = Student(pid=pid,email=stuEmail, name=stuFirstName,lastName=stuLastName, password=bcrypt.hashpw(spw, salt), salt = salt)
			s.add(newStu)
			s.commit()
			s.close()
			session['username'] = pid
			session['logged_in'] = True
			return "You are now registered and logged in as " + session['username'] + ". <script>window.location = '/dashboard';</script>"
		else:
			return "Your password does not match!" + render_template('register.html')
	else:
		return "Please enter a credential before registering." + render_register()

@app.route('/dashboard')
def dashboard():
	if session['username'].isnumeric():
		return render_template('studash.html')
	else:
		return render_template('dashboard.html')

	
@app.route('/addClass', methods=['POST'])
def addClass():
	professorEmail = session['username']
	className = html.escape(request.form['className'])
	classCode = html.escape(request.form['classCode'])
	Session = sessionmaker(bind=engine)
	s = Session()
	newClass = Class(professorEmail=professorEmail,className=className, classCode=classCode)
	profQ = s.query(Professor).get(professorEmail)
	profQ.classes.append(newClass)
	s.add(newClass)
	s.commit()
	s.close()
	return render_template('dashboard.html') + ". <script>window.location = '/dashboard';</script>"
	
@app.route('/getClasses', methods=['POST'])
def getClasses():
	Session = sessionmaker(bind=engine)
	s = Session()
	profQ = s.query(Professor).get(session['username'])
	clist = profQ.classes
	classes = []
	for c in clist:
		newClass = {"classID": c.classID, "professorEmail": c.professorEmail, "className": c.className, "classCode": c.classCode}
		classes.append(newClass)
	#viewAttendance()
	s.close()
	return(json.dumps(classes))
	

@app.route('/myclass', methods=['POST'])
def getClass():
	login()
        Session = sessionmaker(bind=engine)
        s = Session()
        profQ = s.query(Professor).get(session['username'])
        clist = profQ.classes
        classes = []
	ret = ""
        for c in clist:
                newClass = {"classID": c.classID, "professorEmail": c.professorEmail, "className": c.className, "classCode": c.classCode}
                classes.append(newClass)
		ret = ret + str(c.classID) + " " + str(c.className) + "\n"
        #viewAttendance()
        s.close()
        return(ret)


@app.route('/editProfData', methods=['POST'])
def editProfData():
	credential = session['username']
	name = html.escape(request.form['name'])
	lastName = html.escape(request.form['lastName'])
	Session = sessionmaker(bind=engine)
	s = Session()
	profQ = s.query(Professor).get(credential)
	if name:
		profQ.name = name
	if lastName:
		profQ.lastName = lastName
	s.add(profQ)
	s.commit()
	s.close()
	return render_template('dashboard.html') + ". <script>window.location = '/dashboard';</script>"
	
@app.route('/viewAttendance', methods=['POST'])
def viewAttendance():
	credential = session['username']
	request_json = request.get_json()
	try:
		classNum = request_json.get('classID')
	except:
		return render_template('dashboard.html')
	Session = sessionmaker(bind=engine)
	s = Session()
	attendance = []
	## Used for getting all students enrolled in the class, add .count() to end of query
	numStu = s.query(Student).join(association_table).join(Class).filter(association_table.c.classes_classID == classNum).count()
	#query = s.query(Class).get(classNum)
	#students = query.students
	## Used for getting all students checked in add .count() to end of query
	checkedInStuCount = s.query(CheckIn).filter(CheckIn.classID == classNum).count()
	
	#olddate = 0

	date = 0
	checkedInStu = s.query(CheckIn).filter(CheckIn.classID == classNum).all()	
	for q in checkedInStu:
		#olddate = date
		if q.date != date:
			if date != 0:
				attendance.append(attendanceDate)
			date = q.date
			attendanceDate = {"date": q.date.strftime('%Y-%m-%d'), "dateArray": []}
			#print(date)
		studentInfo = [s.query(Student).get(q.studentID)]
		for x in studentInfo:
			newAttendance = {"checkInID": q.checkinID,"pid": x.pid, "studentFirstName": x.name , "studentLastName": x.lastName}
			attendanceDate['dateArray'].append(newAttendance)
	if date == 0:
		return (json.dumps(""))
	attendance.append(attendanceDate)		
			
	s.close()
	#print(attendance)
	#test()
	return (json.dumps(attendance))
	#jsonString = json.dumps(attendance)
	#return render_template('dashboard.html', entities = jsonString)
	
@app.route('/recordAttendance', methods=['POST'])
def recordAttendance():
	login()
	classID = request.form['classID']
	macs = request.form['addresses']
	macs = macs.replace("[", "")
	macs = macs.replace("]", "")
	macs = macs.replace('"', "")
	macs = macs.split(",")
	Session = sessionmaker(bind=engine)
	s = Session()
	query = s.query(Class).get(classID)
	students = query.students
	for stu in students:
		if stu.MAC in macs:
			cin = CheckIn(studentID=stu.pid, classID=classID)
			s.add(cin)
			s.commit()
	return ""

### Student Routes below
@app.route('/studash')
def stuDash():
	if session['username'].isnumeric():
		return render_template('studash.html')
	else:
		return render_template('dashboard.html')
		
@app.route('/editStuData', methods=['POST'])
def editStuData():
	credential = session['username']
	name = html.escape(request.form['name'])
	lastName = html.escape(request.form['lastName'])
	mac = html.escape(request.form['mac'])
	Session = sessionmaker(bind=engine)
	s = Session()
	stuQ = s.query(Student).get(credential)
	if name:
		stuQ.name = name

	if lastName:
		stuQ.lastName = lastName
	
	if mac:
		mac = mac.upper()
		if mac_regex.match(mac):
			stuQ.MAC = mac
		else:
			return "Must be a valid MAC address." + render_template('studash.html')

	s.add(stuQ)
	s.commit()
	s.close()
	return render_template('studash.html')
	
@app.route('/getAllClasses', methods=['POST'])
def getAllClasses():
	studentID = session['username']
	Session = sessionmaker(bind=engine)
	s = Session()
	classcheck = []
	queryCheck = s.query(Class).join(association_table).join(Student).filter(association_table.c.students_pid == studentID)
	for z in queryCheck:
		classcheck.append(z.classID)
	query = s.query(Class).all()
	classes = []
	for q in query:
		queryName = s.query(Professor).filter(Professor.email == q.professorEmail)
		for x in queryName:
			if q.classID not in classcheck:
				newClass = {"classID": q.classID, "professorEmail": q.professorEmail, "className": q.className, "classCode": q.classCode, "professorName": x.name}
				classes.append(newClass)
	s.close()
	return (json.dumps(classes))

@app.route('/joinClass', methods=['POST'])
def joinClass():
	studentID = session['username']
	classID = html.escape(request.form['classID'])
	Session = sessionmaker(bind=engine)
	s = Session()
	classObj = s.query(Class).get(classID)
	stuQ = s.query(Student).get(studentID)
	stuQ.classes.append(classObj)
	s.add(stuQ)
	s.commit()
	s.close()
	return render_template('studash.html')
	
@app.route('/myClasses', methods=['POST'])
def myClasses():
	#login()
	studentID = session['username']
	Session = sessionmaker(bind=engine)
	s = Session()
	#stuQ = s.query(Student).get(studentID)
	#classes = stuQ.classes
	myClass = []
	#for c in classes:
	#	newClass = {"className": c.className, "classCode": c.classCode}
	#	myClass.append(newClass)
	query = s.query(Class).join(association_table).join(Student).filter(association_table.c.students_pid == studentID)
	for q in query:
		classQuery = [s.query(Class).get(q.classID)]
		for x in classQuery:
			newClass = {"className": x.className, "classCode": x.classCode}
			myClass.append(newClass)
	
	s.close()
	return (json.dumps(myClass))

@app.route('/umac', methods=['POST'])
def updateMAC():
	login()
	pid = session['username']
	mac = request.form['mac']
	if mac_regex.match(mac):
		Session = sessionmaker(bind=engine)
		s = Session()
		stu = s.query(Student).get(pid)
		stu.MAC = mac
		s.add(stu)
		s.commit()
		s.close()
		return "MAC updated! Yippee ki-yay! https://www.youtube.com/watch?v=VGF4ibgcHQE"
	else:
		return "Invalid MAC address."
	
app.secret_key = os.urandom(12)
#app.run(debug=True)
app.run(debug=True, host='127.0.0.1', port=6969)
