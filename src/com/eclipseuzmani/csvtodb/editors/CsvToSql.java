package com.eclipseuzmani.csvtodb.editors;

public class CsvToSql {
	private String pre;
	private String post;
	private String detail;

	public String getPre() {
		return pre;
	}

	public void setPre(String pre) {
		this.pre = pre;
	}

	public String getPost() {
		return post;
	}

	public void setPost(String post) {
		this.post = post;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public CsvToSql(String pre, String detail, String post) {
		this.pre = pre;
		this.detail = detail;
		this.post = post;
	}

	public CsvToSql() {
	}
}
