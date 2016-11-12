package me.tigerhix.logic

import tornadofx.getProperty
import tornadofx.property
 
class Student(
        from: String? = null,
        number: String? = null,
        name: String? = null,
        group: String? = null,
        checkIn: String? = "Not yet",
        checkOut: String? = "Not yet"
) {
    
    var from by property(from)
    var number by property(number)
    var name by property(name)
    var group by property(group)
    var checkIn by property(checkIn)
    var checkOut by property(checkOut)
    
    fun fromProperty() = getProperty(Student::from)
    fun numberProperty() = getProperty(Student::number)
    fun nameProperty() = getProperty(Student::name)
    fun groupProperty() = getProperty(Student::group)
    fun checkInProperty() = getProperty(Student::checkIn)
    fun checkOutProperty() = getProperty(Student::checkOut)
    
}