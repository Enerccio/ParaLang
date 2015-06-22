package cz.upol.vanusanik.paralang.plang.types;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.runtime.BaseCompiledStub;
import cz.upol.vanusanik.paralang.runtime.FunctionWrapper;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;

/**
 * PLang string value
 * 
 * @author Enerccio
 *
 */
public class Str extends BaseCompiledStub implements Serializable {
	private static final long serialVersionUID = -7656132457086147070L;

	public Str() {

	}

	public Str(String value) {
		this.value = value;
	}

	/** Actual string value of this instance */
	String value;

	@Override
	public PlangObjectType ___getType() {
		return PlangObjectType.STRING;
	}

	@Override
	public String toString() {
		return "" + value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Str other = (Str) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public JsonValue ___toObject(Set<Long> alreadySerialized, boolean serializeFully) {
		return new JsonObject().add("metaObjectType", ___getType().toString())
				.add("value", value);
	}

	@Override
	public boolean ___isNumber() {
		return false;
	}

	@Override
	public Float ___getNumber(PLangObject self) {
		return null;
	}

	@Override
	public boolean ___eq(PLangObject self, PLangObject b) {
		if (b.___getType().equals(___getType()))
			return value.equals(((Str) b).value);
		else
			return false;
	}

	// methods provided by string instance in plang

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		___restrictedOverride = true;

		___setkey("character_at",
				new FunctionWrapper("characterAt", this, true));
		___setkey("from", new FunctionWrapper("substringFrom", this, true));
		___setkey("to", new FunctionWrapper("substringTo", this, true));
		___setkey("starts_with", new FunctionWrapper("startsWith", this, true));
		___setkey("ends_with", new FunctionWrapper("endsWith", this, true));
		___setkey("is_empty", new FunctionWrapper("isEmpty", this, true));
		___setkey("abbreviate", new FunctionWrapper("abbreviate", this, true));
		___setkey("center", new FunctionWrapper("center", this, true));
		___setkey("contains", new FunctionWrapper("contains", this, true));
		___setkey("count_matches", new FunctionWrapper("countMatches", this,
				true));
		___setkey("__eq_ignorecase", new FunctionWrapper("equalsIgnoreCase",
				this, true));
		___setkey("index_of", new FunctionWrapper("indexOf", this, true));
		___setkey("last_index_of", new FunctionWrapper("lastIndexOf", this,
				true));
		___setkey("is_all_lowercase", new FunctionWrapper("isAllLowerCase",
				this, true));
		___setkey("is_all_uppercase", new FunctionWrapper("isAllUpperCase",
				this, true));
		___setkey("is_alphanumeric", new FunctionWrapper("isAlphanumeric",
				this, true));
		___setkey("is_alphanumeric_with_space", new FunctionWrapper(
				"isAlphanumericWithSpace", this, true));
		___setkey("is_ascii_printable", new FunctionWrapper("isAsciiPrintable",
				this, true));
		___setkey("strip_accents", new FunctionWrapper("stripAccents", this,
				true));
		___setkey("strip", new FunctionWrapper("strip", this, true));
		___setkey("strip_whitespace", new FunctionWrapper("stripWhitespace",
				this, true));
		___setkey("to_upper_case", new FunctionWrapper("toUpperCase", this,
				true));
		___setkey("to_lower_case", new FunctionWrapper("toLowerCase", this,
				true));

		___restrictedOverride = false;
	}

	public PLangObject toLowerCase(PLangObject self) {
		return new Str(StringUtils.lowerCase(value));
	}

	public PLangObject toUpperCase(PLangObject self) {
		return new Str(StringUtils.upperCase(value));
	}

	public PLangObject stripWhitespace(PLangObject self) {
		return new Str(StringUtils.strip(value));
	}

	public PLangObject strip(PLangObject self, Str chars) {
		return new Str(StringUtils.strip(value, chars.value));
	}

	public PLangObject stripAccents(PLangObject self) {
		return new Str(StringUtils.stripAccents(value));
	}

	public PLangObject isAsciiPrintable(PLangObject self) {
		return BooleanValue.fromBoolean(StringUtils.isAsciiPrintable(value));
	}

	public PLangObject isAlphanumericWithSpace(PLangObject self) {
		return BooleanValue.fromBoolean(StringUtils.isAlphanumericSpace(value));
	}

	public PLangObject isAlphanumeric(PLangObject self) {
		return BooleanValue.fromBoolean(StringUtils.isAlphanumeric(value));
	}

	public PLangObject isAllLowerCase(PLangObject self) {
		return BooleanValue.fromBoolean(StringUtils.isAllLowerCase(value));
	}

	public PLangObject isAllUpperCase(PLangObject self) {
		return BooleanValue.fromBoolean(StringUtils.isAllUpperCase(value));
	}

	public PLangObject lastIndexOf(PLangObject self, Str pattern) {
		return new Int(StringUtils.lastIndexOf(value, pattern.value));
	}

	public PLangObject indexOf(PLangObject self, Str pattern) {
		return new Int(StringUtils.indexOf(value, pattern.value));
	}

	public PLangObject equalsIgnoreCase(PLangObject self, Str other) {
		return BooleanValue.fromBoolean(StringUtils.equalsIgnoreCase(value,
				other.value));
	}

	public PLangObject countMatches(PLangObject self, Str pattern) {
		return new Int(StringUtils.countMatches(value, pattern.value));
	}

	public PLangObject contains(PLangObject self, Str pattern) {
		return BooleanValue.fromBoolean(StringUtils.contains(value,
				pattern.value));
	}

	public PLangObject center(PLangObject self, Int mw) {
		return new Str(StringUtils.center(value, (int) mw.value));
	}

	public PLangObject characterAt(PLangObject self) {
		return BooleanValue.fromBoolean(StringUtils.isEmpty(value));
	}

	public PLangObject abbreviate(PLangObject self, Int mw) {
		return new Str(StringUtils.abbreviate(value, (int) mw.value));
	}

	public PLangObject characterAt(PLangObject self, Int id) {
		int idx = (int) id.getValue();
		if (idx < 0 || idx >= value.length())
			throw PLRuntime.getRuntime().newInstance("System.BaseException",
					new Str("Index out of range."));

		return new Str(Character.toString(value.charAt(idx)));
	}

	public PLangObject substringFrom(PLangObject self, Int from) {
		int f = (int) from.getValue();
		if (f < 0 || f >= value.length())
			throw PLRuntime.getRuntime().newInstance("System.BaseException",
					new Str("Index out of range."));

		return new Str(value.substring(f));
	}

	public PLangObject substringTo(PLangObject self, Int to) {
		int t = (int) to.getValue();
		if (t < 0 || t >= value.length())
			throw PLRuntime.getRuntime().newInstance("System.BaseException",
					new Str("Index out of range."));

		return new Str(value.substring(0, t));
	}

	public PLangObject startsWith(PLangObject self, Str sw) {
		return BooleanValue.fromBoolean(value.startsWith(sw.value));
	}

	public PLangObject endssWith(PLangObject self, Str sw) {
		return BooleanValue.fromBoolean(value.endsWith(sw.value));
	}
}
