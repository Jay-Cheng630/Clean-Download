package com.cleanhub;

public class SoftInfo {
    // 直接public
    public String name;
    public String url;
    public String gitRepo; 

    public SoftInfo(String name, String url, String gitRepo) {
        this.name = name;
        this.url = url;
        this.gitRepo = gitRepo;
    }
}