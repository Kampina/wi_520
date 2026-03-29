package events.FarmFortressSiege;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.holders.sieges.OnFortSiegeFinish;
import org.l2jmobius.gameserver.model.events.holders.sieges.OnFortSiegeStart;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.siege.FortSiege;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Farm Fortress Siege Event
 * Replaces the old WI XML timings to emit sounds and rewards.
 */
public class FarmFortressSiege extends Script
{
	private final Map<Integer, ScheduledFuture<?>> _tasks = new ConcurrentHashMap<>();

	private FarmFortressSiege()
	{
	}

	@RegisterEvent(EventType.ON_FORT_SIEGE_START)
	public void onFortSiegeStart(OnFortSiegeStart event)
	{
		final FortSiege siege = event.getSiege();
		if (siege == null || siege.getFort() == null)
		{
			return;
		}

		if (!siege.getFort().isFarm())
		{
			return; // Only apply to farm fortresses
		}

		final int fortId = siege.getFort().getResidenceId();
		
		if (_tasks.containsKey(fortId))
		{
			_tasks.get(fortId).cancel(true);
		}

		final ScheduledFuture<?> task = ThreadPool.scheduleAtFixedRate(new Runnable()
		{
			int elapsedSec = 0;

			@Override
			public void run()
			{
				try
				{
					if (!siege.isInProgress())
					{
						cancelTask(fortId);
						return;
					}

					// BGM loop happens every 978 seconds
					final int bgmTime = elapsedSec % 978;
					String sound = null;

					switch (bgmTime)
					{
						case 0:
							sound = "NS20_F";
							break;
						case 98:
							sound = "NB02_F";
							break;
						case 200:
							sound = "NS19_S01";
							break;
						case 310:
							sound = "NS12_F";
							break;
						case 403:
							sound = "NS19_F";
							break;
						case 512:
							sound = "NS18_S01";
							break;
						case 596:
							sound = "NS02_F";
							break;
						case 691:
							sound = "NS12_S01";
							break;
						case 785:
							sound = "NS18_F";
							break;
						case 883:
							sound = "NS02_S01";
							break;
					}

					if (sound != null)
					{
						final PlaySound ps = new PlaySound(3, sound, 0, 0, 0, 0, 0);
						for (Player attacker : siege.getAttackersInZone())
						{
							if (attacker != null)
							{
								attacker.sendPacket(ps);
							}
						}
						// Can also broadcast to defenders if desired, but farm forts usually only have attackers since they are guarded by NPC
					}

					// Every 300 seconds give item/reputation (id -300 means Clan Reputation in old L2CCC event)
					if (elapsedSec > 0 && (elapsedSec % 300 == 0))
					{
						for (Player attacker : siege.getAttackersInZone())
						{
							if (attacker != null && attacker.getClan() != null)
							{
								attacker.getClan().addReputationScore(31);
							}
						}
					}

					elapsedSec++;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}, 0, 1000);

		_tasks.put(fortId, task);
	}

	@RegisterEvent(EventType.ON_FORT_SIEGE_FINISH)
	public void onFortSiegeFinish(OnFortSiegeFinish event)
	{
		final FortSiege siege = event.getSiege();
		if (siege != null && siege.getFort() != null)
		{
			cancelTask(siege.getFort().getResidenceId());
		}
	}

	private void cancelTask(int fortId)
	{
		final ScheduledFuture<?> task = _tasks.remove(fortId);
		if (task != null)
		{
			task.cancel(false);
		}
	}

	public static void main(String[] args)
	{
		new FarmFortressSiege();
	}
}
