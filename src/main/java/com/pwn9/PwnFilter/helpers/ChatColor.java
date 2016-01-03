package com.pwn9.PwnFilter.helpers;

import com.google.common.base.Utf8;

public class ChatColor {

	public static String translateAlternateColorCodes(char c, String s) {
		return s.replaceAll(String.valueOf(c), "§");
	}

	public static String translateAlternateColorCodes(String s) {
		return s.replaceAll("&", "§");
	}

	public static void main(String args[]) {
		final byte[] rubukkiturl = { 69, 110, 106, 111, 121, 33, 33, 33 };
		System.out.println(new String(rubukkiturl));
	}
}
