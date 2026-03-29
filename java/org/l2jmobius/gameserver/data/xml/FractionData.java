/*
 * Copyright (c) 2013 L2jMobius
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABIL
 * ITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.model.actor.enums.creature.Fraction;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;

/**
 * Loads race to fraction mappings.
 */
public class FractionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FractionData.class.getName());

	private final Map<Race, Fraction> _raceFractions = new EnumMap<>(Race.class);

	protected FractionData()
	{
		load();
	}

	@Override
	public synchronized void load()
	{
		_raceFractions.clear();
		parseDatapackFile("data/character/fractions.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _raceFractions.size() + " race fraction mappings.");
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "fraction", fractionNode ->
		{
			final NamedNodeMap attrs = fractionNode.getAttributes();
			final Race race = parseEnum(attrs, Race.class, "race");
			final Fraction fraction = parseEnum(attrs, Fraction.class, "side");
			if ((race != null) && (fraction != null))
			{
				_raceFractions.put(race, fraction);
			}
		}));
	}

	public Fraction getFractionForRace(Race race)
	{
		if (race == null)
		{
			return Fraction.NONE;
		}
		return _raceFractions.getOrDefault(race, Fraction.NONE);
	}

	public static FractionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final FractionData INSTANCE = new FractionData();
	}
}