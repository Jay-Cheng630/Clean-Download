package com.cleanhub;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class MainApp extends JFrame {

    private List<SoftInfo> dataList = new ArrayList<>();
    private DefaultTableModel tbModel;
    
    public MainApp() {
        // 先搞我电脑上有的告一哈
        dataList.add(new SoftInfo("Visual Studio Code", "https://code.visualstudio.com/", "microsoft/vscode"));
        dataList.add(new SoftInfo("7-Zip", "https://www.7-zip.org/", null));
        dataList.add(new SoftInfo("Git", "https://git-scm.com/", "git/git"));

        setTitle("CleanHub 纯净软件库");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new BorderLayout());

        // ======== 表格部分 ========
        String[] columnNames = {"软件名称", "官网地址", "下载方式"};
        tbModel = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int r, int c) {
                return false; // 不让编辑
            }
        };

        for (int i = 0; i < dataList.size(); i++) {
            SoftInfo info = dataList.get(i);
            String type = (info.gitRepo == null) ? "去官网" : "GitHub直链";
            tbModel.addRow(new Object[]{info.name, info.url, type});
        }

        JTable jTable1 = new JTable(tbModel);
        jTable1.setRowHeight(25);
        JScrollPane js = new JScrollPane(jTable1);
        add(js, BorderLayout.CENTER);

        // ======== 底部按钮部分 ========
        JPanel p1 = new JPanel(); // 放左边按钮
        JPanel p2 = new JPanel(new FlowLayout(FlowLayout.RIGHT)); // 放右边状态
        JLabel lblStatus = new JLabel("就绪中...");
        
        JButton btnOpen = new JButton("打开官网");
        JButton btnFind = new JButton("找下载链接");
        
        p1.add(btnOpen);
        p1.add(btnFind);
        p2.add(lblStatus);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(p1, BorderLayout.WEST);
        bottom.add(p2, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // ======== 监听事件 ========
        // 打开官网
        btnOpen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = jTable1.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(null, "请先点选一行再操作！");
                    return;
                }
                String u = dataList.get(row).url;
                try {
                    Desktop.getDesktop().browse(new URI(u));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "系统打不开浏览器，自己复制吧：" + u);
                }
            }
        });

        // 找直链
        btnFind.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = jTable1.getSelectedRow();
                if (row == -1) {
                    JOptionPane.showMessageDialog(null, "请先选择软件");
                    return;
                }
                
                final SoftInfo info = dataList.get(row); // 必须是final的，不然内部类用不了
                if (info.gitRepo == null) {
                    JOptionPane.showMessageDialog(null, "这个软件没有GitHub仓库，只能去官网下");
                    return;
                }
                
                lblStatus.setText("正在请求网络，稍等...");
                
                // 新开线程
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String realUrl = DownloadUtil.findGitUrl(info.gitRepo);
                        
                        // 拿到结果后再切回界面线程
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (realUrl != null) {
                                    lblStatus.setText("查询成功");
                                    JOptionPane.showMessageDialog(null, "找到了直链：\n" + realUrl);
                                } else {
                                    lblStatus.setText("查询失败");
                                    JOptionPane.showMessageDialog(null, "没找到能用的安装包链接，去官网吧。");
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        // 双击表格直接跳转
        jTable1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
            
                    btnOpen.doClick(); 
                }
            }
        });
    }

    public static void main(String[] args) {
        // 用系统的UI风格，好看点
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 失败了就算了
        }
        
        // 启动窗口
        new MainApp().setVisible(true);
    }
}