package me.tigerhix

import me.tigerhix.logic.Student
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

object External {

    fun import(file: File, excelRawContent: Boolean = false): List<Student> {
        val students = ArrayList<Student>()
        val lines = FileUtils.readLines(file, Charsets.UTF_8)
        lines.forEach {
            if (it.isEmpty() || it.startsWith("Class")) return@forEach
            val content = it.split("	")
            if ((excelRawContent && content.size == 8) || (!excelRawContent && content.size == 6)) {
                students += if (excelRawContent) Student(content[0], content[1], content[4], content[7])
                            else Student(content[0], content[1], content[2], content[3], content[4], content[5])
            } else {
                println("Invalid row: $it")
            }
        }
        return students
    }

}