package com.cleanhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CleanDownload extends JFrame {
    // 1. 定义数据模型（简单起见，第一版直接硬编码在里面，以后可以改成读取JSON文件）
    private static class Software {
        String name;
        String officialSite;
        String githubRepo; // 如果托管在GitHub上，用这个字段获取API直链；没有则填 null

        public Software(String name, String officialSite, String githubRepo) {
            this.name = name;
            this.officialSite = officialSite;
            this.githubRepo = githubRepo;
        }
    }

    private List<Software> allSoftware = new ArrayList<>();
    private List<Software> filteredSoftware = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable table;

    public CleanDownload() {
        // 初始化数据（第一批种子数据）
        initData();

        // 2. 设置窗口基本属性
        setTitle("CleanHub - 纯净软件安装器 (MVP版)");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示
        setLayout(new BorderLayout());

        // 3. 顶部搜索栏
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel searchLabel = new JLabel("搜索软件：");
        JTextField searchField = new JTextField();
        topPanel.add(searchLabel, BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // 4. 中间数据表格
        String[] columnNames = {"软件名称", "官方网址", "下载源类型"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可直接编辑
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(30);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // 5. 底部提示
        JLabel tipLabel = new JLabel("提示：双击表格中的一行，软件会尝试获取直链或直接打开浏览器下载。", JLabel.CENTER);
        tipLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(tipLabel, BorderLayout.SOUTH);

        // 6. 绑定事件：搜索框输入监听
        searchField.addCaretListener(e -> filterData(searchField.getText().trim()));

        // 7. 绑定事件：表格双击监听
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        Software selected = filteredSoftware.get(selectedRow);
                        handleDownloadAction(selected);
                    }
                }
            }
        });

        // 初始加载数据到表格
        filterData("");
    }

    private void initData() {
        // 这里手动添加几款我的电脑上就有的软件试试水
        allSoftware.add(new Software("Visual Studio Code", "https://code.visualstudio.com/", "microsoft/vscode"));
        allSoftware.add(new Software("Git", "https://git-scm.com/", "git/git"));
        allSoftware.add(new Software("7-Zip", "https://www.7-zip.org/", null)); // 7-Zip 没有GitHub官方仓库
        allSoftware.add(new Software("Notepad++", "https://notepad-plus-plus.org/", null));
        allSoftware.add(new Software("Typora", "https://typora.io/", null));
    }

    // 搜索过滤逻辑
    private void filterData(String keyword) {
        if (keyword.isEmpty()) {
            filteredSoftware = new ArrayList<>(allSoftware);
        } else {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            filteredSoftware = allSoftware.stream()
                    .filter(s -> s.name.toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                    .collect(Collectors.toList());
        }
        // 刷新表格
        tableModel.setRowCount(0); // 清空
        for (Software s : filteredSoftware) {
            String sourceType = (s.githubRepo != null) ? "GitHub API 直链" : "官方网页跳转";
            tableModel.addRow(new Object[]{s.name, s.officialSite, sourceType});
        }
    }

    // 处理下载逻辑（核心逻辑）
    private void handleDownloadAction(Software software) {
        if (software.githubRepo != null) {
            // 如果有GitHub仓库，尝试通过API获取下载直链
            new Thread(() -> { // 开启新线程，防止网络请求卡死UI
                String downloadUrl = fetchGitHubLatestDownloadUrl(software.githubRepo);
                SwingUtilities.invokeLater(() -> {
                    if (downloadUrl != null) {
                        int choice = JOptionPane.showConfirmDialog(this,
                                "已找到官方最新版直链：\n" + downloadUrl + "\n\n是否立即下载？",
                                "获取成功", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.YES_OPTION) {
                            openWebpage(downloadUrl); // 这里暂且调用浏览器下载，下一版可以写Java内置下载
                        }
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "未能获取到直链（可能是网络问题或API限制），将为你打开官网。",
                                "提示", JOptionPane.WARNING_MESSAGE);
                        openWebpage(software.officialSite);
                    }
                });
            }).start();
        } else {
            // 没有API，直接打开官网
            openWebpage(software.officialSite);
        }
    }

    // 调用系统默认浏览器打开网页
    private void openWebpage(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                JOptionPane.showMessageDialog(this, "系统不支持自动打开浏览器，请手动访问：" + url);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "打开网页失败：" + e.getMessage());
        }
    }

    // 请求 GitHub API 获取最新版安装包直链
    private String fetchGitHubLatestDownloadUrl(String repo) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
                    .header("User-Agent", "CleanHub-App")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray assets = jsonObject.getAsJsonArray("assets");
                // 寻找 .exe 或 .msi 安装包
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString().toLowerCase();
                    if (name.endsWith(".exe") || name.endsWith(".msi")) {
                        return asset.get("browser_download_url").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("获取GitHub直链失败: " + e.getMessage());
        }
        return null;
    }

    public static void main(String[] args) {
        // 使用 SwingUtilities 确保 UI 线程安全
        SwingUtilities.invokeLater(() -> {
            new CleanDownload().setVisible(true);
        });
    }
}