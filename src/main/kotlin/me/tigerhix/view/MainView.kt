package me.tigerhix.view

import com.sun.javafx.scene.control.skin.TableViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.layout.BorderPane
import javafx.scene.text.Text
import javafx.util.Duration
import me.tigerhix.External
import me.tigerhix.logic.Student
import me.tigerhix.logic.StudentModel
import org.apache.commons.io.FileUtils
import org.controlsfx.control.Notifications
import tornadofx.*
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.comparisons.compareBy
import kotlin.comparisons.thenBy

val TAB = "	"
val DATA_FILE = File("data.txt")
val DATA_BAK_FILE = File("data.txt.bak")
val classOrder = "WYCKSJ"
val classMapping = mapOf(1 to 'W', 2 to 'Y', 3 to 'C', 4 to 'K', 5 to 'S', 6 to 'J')
val studentPlaceholder = Student("N/A", "N/A", "N/A", "N/A", "N/A", "N/A")

class MainView : View("Organizer") {

    override val root = BorderPane()

    var students: ObservableList<Student> = ArrayList<Student>().observable()

    var studentTable: TableView<Student> by singleAssign()
    val studentModel = StudentModel(studentPlaceholder)

    var queryTextField: TextField by singleAssign()
    var queryResponseText: Text by singleAssign()

    var query: String = ""
    var queryDelegate: String by property(query)
    fun queryProperty() = getProperty(MainView::queryDelegate)

    var mode: Mode = Mode.CHECK_IN

    init {
        try { addStageIcon(Image("file:icon.png")) } catch (ignore: Exception) {}
        if (!DATA_FILE.exists()) {
            val alert = Alert(AlertType.ERROR)
            alert.title = "Oops!"
            alert.headerText = "The data file does not exist."
            alert.contentText = "Call the IT guy for help!"
            alert.showAndWait()
            Platform.exit()
        } else {
            students = External.import(DATA_FILE).observable()
        }
        with(root) {
            left = form {
                fieldset("Lookup", FontAwesomeIconView(FontAwesomeIcon.SEARCH)) {
                    field("Mode") {
                        togglegroup {
                            radiobutton("Check in") { userData = Mode.CHECK_IN; isSelected = true }
                            radiobutton("Check out") { userData = Mode.CHECK_OUT }
                        }.selectedToggleProperty().addListener { observable, oldValue, newValue ->
                            mode = newValue.userData as Mode
                        }
                    }
                    field("Query") {
                        textfield(queryProperty()) {
                            queryTextField = this
                            textProperty().addListener { observableValue, oldValue, newValue ->
                                onQuery(newValue)
                            }
                        }.apply {
                            promptText = "e.g. 4W01, 4W1 or 411"
                            setOnAction { event ->
                                onAction()
                            }
                        }
                    }
                    text { queryResponseText = this }
                }
            }.apply { minWidth = 250.0; prefWidth = 250.0; maxWidth = 250.0 }
            center = form {
                fieldset("Record", FontAwesomeIconView(FontAwesomeIcon.USER)) {
                    field("Class") {
                        text(studentModel.from)
                    }
                    field("No.") {
                        text(studentModel.number)
                    }
                    field("Name") {
                        text(studentModel.name)
                    }
                    field("Group") {
                        text(studentModel.group)
                    }
                    field("Check In") {
                        val checkInField = textfield(studentModel.checkIn)
                        button("Now") {
                            this.minWidth = 50.0
                            setOnAction {
                                checkInField.text = getNow()
                            }
                        }
                    }
                    field("Check Out") {
                        val checkOutField = textfield(studentModel.checkOut)
                        button("Now") {
                            this.minWidth = 50.0
                            setOnAction {
                                checkOutField.text = getNow()
                            }
                        }
                    }
                    field {
                        button("Save") {
                            prefWidth = 70.0
                            disableProperty().bind(studentModel.dirtyStateProperty().not())
                            setOnAction {
                                commitStudentModel()
                            }
                        }
                        button("Revert") {
                            prefWidth = 70.0
                            setOnAction {
                                studentModel.rollback()
                            }
                        }
                    }
                }
            }.apply { minWidth = 250.0; prefWidth = 250.0; maxWidth = 250.0 }
            right = tableview(students) {
                studentTable = this
                column("Class", Student::fromProperty)
                column("No.", Student::numberProperty)
                column("Name", Student::nameProperty)
                column("Group", Student::groupProperty)
                column("Check In", Student::checkInProperty)
                column("Check Out", Student::checkOutProperty)

                studentModel.rebindOnChange(this) { selectedStudent ->
                    student = selectedStudent ?: studentPlaceholder
                }
            }.apply { minWidth = 480.0; prefWidth = 480.0; maxWidth = 480.0 }
        }
        Platform.runLater { queryTextField.requestFocus() }
        if (DATA_FILE.exists()) {
            FileUtils.copyFile(DATA_FILE, DATA_BAK_FILE)
            save()
        }
    }

