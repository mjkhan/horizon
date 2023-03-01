/* Copyright (c) 2020 Emjay Khan. All rights reserved. */

package horizon.sql.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import horizon.base.AbstractComponent;
import horizon.sql.Parameters;

public class Param extends AbstractComponent {
	public static List<Param> parse(String str) {
		return !isEmpty(str) ? new Parser(str).parse() : Collections.emptyList();
	}

	private Parameters.Type type;
	private String
		token,
		ref,
		baseRef;

	public boolean toPrepare() {
		return token.startsWith("#{");
	}

	public Parameters.Type type() {
		return ifEmpty(type, () -> Parameters.Type.IN);
	}

	/**Returns the token.
	 * @return the token
	 */
	public String token() {
		return token;
	}

	private Param setToken(String token) {
		this.token = token;
		ref = token.substring(2, token.length() - 1).trim();

		//TODO: validate with regexp?
		int pos = toPrepare() ? ref.indexOf(":") : -1;
		if (pos > -1) {
			String s = ref.toUpperCase();
			if (s.startsWith("OUT:"))
				type = Parameters.Type.OUT;
			else if (s.startsWith("IN:"))
				type = Parameters.Type.IN;
			else
				throw new RuntimeException("Invalid parameter: " + token);
			ref = ref.substring(pos + 1).trim();
		}
		if (isEmpty(ref))
			throw new RuntimeException("Invalid parameter: " + token);

		pos = ref.indexOf(".");
		baseRef = pos < 0 ? ref : ref.substring(0, pos);

		return this;
	}

	/**Returns the ref.
	 * @return the ref
	 */
	public String ref() {
		return ref;
	}

	public String baseRef() {
		return baseRef;
	}

	@Override
	public String toString() {
		return String.format("%s{token:\"%s\", ref:\"%s\", type:%s}", getClass().getName(), token, ref, type());
	}

	private static class Parser {
		String str;
		ArrayList<Param> params;

		Parser(String s) {
			str = s;
		}

		List<Param> parse() {
			process(0);
			return ifEmpty(params, Collections::emptyList);
		}

		void process(int fromIndex) {
			int dstart = str.indexOf("${", fromIndex),
				sstart = str.indexOf("#{", fromIndex);
			if (dstart < 0 && sstart < 0) return;

			int start = dstart < 0 || sstart < 0 ? Math.max(dstart, sstart) : Math.min(dstart, sstart),
				end = str.indexOf("}", start);
			if (end < 0) return;

			if (params == null)
				params = new ArrayList<>();
			params.add(new Param().setToken(str.substring(start, end + 1)));

			process(end + 2);
		}
	}
}