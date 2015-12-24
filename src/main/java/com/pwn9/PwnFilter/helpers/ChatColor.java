package com.pwn9.PwnFilter.helpers;

public class ChatColor {

	public static String translateAlternateColorCodes(char c, String s) {
		return s.replaceAll(String.valueOf(c), "§");
	}

	public static String translateAlternateColorCodes(String s) {
		return s.replaceAll("&", "§");
	}
}
