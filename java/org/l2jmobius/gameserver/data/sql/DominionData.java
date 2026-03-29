/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;

public class DominionData
{
	private static final Logger LOGGER = Logger.getLogger(DominionData.class.getName());
	private static final String SELECT_DOMINION_FLAGS = "SELECT id, wards FROM dominion";

	public Map<Integer, int[]> loadDominionFlags()
	{
		final Map<Integer, int[]> dominionFlags = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_DOMINION_FLAGS);
			ResultSet rs = statement.executeQuery())
		{
			while (rs.next())
			{
				final int territoryId = rs.getInt("id");
				final String wards = rs.getString("wards");
				dominionFlags.put(territoryId, parseWardList(wards, territoryId));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load dominion ward data.", e);
		}
		return dominionFlags;
	}

	private int[] parseWardList(String wards, int territoryId)
	{
		if ((wards == null) || wards.isBlank())
		{
			return new int[]
			{
				territoryId
			};
		}

		final List<Integer> values = new ArrayList<>();
		for (String token : wards.split(";"))
		{
			if (token.isBlank())
			{
				continue;
			}

			try
			{
				values.add(Integer.parseInt(token.trim()));
			}
			catch (NumberFormatException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Invalid dominion ward token '" + token + "' for territory " + territoryId + '.', e);
			}
		}

		if (values.isEmpty())
		{
			values.add(territoryId);
		}

		final int[] result = new int[values.size()];
		for (int i = 0; i < values.size(); i++)
		{
			result[i] = values.get(i);
		}
		return result;
	}

	public static DominionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final DominionData INSTANCE = new DominionData();
	}
}