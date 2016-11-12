package me.tigerhix.logic

import tornadofx.ViewModel

class StudentModel(var student: Student) : ViewModel() {

    val from = bind { student.fromProperty() }
    val number = bind { student.numberProperty() }
    val name = bind { student.nameProperty() }
    val group = bind { student.groupProperty() }
    val checkIn = bind { student.checkInProperty() }
    val checkOut = bind { student.checkOutProperty() }

}