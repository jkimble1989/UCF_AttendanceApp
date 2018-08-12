import datetime
from sqlalchemy import *
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship, backref


Base = declarative_base()
engine = create_engine('sqlite:///students.db', echo=True)

# For further reading
# http://docs.sqlalchemy.org/en/latest/orm/basic_relationships.html

association_table = Table('association', Base.metadata,
		Column('students_pid', String, ForeignKey('students.pid')),
		Column('classes_classID', String, ForeignKey('classes.classID'))
		)

class Student(Base):
	__tablename__ = 'students'
	pid = Column(String, primary_key=True)
	name = Column(String)
	lastName = Column(String)
	email = Column(String)
	password = Column(String)
	salt = Column(String)
	MAC = Column(String)
	# This creates a many to many relationship between students and classes,
	# by appending classes onto this list, students are automatically added
	# to a list of enrolled students for that class.
	# Alternatively, appending a student onto a classes list of students
	# will automatically add that class to the students list of classes
	classes = relationship("Class", secondary=association_table, backref="students")
	

class Class(Base):
	__tablename__ = 'classes'

	classID = Column(Integer, primary_key=True, autoincrement=True)
	professorEmail = Column(Integer, ForeignKey('professors.email'), nullable=False)
	className = Column(String)
	classCode = Column(String)
	dateCreated = Column(Date, default=datetime.date.today)
	professor = relationship("Professor", back_populates="classes")


class Professor(Base):
	__tablename__ = 'professors'

	#professorID = Column(Integer, autoincrement=True)
	pid = Column(String)
	name = Column(String)
	lastName = Column(String)
	email = Column(String, primary_key=True)
	password = Column(String)
	salt = Column(String)
	# This relationship allows for us to append on to the professors class list, and for
	# the class to automatically know the professor teaching it.
	classes = relationship("Class", back_populates="professor")


class CheckIn(Base):
	__tablename__ = 'checkins'
	checkinID = Column(Integer, autoincrement=True, primary_key=True)
	studentID = Column(String, ForeignKey('students.pid'), nullable=False)
	classID = Column(String, ForeignKey('classes.classID'), nullable=False)
	date = Column(Date, default=datetime.date.today)


Base.metadata.create_all(engine)