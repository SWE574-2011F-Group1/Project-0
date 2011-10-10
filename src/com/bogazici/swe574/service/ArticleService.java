package com.bogazici.swe574.service;

import java.util.List;

import com.bogazici.swe574.model.Article;


public interface ArticleService {

  public void addArticle(Article article);

  public List<Article> listArticles();
}