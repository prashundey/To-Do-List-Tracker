package com.dey.todolist;

import com.dey.todolist.datamodel.ToDoData;
import com.dey.todolist.datamodel.ToDoItem;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class DialogController {
    @FXML
    private TextField shortDescriptionField;

    @FXML
    private TextArea detailsArea;

    @FXML
    private DatePicker deadlinePicker;

    public ToDoItem processResults() {
        String title = shortDescriptionField.getText();
        String details = detailsArea.getText();
        LocalDate deadline = deadlinePicker.getValue();

        ToDoItem newItem = new ToDoItem(title, details,deadline);
        ToDoData.getInstance().addToDoItem(newItem);

        return newItem;
    }


}
