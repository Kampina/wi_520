package org.l2jmobius.gameserver.managers.custom;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.AltSettingsConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.network.serverpackets.ExVitalityPointInfo;

public class PvpVitality {
    private final Player _player;
    private int _level = 0;
    private int _points = 0;
    private ScheduledFuture<?> _decreaseTask = null;

    public PvpVitality(Player player) {
        _player = player;
    }

    public synchronized void resetOrCancel() {
        if (_decreaseTask != null) {
            _decreaseTask.cancel(false);
            _decreaseTask = null;
        }
        if (_level > 0 || _points > 0) {
            _level = 0;
            _points = 0;
            updateVisuals();
            _player.getStat().recalculateStats(true);
        }
    }

    public synchronized void onDeath() {
        if (_level > 0 || _points > 0) {
            _level = Math.max(0, _level - 1);
            _points = 0;
            updateVisuals();
            _player.getStat().recalculateStats(true);
        }
    }

    public synchronized void onPkStateChange(boolean isPk) {
        if (isPk) {
            resetOrCancel();
        }
    }

    public synchronized void onPartyLeave() {
        resetOrCancel();
    }

    public synchronized void onPvpKill() {
        if (!AltSettingsConfig.ALT_VIT_SYSTEM_ENABLED) return;
        if (_player.getReputation() < 0) return;
        if (_level >= AltSettingsConfig.ALT_VIT_MAX_ENRAGE_LVL) return;

        _points++;
        if (_points >= AltSettingsConfig.ALT_VIT_KILLS_TO_STAGE_UP) {
            _points = 0;
            _level++;
            if (_level > AltSettingsConfig.ALT_VIT_MAX_ENRAGE_LVL) {
                _level = AltSettingsConfig.ALT_VIT_MAX_ENRAGE_LVL;
            }
            if (AltSettingsConfig.ALT_VIT_SHOW_MESSAGES) {
                _player.sendMessage("Уровень активности повышен до " + _level + "!");
            }
            _player.getStat().recalculateStats(true);
        }
        updateVisuals();
        startDecreaseTask();
    }

    private synchronized void processDecrease() {
        if (_points > 0) {
            _points--;
        } else if (_level > 0) {
            _level--;
            if (_level > 0) {
                _points = AltSettingsConfig.ALT_VIT_KILLS_TO_STAGE_UP - 1;
            } else {
                _points = 0;
            }
            if (AltSettingsConfig.ALT_VIT_SHOW_MESSAGES) {
                _player.sendMessage("Уровень активности снижен до " + _level + ".");
            }
            _player.getStat().recalculateStats(true);
        }
        updateVisuals();
        if (_level == 0 && _points == 0) {
            if (_decreaseTask != null) {
                _decreaseTask.cancel(false);
                _decreaseTask = null;
            }
        }
    }

    private void startDecreaseTask() {
        if (_decreaseTask != null) {
            _decreaseTask.cancel(false);
        }
        long delayMillis = (_level == AltSettingsConfig.ALT_VIT_MAX_ENRAGE_LVL) 
            ? AltSettingsConfig.ALT_MAX_LVL_DECREASE_DELAY 
            : AltSettingsConfig.ALT_VIT_DECREASE_DELAY_MILLIS;

        long intervalMillis = AltSettingsConfig.ALT_VIT_DECREASE_DELAY_MILLIS;
        if(intervalMillis < 1000) intervalMillis = 60000;

        _decreaseTask = ThreadPool.scheduleAtFixedRate(this::processDecrease, delayMillis, intervalMillis);
    }

    private void updateVisuals() {
        if (!AltSettingsConfig.ALT_VIT_SYSTEM_ENABLED) return;
        int maxPoints = AltSettingsConfig.ALT_VIT_MAX_ENRAGE_LVL * AltSettingsConfig.ALT_VIT_KILLS_TO_STAGE_UP;
        int currentTotal = (_level * AltSettingsConfig.ALT_VIT_KILLS_TO_STAGE_UP) + _points;
        int virtualPoints = 0;
        if (maxPoints > 0) {
            virtualPoints = (int) (((double)currentTotal / maxPoints) * 35000);
        }
        virtualPoints = Math.min(35000, virtualPoints);
        _player.sendPacket(new ExVitalityPointInfo(virtualPoints));
        
        if (_level >= AltSettingsConfig.ALT_VIT_MAX_ENRAGE_LVL) {
            if (!_player.getEffectList().hasAbnormalVisualEffect(AbnormalVisualEffect.NAVIT_ADVENT)) {
                _player.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.NAVIT_ADVENT);
                _player.broadcastCharInfo();
            }
        } else {
            if (_player.getEffectList().hasAbnormalVisualEffect(AbnormalVisualEffect.NAVIT_ADVENT)) {
                _player.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.NAVIT_ADVENT);
                _player.broadcastCharInfo();
            }
        }
    }

    public double getBonusMultiplier() {
        if (!AltSettingsConfig.ALT_VIT_SYSTEM_ENABLED || _level == 0) return 0.0;
        return (_level * AltSettingsConfig.ALT_VIT_DMG_PERCENT_PER_ENRAGE_LVL) / 100.0;
    }
}
