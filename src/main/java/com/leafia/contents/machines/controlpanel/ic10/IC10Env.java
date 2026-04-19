package com.leafia.contents.machines.controlpanel.ic10;

import com.hbm.inventory.control_panel.types.DataValueComposite;
import com.leafia.contents.machines.controlpanel.nodes.pack.NodeIC10;
import com.leafia.settings.AddonConfig;
import com.llib.exceptions.LeafiaDevFlaw;

import java.util.HashMap;
import java.util.Map;

public class IC10Env {
	public NodeIC10 node;
	public DataValueComposite output = new DataValueComposite();
	public Map<Integer,Object> register = new HashMap<>();
	public Map<String,Object> labels = new HashMap<>();
	public Map<String,String> aliases = new HashMap<>();
	public Object[] stack = new Object[AddonConfig.ic10maxstack];
	public String error = null;
	public int line = 0;
	public static final int register_sp = -1;
	public static final int register_ra = -2;
	public int yield = 0;
	public void setRegister(int index,Object value) {
		if (value instanceof String s) {
			if (s.isEmpty()) {
				register.remove(index);
				return;
			}
		} else if (value instanceof Double d) {
			if (d == 0) {
				register.remove(index);
				return;
			}
		}
		register.put(index,value);
	}
	public Object getValue(String str) {
		if (labels.containsKey(str)) return labels.get(str);
		String out = aliases.get(str);
		if (out == null) return str;
		if (out.equals("sp")) return register.get(register_sp);
		if (out.equals("ra")) return register.get(register_ra);
		if (out.startsWith("r")) {
			Integer index = tonumber(out.substring(1));
			if (index == null)
				error = "IncorrectVariable";
			else if (index < 0 || index > AddonConfig.ic10maxregisters)
				error = "OutOfRegisterBounds";
		} else
			return str;
		return "";
	}
	public double getNumber(String str) {
		Object object = getValue(str);
		if (object instanceof Number n)
			return n.doubleValue();
		else if (object instanceof String s) {
			Double d = tonumberd(s);
			if (d == null)
				error = "IncorrectVariable";
			else
				return d;
			return 0;
		} else
			throw new LeafiaDevFlaw("what");
	}
	public String input(String key) {
		return "Code me plz";
	}
	public void output(String key,String value) {
		output.setValueOf(key,value);
	}

	// UTILITY //
	boolean equals(Object a,Object b) {
		if (a == null)
			return b == null;
		return a.equals(b);
	}
	Integer tonumber(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ignored) {}
		return null;
	}
	Double tonumberd(String s) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException ignored) {}
		return null;
	}
}
