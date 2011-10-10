package com.bogazici.swe574.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "article")
public class Article {

  @Id
  @GeneratedValue
  @Column(name = "article_id")
  private Long articleId;

  @Column(name = "article_name", nullable = false, length=20)
  private String articleName;

  @Column(name = "article_desc", nullable = false)
  private String articleDesc;
  
  @Column(name = "date_added")
  private Date addedDate;
  
  public Article() {    
  }
  
  public Long getArticleId() {
    return articleId;
  }

  public void setArticleId(Long articleId) {
    this.articleId = articleId;
  }

  public String getArticleName() {
    return articleName;
  }

  public void setArticleName(String articleName) {
    this.articleName = articleName;
  }

  public String getArticleDesc() {
    return articleDesc;
  }

  public void setArticleDesc(String articleDesc) {
    this.articleDesc = articleDesc;
  }

  public Date getAddedDate() {
    return addedDate;
  }

  public void setAddedDate(Date addedDate) {
    this.addedDate = addedDate;
  }  
}
