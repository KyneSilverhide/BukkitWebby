package com.kyne.webby.rtk.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {

	private static final long serialVersionUID = 6552905039083248520L;

	private final String login;
	private final String password;
	private final List<String> rights;

	public User(final String login, final String password, final List<String> rights) {
		this.login = login;
		this.password = password;
		this.rights = rights;
	}

	public User(final String login, final String password) {
		this.login = login;
		this.password = password;
		this.rights = new ArrayList<String>();
	}

	public String getLogin() {
		return this.login;
	}
	public String getPassword() {
		return this.password;
	}
	public List<String> getRights() {
		return this.rights;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.login == null) ? 0 : this.login.hashCode());
		result = prime * result
				+ ((this.password == null) ? 0 : this.password.hashCode());
		result = prime * result + ((this.rights == null) ? 0 : this.rights.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final User other = (User) obj;
		if (this.login == null) {
			if (other.login != null) {
				return false;
			}
		} else if (!this.login.equals(other.login)) {
			return false;
		}
		if (this.password == null) {
			if (other.password != null) {
				return false;
			}
		} else if (!this.password.equals(other.password)) {
			return false;
		}
		if (this.rights == null) {
			if (other.rights != null) {
				return false;
			}
		} else if (!this.rights.equals(other.rights)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "User [login=" + this.login + ", password=" + this.password + ", rights="
				+ this.rights + "]";
	}
}
