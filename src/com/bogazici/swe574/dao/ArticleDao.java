package com.bogazici.swe574.dao;

import java.util.List;

import com.bogazici.swe574.model.Article;



public interface ArticleDao {
  // To Save the article detail
  public void saveArticle ( Article Article );
  
  // To get list of all articles
  public List<Article> listArticles();
}