    private fun commitStudentModel() {
        studentModel.commit()
        save()
    }

    fun onQuery(query: String) {
        if (query.isEmpty()) return
        val query = query.toUpperCase()
        var response = ""
        var invalidFormat = (query.isNotEmpty() && !query[0].isDigit())
        invalidFormat = invalidFormat || (query.isNotEmpty() && query[0].isDigit() && query[0].toString().toInt() !in 1..6)
        invalidFormat = invalidFormat || (query.length > 1 && !query[1].isLetterOrDigit())
        invalidFormat = invalidFormat || (query.length > 1 && query[1].isDigit() && query[1].toString().toInt() !in 1..6)
        invalidFormat = invalidFormat || (query.length > 1 && query[1].isLetter() && query[1] !in classMapping.values)
        invalidFormat = invalidFormat || (query.length > 2 && !query[2].isDigit())
        invalidFormat = invalidFormat || (query.length > 3 && !query[3].isDigit())
        invalidFormat = invalidFormat || (query.length > 4)
        if (invalidFormat) {
            response = "Invalid format."
            studentTable.selectionModel.clearSelection()
        } else {
            if (query.length < 3) {
                studentTable.selectionModel.clearSelection()
            } else {
                val form = query[0].toString()
                var className = query[1]
                if (className.isDigit()) {
                    className = classMapping[className.toString().toInt()] ?: 'Z'
                }
                val from = form + className
                val number = query.substring(2, query.length).toInt().toString()

                var result: Student? = null
                for (student in students) {
                    if (student.from == from && student.number == number) {
                        result = student
                        break
                    }
                }

                if (result != null) {
                    response = "Result found and selected."
                    studentTable.selectionModel.select(result)
                    // Show selected row in the middle
                    Platform.runLater {
                        val ts = studentTable.skin as TableViewSkin<*>
                        val vf = ts.children[1] as VirtualFlow<*>

                        val first = vf.firstVisibleCellWithinViewPort.index
                        val last = vf.lastVisibleCellWithinViewPort.index

                        if (studentTable.selectionModel.selectedIndex - (last - first) / 2 >= 0) {
                            vf.scrollTo(studentTable.selectionModel.selectedIndex - (last - first) / 2)
                        }
                    }
                } else {
                    response = "Result not found."
                    studentTable.selectionModel.clearSelection()
                }
            }
        }
        queryResponseText.text = response
    }

    fun onAction() {
        studentTable.selectedItem?.apply {
            val formattedTime = getNow()
            var message: String = ""
            when (mode) {
                Mode.CHECK_IN -> {
                    checkIn = formattedTime
                    message = "Checked in!"
                }
                Mode.CHECK_OUT -> {
                    checkOut = formattedTime
                    message = "Checked out!"
                }
            }
            studentModel.rollback()
            Notifications.create()
                    .title(message)
                    .text("$from ($number) $name")
                    .position(Pos.BOTTOM_RIGHT)
                    .hideAfter(Duration.seconds(5.0))
                    .show()
            queryTextField.clear()
            commitStudentModel()
        }
    }

    fun save() {
        val builder = StringBuilder()
        val sortedStudents = students.sortedWith(
                compareBy<Student> { it.from[0] }
                        .thenBy(Comparator { a, b -> classOrder.indexOf(a) - classOrder.indexOf(b) }) { it.from[1] }
                        .thenBy { it.number.toInt() }
        )
        builder
                .append("Class")
                .append(TAB)
                .append("No.")
                .append(TAB)
                .append("Name")
                .append(TAB)
                .append("Group")
                .append(TAB)
                .append("Check In")
                .append(TAB)
                .append("Check Out")
                .append("\n")
        for (student in sortedStudents) {
            builder
                    .append(student.from)
                    .append(TAB)
                    .append(student.number)
                    .append(TAB)
                    .append(student.name)
                    .append(TAB)
                    .append(student.group)
                    .append(TAB)
                    .append(student.checkIn)
                    .append(TAB)
                    .append(student.checkOut)
                    .append("\n")
        }
        PrintWriter(DATA_FILE).use {
            out ->
            out.println(builder.toString())
        }
    }

    fun getNow(): String = SimpleDateFormat("HH:mm:ss").format(Date())

}

enum class Mode {

    CHECK_IN, CHECK_OUT

}