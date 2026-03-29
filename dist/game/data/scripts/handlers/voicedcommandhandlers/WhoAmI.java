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
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.voicedcommandhandlers;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.model.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.stats.TraitType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

public class WhoAmI implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"whoami"
	};
	
	private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ENGLISH)));
	
	@Override
	public boolean onCommand(String command, Player activeChar, String params)
	{
		if (!"whoami".equals(command))
		{
			return false;
		}
		
		final CreatureStat stat = activeChar.getStat();
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(activeChar, "data/html/command/whoami.htm");
		html.replace("%hpRegen%", format(stat.getHpRegen()));
		html.replace("%hpDrain%%", percent(stat.getValue(Stat.ABSORB_DAMAGE_PERCENT, 0) * 100));
		html.replace("%mpRegen%", format(stat.getMpRegen()));
		html.replace("%mpGain%%", percent(stat.getValue(Stat.ABSORB_MANA_DAMAGE_PERCENT, 0) * 100));
		html.replace("%cpRegen%", format(stat.getCpRegen()));
		html.replace("%hpGain%%", percent(Math.max(0, (stat.getValue(Stat.HEAL_EFFECT, 1) - 1) * 100)));
		html.replace("%critPerc%%", percent(stat.getCriticalDmg(0)));
		html.replace("%critStatic%", format(stat.getValue(Stat.CRITICAL_DAMAGE_ADD, 0)));
		html.replace("%mCritRate%", format(activeChar.getMCriticalHit()));
		html.replace("%blowRate%", format(stat.getValue(Stat.BLOW_RATE, 0)));
		html.replace("%shieldDef%", format(stat.getValue(Stat.SHIELD_DEFENCE, 0)));
		html.replace("%shieldRate%", format(stat.getValue(Stat.SHIELD_DEFENCE_RATE, 0)));
		html.replace("%xpRate%", rate(RatesConfig.RATE_XP, activeChar.hasPremiumStatus() ? PremiumSystemConfig.PREMIUM_RATE_XP : 1));
		html.replace("%spRate%", rate(RatesConfig.RATE_SP, activeChar.hasPremiumStatus() ? PremiumSystemConfig.PREMIUM_RATE_SP : 1));
		html.replace("%dropRate%", rate(RatesConfig.RATE_DEATH_DROP_CHANCE_MULTIPLIER, activeChar.hasPremiumStatus() ? PremiumSystemConfig.PREMIUM_RATE_DROP_CHANCE : 1));
		html.replace("%spoilRate%", rate(RatesConfig.RATE_SPOIL_DROP_CHANCE_MULTIPLIER, activeChar.hasPremiumStatus() ? PremiumSystemConfig.PREMIUM_RATE_SPOIL_CHANCE : 1));
		html.replace("%adenaRate%", rate(RatesConfig.RATE_DEATH_DROP_AMOUNT_MULTIPLIER, activeChar.hasPremiumStatus() ? PremiumSystemConfig.PREMIUM_RATE_DROP_AMOUNT : 1));
		html.replace("%fireResist%", format(stat.getDefenseElementValue(AttributeType.FIRE)));
		html.replace("%waterResist%", format(stat.getDefenseElementValue(AttributeType.WATER)));
		html.replace("%windResist%", format(stat.getDefenseElementValue(AttributeType.WIND)));
		html.replace("%earthResist%", format(stat.getDefenseElementValue(AttributeType.EARTH)));
		html.replace("%holyResist%", format(stat.getDefenseElementValue(AttributeType.HOLY)));
		html.replace("%darkResist%", format(stat.getDefenseElementValue(AttributeType.DARK)));
		html.replace("%fireAbsorb%", percent(stat.getValue(Stat.FIRE_ABSORB, 0) + stat.getValue(Stat.FIRE_ABB, 0) * 100));
		html.replace("%waterAbsorb%", percent(stat.getValue(Stat.WATER_ABSORB, 0) + stat.getValue(Stat.WATER_ABB, 0) * 100));
		html.replace("%windAbsorb%", percent(stat.getValue(Stat.WIND_ABSORB, 0) + stat.getValue(Stat.WIND_ABB, 0) * 100));
		html.replace("%earthAbsorb%", percent(stat.getValue(Stat.EARTH_ABSORB, 0) + stat.getValue(Stat.EARTH_ABB, 0) * 100));
		html.replace("%holyAbsorb%", percent(stat.getValue(Stat.HOLY_ABSORB, 0) + stat.getValue(Stat.HOLY_ABB, 0) * 100));
		html.replace("%darkAbsorb%", percent(stat.getValue(Stat.DARK_ABSORB, 0) + stat.getValue(Stat.DARK_ABB, 0) * 100));
		html.replace("%swordResist%", format(stat.getDefenceTrait(TraitType.SWORD)));
		html.replace("%dualResist%", format(stat.getDefenceTrait(TraitType.DUAL)));
		html.replace("%bluntResist%", format(stat.getDefenceTrait(TraitType.BLUNT)));
		html.replace("%poleResist%", format(stat.getDefenceTrait(TraitType.POLE)));
		html.replace("%fistResist%", format(stat.getDefenceTrait(TraitType.FIST)));
		html.replace("%daggerResist%", format(stat.getDefenceTrait(TraitType.DAGGER)));
		html.replace("%bowResist%", format(stat.getDefenceTrait(TraitType.BOW)));
		html.replace("%crossbowResist%", format(stat.getDefenceTrait(TraitType.CROSSBOW)));
		html.replace("%bleedPower%", format(stat.getAttackTrait(TraitType.BLEED)));
		html.replace("%bleedResist%", format(stat.getDefenceTrait(TraitType.BLEED)));
		html.replace("%stunPower%", format(stat.getAttackTrait(TraitType.SHOCK)));
		html.replace("%stunResist%", format(stat.getDefenceTrait(TraitType.SHOCK)));
		html.replace("%debuffPower%", format(stat.getAttackTrait(TraitType.DERANGEMENT)));
		html.replace("%debuffResist%", percent(stat.getValue(Stat.RESIST_ABNORMAL_DEBUFF, 0) * 100));
		html.replace("%poisonPower%", format(stat.getAttackTrait(TraitType.POISON)));
		html.replace("%poisonResist%", format(stat.getDefenceTrait(TraitType.POISON)));
		html.replace("%paralyzePower%", format(stat.getAttackTrait(TraitType.PARALYZE)));
		html.replace("%paralyzeResist%", format(stat.getDefenceTrait(TraitType.PARALYZE)));
		html.replace("%rootPower%", format(Math.max(stat.getAttackTrait(TraitType.ROOT_PHYSICALLY), stat.getAttackTrait(TraitType.ROOT_MAGICALLY))));
		html.replace("%rootResist%", format(Math.max(stat.getDefenceTrait(TraitType.ROOT_PHYSICALLY), stat.getDefenceTrait(TraitType.ROOT_MAGICALLY))));
		html.replace("%mentalPower%", format(stat.getAttackTrait(TraitType.PSYCHIC)));
		html.replace("%mentalResist%", format(stat.getDefenceTrait(TraitType.PSYCHIC)));
		html.replace("%sleepPower%", format(stat.getAttackTrait(TraitType.SLEEP)));
		html.replace("%sleepResist%", format(stat.getDefenceTrait(TraitType.SLEEP)));
		html.replace("%cancelPower%", format(stat.getValue(Stat.ATTACK_CANCEL, 0)));
		html.replace("%cancelResist%", percent(stat.getValue(Stat.RESIST_DISPEL_BUFF, 0) * 100));
		html.replace("%critChanceResist%%", percent(stat.getValue(Stat.DEFENCE_CRITICAL_RATE, 0)));
		html.replace("%critDamResist%%", percent(stat.getValue(Stat.DEFENCE_CRITICAL_DAMAGE, 0)));
		activeChar.sendPacket(html);
		return true;
	}
	
	private static String rate(double baseRate, double modifier)
	{
		return "x" + format(baseRate * modifier);
	}
	
	private static String percent(double value)
	{
		return format(value) + "%";
	}
	
	private static String format(double value)
	{
		return DECIMAL_FORMAT.get().format(value);
	}
	
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}