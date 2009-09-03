/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.rubypeople.rdt.internal.ui.compare;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.core.runtime.Assert;
import org.rubypeople.rdt.core.ToolFactory;
import org.rubypeople.rdt.core.compiler.IScanner;
import org.rubypeople.rdt.core.compiler.InvalidInputException;


/**
 * A comparator for Ruby tokens.
 */
public class RubyTokenComparator implements ITokenComparator {
		
	private String fText;
	private boolean fShouldEscape= true;
	private List<Integer> fStarts;
	private List<Integer> fLengths;

	/**
	 * Creates a TokenComparator for the given string.
	 */
	public RubyTokenComparator(String text, boolean shouldEscape) {
		
		Assert.isNotNull(text);
		
		fText= text;
		fShouldEscape= shouldEscape;
		
		int length= fText.length();
		fStarts= new ArrayList<Integer>(length + 1); // try to never grow list
		fLengths= new ArrayList<Integer>(length + 1);
		
		IScanner scanner= ToolFactory.createScanner(true, true, false, false); // returns comments & whitespace
		scanner.setSource(fText.toCharArray());
		try {
			int endPos= 0;
			while (scanner.getNextToken() != IScanner.TokenNameEOF) {
				int start= scanner.getCurrentTokenStartPosition();
				int end= scanner.getCurrentTokenEndPosition()+1;
				fStarts.add(start);
				fLengths.add(end - start);
				endPos= end;
				// What if the scanner goes nuts and never returns EOF? ugly escape valve here
				if (fStarts.size() > (length * 2))
					break;
			}
			// workaround for #13907
			if (endPos < length) {
				fStarts.add(endPos);
				fLengths.add(length-endPos);
			}
		} catch (InvalidInputException ex) {
			// NeedWork
		}
	}	

	/**
	 * Returns the number of token in the string.
	 *
	 * @return number of token in the string
	 */
	public int getRangeCount() {
		return fStarts.size();
	}

	/* (non Rubydoc)
	 * see ITokenComparator.getTokenStart
	 */
	public int getTokenStart(int index) {
		if (index >= 0 && index < getRangeCount())
			return fStarts.get(index);
		if (getRangeCount() > 0)
			return fStarts.get(getRangeCount()-1) + fLengths.get(getRangeCount()-1);
		return 0;
	}

	/* (non Rubydoc)
	 * see ITokenComparator.getTokenLength
	 */
	public int getTokenLength(int index) {
		if (index < getRangeCount())
			return fStarts.get(index);
		return 0;
	}
	
	/**
	 * Returns <code>true</code> if a token given by the first index
	 * matches a token specified by the other <code>IRangeComparator</code> and index.
	 *
	 * @param thisIndex	the number of the token within this range comparator
	 * @param other the range comparator to compare this with
	 * @param otherIndex the number of the token within the other comparator
	 * @return <code>true</code> if the token are equal
	 */
	public boolean rangesEqual(int thisIndex, IRangeComparator other, int otherIndex) {
		if (other != null && getClass() == other.getClass()) {
			RubyTokenComparator tc= (RubyTokenComparator) other;	// safe cast
			int thisLen= getTokenLength(thisIndex);
			int otherLen= tc.getTokenLength(otherIndex);
			if (thisLen == otherLen)
				return fText.regionMatches(false, getTokenStart(thisIndex), tc.fText, tc.getTokenStart(otherIndex), thisLen);
		}
		return false;
	}

	/**
	 * Aborts the comparison if the number of tokens is too large.
	 *
	 * @return <code>true</code> to abort a token comparison
	 */
	public boolean skipRangeComparison(int length, int max, IRangeComparator other) {

		if (!fShouldEscape)
			return false;

		if (getRangeCount() < 50 || other.getRangeCount() < 50)
			return false;

		if (max < 100)
			return false;

		if (length < 100)
			return false;

		if (max > 800)
			return true;

		if (length < max / 4)
			return false;

		return true;
	}
}
