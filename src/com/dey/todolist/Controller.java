package com.dey.todolist;

import com.dey.todolist.datamodel.ToDoData;
import com.dey.todolist.datamodel.ToDoItem;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

public class Controller {

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    private ListView<ToDoItem> toDoListView;

    @FXML
    private TextArea itemDetailsTextArea;

    @FXML
    private Label deadlineLabel;

    @FXML
    private ContextMenu listContextMenu;

    @FXML
    private ToggleButton filterToggleButton;

    private FilteredList<ToDoItem> filteredList;
    private Predicate<ToDoItem> wantAllItems;
    private Predicate<ToDoItem> wantTodaysItems;



    public void initialize() {

        listContextMenu = new ContextMenu();
        MenuItem deleteMenuItem = new MenuItem("Delete");
        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ToDoItem item = toDoListView.getSelectionModel().getSelectedItem();
                deleteItem(item);
            }
        });

        listContextMenu.getItems().addAll(deleteMenuItem);

        toDoListView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<ToDoItem>() {
            @Override
            public void changed(ObservableValue<? extends ToDoItem> observable, ToDoItem oldValue, ToDoItem newValue) {
                if (newValue != null) {
                    ToDoItem item = toDoListView.getSelectionModel().getSelectedItem();
                    itemDetailsTextArea.setText(item.getDetails());
                    DateTimeFormatter df = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                    deadlineLabel.setText(df.format(item.getDeadline()));
                }
            }
        });

        wantAllItems = new Predicate<ToDoItem>() {
            @Override
            public boolean test(ToDoItem toDoItem) {
                return true;
            }
        };

        wantTodaysItems = new Predicate<ToDoItem>() {
            @Override
            public boolean test(ToDoItem toDoItem) {
                return toDoItem.getDeadline().equals(LocalDate.now());
            }
        };

        filteredList = new FilteredList<ToDoItem>(ToDoData.getInstance().getToDoItems(), wantAllItems);

        SortedList<ToDoItem> sortedList = new SortedList<ToDoItem>(filteredList,
                new Comparator<ToDoItem>() {
                    @Override
                    public int compare(ToDoItem o1, ToDoItem o2) {
                        return o1.getDeadline().compareTo(o2.getDeadline());
                    }
                }
        );

        //toDoListView.setItems(ToDoData.getInstance().getToDoItems());
        toDoListView.setItems(sortedList);
        toDoListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        toDoListView.getSelectionModel().selectFirst();

        toDoListView.setCellFactory(new Callback<ListView<ToDoItem>, ListCell<ToDoItem>>() {
            @Override
            public ListCell<ToDoItem> call(ListView<ToDoItem> param) {
                ListCell<ToDoItem> cell = new ListCell<ToDoItem>() {
                    @Override
                    protected void updateItem(ToDoItem item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty)
                            setText(null);
                        else {
                            setText(item.getShortDescription());
                            if (item.getDeadline().isBefore((LocalDate.now().plusDays(1))))
                                setTextFill(Color.RED);
                            else if (item.getDeadline().equals(LocalDate.now().plusDays(1)))
                                setTextFill(Color.BLUE);
                        }
                    }
                };

                cell.emptyProperty().addListener(
                        // Lambda
                        (obs, wasEmpty, isNowEmpty) -> {
                            if (isNowEmpty)
                                cell.setContextMenu(null);
                            else
                                cell.setContextMenu(listContextMenu);
                        }
                );

                return cell;
            }
        });
    }

    @FXML
    public void showNewItemDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Task");

        // Make Main Border owner of the Dialog Pane
        dialog.initOwner(mainBorderPane.getScene().getWindow());

        // Store Dialog FXML path
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("toDoItemDialog.fxml"));

        // Attach FXML file in path
        try {
            dialog.getDialogPane().setContent(fxmlLoader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            DialogController controller = fxmlLoader.getController();
            ToDoItem newItem = controller.processResults();
            toDoListView.getSelectionModel().select(newItem);
        }
        else {
            System.out.println("cancel pressed");
        }
    }

    @FXML
    public void handleKeyPressed(KeyEvent keyEvent) {
        ToDoItem selectedItem = toDoListView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            if (keyEvent.getCode().equals(KeyCode.DELETE) || keyEvent.getCode().equals(KeyCode.BACK_SPACE)) {
                deleteItem(selectedItem);
            }
        }
    }

    @FXML
    public void handleClickListView() {
        ToDoItem item = toDoListView.getSelectionModel().getSelectedItem();
        itemDetailsTextArea.setText(item.getDetails());
        deadlineLabel.setText(item.getDeadline().toString());
    }

    private void deleteItem(ToDoItem item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete To Do Item");
        alert.setHeaderText("Delete Item: " + item.getShortDescription());
        alert.setContentText("Are You Sure?");
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            ToDoData.getInstance().deleteItem(item);
        }
    }

    @FXML
    public void handleFilterButton() {
        ToDoItem prevSelectedItem = toDoListView.getSelectionModel().getSelectedItem();

        if (filterToggleButton.isSelected()) {
            filteredList.setPredicate(wantTodaysItems);

            if (filteredList.isEmpty()) {
                itemDetailsTextArea.clear();
                deadlineLabel.setText("");
            }

            else if (filteredList.contains(prevSelectedItem)) {
                toDoListView.getSelectionModel().select(prevSelectedItem);
            }

            else {
                toDoListView.getSelectionModel().selectFirst();
            }
        }

        else {
            filteredList.setPredicate(wantAllItems);
            toDoListView.getSelectionModel().select(prevSelectedItem);
        }
    }

    @FXML
    public void handleExit() {
        Platform.exit();
    }
}