package org.example.disktest2.file;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import org.example.disktest2.file.entity.FileModel;
import org.example.disktest2.file.Controller.OSManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class TestFileSystem implements Initializable {
    @FXML
    private TextArea inputArea;
    @FXML
    private GridPane diskPane;
    @FXML
    private GridPane FatTitlePane;
    @FXML
    private GridPane FatPane;
   /* public static void main(String[] args) {
        try{
            OSManager manager = new OSManager();
            menu(manager);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/

    @FXML
    private TreeView<FileModel> dirTree;   // 挂实体，方便右键、重命名取数据

    private String getPath(FileModel fm) {
        StringBuilder sb = new StringBuilder(fm.getName());
        FileModel p = fm.getFather();
        while (p != null) {
            sb.insert(0, p.getName() + "/");
            p = p.getFather();
        }
        return sb.toString();
    }

    private void addContextMenu(OSManager osManager) {
        // 创建右键菜单
        ContextMenu contextMenu = new ContextMenu();

        // 新建文件夹菜单项
        MenuItem newDirItem = new MenuItem("新建文件夹");
        newDirItem.setOnAction(event -> {
            TreeItem<FileModel> selectedItem = dirTree.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                showAlert("提示", "请先选择一个目录");
                return;
            }

            FileModel parentDir = selectedItem.getValue();
            if (parentDir.getAttr() != 3) { // 确保选中的是目录
                showAlert("错误", "只能在目录中创建文件夹");
                return;
            }

            // 弹出输入对话框获取文件夹名称
            TextInputDialog dialog = new TextInputDialog("新文件夹");
            dialog.setTitle("新建文件夹");
            dialog.setHeaderText("请输入文件夹名称:");
            dialog.setContentText("名称:");
            dialog.showAndWait().ifPresent(dirName -> {
                if (dirName.isEmpty()) {
                    showAlert("错误", "文件夹名称不能为空");
                    return;
                }

                // 构建完整路径
                String parentPath = buildFullPath(parentDir);
                String fullPath = parentPath + "\\" + dirName;
                int result = osManager.createDirectoryByPath(fullPath);

                // 根据操作结果显示提示
                switch (result) {
                    case 0:
                        showAlert("成功", "文件夹创建成功");
                        refreshTree(osManager);
                        checkDisk(osManager);
                        checkFat(osManager);
                        break;
                    case 1:
                        showAlert("错误", "文件夹已存在");
                        break;
                    case 2:
                        showAlert("错误", "磁盘空间不足");
                        break;
                    case 3:
                        showAlert("错误", "路径错误");
                        break;
                }
            });
        });

        // 新建文件菜单项
        MenuItem newFileItem = new MenuItem("新建文件");
        newFileItem.setOnAction(event -> {
            TreeItem<FileModel> selectedItem = dirTree.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                showAlert("提示", "请先选择一个目录");
                return;
            }

            FileModel parentDir = selectedItem.getValue();
            if (parentDir.getAttr() != 3) { // 确保选中的是目录
                showAlert("错误", "只能在目录中创建文件");
                return;
            }

            // 弹出输入对话框获取文件名称
            TextInputDialog dialog = new TextInputDialog("新文件.txt");
            dialog.setTitle("新建文件");
            dialog.setHeaderText("请输入文件名称:");
            dialog.setContentText("名称:");
            dialog.showAndWait().ifPresent(fileName -> {
                if (fileName.isEmpty()) {
                    showAlert("错误", "文件名称不能为空");
                    return;
                }

                // 构建完整路径
                String parentPath = buildFullPath(parentDir);
                String fullPath = parentPath + "\\" + fileName;
                int result = osManager.createFileByPath(fullPath);

                // 根据操作结果显示提示
                switch (result) {
                    case 0:
                        showAlert("成功", "文件创建成功");
                        refreshTree(osManager);
                        checkDisk(osManager);
                        checkFat(osManager);
                        break;
                    case 1:
                        showAlert("错误", "文件已存在");
                        break;
                    case 2:
                        showAlert("错误", "磁盘空间不足");
                        break;
                    case 3:
                        showAlert("错误", "路径错误");
                        break;
                }
            });
        });

        // 删除菜单项
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(event -> {
            // 获取选中节点
            TreeItem<FileModel> selectedItem = dirTree.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                showAlert("提示", "请选择要删除的项目");
                return;
            }
            FileModel target = selectedItem.getValue();
            String targetName = target.getName();
            FileModel parent = target.getFather();

            // 构建准确路径（
            String fullPath = buildFullPath(target);

            // 确认删除
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setHeaderText("确定删除「" + targetName + "」吗？");
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        //根据类型调用对应删除方法（避免调用错误的底层方法）
                        int res = (target.getAttr() == 3)
                                ? osManager.removeDirectoryByPath(fullPath)
                                : osManager.deleteFileByPathAndFile(target,fullPath);

                        if (res == 0) {
                            showAlert("成功", "删除成功");
                            checkDisk(osManager);
                            checkFat(osManager);
                            refreshTree(osManager);//目录树刷新
                        } else {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("删除失败");
                            alert.setHeaderText("目录不为空，请检查。");
                            alert.showAndWait();
                        }
                    } catch (Exception e) {
                        showAlert("错误", "删除失败：" + e.getMessage());
                    }
                }
            });
        });

        // 重命名菜单项
        MenuItem renameItem = new MenuItem("重命名");
        renameItem.setOnAction(event -> {
            TreeItem<FileModel> selectedItem = dirTree.getSelectionModel().getSelectedItem();
            if (selectedItem == null) {
                showAlert("提示", "请先选择要重命名的项目");
                return;
            }

            FileModel target = selectedItem.getValue();
            FileModel parent = target.getFather();
            String fullPath = buildFullPath(target);
            osManager.nowCatalog = parent;
            if (parent == null) {
                showAlert("错误", "根目录不能重命名");
                return;
            }

            // 弹出输入对话框获取新名称
            TextInputDialog dialog = new TextInputDialog(target.getName());
            dialog.setTitle("重命名");
            dialog.setHeaderText("请输入新名称:");
            dialog.setContentText("名称:");
            dialog.showAndWait().ifPresent(newName -> {
                if (newName.isEmpty()) {
                    showAlert("错误", "名称不能为空");
                    return;
                }

                if (parent.subMap.containsKey(newName)) {
                    showAlert("错误", "名称已存在");
                    return;
                }

                // 执行重命名操作
                String oldName = target.getName();
                osManager.reName(target,fullPath,oldName, newName);
                showAlert("成功", "重命名成功");
                checkDisk(osManager);
                checkFat(osManager);
                refreshTree(osManager);//目录树刷新
            });
        });

        // 将菜单项添加到菜单
        contextMenu.getItems().addAll(newDirItem, newFileItem, new SeparatorMenuItem(), deleteItem, renameItem);

        // 设置树视图的右键菜单
        dirTree.setContextMenu(contextMenu);

        //
        dirTree.setOnMouseClicked(event -> {
            if (event.getButton().equals(javafx.scene.input.MouseButton.SECONDARY)) {
                TreeItem<FileModel> selectedItem = dirTree.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    contextMenu.show(dirTree, event.getScreenX(), event.getScreenY());
                } else {
                    contextMenu.hide(); // 未选中项时隐藏
                }
            }
        });
    }

    private String buildFullPath(FileModel dir) {
        if (dir == null) {
            return "";
        }

        StringBuilder path = new StringBuilder();
        FileModel current = dir;

        while (current != null && current != osManager.getRoot()) { // 注意这里需要获取OSManager实例
            if (path.length() > 0) {
                path.insert(0, "\\");
            }
            path.insert(0, current.getName());
            current = current.getFather();
        }

        if (path.length() > 0) {
            path.insert(0, "\\");
        }
        path.insert(0, "root");

        return path.toString();
    }

    // 在TestFileSystem类中添加成员变量
    private OSManager osManager;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.osManager = new OSManager(); // 初始化并保存实例
        menu(osManager);
        initDisk();
        checkDisk(osManager);
        initFat(osManager);
        checkFat(osManager);
        initializeTree(osManager);
    }

    // 辅助方法：显示提示对话框
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /** 只在 UI 初始化时调用一次 */
    private void initializeTree(OSManager osManager) {
        TreeItem<FileModel> rootItem = createNode(osManager.getRoot());
        dirTree.setRoot(rootItem);
        dirTree.setShowRoot(true);
        addContextMenu(osManager);          // 右键菜单
    }

    /** 递归建节点 */
    private TreeItem<FileModel> createNode(FileModel model) {
        TreeItem<FileModel> item = new TreeItem<>(model);
        if (model.getAttr() == 3) {
            for (FileModel child : model.subMap.values()) {
                item.getChildren().add(createNode(child));
            }
        }
        return item;
    }

    public void refreshTree(OSManager osManager) {
        dirTree.setRoot(createNode(osManager.getRoot()));
    }

//    @Override
//    public void initialize(URL url, ResourceBundle resourceBundle) {
//        OSManager manager = new OSManager();
//        menu(manager);
//        initDisk();
//        checkDisk(manager);
//        initFat(manager);
//        checkFat(manager);
//        initializeTree(manager);
//    }

    public void menu(OSManager manager) {
        /*Scanner sc = new Scanner(System.in);
        String str = null;
        System.out.println("***********" + "欢迎使用文件模拟操作系统" + "***********");
        System.out.println();*/
        manager.showFile();

        inputArea.setPrefColumnCount(10);
        inputArea.setPrefRowCount(15);
        inputArea.setWrapText(true);
        inputArea.setEditable(true);
        inputArea.setPromptText("请输入命令");
        //System.out.println("请输入命令（输入help查看命令表）：");
        inputArea.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    // 当按下回车键时，执行的操作
                    String str = inputArea.getText();

                    if (str.equals("exit")) {
                        System.out.println("感谢您的使用！");
                    }

                    int res = 0;

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("指令错误");
                    alert.setHeaderText("您所输入的命令有误，请检查。");
                    alert.setContentText("请重新输入");
                    String[] strs = parseCommand(str);
                    switch (strs[0]) {
                        case "create":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.createFileByPath(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("当前目录下已存在同名文件，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2){
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("当前磁盘空间已满。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "delete":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.deleteFileByPath(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("文件不存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("该名称不是文件而是目录，请检查。");
                                    alert.showAndWait();
                                }
                                else if(res == 3) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("当前磁盘为空，请检查。");
                                    alert.showAndWait();
                                } else if(res == 4) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "type":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.typeFile(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("显示失败");
                                    alert.setHeaderText("文件不存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("显示失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3){
                                    alert.setTitle("显示失败");
                                    alert.setHeaderText("当前操作对象不是文件是目录，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "copy":
                            if (strs.length < 3) {
                                alert.showAndWait();
                            } else {
                                res = manager.copyFile(strs[1], strs[2]);
                                if(res == 1) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("源文件不存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("目标路径错误，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("磁盘空间不足，请检查。");
                                    alert.showAndWait();
                                } else if(res == 4) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("目标文件已存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 5) {
                                    alert.setTitle("拷贝失败");
                                    alert.setHeaderText("不能复制目录，只能复制文件。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "mkdir":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                res = manager.createDirectoryByPath(strs[1]);
                                if(res == 1) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("目录已存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("磁盘空间不足，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("创建失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        case "rmdir":
                            if (strs.length < 2) {
                                alert.showAndWait();
                            } else {
                                try {
                                    res = manager.removeDirectoryByPath(strs[1]);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                if(res == 1) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("目录不存在，请检查。");
                                    alert.showAndWait();
                                } else if(res == 2) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("目录不为空，请检查。");
                                    alert.showAndWait();
                                } else if(res == 3) {
                                    alert.setTitle("删除失败");
                                    alert.setHeaderText("路径错误，请检查。");
                                    alert.showAndWait();
                                }
                            }
                            break;
                        /*case "help": {
                            System.out.println("命令如下");
                            inputArea.setText("命令如下（空格不能省略）：");
                            break;
                        }*/
                        default: {
                            for (String st : strs)
                                System.out.println(st);
                            //System.out.println("您所输入的命令有误，请检查！");
                            alert.showAndWait();
                        }
                    }
                    //System.out.println("请输入命令（输入help查看命令表）：");
                    inputArea.clear();
                    inputArea.setPromptText("请输入命令：");
                    checkDisk(manager);
                    checkFat(manager);
                    refreshTree(manager);//目录树刷新
                }

            }
        });

    }

    // 解析命令格式，支持路径
    public static String[] parseCommand(String str) {
        String[] parts = str.trim().split("\\s+");
        ArrayList<String> result = new ArrayList<>();

        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    public void initDisk() {
        diskPane.getColumnConstraints().clear();
        diskPane.getRowConstraints().clear();
        diskPane.setHgap(8);
        diskPane.setVgap(10);

        for (int c = 0; c < 16; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 16.0);
            diskPane.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < 8; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(100.0 / 8.0);
            diskPane.getRowConstraints().add(rc);
        }

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 16; col++) {
                int blockIndex = row * 16 + col;
                StackPane cell = new StackPane();
                Label label = new Label(String.valueOf(blockIndex));
                label.setStyle("-fx-font-size: 12px; -fx-text-fill: #333333;");
                StackPane.setAlignment(label, Pos.CENTER);
                cell.getChildren().add(label);
                cell.setStyle("-fx-background-color: " + (blockIndex <= 1 ? "#ffd6d6" : "#d6ffd6") + "; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-radius: 4; -fx-border-radius: 4;");
                cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                diskPane.add(cell, col, row);
            }
        }
    }

    //把已被占用的磁盘块变为淡红色，空闲为淡绿色
    public void checkDisk(OSManager osManager) {
        int[] fat = osManager.getFat();
        for (int i = 0; i < 128; i++) {
            int row = i / 16;
            int col = i % 16;
            StackPane cell = getCell(diskPane, row, col);
            if (cell == null) continue;
            boolean used = (i <= 1) || fat[i] != 0;
            String color = used ? "#ffd6d6" : "#d6ffd6";
            cell.setStyle("-fx-background-color: " + color + "; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-background-radius: 4; -fx-border-radius: 4;");
        }
    }

    public StackPane getCell(GridPane gridPane, int row, int col) {
        for (Node node : gridPane.getChildren()) {
            Integer rowIndex = GridPane.getRowIndex(node);
            Integer colIndex = GridPane.getColumnIndex(node);
            if (rowIndex != null && colIndex != null && rowIndex.equals(row) && colIndex.equals(col)) {
                if (node instanceof StackPane) {
                    return (StackPane) node;
                }
            }
        }
        return null;
    }

    //初始化FAT表
    public void initFat(OSManager manager) {
        FatTitlePane.getChildren().clear();
        FatTitlePane.getColumnConstraints().clear();
        for (int c = 0; c < 3; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3.0);
            FatTitlePane.getColumnConstraints().add(cc);
        }
        Label h1 = new Label("index");
        Label h2 = new Label("next");
        Label h3 = new Label("name");
        for (Label h : new Label[]{h1,h2,h3}) {
            h.setStyle("-fx-font-weight: bold; -fx-padding: 6 8; -fx-text-fill: #333333;");
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER_LEFT);
        }
        FatTitlePane.add(h1,0,0);
        FatTitlePane.add(h2,1,0);
        FatTitlePane.add(h3,2,0);

        FatPane.getColumnConstraints().clear();
        for (int c = 0; c < 3; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3.0);
            FatPane.getColumnConstraints().add(cc);
        }

        FatPane.setVgap(2);
        FatPane.setHgap(0); // Remove horizontal gap to match header alignment
        renderFat(manager);
    }

    //FAT表状态改变
    public void checkFat(OSManager manager) {
        renderFat(manager);
    }

    private void renderFat(OSManager manager) {
        List<FileModel> totalFiles = manager.getTotalFiles();
        int[] fat = manager.getFat();

        String[] nameByIndex = new String[128];
        for (FileModel fileModel : totalFiles) {
            int i = fileModel.getStartNum();
            int count = 0;
            int maxIterations = 128;
            while (i >= 0 && i < 128 && count < maxIterations) {
                // Use the file name from FileModel
                nameByIndex[i] = fileModel.getName();
                if (fat[i] == -1) break;
                i = fat[i];
                count++;
            }
        }

        FatPane.getChildren().clear();
        nameByIndex[0] = "FAT";
        nameByIndex[1] = "FAT";
        for (int i = 0; i < 128; i++) {
            Label c0 = new Label(String.valueOf(i));
            Label c1 = new Label(String.valueOf(fat[i]));
            Label c2 = new Label(nameByIndex[i] == null ? "" : nameByIndex[i]);
            for (Label l : new Label[]{c0,c1,c2}) {
                l.setStyle("-fx-padding: 6 8; -fx-text-fill: #333333; -fx-background-color: white; -fx-border-color: #eeeeee; -fx-border-width: 0 0 1 0;");
                l.setMaxWidth(Double.MAX_VALUE);
                l.setAlignment(Pos.CENTER_LEFT);
                l.setPrefWidth(Region.USE_COMPUTED_SIZE);
            }
            FatPane.add(c0, 0, i);
            FatPane.add(c1, 1, i);
            FatPane.add(c2, 2, i);
        }
    }


}
