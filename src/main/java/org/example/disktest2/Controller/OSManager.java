package org.example.disktest2.Controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OSManager {
    private Map<String,FileModel> totalFiles = new HashMap<String, FileModel>();
    //FAT表
    private int[] fat = new int[128];

    public Map<String, FileModel> getTotalFiles() {
        return totalFiles;
    }

    //根目录
    private FileModel root = new FileModel("root",1);
    private FileModel nowCatalog = root;

    public FileModel getRoot(){
        return root;
    }

    public OSManager(){
        for (int i = 2; i < 128; i++) {
            fat[i]=0;
        }
        fat[0] = 126; //磁盘剩余块数
        fat[1] = -1; //存根目录,表示已被占用

        root.setFather(root);
        totalFiles.put("root",root);
    }

    public int[] getFat() {
        return fat;
    }

    public int setFat(int size) {
        int[] startNum = new int[size];
        int i = 2;
        //j表示分给该文件的第j个块
        for(int j = 0; j < size; i++) {
            if(fat[i] == 0) {
                startNum[j] = i;
                if(j > 0) {
                    fat[startNum[j-1]] = i; //fat表的上一块指向下一块
                }
                j++;
            }
        }
        fat[i-1] = -1;
        return startNum[0]; //返回第一块
    }

    public void delFat(int startNum){
        int i = startNum;
        while(fat[i] != -1) {
            int j = fat[i];
            fat[i] = 0;
            i = j;
        }
        fat[i] = 0;
    }

    public void addFat(int startNum, int addSize) {
        int now = startNum;
        int next = fat[startNum];
        while(next != -1) {
            now = next;
            next = fat[now];
        }

        for(int j=0, i=2 ; j<addSize ; i++) {
            if(fat[i] == 0) {
                fat[now] = i;
                now = i;
                j++;
            }
        }
        fat[now] = -1;
    }

    //创建文件
    public int createFile(String name, String type, int size) {
        if(fat[0] >= size) {
            FileModel value = nowCatalog.subMap.get(name);
            if(value != null) {
                if(value.getAttr()==3) {
                    int startNum = setFat(size);
                    FileModel file = new FileModel(name,type,startNum,size);
                    file.setFather(nowCatalog);
                    nowCatalog.subMap.put(name,file);
                    totalFiles.put(file.getName(), file);
                    fat[0] -= size;

                    File newFile = new File(".\\files\\" + name);

                    System.out.println("创建成功！");
                    showFile();
                    return 0;
                } else {
                    System.out.println("创建失败,已存在同名文件");
                    showFile();
                    return 1;
                }
            } else {
                int startNum = setFat(size);
                FileModel file = new FileModel(name,type,startNum,size);
                file.setFather(nowCatalog);
                nowCatalog.subMap.put(name,file);
                totalFiles.put(file.getName(), file);
                fat[0] -= size;

                File newFile = new File(name);

                System.out.println("创建成功！");
                showFile();
                return 0;
            }
        } else {
            System.out.println("磁盘空间已满，创建失败");
            return 2;
        }
    }

    //创建目录
    public int createCatalog(String name) {
        if(fat[0] >= 1) {
            FileModel value = nowCatalog.subMap.get(name);
            if(((value != null) && (value.getAttr() == 2))) {
                FileModel catalog = new FileModel(name,setFat(1));
                catalog.setFather(nowCatalog);
                nowCatalog.subMap.put(name,catalog);
                fat[0] --;
                totalFiles.put(catalog.getName(), catalog);
                System.out.println("创建目录成功！");
                showFile();
                return 0;
            } else if (value == null) {
                FileModel catalog = new FileModel(name,setFat(1));
                catalog.setFather(nowCatalog);
                nowCatalog.subMap.put(name,catalog);
                fat[0] --;
                totalFiles.put(catalog.getName(), catalog);
                System.out.println("创建目录成功！");
                showFile();
                return 0;
            } else {
                System.out.println("创建失败,已存在同名目录");
                showFile();
                return 1;
            }
        } else {
            System.out.println("磁盘空间已满，创建失败");
            return 2;
        }
    }


    // 新的文件创建方法
    public int createFileByPath(String path) {
        String fileName = getFileName(path);
        String parentPath = getParentPath(path);

        System.out.println(fileName + " " + parentPath);
        if (fileName.isEmpty()) {
            System.out.println("文件名为空");
            return 3; // 路径错误
        }

        FileModel parentDir = navigateToPath(parentPath);
        System.out.println("parentDir " + parentDir);
        if (parentDir == null) {
            System.out.println("文件父目录为空");
            return 3; // 路径错误
        }

        // 检查文件是否已存在
        if (parentDir.subMap.containsKey(fileName)) {
            return 1; // 文件已存在
        }

        // 检查磁盘空间
        if (fat[0] < 1) {
            return 2; // 磁盘空间不足
        }

        // 解析文件名和扩展名
        String name = fileName;
        String type = "";
        int size = 1; // 默认大小

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            name = fileName.substring(0, dotIndex);
            type = fileName.substring(dotIndex + 1);
        }

        // 创建文件
        int startNum = setFat(size);
        FileModel file = new FileModel(name, type, startNum, size);
        file.setFather(parentDir);
        parentDir.subMap.put(fileName, file);
        totalFiles.put(fileName, file);
        fat[0] -= size;

        System.out.println("创建文件成功：" + path);
        showFile();
        return 0;
    }

    public int deleteFileByPath(String path) {
        String fileName = getFileName(path);
        String parentPath = getParentPath(path);

        if (fileName.isEmpty()) {
            return 4; // 路径错误
        }

        FileModel parentDir = navigateToPath(parentPath);
        if (parentDir == null) {
            return 4; // 路径错误
        }

        FileModel file = parentDir.subMap.get(fileName);
        if (file == null) {
            return 1; // 文件不存在
        }

        if (!file.subMap.isEmpty()) {
            return 2; // 目录不为空
        }

        if (fat[0] >= 126) {
            return 3; // 磁盘为空
        }

        // 删除文件
        delFat(file.getStartNum());
        parentDir.subMap.remove(fileName);
        totalFiles.remove(fileName);
        fat[0] += file.getSize();

        System.out.println("删除文件成功：" + path);
        showFile();
        return 0;
    }

    //  展示文件
    public void showFile() {
        System.out.println("***************** < " + nowCatalog.getName() + " > *****************");
        if(!nowCatalog.subMap.isEmpty()) {
            for(FileModel value : nowCatalog.subMap.values()) {
                if(value.getAttr() == 3) { //目录文件
                    System.out.println("文件名 : " + value.getName());
                    System.out.println("操作类型 ： " + "文件夹");
                    System.out.println("起始盘块 ： " + value.getStartNum());
                    System.out.println("大小 : " + value.getSize());
                    System.out.println("<-------------------------------------->");
                }
                else if(value.getAttr() == 2) {
                    System.out.println("文件名 : " + value.getName() + "." + value.getType());
                    System.out.println("操作类型 ： " + "可读可写文件");
                    System.out.println("起始盘块 ： " + value.getStartNum());
                    System.out.println("大小 : " + value.getSize());
                    System.out.println("<-------------------------------------->");
                }
            }
        }
        for(int i =0; i<2; i++)
            System.out.println();
        System.out.println("磁盘剩余空间 ：" + fat[0] + "            " + "退出系统请输入exit");
        System.out.println();
    }

    //删除文件
    public int delFile(String name) {
        if(fat[0] < 126) {
            FileModel value = nowCatalog.subMap.get(name);
            if(value == null) {
                System.out.println("删除失败，文件不存在");
                return 1;
            } else if (!value.subMap.isEmpty()) {
                System.out.println("删除失败，该目录不为空");
                return 2;
            } else {
                delFat(value.getStartNum());
                nowCatalog.subMap.remove(name);
                totalFiles.remove(name); // 从totalFiles中移除文件
                fat[0] += value.getSize();
                System.out.println("删除成功");
                showFile();
                return 0;
            }
        } else {
            System.out.println("磁盘为空，删除失败");
            return 3;
        }
    }

    //重命名文件
    public void reName(String name, String newName) {
        FileModel value = nowCatalog.subMap.get(name);
        if(value == null) {
            System.out.println("当前文件不存在，请检查名字");
        } else {
            FileModel value2 = nowCatalog.subMap.get(newName);
            if(value2 == null) {
                value.setName(newName);
                nowCatalog.subMap.remove(name);
                nowCatalog.subMap.put(newName,value);
                System.out.println("重命名成功");
            } else {
                System.out.println("新名字已存在");
            }
        }
        showFile();
    }

    //打开文件
    public void openFile(String name) {
        if(nowCatalog.subMap.containsKey(name)) {
            FileModel value = nowCatalog.subMap.get(name);
            nowCatalog = value;
            if(value.getAttr() == 2) {
                System.out.println("成功打开文件，当前文件大小为" + value.getSize());
            } else {
                System.out.println("成功打开文件夹");
                showFile();
            }
        } else {
            System.out.println("不存在该文件，打开失败");
        }
    }

    //在文件里面添加内容
    public void addContent(String name, int addSize) {
        if(fat[0] >= addSize) {
            if(nowCatalog.subMap.containsKey(name)) {
                FileModel value = nowCatalog.subMap.get(name);
                if(value.getAttr() == 2) {
                    value.setSize(value.getSize() + addSize);
                    addFat(value.getStartNum(),addSize);
                    System.out.println("添加成功，正在打开文件");
                    openFile(name);
                } else {
                    System.out.println("不可向文件夹添加内容");
                }
            } else {
                System.out.println("不存在该文件，添加失败");
            }
        } else {
            System.out.println("磁盘空间不足，添加失败");
        }
    }

    //返回父目录
    public void backFile() {
        if(nowCatalog.getFather() == null) {
            System.out.println("无父目录");
        } else {
            nowCatalog = nowCatalog.getFather();
            System.out.println("已返回上一级目录");
            showFile();
        }
    }

    //根据路径查找文件
    public void searchFile(String[] roadName) {

        FileModel temp = nowCatalog;

        if(totalFiles.containsKey(roadName[roadName.length - 1])) {
            nowCatalog = root;
            if(nowCatalog.getName().equals(roadName[1])) {
                for(int i = 1; i < roadName.length ; i++) {
                    if(nowCatalog.subMap.containsKey(roadName[i])) {
                        nowCatalog = nowCatalog.subMap.get(roadName[i]);
                    } else {
                        System.out.println("路径错误，查找失败");
                        nowCatalog = temp; //恢复原状态
                        showFile();
                    }
                }
            } else {
                System.out.println("请输入绝对路径");
            }
        } else {
            System.out.println("不存在该文件");
            showFile();
        }
    }


    public void showFAT() {

        for (int j = 0; j < 125; j += 5) {
            System.out.println("第几项 | " + j + "        " + (j + 1) + "        " + (j + 2) + "        "
                    + (j + 3) + "        " + (j + 4));
            System.out.println("内容    | " + fat[j] + "        " + fat[j + 1] + "        " + fat[j + 2]
                    + "        " + fat[j + 3] + "        " + fat[j + 4]);
            System.out.println();
        }
        int j = 125;
        System.out.println("第几项 | " + j + "        " + (j + 1) + "        " + (j + 2));
        System.out.println("内容    | " + fat[j] + "        " + fat[j + 1] + "        " + fat[j + 2]);
        System.out.println();
        showFile();
    }

    // 路径解析和导航的辅助方法，返回父目录
    private FileModel navigateToPath(String path) {
        if (path == null) {
            return null;
        }

        if (path.isEmpty()) {
            return root;
        }

        // 处理路径，移除开头的反斜杠
        if (path.startsWith("\\")) {
            path = path.substring(1);
        }
        System.out.println("path " + path);


        String[] pathParts = path.split("\\\\");
        //System.out.println("pathParts  " + pathParts[0]);

        FileModel current = root;
        //父目录为root
        if(pathParts[0].equals("root") && pathParts.length == 1) return current;

        for (String part : pathParts) {
            if (part.isEmpty()) continue;
            System.out.println("part " + part);
            if (current.subMap.containsKey(part)) {
                FileModel next = current.subMap.get(part);
                if (next.getAttr() == 3) { // 是目录
                    current = next;
                } else {
                    return null; // 路径中有文件，不是目录
                }
            } else {
                return null; // 路径不存在
            }
        }

        return current;
    }

    private FileModel getFileByPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        // 处理路径，移除开头的反斜杠
        if (path.startsWith("\\")) {
            path = path.substring(1);
        }

        if (path.isEmpty()) {
            return root;
        }

        String[] pathParts = path.split("\\\\");
        FileModel current = root;

        // 导航到父目录
        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            if (part.isEmpty()) continue;

            if (current.subMap.containsKey(part)) {
                FileModel next = current.subMap.get(part);
                if (next.getAttr() == 3) { // 是目录
                    current = next;
                } else {
                    return null; // 路径中有文件，不是目录
                }
            } else {
                return null; // 路径不存在
            }
        }

        // 获取最后一个部分（文件名或目录名）
        String fileName = pathParts[pathParts.length - 1];
        return current.subMap.get(fileName);
    }

    //获取完整父路径
    private String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 处理路径，移除开头的反斜杠
        if (path.startsWith("\\")) {
            path = path.substring(1);
        }

        if (path.isEmpty()) {
            return "";
        }

        String[] pathParts = path.split("\\\\");
        if (pathParts.length <= 1) {
            return "";
        }

        StringBuilder parentPath = new StringBuilder();
        for (int i = 0; i < pathParts.length - 1; i++) {
            if (i > 0) {
                parentPath.append("\\");
            }
            parentPath.append(pathParts[i]);
        }

        return parentPath.toString();
    }

    //获取文件名
    private String getFileName(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // 处理路径，移除开头的反斜杠
        if (path.startsWith("\\")) {
            path = path.substring(1);
        }

        if (path.isEmpty()) {
            return "";
        }

        String[] pathParts = path.split("\\\\");
        return pathParts[pathParts.length - 1];
    }

    //打开文件，显示文件内容
    public int typeFile(String path) {
        String fileName = getFileName(path);
        String parentPath = getParentPath(path);

        if (fileName.isEmpty()) {
            return 2; // 路径错误
        }

        FileModel parentDir = navigateToPath(parentPath);
        System.out.println("type操作，文件父目录为" + parentDir.getName());
        if (parentDir == null) {
            return 2; // 路径错误
        }

        System.out.println("type操作，文件名为：" + fileName);
        FileModel file = parentDir.subMap.get(fileName);
        System.out.println("type操作的file.attribution" + file.getAttr());
        if (file == null) {
            return 1; // 文件不存在
        }

        if (file.getAttr() == 2) { // 是文件
            System.out.println("文件内容显示：" + path);
            System.out.println("文件名：" + file.getName() + "." + file.getType());
            System.out.println("大小：" + file.getSize() + " 块");
            System.out.println("起始块：" + file.getStartNum());
        } else {
            System.out.println("这是一个目录，不是文件");
            return 3;
        }

        return 0;
    }

    public int copyFile(String sourcePath, String targetPath) {
        String sourceFileName = getFileName(sourcePath);
        String sourceParentPath = getParentPath(sourcePath);
        String targetFileName = getFileName(targetPath);
        String targetParentPath = getParentPath(targetPath);

        if (sourceFileName.isEmpty() || targetFileName.isEmpty()) {
            return 2; // 路径错误
        }

        // 获取源文件
        FileModel sourceParentDir = navigateToPath(sourceParentPath);
        if (sourceParentDir == null) {
            return 1; // 源文件不存在
        }

        FileModel sourceFile = sourceParentDir.subMap.get(sourceFileName);
        if (sourceFile == null) {
            return 1; // 源文件不存在
        }

        // 获取目标目录
        FileModel targetParentDir = navigateToPath(targetParentPath);
        if (targetParentDir == null) {
            return 2; // 目标路径错误
        }

        // 检查目标文件是否已存在
        if (targetParentDir.subMap.containsKey(targetFileName)) {
            return 4; // 目标文件已存在
        }

        // 检查磁盘空间
        if (fat[0] < sourceFile.getSize()) {
            return 3; // 磁盘空间不足
        }

        // 复制文件
        int startNum = setFat(sourceFile.getSize());
        FileModel newFile = new FileModel(targetFileName, sourceFile.getType(), startNum, sourceFile.getSize());
        newFile.setFather(targetParentDir);
        targetParentDir.subMap.put(targetFileName, newFile);
        totalFiles.put(targetFileName, newFile);
        fat[0] -= sourceFile.getSize();

        System.out.println("复制文件成功：" + sourcePath + " -> " + targetPath);
        showFile();
        return 0;
    }

    public int createDirectoryByPath(String path) {
        String dirName = getFileName(path);
        String parentPath = getParentPath(path);

        if (dirName.isEmpty()) {
            return 3; // 路径错误
        }

        FileModel parentDir = navigateToPath(parentPath);
        if (parentDir == null) {
            return 3; // 路径错误
        }

        // 检查目录是否已存在
        if (parentDir.subMap.containsKey(dirName)) {
            return 1; // 目录已存在
        }

        // 检查磁盘空间
        if (fat[0] < 1) {
            return 2; // 磁盘空间不足
        }

        // 创建目录
        int startNum = setFat(1);
        FileModel directory = new FileModel(dirName, startNum);
        directory.setFather(parentDir);
        parentDir.subMap.put(dirName, directory);
        totalFiles.put(dirName, directory);
        fat[0]--;

        System.out.println("创建目录成功：" + path);
        showFile();
        return 0;
    }

    public int removeDirectoryByPath(String path) {
        String dirName = getFileName(path);
        String parentPath = getParentPath(path);

        if (dirName.isEmpty()) {
            return 3; // 路径错误
        }

        FileModel parentDir = navigateToPath(parentPath);
        if (parentDir == null) {
            return 3; // 路径错误
        }

        FileModel directory = parentDir.subMap.get(dirName);
        if (directory == null) {
            return 1; // 目录不存在
        }

        if (!directory.subMap.isEmpty()) {
            return 2; // 目录不为空
        }

        // 删除目录
        delFat(directory.getStartNum());
        parentDir.subMap.remove(dirName);
        totalFiles.remove(dirName);
        fat[0] += directory.getSize();

        System.out.println("删除目录成功：" + path);
        showFile();
        return 0;
    }
}


