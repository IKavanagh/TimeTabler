/*
 * Copyright 2013 Ian Kavanagh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ie.clashoftheash.timetabler.utils;

public class Utils {

	/**
	 * A helper method to add 2 arrays of integers
	 * 
	 * @param array1
	 *            1st array
	 * @param array2
	 *            2nd array
	 * @return resultant array from adding array1 and array2
	 */
	public static int[] addArrays(int[] array1, int[] array2) {
		if (array2.length > array1.length) {
			// Swap arrays so as not to rewrite code
			int[] temp = array1;
			array1 = array2;
			array2 = temp;
		}

		int[] singleArray = new int[array1.length];
		int i;
		for (i = 0; i < array2.length; i++)
			singleArray[i] = array1[i] + array2[i];

		for (; i < array1.length; i++)
			singleArray[i] = array1[i];

		return singleArray;
	}

	/**
	 * Helper function to capitalise each word in a string
	 * 
	 * @param sentence
	 *            string to have each word capitalised
	 * @return string with each word capitalised
	 */
	public static String capitaliseEachWord(String sentence) {
		sentence = sentence.trim();

		char[] letters = sentence.toCharArray();

		boolean capitalise = true;

		for (int i = 0; i < letters.length; i++) {

			if (capitalise) {
				if (Character.isLetter(letters[i])) {
					letters[i] = Character.toUpperCase(letters[i]);
					capitalise = false;
				}
			}

			if (Character.isISOControl(letters[i])
					|| Character.isWhitespace(letters[i]) || letters[i] == '/') {
				capitalise = true;
			}
		}

		return new String(letters);
	}

	public static String replaceEncodedChars(String s) {
		if (s == null)
			return null;
		return s.replaceAll("&Amp;", "&");
	}

}
