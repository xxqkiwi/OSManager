package org.example.disktest2.layout;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.example.disktest2.entity.FileModel;
import org.example.disktest2.Controller.OSManager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        // 空对象直接返回空路径
        if (fm == null) {
            return "";
        }

        StringBuilder pathBuilder = new StringBuilder();
        FileModel current = fm;
        int maxDepth = 1000; // 最大层级限制（防止异常数据导致无限循环）
        int depth = 0;

        // 从当前节点向上遍历父目录，直到根目录（father为null）
        while (!current.toString().equals("root")) {
            // 超过最大层级，说明可能存在循环引用，返回错误信息
            if (depth++ >= maxDepth) {
                return "[ERROR] 路径层级超过上限，可能存在循环引用";
            }

            // 拼接当前节点名称（根目录直接添加，子节点前加斜杠）
            if (pathBuilder.length() == 0) {
                // 第一个节点（当前节点自身）直接添加名称
                pathBuilder.append(current.getName());
            } else {
                // 非第一个节点（父目录）添加到前面，并加斜杠分隔
                pathBuilder.insert(0, current.getName() + "/");
            }

            // 移动到父目录
            current = current.getFather();
        }
        //还要加上root
        pathBuilder.insert(0, current.getName() + "/");
        return pathBuilder.toString();
    }

    private ContextMenu itemMenu;

    private void addContextMenu(OSManager osManager) {
        // 初始化右键菜单
        itemMenu = new ContextMenu();

        // 1. 删除菜单项
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setOnAction(e -> {
            // 获取当前选中的节点
            TreeItem<FileModel> selectedItem = dirTree.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;

            FileModel targetFile = selectedItem.getValue();
            // 禁止删除根目录
            if (targetFile == osManager.getRoot()) {
                new Alert(Alert.AlertType.WARNING, "根目录不能删除").showAndWait();
                return;
            }

            FileModel parentDir = targetFile.getFather();
            // 获取目标文件的完整路径
            String targetPath = targetFile.getName();
            // 调用OSManager的删除方法
            int result;

                result = osManager.deleteFileByPathAndParentDir(targetPath,targetFile,parentDir);


            // 根据返回结果显示提示
            switch (result) {
                case 0:
                    // 删除成功，从树中移除节点
                    selectedItem.getParent().getChildren().remove(selectedItem);
                    new Alert(Alert.AlertType.INFORMATION, "删除成功").showAndWait();
                    break;
                case 1:
                    new Alert(Alert.AlertType.ERROR, "删除失败：文件不存在").showAndWait();
                    break;
                case 2:
                    new Alert(Alert.AlertType.ERROR, "删除失败：目录不为空").showAndWait();
                    break;
                case 3:
                    new Alert(Alert.AlertType.ERROR, "删除失败：磁盘为空").showAndWait();
                    break;
                case 4:
                    new Alert(Alert.AlertType.ERROR, "删除失败：路径错误").showAndWait();
                    break;
                case 5:
                    new Alert(Alert.AlertType.ERROR, "删除失败：禁止删除根目录").showAndWait();
                    break;
                default:
                    new Alert(Alert.AlertType.ERROR, "删除失败：未知错误").showAndWait();
            }

            // 刷新磁盘和FAT表显示
            checkDisk(osManager);
            checkFat(osManager);
        });

        // 2. 新建文件菜单项
        MenuItem newFileItem = new MenuItem("新建文件");
        newFileItem.setOnAction(e -> {
            TreeItem<FileModel> parentItem = dirTree.getSelectionModel().getSelectedItem();//目录树（dirTree）中获取当前选中的节点，并将其赋值给类型为 TreeItem<FileModel> 的变量 parentItem。
            //System.out.println(parentItem.toString());
            if (parentItem == null) {
                new Alert(Alert.AlertType.WARNING, "请先选中父目录").showAndWait();
                return;
            }

            // 验证选中的父节点是否为目录
            FileModel parentDir = parentItem.getValue();
            if (parentDir.getAttr() != 3) { // 假设3代表目录属性
                new Alert(Alert.AlertType.ERROR, "只能在目录中创建文件").showAndWait();
                return;
            }

            //System.out.println(parentItem.getValue().father);正确的

            // 弹出输入文件名的对话框
            TextInputDialog dialog = new TextInputDialog("新文件.txt");
            dialog.setTitle("新建文件");
            dialog.setHeaderText(null);
            dialog.setContentText("请输入文件名（含扩展名）：");
            Optional<String> result = dialog.showAndWait();//显示一个输入对话框（这里是TextInputDialog），并等待用户输入（阻塞当前线程，直到用户点击 “确定” 或 “取消”）。

            result.ifPresent(fileName -> {
                fileName = fileName.trim();
                if (fileName.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "文件名不能为空").showAndWait();
                    return;
                }
                if (!fileName.contains(".")) {
                    new Alert(Alert.AlertType.WARNING, "文件名必须包含扩展名（如：file.txt）").showAndWait();
                    return;
                }

                // 构建完整路径
                String parentPath = getPath(parentDir);
                String fullPath = parentPath.endsWith("/") ? parentPath + fileName : parentPath + "/" + fileName;

                // 调用OSManager的创建文件方法
                int createResult = osManager.createFileByPathAndParentDir(fullPath,parentDir);
                switch (createResult) {
                    case 0:
                        new Alert(Alert.AlertType.INFORMATION, "文件创建成功").showAndWait();
                        // 刷新目录树
                        refreshTree(osManager);
                        break;
                    case 1:
                        new Alert(Alert.AlertType.ERROR, "创建失败：文件已存在").showAndWait();
                        break;
                    case 2:
                        new Alert(Alert.AlertType.ERROR, "创建失败：磁盘空间不足").showAndWait();
                        break;
                    case 3:
                        new Alert(Alert.AlertType.ERROR, "创建失败：路径错误").showAndWait();
                        break;
                    default:
                        new Alert(Alert.AlertType.ERROR, "创建失败：未知错误").showAndWait();
                }

                // 刷新磁盘和FAT表显示
                checkDisk(osManager);
                checkFat(osManager);
            });
        });

        // 3. 新建文件夹菜单项
        MenuItem newDirItem = new MenuItem("新建文件夹");
        newDirItem.setOnAction(e -> {
            TreeItem<FileModel> parentItem = dirTree.getSelectionModel().getSelectedItem();
            if (parentItem == null) {
                new Alert(Alert.AlertType.WARNING, "请先选中父目录").showAndWait();
                return;
            }

            // 验证选中的父节点是否为目录
            FileModel parentDir = parentItem.getValue();
            if (parentDir.getAttr() != 3) { // 假设3代表目录属性
                new Alert(Alert.AlertType.ERROR, "只能在目录中创建文件夹").showAndWait();
                return;
            }

            // 弹出输入文件夹名的对话框
            TextInputDialog dialog = new TextInputDialog("新文件夹");
            dialog.setTitle("新建文件夹");
            dialog.setHeaderText(null);
            dialog.setContentText("请输入文件夹名：");
            Optional<String> result = dialog.showAndWait();

            result.ifPresent(dirName -> {
                dirName = dirName.trim();
                if (dirName.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "文件夹名不能为空").showAndWait();
                    return;
                }

                // 保存当前目录，用于临时切换后恢复
                FileModel originalDir = osManager.nowCatalog;
                // 切换到目标父目录
                osManager.nowCatalog = parentDir;

                // 调用OSManager的创建目录方法
                int createResult = osManager.createDirectoryByPath(dirName);
                switch (createResult) {
                    case 0:
                        new Alert(Alert.AlertType.INFORMATION, "文件夹创建成功").showAndWait();
                        // 刷新目录树
                        refreshTree(osManager);
                        break;
                    case 1:
                        new Alert(Alert.AlertType.ERROR, "创建失败：目录已存在").showAndWait();
                        break;
                    case 2:
                        new Alert(Alert.AlertType.ERROR, "创建失败：磁盘空间不足").showAndWait();
                        break;
                    default:
                        new Alert(Alert.AlertType.ERROR, "创建失败：未知错误").showAndWait();
                }

                // 恢复原始目录
                osManager.nowCatalog = originalDir;
                // 刷新磁盘和FAT表显示
                checkDisk(osManager);
                checkFat(osManager);
            });
        });

        // 4.创建“重命名”菜单项
        MenuItem renameItem = new MenuItem("重命名");
        renameItem.setOnAction(event -> {
                    // 获取选中的节点
                    TreeItem<FileModel> selectedItem = dirTree.getSelectionModel().getSelectedItem();
                    if (selectedItem == null) {
                        return; // 未选中节点，不处理
                    }
                    FileModel fileModel = selectedItem.getValue();
                    if (fileModel == null) {
                        return;
                    }

                    // 1. 创建输入对话框，默认显示当前名称
                    TextInputDialog dialog = new TextInputDialog(fileModel.getName());
                    dialog.setTitle("重命名");
                    dialog.setHeaderText("请输入新名称:");
                    dialog.setContentText("名称:");

                    // 2. 显示对话框并处理结果
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(newName -> {
                        // 3. 校验新名称合法性
                        //if (validateNewName(newName.trim(), fileModel)) {
                            // 4. 合法则更新名称
                            updateName(fileModel, selectedItem, newName.trim());
                        //}
                    });
        });


        // 将菜单项添加到菜单
        itemMenu.getItems().addAll(deleteItem, newFileItem, newDirItem,renameItem);

        dirTree.setCellFactory(param -> {
            TreeCell<FileModel> cell = new TextFieldTreeCell<>(); // 使用默认的文本单元格

            // 为单元格绑定右键点击事件
            cell.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !cell.isEmpty()) {
                    // 选中当前单元格对应的TreeItem
                    dirTree.getSelectionModel().select(cell.getTreeItem());
                    // 显示菜单
                    itemMenu.show(cell, event.getScreenX(), event.getScreenY());
                } else {
                    // 点击空白处或左键点击时隐藏菜单
                    itemMenu.hide();
                }
            });

            return cell;
        });
    }

    private void updateName(FileModel fileModel, TreeItem<FileModel> treeItem, String newName) {
        String oldName = fileModel.getName();
        // 更新FileModel自身名称
        fileModel.setName(newName);
        // 更新父节点subMap的key（保持映射一致）
        FileModel parent = fileModel.father;
        if (parent != null) {
            parent.subMap.remove(oldName);
            parent.subMap.put(newName, fileModel);
        }
        // 刷新树形节点显示
        treeItem.setValue(fileModel); // 触发UI更新
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        OSManager manager = new OSManager();
        menu(manager);
        initDisk();
        checkDisk(manager);
        initFat(manager);
        checkFat(manager);
        initializeTree(manager);
    }

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
        for (int i = 2; i < 128; i++) {
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