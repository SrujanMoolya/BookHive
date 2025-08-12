package com.svvaap.bookhive;

public class Book {
    public String id;
    public String title;
    public String author;
    public String category;
    public String language;
    public String description;
    public double price;
    public String coverImageUrl;
    public String fileUrl;
    public String visibility;
    public String uploadDate;

    public Book() {}

    public Book(String id, String title, String author, String category, String language, String description, double price, String coverImageUrl, String fileUrl, String visibility, String uploadDate) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.language = language;
        this.description = description;
        this.price = price;
        this.coverImageUrl = coverImageUrl;
        this.fileUrl = fileUrl;
        this.visibility = visibility;
        this.uploadDate = uploadDate;
    }
} 