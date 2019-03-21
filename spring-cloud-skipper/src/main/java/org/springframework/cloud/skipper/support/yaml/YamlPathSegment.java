/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.skipper.support.yaml;

/**
 * A YamlPathSegment is a 'primitive' NodeNavigator operation. More complex
 * operations (i.e {@link YamlPath}) are composed as sequences of 0 or more
 * {@link YamlPathSegment}s.
 *
 * @author Kris De Volder
 */
abstract class YamlPathSegment {

	protected abstract String getValueCode();

	protected abstract char getTypeCode();

	public boolean canEmpty() {
		// All path segments implement a real 'one step' movement,
		return false;
	}

	@Override
	public String toString() {
		return toNavString();
	}

	public abstract String toNavString();

	public abstract String toPropString();

	public abstract Integer toIndex();

	public abstract YamlPathSegmentType getType();

	public static YamlPathSegment valueAt(String key) {
		return new ValAtKey(key);
	}

	public static YamlPathSegment valueAt(int index) {
		return new AtIndex(index);
	}

	public static YamlPathSegment keyAt(String key) {
		return new KeyAtKey(key);
	}

	public static YamlPathSegment anyChild() {
		return AnyChild.INSTANCE;
	}

	public String encode() {
		return getTypeCode() + getValueCode();
	}

	public static YamlPathSegment decode(String code) {
		switch (code.charAt(0)) {
		case '*':
			return anyChild();
		case '.':
			return valueAt(code.substring(1));
		case '&':
			return keyAt(code.substring(1));
		case '[':
			return valueAt(Integer.parseInt(code.substring(1)));
		default:
			throw new IllegalArgumentException("Can't decode YamlPathSegment from '" + code + "'");
		}
	}

	public enum YamlPathSegmentType {
		// Go to value associate with given key in a map.
		VAL_AT_KEY,
		// Go to the key node associated with a given key in a map.
		KEY_AT_KEY,
		// Go to value associate with given index in a sequence
		VAL_AT_INDEX,
		// Go to any child (assumes you are using ambiguous traversal
		// method, otherwise this is probably not very useful)
		ANY_CHILD
	}

	public static final class AnyChild extends YamlPathSegment {

		static AnyChild INSTANCE = new AnyChild();

		private AnyChild() {
		}

		@Override
		public String toNavString() {
			return ".*";
		}

		@Override
		public String toPropString() {
			return "*";
		}

		@Override
		public Integer toIndex() {
			return null;
		}

		@Override
		public YamlPathSegmentType getType() {
			return YamlPathSegmentType.ANY_CHILD;
		}

		@Override
		protected char getTypeCode() {
			return '*';
		};

		@Override
		protected String getValueCode() {
			return "";
		}

	}

	public static final class AtIndex extends YamlPathSegment {

		private int index;

		AtIndex(int index) {
			this.index = index;
		}

		@Override
		public String toNavString() {
			return "[" + index + "]";
		}

		@Override
		public String toPropString() {
			return "[" + index + "]";
		}

		@Override
		public YamlPathSegmentType getType() {
			return YamlPathSegmentType.VAL_AT_INDEX;
		}

		@Override
		public Integer toIndex() {
			return index;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + index;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			AtIndex other = (AtIndex) obj;
			if (index != other.index) {
				return false;
			}
			return true;
		}

		@Override
		protected char getTypeCode() {
			return '[';
		}

		@Override
		protected String getValueCode() {
			return "" + index;
		}
	}

	public static class ValAtKey extends YamlPathSegment {

		private String key;

		ValAtKey(String key) {
			this.key = key;
		}

		@Override
		public String toNavString() {
			if (key.indexOf('.') >= 0) {
				return "[" + key + "]";
			}
			return "." + key;
		}

		@Override
		public String toPropString() {
			// Don't start with a '.' if trying to build a 'self contained'
			// expression.
			return key;
		}

		@Override
		public YamlPathSegmentType getType() {
			return YamlPathSegmentType.VAL_AT_KEY;
		}

		@Override
		public Integer toIndex() {
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			ValAtKey other = (ValAtKey) obj;
			if (key == null) {
				if (other.key != null) {
					return false;
				}
			}
			else if (!key.equals(other.key)) {
				return false;
			}
			return true;
		}

		@Override
		protected char getTypeCode() {
			return '.';
		}

		@Override
		protected String getValueCode() {
			return key;
		}
	}

	public static final class KeyAtKey extends ValAtKey {

		KeyAtKey(String key) {
			super(key);
		}

		@Override
		public YamlPathSegmentType getType() {
			return YamlPathSegmentType.KEY_AT_KEY;
		}

		@Override
		protected char getTypeCode() {
			return '&';
		}

	}
}
